package ke.tahadhari.data

import ke.tahadhari.data.remote.OpenMeteoApi
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.RiverOutlook
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches and combines rainfall + river data for a county.
 * TODO(week 2): add an offline cache (SQLDelight or Room) and serve cached data
 * with a "last updated" time when the network fails.
 */
class ForecastRepository(private val api: OpenMeteoApi) {

    suspend fun forecastFor(county: County): CountyForecast = coroutineScope {
        val rainCall = async { api.dailyRainfall(county.location) }
        val riverCall = county.riverPoint?.let { point ->
            async { runCatching { api.riverDischarge(point) }.getOrNull() }
        }

        val rain = rainCall.await().daily
        val days = rain.time.mapIndexed { i, date ->
            DailyRain(
                date = date,
                precipitationMm = rain.precipitationSum.getOrNull(i) ?: 0.0,
                probabilityPercent = rain.precipitationProbabilityMax.getOrNull(i),
            )
        }
        val today = days.firstOrNull()?.date ?: ""

        val river = riverCall?.await()?.daily?.let { flood ->
            RiverOutlook.fromSeries(county.riverName, flood.time, flood.riverDischarge, today)
        }

        CountyForecast(county = county, days = days, river = river, fetchedAtDate = today)
    }
}
