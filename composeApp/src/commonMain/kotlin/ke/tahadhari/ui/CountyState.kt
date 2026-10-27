package ke.tahadhari.ui

import ke.tahadhari.AppGraph
import ke.tahadhari.data.County
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Freshness
import ke.tahadhari.domain.RiskAssessment
import ke.tahadhari.domain.RiskEngine
import kotlinx.coroutines.CancellationException

sealed interface LoadState {
    data object Loading : LoadState

    /**
     * Data to show. [updatedAtMillis] is when the forecast was downloaded (epoch millis);
     * [fromCache] is true when the network failed and this is the last saved forecast.
     * The UI should show "updated …" text (see [updatedText]) and an offline note when [fromCache].
     */
    data class Ready(
        val forecast: CountyForecast,
        val assessment: RiskAssessment,
        val updatedAtMillis: Long,
        val fromCache: Boolean,
    ) : LoadState

    /**
     * Nothing safe to show. [staleSinceMillis] is set when we had a saved forecast but it was too
     * old to use (see [Freshness.isTooOldToShow]); the UI can then use `Strings.savedForecastTooOld`.
     * [message] is a technical/English fallback description.
     */
    data class Failed(val message: String, val staleSinceMillis: Long? = null) : LoadState
}

suspend fun loadCounty(county: County): LoadState =
    loadCounty(county, AppGraph.repository, AppGraph.riskEngine, AppGraph.nowMillis())

/** Testable version: the risk level is always recomputed from data, never cached. */
suspend fun loadCounty(
    county: County,
    repository: ForecastRepository,
    riskEngine: RiskEngine,
    nowMillis: Long,
): LoadState = try {
    val result = repository.forecastFor(county)
    if (result.fromCache && Freshness.isTooOldToShow(result.fetchedAtMillis, nowMillis)) {
        val date = Dates.isoDateFromEpochMillis(result.fetchedAtMillis)
        LoadState.Failed(
            "Couldn't load the forecast and the saved one from $date is too old to use.",
            staleSinceMillis = result.fetchedAtMillis,
        )
    } else {
        LoadState.Ready(
            forecast = result.forecast,
            assessment = riskEngine.assess(result.forecast),
            updatedAtMillis = result.fetchedAtMillis,
            fromCache = result.fromCache,
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    LoadState.Failed(e.message ?: e::class.simpleName ?: "error")
}
