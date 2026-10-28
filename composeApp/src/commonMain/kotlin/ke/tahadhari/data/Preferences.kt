package ke.tahadhari.data

import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * What the user chose last time. [countyCode] is null until they pick a county.
 * [onboardingDone] is true once the first-launch intro was finished or skipped.
 * [riskAlertsEnabled] is the opt-in "notify me if risk rises to High or Severe" switch (Android); off by default.
 */
data class UserPreferences(
    val countyCode: Int? = null,
    val language: Language = Language.ENGLISH,
    val onboardingDone: Boolean = false,
    val riskAlertsEnabled: Boolean = false,
    /** Up to [WatchList.MAX] other counties the user follows, in the order added. Never contains [countyCode]. */
    val watchCodes: List<Int> = emptyList(),
)

/** Persists [UserPreferences]. Missing, corrupt or unknown values fall back to defaults. */
class Preferences(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Stored(val countyCode: Int? = null, val language: String? = null, val onboardingDone: Boolean = false, val riskAlertsEnabled: Boolean = false, val watchCodes: JsonElement? = null)

    suspend fun load(): UserPreferences {
        val text = store.get(KEY) ?: return UserPreferences()
        val stored = try {
            json.decodeFromString(Stored.serializer(), text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return UserPreferences()
        }
        val own = stored.countyCode?.takeIf { Counties.byCode(it) != null }
        // Untrusted text: only integers (not strings) count; unknown codes, duplicates and the own county are dropped.
        val watched = (stored.watchCodes as? JsonArray).orEmpty().mapNotNull { el ->
            (el as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        }
        return UserPreferences(
            countyCode = own,
            watchCodes = WatchList.sanitize(watched, own),
            language = Language.entries.firstOrNull { it.name == stored.language } ?: Language.ENGLISH,
            onboardingDone = stored.onboardingDone,
            riskAlertsEnabled = stored.riskAlertsEnabled,
        )
    }

    suspend fun save(prefs: UserPreferences): Boolean {
        val watch = JsonArray(WatchList.sanitize(prefs.watchCodes, prefs.countyCode).map { JsonPrimitive(it) })
        return store.put(KEY, json.encodeToString(Stored.serializer(), Stored(prefs.countyCode, prefs.language.name, prefs.onboardingDone, prefs.riskAlertsEnabled, watch)))
    }

    /** Choosing a county as your own removes it from the watch list. */
    suspend fun setCounty(countyCode: Int?): Boolean =
        update { it.copy(countyCode = countyCode, watchCodes = WatchList.sanitize(it.watchCodes, countyCode)) }

    suspend fun setWatchCodes(codes: List<Int>): Boolean = update { it.copy(watchCodes = WatchList.sanitize(codes, it.countyCode)) }

    suspend fun setLanguage(language: Language): Boolean = update { it.copy(language = language) }

    suspend fun setOnboardingDone(done: Boolean): Boolean = update { it.copy(onboardingDone = done) }

    suspend fun setRiskAlertsEnabled(enabled: Boolean): Boolean = update { it.copy(riskAlertsEnabled = enabled) }

    private val updateLock = Mutex()

    private suspend fun update(change: (UserPreferences) -> UserPreferences): Boolean =
        updateLock.withLock { save(change(load())) }

    private companion object {
        const val KEY = "preferences"
    }
}
