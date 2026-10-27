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
    val version: Int = CURRENT_VERSION,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** Per-county forecast cache on top of a [KeyValueStore]. Corrupt or unknown entries read as null. */
class ForecastCache(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun read(countyCode: Int): CachedForecast? {
        val text = store.get(keyFor(countyCode)) ?: return null
        val entry = try {
            json.decodeFromString(CachedForecast.serializer(), text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null // corrupt file: ignore, it will be overwritten on the next success
        }
        return entry.takeIf {
            it.version == CachedForecast.CURRENT_VERSION && it.countyCode == countyCode && it.rainfall.time.isNotEmpty()
        }
    }

    suspend fun write(entry: CachedForecast): Boolean =
        store.put(keyFor(entry.countyCode), json.encodeToString(CachedForecast.serializer(), entry))

    private fun keyFor(countyCode: Int) = "forecast-$countyCode"
}
