package ke.tahadhari.ui.dashboard

import ke.tahadhari.data.County
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.TemplateExplainer
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import kotlin.math.roundToInt

/**
 * Builds the printable county briefing: one self-contained HTML page (inline CSS, no scripts, no
 * external files) that county teams save, open in a browser and print for a morning briefing.
 *
 * Pure and deterministic: the explanations come from [TemplateExplainer], never from the AI, so the
 * briefing works offline. Every piece of text is run through [escapeHtml]. A county without data
 * is shown as "No data" and never as Low.
 */
object BriefingExport {
    private val explainer = TemplateExplainer()

    /** Default file name, using the Nairobi date: `tahadhari-briefing-2026-10-28.html`. */
    fun fileName(nowMillis: Long): String = "tahadhari-briefing-${Dates.isoDateFromEpochMillis(nowMillis)}.html"

    /** [counties] in any order: rows are sorted here with [rankCounties] (highest risk first, no-data after). */
    fun build(counties: List<County>, states: Map<Int, LoadState>, language: Language, generatedAtMillis: Long): String {
        val s = Strings.of(language)
        val ranked = rankCounties(counties, states)
        val lang = if (language == Language.SWAHILI) "sw" else "en"
        val date = Dates.isoDateFromEpochMillis(generatedAtMillis)
        val time = nairobiTime(generatedAtMillis)
        val counts = statusCounts(counties, states)
        val ranks = ranked.filter { states[it.code] is LoadState.Ready }.mapIndexed { i, c -> c.code to i + 1 }.toMap()

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"$lang\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>${escapeHtml(s.briefingTitle)} · $date</title>")
            appendLine("<style>$CSS</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<header>")
            appendLine("<p class=\"app\">${escapeHtml(s.appName)}</p>")
            appendLine("<h1>${escapeHtml(s.briefingTitle)}</h1>")
            appendLine("<p class=\"meta\">${escapeHtml(s.briefingGenerated(date, time))}</p>")
            appendLine("<p class=\"note\">${escapeHtml(s.disclaimer)}</p>")
            appendLine("<p class=\"screen-only hint\">${escapeHtml(s.briefingPrintHint)}</p>")
            appendLine("</header>")

            appendLine("<section>")
            appendLine("<h2>${escapeHtml(s.briefingSummaryTitle)}</h2>")
            appendLine("<ul class=\"counts\">")
            RiskLevel.entries.reversed().forEach { level ->
                val n = counts[StatusFilter.Level(level)] ?: 0
                appendLine("<li class=\"${levelClass(level)}\"><span class=\"n\">$n</span> ${escapeHtml(s.risk(level))}</li>")
            }
            appendLine("<li class=\"nodata\"><span class=\"n\">${counts[StatusFilter.Failed] ?: 0}</span> ${escapeHtml(s.noDataShort)}</li>")
            val loading = counts[StatusFilter.Loading] ?: 0
            if (loading > 0) appendLine("<li class=\"nodata\"><span class=\"n\">$loading</span> ${escapeHtml(s.loadingShort)}</li>")
            appendLine("</ul>")
            appendLine("</section>")

            appendLine("<section>")
            appendLine("<h2>${escapeHtml(s.briefingTableTitle)}</h2>")
            appendLine("<table>")
            appendLine("<thead><tr>" + listOf(s.colRank, s.colCounty, s.colLevel, s.colWettestDay, s.colThreeDay, s.colRiver, s.colUpdated, s.colStatus)
                .joinToString("") { "<th scope=\"col\">${escapeHtml(it)}</th>" } + "</tr></thead>")
            appendLine("<tbody>")
            ranked.forEach { c -> appendLine(row(c, states[c.code], ranks[c.code], s, language)) }
            appendLine("</tbody>")
            appendLine("</table>")
            appendLine("</section>")

