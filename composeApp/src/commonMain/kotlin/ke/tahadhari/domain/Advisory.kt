package ke.tahadhari.domain

/**
 * An official advisory (a KMD weekly county forecast, a county disaster notice), copied word for
 * word from [sourceUrl]. The AI gets it as data inside `<advisory>` tags; the app shows it with a
 * link to the source. It never changes the risk level, and the app never writes advisory text.
 *
 * Dates are ISO `yyyy-MM-dd` in Nairobi time, both inclusive. [countyCodes] empty means the whole country.
 */
data class Advisory(
    val issuer: String,
    val title: String,
    val validFrom: String,
    val validTo: String,
    val sourceUrl: String,
    val text: String,
    val countyCodes: Set<Int> = emptySet(),
)

object Advisories {
    /** Same limit as `ExplainContract.MAX_ADVISORY_CHARS` (domain cannot depend on `shared`; a test keeps them equal). */
    const val MAX_TEXT_CHARS = 2_000

    private val isoDate = Regex("""\d{4}-\d{2}-\d{2}""")

    /**
     * The advisory to use for [countyCode] on [todayIso], or null. Expired and not-yet-valid
     * advisories are never used. A county advisory wins over a national one, then the newest.
     */
    fun current(all: List<Advisory>, countyCode: Int, todayIso: String): Advisory? =
        all.filter { (it.countyCodes.isEmpty() || countyCode in it.countyCodes) && todayIso >= it.validFrom && todayIso <= it.validTo }
            .sortedWith(compareByDescending<Advisory> { it.countyCodes.isNotEmpty() }.thenByDescending { it.validFrom })
            .firstOrNull()

    /**
     * Problems with one advisory entry, empty when it is fine. Only `https` links on government
     * hosts (`.go.ke`) are accepted, so every advisory points to an official source.
     */
    fun problems(a: Advisory, knownCountyCodes: Set<Int>): List<String> = buildList {
        if (a.issuer.isBlank()) add("issuer is blank")
        if (a.title.isBlank()) add("title is blank")
        if (a.text.isBlank()) add("text is blank")
        if (a.text.length > MAX_TEXT_CHARS) add("text is longer than $MAX_TEXT_CHARS characters")
        if (!isoDate.matches(a.validFrom) || !isoDate.matches(a.validTo)) add("dates must be yyyy-MM-dd")
        else if (a.validFrom > a.validTo) add("validFrom is after validTo")
        if (!OfficialLinks.isOpenable(a.sourceUrl)) add("sourceUrl must be https")
        else {
            val host = a.sourceUrl.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#').lowercase()
            if (!host.endsWith(".go.ke")) add("sourceUrl must be on a .go.ke host")
        }
        (a.countyCodes - knownCountyCodes).takeIf { it.isNotEmpty() }?.let { add("unknown county codes $it") }
    }

    /**
     * The text sent to the AI: who issued it and when it is valid, then the advisory itself,
     * cut to [MAX_TEXT_CHARS] so the request always passes validation.
     */
    fun promptText(a: Advisory): String =
        "${a.issuer}: ${a.title} (valid ${a.validFrom} to ${a.validTo})\n${a.text.trim()}".take(MAX_TEXT_CHARS)
}
