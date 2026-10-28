package ke.tahadhari.data

import ke.tahadhari.data.remote.FloodDaily
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.data.store.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The raw data of the last successful fetch for one county. We store API data, not the risk
 * level, so the risk is always recomputed by RiskEngine (threshold changes apply to cached data).
 */
@Serializable
data class CachedForecast(
    val countyCode: Int,
    val fetchedAtMillis: Long,
    val rainfall: RainfallDaily,
    val flood: FloodDaily? = null,
    /** When [flood] was fetched. Can be older than [fetchedAtMillis] if the flood API failed. */
    val floodFetchedAtMillis: Long? = null,
    /**
     * Set only for a forecast at the user's own (rounded) point instead of the county HQ. Entries
     * with a point are stored under a different key, so they never replace or mix with HQ forecasts.
     */
    val pointLatitude: Double? = null,
    val pointLongitude: Double? = null,
    /** Soil moisture 9-27 cm (m³/m³) at [fetchedAtMillis]; null when the API gave none. */
    val soilMoistureM3m3: Double? = null,
    val version: Int = CURRENT_VERSION,
) {
    val point: GeoPoint? get() = if (pointLatitude != null && pointLongitude != null) GeoPoint(pointLatitude, pointLongitude) else null

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Per-county forecast cache on top of a [KeyValueStore]. Corrupt or unknown entries read as null. */
class ForecastCache(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Reads the HQ forecast ([point] null) or the saved forecast for exactly this rounded [point]. */
    suspend fun read(countyCode: Int, point: GeoPoint? = null): CachedForecast? {
        val text = store.get(keyFor(countyCode, point)) ?: return null
        val entry = try {
            json.decodeFromString(CachedForecast.serializer(), text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null // corrupt file: ignore, it will be overwritten on the next success
        }
        return entry.takeIf {
            it.version == CachedForecast.CURRENT_VERSION && it.countyCode == countyCode && it.point == point && it.rainfall.time.isNotEmpty()
        }
    }

    suspend fun write(entry: CachedForecast): Boolean =
        store.put(keyFor(entry.countyCode, entry.point), json.encodeToString(CachedForecast.serializer(), entry))

    companion object {
        /** HQ forecasts: `forecast-47`. Own-point forecasts: `forecast-47-here` (one per county, so storage stays bounded). */
        fun keyFor(countyCode: Int, point: GeoPoint? = null): String =
            if (point == null) "forecast-$countyCode" else "forecast-$countyCode-here"

        /** The user's point when [county] is a county moved to the user's location, else null (HQ forecast). */
        fun pointOf(county: County): GeoPoint? =
            Counties.byCode(county.code)?.location?.let { hq -> county.location.takeIf { it != hq } }
    }
}