            appendLine("<section>")
            appendLine("<h2>${escapeHtml(s.briefingHighlightsTitle)}</h2>")
            val highlights = ranked.filter { (states[it.code] as? LoadState.Ready)?.assessment?.level.let { l -> l != null && l >= RiskLevel.HIGH } }
            if (highlights.isEmpty()) {
                appendLine("<p>${escapeHtml(s.briefingNoHighlights)}</p>")
            }
            highlights.forEach { c ->
                val ready = states[c.code] as LoadState.Ready
                val explanation = explainer.build(c, ready.assessment, language)
                appendLine("<article class=\"county ${levelClass(ready.assessment.level)}\">")
                appendLine("<h3>${escapeHtml(c.name)} <span class=\"badge ${levelClass(ready.assessment.level)}\">${escapeHtml(s.risk(ready.assessment.level))}</span></h3>")
                if (ready.fromCache) appendLine("<p class=\"cached\">${escapeHtml(s.savedDataShort)} · ${escapeHtml(updated(ready.updatedAtMillis, language))}</p>")
                appendLine("<p class=\"headline\">${escapeHtml(explanation.headline)}</p>")
                appendLine("<p>${escapeHtml(explanation.body)}</p>")
                appendLine("<h4>${escapeHtml(s.whatToDo)}</h4>")
                appendLine("<ul>")
                explanation.actions.forEach { appendLine("<li>${escapeHtml(it)}</li>") }
                appendLine("</ul>")
                appendLine("</article>")
            }
            appendLine("</section>")

            appendLine("<footer>")
            appendLine("<p>${escapeHtml(s.disclaimer)}</p>")
            appendLine("<p>${escapeHtml(s.mapAttribution)}</p>")
            appendLine("</footer>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun row(c: County, state: LoadState?, rank: Int?, s: Strings, language: Language): String {
        val cells = when (state) {
            is LoadState.Ready -> {
                val a = state.assessment
                val wettest = a.wettestDay?.let { "${Dates.dayName(it.date, language)} · ${it.precipitationMm.roundToInt()} mm" } ?: "–"
                val river = a.river?.let { "×${CsvExport.decimal(it.riseRatio, 1)} ${it.riverName ?: c.riverName ?: ""}".trim() } ?: "–"
                listOf(
                    rank.toString(), c.name,
                    "<span class=\"badge ${levelClass(a.level)}\">${escapeHtml(s.risk(a.level))}</span>",
                    escapeHtml(wettest), "${a.maxThreeDayTotalMm.roundToInt()} mm", escapeHtml(river),
                    escapeHtml(updated(state.updatedAtMillis, language)),
                    escapeHtml(if (state.fromCache) s.savedDataShort else s.statusLive),
                ).mapIndexed { i, v -> if (i == 1) escapeHtml(v) else v }
            }
            is LoadState.Failed -> noData(c, s.noDataShort, state.staleSinceMillis?.let { updated(it, language) } ?: "–", s)
            else -> noData(c, s.loadingShort, "–", s)
        }
        val cls = (state as? LoadState.Ready)?.let { levelClass(it.assessment.level) } ?: "nodata"
        return "<tr class=\"$cls\">" + cells.joinToString("") { "<td>$it</td>" } + "</tr>"
    }

    /** A row without data: the level cell says so in words; it is never "Low". */
    private fun noData(c: County, label: String, updated: String, s: Strings): List<String> = listOf(
        "–", escapeHtml(c.name), "<span class=\"badge nodata\">${escapeHtml(label)}</span>",
        "–", "–", escapeHtml(c.riverName.orEmpty()).ifEmpty { "–" }, escapeHtml(updated), escapeHtml(s.noDataShort),
    )

    private fun levelClass(level: RiskLevel) = "lvl-${level.name.lowercase()}"

    private fun updated(millis: Long, language: Language): String =
        "${Dates.shortDate(Dates.isoDateFromEpochMillis(millis), language)}, ${nairobiTime(millis)}"

    /** `HH:mm` in Nairobi time. */
    internal fun nairobiTime(millis: Long): String {
        val minuteOfDay = ((millis + Dates.NAIROBI_UTC_OFFSET_MINUTES * 60_000L).floorDiv(60_000L)).mod(1_440L)
        return (minuteOfDay / 60).toString().padStart(2, '0') + ":" + (minuteOfDay % 60).toString().padStart(2, '0')
    }

