package ke.tahadhari.ui.mobile

import ke.tahadhari.data.County
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskAssessment
import ke.tahadhari.domain.RiskReason
import ke.tahadhari.domain.TemplateExplainer
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import kotlin.math.roundToInt

/** About 2-3 SMS. The message drops actions (never the safety lines) to stay under this. */
const val SHARE_ALERT_MAX_CHARS = 400
private const val MAX_ACTIONS = 3

/**
 * Short plain-text alert for forwarding by WhatsApp or SMS: county and level, the main reason,
 * the top actions, emergency numbers and the "not an official warning" line.
 *
 * Pure. The level comes from [LoadState.Ready.assessment] (`RiskEngine`). Actions come from the
 * deterministic [TemplateExplainer], never from AI text. Callers offer sharing only for Ready state.
 * When the data is a saved forecast, its date is included.
 */
fun shareAlertMessage(county: County, ready: LoadState.Ready, language: Language): String {
    val strings = Strings.of(language)
    val a = ready.assessment
    val actions = TemplateExplainer().build(county, a, language).actions
        .filterNot { "999" in it } // the emergency line below already says it
        .take(MAX_ACTIONS)

    fun render(n: Int): String = buildList {
        add("${strings.riskHeadline(a.level)}: ${strings.countyLabel(county.name)}")
        add(mainReasonText(a, language, strings))
        if (ready.fromCache) add(strings.shareMsgSaved(Dates.isoDateFromEpochMillis(ready.updatedAtMillis)))
        actions.take(n).forEachIndexed { i, text -> add("${i + 1}. $text") }
        add(strings.shareMsgEmergency)
        add(strings.shareMsgDisclaimer)
        add(strings.shareMsgVia)
    }.joinToString("\n")

    var n = actions.size
    while (n > 0 && render(n).length > SHARE_ALERT_MAX_CHARS) n--
    return render(n)
}

/**
 * The single most important reason, e.g. "Up to 99 mm of rain on Thursday" or "Tana River rising to 3×".
 * [RiskAssessment.reasons] is sorted by level, highest first. Falls back to "no heavy rain" only when
 * there are no reasons at all.
 */
fun mainReasonText(a: RiskAssessment, language: Language, strings: Strings = Strings.of(language)): String =
    when (val r = a.reasons.firstOrNull { it !is RiskReason.SaturatedGround } ?: a.reasons.firstOrNull()) {
        null -> strings.reasonNone
        is RiskReason.HeavyDay -> strings.shareMsgRainDay(r.mm.roundToInt(), Dates.dayName(r.date, language))
        is RiskReason.WetSpell -> strings.shareMsgRainSpell(r.totalMm.roundToInt(), Dates.dayName(r.startDate, language))
        is RiskReason.RiverRising -> strings.shareMsgRiver(r.riverName ?: strings.nearbyRiver, ratioText(r.ratio))
        else -> strings.riskHeadline(r.level) // a reason type added later: still say something true
    }

/** "3" for 3.0, "3.1" for 3.14. */
private fun ratioText(ratio: Double): String {
    val tenths = (ratio * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}
