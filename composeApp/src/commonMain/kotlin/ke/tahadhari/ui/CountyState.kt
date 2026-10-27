package ke.tahadhari.ui

import ke.tahadhari.AppGraph
import ke.tahadhari.data.County
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.RiskAssessment
import kotlinx.coroutines.CancellationException

sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val forecast: CountyForecast, val assessment: RiskAssessment) : LoadState
    data class Failed(val message: String) : LoadState
}

suspend fun loadCounty(county: County): LoadState = try {
    val forecast = AppGraph.repository.forecastFor(county)
    LoadState.Ready(forecast, AppGraph.riskEngine.assess(forecast))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    LoadState.Failed(e.message ?: e::class.simpleName ?: "error")
}
