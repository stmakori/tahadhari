package ke.tahadhari

import ke.tahadhari.data.County
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiverOutlook
import ke.tahadhari.ui.LoadState

/** Builds a Ready state whose level comes from RiskEngine, from a 7-day rainfall list. */
fun readyState(
    county: County,
    mm: List<Double>,
    river: RiverOutlook? = null,
    updatedAtMillis: Long = 1_793_000_000_000L,
    fromCache: Boolean = false,
): LoadState.Ready {
    val days = mm.mapIndexed { i, v -> DailyRain("2026-11-0${2 + i}", v, 60) }
    val forecast = CountyForecast(county, days, river, "2026-11-02")
    return LoadState.Ready(forecast, RiskEngine().assess(forecast), updatedAtMillis, fromCache)
}

/** Rainfall that RiskEngine rates LOW, MODERATE, HIGH and SEVERE respectively. */
object Rain {
    val low = listOf(1.0, 2.0, 3.0, 0.0, 1.0, 2.0, 0.0)
    val moderate = listOf(5.0, 12.0, 25.0, 10.0, 3.0, 0.0, 1.0)
    val high = listOf(4.0, 12.0, 28.0, 55.0, 30.0, 9.0, 0.5)
    val severe = listOf(10.0, 30.0, 45.0, 85.0, 50.0, 12.0, 3.0)
}