    /** Escapes text for HTML element content and quoted attribute values. */
    fun escapeHtml(text: String): String {
        val sb = StringBuilder(text.length + 8)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    // Risk is shown by label AND colour; colours are soft tints with dark text (WCAG AA) and a strong left border.
    private const val CSS = """
:root{color-scheme:light}
*{box-sizing:border-box}
body{font-family:system-ui,-apple-system,"Segoe UI",Roboto,Arial,sans-serif;color:#171d1e;background:#fff;margin:0 auto;padding:24px;max-width:1000px;line-height:1.45;font-size:14px;-webkit-print-color-adjust:exact;print-color-adjust:exact}
.app{margin:0;color:#0e6670;font-weight:700;letter-spacing:.04em;text-transform:uppercase;font-size:12px}
h1{margin:.1em 0 .2em;font-size:26px}
h2{font-size:18px;margin:1.6em 0 .6em;break-after:avoid;border-bottom:2px solid #0e6670;padding-bottom:4px}
h3{margin:0 0 .3em;font-size:16px}
h4{margin:.8em 0 .2em;font-size:13px}
.meta{margin:0 0 .8em;color:#3f4949}
.note{border:1px solid #6f7979;border-radius:6px;padding:8px 12px;background:#f1f5f5;margin:0}
.hint{color:#3f4949;font-size:12px}
.counts{list-style:none;display:flex;flex-wrap:wrap;gap:8px;padding:0;margin:0}
.counts li{border:1px solid #6f7979;border-left-width:8px;border-radius:6px;padding:6px 12px;font-weight:600}
.counts .n{font-size:20px;font-weight:700;margin-right:4px}
table{width:100%;border-collapse:collapse;font-size:12.5px}
th,td{text-align:left;padding:5px 7px;border-bottom:1px solid #bec8c9;vertical-align:top}
thead{display:table-header-group}
th{background:#e9efef;border-bottom:2px solid #6f7979}
tr{break-inside:avoid;page-break-inside:avoid}
.badge{display:inline-block;border:1px solid;border-radius:999px;padding:1px 9px;font-weight:700;white-space:nowrap}
.lvl-low{background:#ddf2df;color:#0f3d14;border-color:#2e7d32}
.lvl-moderate{background:#fff1c2;color:#4a3800;border-color:#b78a00}
.lvl-high{background:#ffe2d2;color:#5c1f00;border-color:#c2410c}
.lvl-severe{background:#ffdad6;color:#5f0f0a;border-color:#b3261e}
.nodata{background:#fff;color:#3f4949;border:1px dashed #6f7979}
tr.lvl-low,tr.lvl-moderate,tr.lvl-high,tr.lvl-severe,tr.nodata{background:none;color:inherit;border:0}
.badge.lvl-low,.badge.lvl-moderate,.badge.lvl-high,.badge.lvl-severe{border-style:solid}
.county{border:1px solid #6f7979;border-left-width:8px;border-radius:6px;padding:10px 14px;margin:0 0 12px;break-inside:avoid;page-break-inside:avoid}
.county.lvl-high{border-left-color:#c2410c;background:#fffaf7;color:#171d1e}
.county.lvl-severe{border-left-color:#b3261e;background:#fffafa;color:#171d1e}
.counts li.lvl-low{border-left-color:#2e7d32}.counts li.lvl-moderate{border-left-color:#b78a00}
.counts li.lvl-high{border-left-color:#c2410c}.counts li.lvl-severe{border-left-color:#b3261e}
.counts li{background:#fff;color:#171d1e}
.headline{font-weight:700;margin:.2em 0}
.cached{color:#93000a;font-weight:600;margin:.2em 0}
.county p{margin:.3em 0}.county ul{margin:.2em 0;padding-left:20px}
footer{margin-top:2em;border-top:1px solid #bec8c9;padding-top:8px;color:#3f4949;font-size:12px}
@media print{body{padding:0;max-width:none;font-size:11pt}@page{margin:14mm}.screen-only{display:none}h2{margin-top:1.2em}}
"""
}
