package ke.tahadhari.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ke.tahadhari.data.GeoPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Client for the free Open-Meteo APIs (no API key needed).
 * Forecast docs: https://open-meteo.com/en/docs
 * Flood docs (GloFAS river discharge): https://open-meteo.com/en/docs/flood-api
 */
class OpenMeteoApi(private val client: HttpClient) {

    suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int = 7): RainfallResponse =
        client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("daily", "precipitation_sum,precipitation_probability_max")
            parameter("timezone", "Africa/Nairobi")
            parameter("forecast_days", forecastDays)
        }.body()

    suspend fun riverDischarge(point: GeoPoint, pastDays: Int = 30, forecastDays: Int = 7): FloodResponse =
        client.get("https://flood-api.open-meteo.com/v1/flood") {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("daily", "river_discharge")
            parameter("past_days", pastDays)
            parameter("forecast_days", forecastDays)
        }.body()
}

@Serializable
data class RainfallResponse(val daily: RainfallDaily)

@Serializable
data class RainfallDaily(
    val time: List<String>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double?>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
)

@Serializable
data class FloodResponse(val daily: FloodDaily)

@Serializable
data class FloodDaily(
    val time: List<String>,
    @SerialName("river_discharge") val riverDischarge: List<Double?>,
)
