package ke.tahadhari.shared.ai

import kotlinx.serialization.Serializable

/**
 * What the app sends to the server. Contains only forecast facts the app has already
 * computed. The risk level is decided by the app's RiskEngine, never by the AI.
 */
@Serializable
data class ExplainRequest(
    val language: String,               // "en" or "sw"
    val countyName: String,
    val town: String,
    val riskLevel: String,              // LOW, MODERATE, HIGH, SEVERE
    val wettestDayName: String? = null, // already in the requested language, e.g. "Jumatano"
    val wettestDayDate: String? = null,
    val wettestDayMm: Double? = null,
    val wettestDayProbability: Int? = null,
    val threeDayTotalMm: Double = 0.0,
    val riverName: String? = null,
    val riverRiseRatio: Double? = null,
    val riverPeakDayName: String? = null,
    val hotspots: List<String> = emptyList(),
    val reasons: List<String> = emptyList(), // HEAVY_DAY, WET_SPELL, RIVER_RISING, LANDSLIDE
    val advisoryText: String? = null,        // optional official KMSA/county advisory text
)

/** What the AI must return, after parsing and validation. */
@Serializable
data class ExplainResponse(
    val headline: String,
    val body: String,
    val actions: List<String>,
)

object ExplainContract {
    val LANGUAGES = setOf("en", "sw")
    val LEVELS = setOf("LOW", "MODERATE", "HIGH", "SEVERE")
    val REASONS = setOf("HEAVY_DAY", "WET_SPELL", "RIVER_RISING", "LANDSLIDE")
    const val MAX_ADVISORY_CHARS = 2_000
    const val MAX_HOTSPOTS = 10

    /** Returns an error message, or null when the request is acceptable. */
    fun validateRequest(r: ExplainRequest): String? = when {
        r.language !in LANGUAGES -> "language must be one of $LANGUAGES"
        r.riskLevel !in LEVELS -> "riskLevel must be one of $LEVELS"
        r.countyName.isBlank() || r.countyName.length > 40 -> "invalid countyName"
        r.town.length > 40 -> "invalid town"
        r.reasons.any { it !in REASONS } -> "unknown reason"
        r.hotspots.size > MAX_HOTSPOTS || r.hotspots.any { it.length > 60 } -> "too many or too long hotspots"
        (r.advisoryText?.length ?: 0) > MAX_ADVISORY_CHARS -> "advisoryText too long"
        (r.wettestDayMm ?: 0.0) !in 0.0..1_000.0 -> "wettestDayMm out of range"
        r.threeDayTotalMm !in 0.0..2_000.0 -> "threeDayTotalMm out of range"
        (r.riverRiseRatio ?: 1.0) !in 0.0..100.0 -> "riverRiseRatio out of range"
        else -> null
    }

    /**
     * Trims and checks a parsed AI response. Returns null if it is unusable,
     * in which case callers must fall back to the offline template.
     */
    fun validateResponse(r: ExplainResponse): ExplainResponse? {
        val headline = r.headline.trim()
        val body = r.body.trim()
        val actions = r.actions.map { it.trim().removePrefix("-").removePrefix("•").trim() }.filter { it.isNotEmpty() }
        val ok = headline.length in 3..120 &&
            body.length in 20..700 &&
            actions.size in 1..5 &&
            actions.all { it.length <= 220 }
        return if (ok) ExplainResponse(headline, body, actions) else null
    }
}
