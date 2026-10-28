package ke.tahadhari.data

import ke.tahadhari.data.remote.EnsembleApi
import ke.tahadhari.domain.HeavyRainChance
import ke.tahadhari.domain.RiskThresholds
import kotlinx.coroutines.CancellationException

/**
 * Chance of heavy rain from the forecast ensemble, for ONE county at a time (never fetch all 47).
 * Returns null on any failure so the UI hides the line. Not cached, not used by RiskEngine.
 */
class EnsembleRepository(
    private val api: EnsembleApi,
    private val thresholds: RiskThresholds = RiskThresholds(),
) {
    suspend fun chanceFor(county: County): HeavyRainChance? = try {
        HeavyRainChance.fromMembers(
            api.dailyMembers(county.location),
            moderateMm = thresholds.dailyModerateMm,
            highMm = thresholds.dailyHighMm,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
