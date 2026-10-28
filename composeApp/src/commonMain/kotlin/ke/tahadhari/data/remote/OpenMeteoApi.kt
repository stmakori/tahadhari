package ke.tahadhari.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import ke.tahadhari.data.GeoPoint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/**
 * Client for the free Open-Meteo APIs (no API key needed).
 * Forecast docs: https://open-meteo.com/en/docs
 * Flood docs (GloFAS river discharge): https://open-meteo.com/en/docs/flood-api
 */
interface ForecastApi {
    suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int = 7): RainfallResponse
    suspend fun riverDischarge(point: GeoPoint, pastDays: Int = 30, forecastDays: Int = 7): FloodResponse

    /**
     * Rainfall for many points in as few HTTP calls as possible. The result has exactly one entry
     * per point, in the same order; an entry is null when that point's data was missing or unusable.
     * Throws when a whole call fails (callers then fall back to their cache).
     * The default asks point by point, so simple implementations (fakes) keep working.
     */
    suspend fun dailyRainfallBatch(points: List<GeoPoint>, forecastDays: Int = 7): List<RainfallResponse?> =
        points.map { dailyRainfall(it, forecastDays) }

    /** Same as [dailyRainfallBatch] for river discharge. */
    suspend fun riverDischargeBatch(points: List<GeoPoint>, pastDays: Int = 30, forecastDays: Int = 7): List<FloodResponse?> =
        points.map { riverDischarge(it, pastDays, forecastDays) }
}

/** Open-Meteo accepts many comma-separated coordinates per call; stay well under its limit and URL length. */
const val MAX_POINTS_PER_CALL = 50

class OpenMeteoApi(private val client: HttpClient) : ForecastApi {
    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse =
        client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("daily", "precipitation_sum,precipitation_probability_max")
            parameter("timezone", "Africa/Nairobi")
            parameter("forecast_days", forecastDays)
        }.body()

    override suspend fun riverDischarge(point: GeoPoint, pastDays: Int, forecastDays: Int): FloodResponse =
        client.get("https://flood-api.open-meteo.com/v1/flood") {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("daily", "river_discharge")
            parameter("past_days", pastDays)
            parameter("forecast_days", forecastDays)
        }.body()

    override suspend fun dailyRainfallBatch(points: List<GeoPoint>, forecastDays: Int): List<RainfallResponse?> =
        points.chunked(MAX_POINTS_PER_CALL).flatMap { chunk ->
            val text = client.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", chunk.joinToString(",") { it.latitude.toString() })
                parameter("longitude", chunk.joinToString(",") { it.longitude.toString() })
                parameter("daily", "precipitation_sum,precipitation_probability_max")
                parameter("timezone", "Africa/Nairobi")
                parameter("forecast_days", forecastDays)
            }.bodyAsText()
            decodeBatch(text, chunk.size, RainfallResponse.serializer())
        }

    override suspend fun riverDischargeBatch(points: List<GeoPoint>, pastDays: Int, forecastDays: Int): List<FloodResponse?> =
        points.chunked(MAX_POINTS_PER_CALL).flatMap { chunk ->
            val text = client.get("https://flood-api.open-meteo.com/v1/flood") {
                parameter("latitude", chunk.joinToString(",") { it.latitude.toString() })
                parameter("longitude", chunk.joinToString(",") { it.longitude.toString() })
                parameter("daily", "river_discharge")
                parameter("past_days", pastDays)
                parameter("forecast_days", forecastDays)
            }.bodyAsText()
            decodeBatch(text, chunk.size, FloodResponse.serializer())
        }

    private fun <T> decodeBatch(text: String, expected: Int, serializer: KSerializer<T>): List<T?> =
        parseBatch(lenientJson, text, expected, serializer)
}

/**
 * Open-Meteo answers a multi-point request with a JSON array (one object per point, same order)
 * but a single-point request with a bare object. Handles both; an element that does not parse is
 * null. Throws if the number of elements does not match [expected], because mapping results to
 * the wrong county would be worse than failing.
 */
fun <T> parseBatch(json: Json, text: String, expected: Int, serializer: KSerializer<T>): List<T?> {
    val root = json.parseToJsonElement(text)
    val elements = if (root is JsonArray) root.toList() else listOf(root)
    check(elements.size == expected) { "Expected $expected locations but got ${elements.size}" }
    return elements.map { el ->
        try {
            json.decodeFromJsonElement(serializer, el)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
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
