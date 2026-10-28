package ke.tahadhari.ui.mobile

import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskAssessment
import ke.tahadhari.domain.TemplateExplainer
import ke.tahadhari.ui.Strings

/**
 * Text for "Read aloud". [text] is in the app language; [englishFallback] is the same alert in
 * English, used when the device has no Swahili voice.
 */
data class SpokenAlert(val text: String, val englishFallback: String)

/** About 60 seconds at a normal speaking rate (roughly 150 words a minute), with some margin. */
const val SPOKEN_ALERT_MAX_WORDS = 130

/**
 * Builds what the phone says: risk level and place, the short explanation, what to do, and the
 * "not an official warning" line. Pure.
 *
 * The text comes from the deterministic [TemplateExplainer] (never AI text) so it is predictable,
 * and the level comes from [assessment] (`RiskEngine`). Actions are dropped from the end, never the
 * headline, explanation or disclaimer, to stay within [SPOKEN_ALERT_MAX_WORDS] where possible.
 * [fromCache] adds a line saying this is a saved forecast.
 */
fun spokenAlert(county: County, assessment: RiskAssessment, language: Language, fromCache: Boolean = false): SpokenAlert =
    SpokenAlert(
        text = spokenText(county, assessment, language, fromCache),
        englishFallback = spokenText(county, assessment, Language.ENGLISH, fromCache),
    )

private fun spokenText(county: County, a: RiskAssessment, language: Language, fromCache: Boolean): String {
    val strings = Strings.of(language)
    val e = TemplateExplainer().build(county, a, language)
    val head = buildList {
        add(e.headline)
        add(e.body)
        if (fromCache) add(strings.speakSaved)
    }
    val tail = listOf(strings.speakDisclaimer)
    var n = e.actions.size
    fun render(count: Int): String {
        val actions = if (count > 0) listOf(strings.whatToDo) + e.actions.take(count) else emptyList()
        return cleanForSpeech(head + actions + tail, language)
    }
    while (n > 0 && wordCount(render(n)) > SPOKEN_ALERT_MAX_WORDS) n--
    return render(n)
}

private fun wordCount(text: String): Int = text.split(' ').count { it.isNotBlank() }

/**
 * Makes sentences safe for a speech engine: units and symbols become words, emergency numbers are
 * spelled digit by digit, and anything else that is not a letter, digit or basic punctuation
 * (emoji, arrows, brackets) is removed. Each part ends with a full stop so the voice pauses.
 */
internal fun cleanForSpeech(parts: List<String>, language: Language): String {
    val sw = language == Language.SWAHILI
    return parts.joinToString(" ") { part ->
        val t = speakable(part, sw).trim().trimEnd('.', ',', ';', ':', '!', '?', ' ')
        if (t.isEmpty()) "" else "$t."
    }.replace(Regex("\\s+"), " ").trim()
}

private val mmRegex = Regex("(\\d)\\s*mm\\b")
private val emergencyNumbers = Regex("\\b(999|112|1199)\\b")

private fun speakable(raw: String, sw: Boolean): String {
    var t = raw
        .replace(mmRegex) { "${it.groupValues[1]} ${if (sw) "milimita" else "millimetres"}" }
        .replace("×", if (sw) " mara " else " times ")
        .replace("%", if (sw) " asilimia" else " percent")
        .replace("&", if (sw) " na " else " and ")
        .replace("°C", if (sw) " nyuzi joto" else " degrees")
        .replace("°", " ")
        .replace("→", ", ").replace("←", ", ").replace("—", ", ").replace("–", ", ")
        .replace("…", ".").replace("’", "'")
        .replace("/", " ")
    t = emergencyNumbers.replace(t) { it.value.toCharArray().joinToString(" ") }
    t = buildString {
        for (c in t) {
            if (c.isLetterOrDigit() || c == ' ' || c in ".,;:!?'-") append(c) else append(' ')
        }
    }
    return t.replace(Regex("\\s+"), " ").replace(Regex("\\s+([.,;:!?])"), "$1")
}
