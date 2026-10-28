package ke.tahadhari.ai

import ke.tahadhari.data.County
import ke.tahadhari.domain.*
import ke.tahadhari.shared.ai.ExplainContract
import ke.tahadhari.shared.ai.ExplainRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Asks the AI for a friendlier explanation, but never depends on it:
 * on timeout, network error or bad output it returns the offline template instead.
 * The risk level always comes from RiskEngine; the AI only writes the words.
 */
class AiExplainer(
    private val client: ExplainClient,
    private val fallback: TemplateExplainer = TemplateExplainer(),
    private val timeoutMillis: Long = 12_000,
) : AlertExplainer {

    private val cache = mutableMapOf<ExplainRequest, Explanation>()
    private val lock = Mutex()

    override suspend fun explain(county: County, assessment: RiskAssessment, language: Language): Explanation {
        val request = toRequest(county, assessment, language)
        lock.withLock { cache[request] }?.let { return it }

        return try {
            val response = withTimeout(timeoutMillis) { client.explain(request) }
            val valid = ExplainContract.validateResponse(response) ?: return fallback.build(county, assessment, language)
            Explanation(valid.headline, valid.body, valid.actions, ExplanationSource.AI)
                .also { result -> lock.withLock { cache[request] = result } }
        } catch (e: TimeoutCancellationException) {
            fallback.build(county, assessment, language)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallback.build(county, assessment, language)
        }
    }

    companion object {
        fun toRequest(county: County, a: RiskAssessment, language: Language): ExplainRequest {
            val day = a.wettestDay
            return ExplainRequest(
                language = if (language == Language.SWAHILI) "sw" else "en",
                countyName = county.name,
                town = county.town,
                riskLevel = a.level.name,
                wettestDayName = day?.let { Dates.dayName(it.date, language) },
                wettestDayDate = day?.date,
                wettestDayMm = day?.precipitationMm,
                wettestDayProbability = day?.probabilityPercent,
                threeDayTotalMm = a.maxThreeDayTotalMm,
                riverName = a.river?.riverName,
                riverRiseRatio = a.river?.riseRatio,
                riverPeakDayName = a.river?.let { Dates.dayName(it.peakDate, language) },
                hotspots = county.hotspots,
                reasons = a.reasons.map {
                    when (it) {
                        is RiskReason.HeavyDay -> "HEAVY_DAY"
                        is RiskReason.WetSpell -> "WET_SPELL"
                        is RiskReason.RiverRising -> "RIVER_RISING"
                        is RiskReason.LandslideRisk -> "LANDSLIDE"
                        is RiskReason.SaturatedGround -> "SATURATED_GROUND"
                        is RiskReason.UpstreamRain -> "UPSTREAM_RAIN"
                    }
                },
            )
        }
    }
}
