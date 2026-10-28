package ke.tahadhari.ui.dashboard

import ke.tahadhari.data.County
import ke.tahadhari.domain.Dates
import ke.tahadhari.ui.LoadState
import kotlin.math.roundToLong

/**
 * Builds the dashboard CSV export (RFC 4180: comma-separated, CRLF line endings, fields with
 * commas, quotes or line breaks quoted and quotes doubled). Pure, so it is unit-tested.
 *
 * The file is a data exchange format, so column names and values are fixed English/machine
 * values (levels as `low`, `moderate`, `high`, `severe`) whatever the UI language.
 * The first line is a `#` note: guidance only, not an official warning, with data attribution.
 */
object CsvExport {
    val columns = listOf(
        "county_code", "county", "hq_town", "risk_level", "wettest_day_date", "wettest_day_mm",
        "max_3day_total_mm", "river", "river_rise_ratio", "updated_at", "from_cache", "status",
    )

    const val NOTE = "Guidance only, not an official warning. Follow Kenya Meteorological Service and county " +
        "government advisories. Risk levels use Tahadhari heuristic thresholds, not official KMSA criteria. " +
        "Forecast data: Open-Meteo.com (CC BY 4.0), river discharge from GloFAS."

    /** Rows in the order of [counties] (pass the ranked list). Counties with no state are "loading". */
    fun build(counties: List<County>, states: Map<Int, LoadState>, generatedAtMillis: Long): String {
        val lines = mutableListOf<String>()
        lines += row(listOf("# Tahadhari flood risk export, generated ${isoDateTime(generatedAtMillis)}. $NOTE"))
        lines += row(columns)
        counties.forEach { c ->
            val base = listOf(c.code.toString(), c.name, c.town)
            lines += row(
                when (val s = states[c.code]) {
                    is LoadState.Ready -> {
                        val a = s.assessment
                        base + listOf(
                            a.level.name.lowercase(),
                            a.wettestDay?.date.orEmpty(),
                            a.wettestDay?.precipitationMm?.let { decimal(it, 1) }.orEmpty(),
                            decimal(a.maxThreeDayTotalMm, 1),
                            a.river?.let { it.riverName ?: c.riverName }.orEmpty(),
                            a.river?.let { decimal(it.riseRatio, 2) }.orEmpty(),
                            isoDateTime(s.updatedAtMillis),
                            s.fromCache.toString(),
                            "ok",
                        )
                    }
                    // Never a level for a county without data.
                    is LoadState.Failed -> base + listOf("", "", "", "", c.riverName.orEmpty(), "",
                        s.staleSinceMillis?.let(::isoDateTime).orEmpty(), "", "failed")
                    else -> base + listOf("", "", "", "", c.riverName.orEmpty(), "", "", "", "loading")
                },
            )
        }
        return lines.joinToString("\r\n", postfix = "\r\n")
    }

    /** Default file name, using the Nairobi date: `tahadhari-risk-2026-10-27.csv`. */
    fun fileName(nowMillis: Long): String = "tahadhari-risk-${Dates.isoDateFromEpochMillis(nowMillis)}.csv"

    fun escape(field: String): String {
        // Guard against spreadsheet formula injection from any text field.
        val safe = if (field.isNotEmpty() && field[0] in "=+@") "'$field" else field
        val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            safe != safe.trim()
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private fun row(fields: List<String>) = fields.joinToString(",") { escape(it) }

    /** Fixed-point decimal without platform formatting, e.g. decimal(2.345, 2) = "2.35". */
    internal fun decimal(value: Double, places: Int): String {
        var factor = 1L
        repeat(places) { factor *= 10 }
        val scaled = (value * factor).roundToLong()
        val sign = if (scaled < 0) "-" else ""
        val abs = if (scaled < 0) -scaled else scaled
        val whole = abs / factor
        if (places == 0) return "$sign$whole"
        val frac = (abs % factor).toString().padStart(places, '0')
        return "$sign$whole.$frac"
    }

    /** Epoch millis -> ISO 8601 local Nairobi time, e.g. `2026-10-27T14:05+03:00`. */
    internal fun isoDateTime(epochMillis: Long): String {
        val offset = Dates.NAIROBI_UTC_OFFSET_MINUTES
        val date = Dates.isoDateFromEpochMillis(epochMillis, offset)
        val minuteOfDay = ((epochMillis + offset * 60_000L).floorDiv(60_000L)).mod(1_440L)
        val hh = (minuteOfDay / 60).toString().padStart(2, '0')
        val mm = (minuteOfDay % 60).toString().padStart(2, '0')
        val oh = (offset / 60).toString().padStart(2, '0')
        val om = (offset % 60).toString().padStart(2, '0')
        return "${date}T$hh:$mm+$oh:$om"
    }
}
