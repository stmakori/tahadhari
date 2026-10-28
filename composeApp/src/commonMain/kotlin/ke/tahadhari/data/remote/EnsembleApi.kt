package ke.tahadhari.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import ke.tahadhari.data.GeoPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/** Daily rainfall (mm) per ensemble member for the next days. One inner list per member. */
interface EnsembleApi {
    suspend fun dailyMembers(point: GeoPoint, forecastDays: Int = 7): List<List<Double?>>
}

/**
 * Open-Meteo Ensemble API, ECMWF IFS 0.25 degree model (51 members). Daily `precipitation_sum` is
 * supported directly, about 5 KB per point. https://open-meteo.com/en/docs/ensemble-api
 */
class OpenMeteoEnsembleApi(private val client: HttpClient) : EnsembleApi {
    override suspend fun dailyMembers(point: GeoPoint, forecastDays: Int): List<List<Double?>> {
        val text = client.get("https://ensemble-api.open-meteo.com/v1/ensemble") {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("models", "ecmwf_ifs025")
            parameter("daily", "precipitation_sum")
            parameter("timezone", "Africa/Nairobi")
            parameter("forecast_days", forecastDays)
        }.bodyAsText()
        return EnsembleResponseParser.members(text)
    }
}

object EnsembleResponseParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Pulls every `precipitation_sum` (control run) and `precipitation_sum_memberNN` series out of the
     * `daily` object. Throws when there is no `daily` object (e.g. an API error body).
     */
    fun members(text: String): List<List<Double?>> {
        val daily = json.parseToJsonElement(text).jsonObject["daily"]?.jsonObject
            ?: error("No daily data in ensemble response")
        return daily.entries
            .filter { (key, value) ->
                (key == "precipitation_sum" || key.startsWith("precipitation_sum_member")) && value is JsonArray
            }
            .map { (_, value) ->
                (value as JsonArray).map { (it as? JsonPrimitive)?.takeUnless { p -> p is JsonNull }?.doubleOrNull }
            }
    }
}
