package ke.tahadhari.data

import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.Language
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the user chose last time. [countyCode] is null until they pick a county.
 * [onboardingDone] is true once the first-launch intro was finished or skipped.
 */
data class UserPreferences(
    val countyCode: Int? = null,
    val language: Language = Language.ENGLISH,
    val onboardingDone: Boolean = false,
)

/** Persists [UserPreferences]. Missing, corrupt or unknown values fall back to defaults. */
class Preferences(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Stored(val countyCode: Int? = null, val language: String? = null, val onboardingDone: Boolean = false)

    suspend fun load(): UserPreferences {
        val text = store.get(KEY) ?: return UserPreferences()
        val stored = try {
            json.decodeFromString(Stored.serializer(), text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return UserPreferences()
        }
        return UserPreferences(
            countyCode = stored.countyCode?.takeIf { Counties.byCode(it) != null },
            language = Language.entries.firstOrNull { it.name == stored.language } ?: Language.ENGLISH,
            onboardingDone = stored.onboardingDone,
        )
    }

    suspend fun save(prefs: UserPreferences): Boolean =
        store.put(KEY, json.encodeToString(Stored.serializer(), Stored(prefs.countyCode, prefs.language.name, prefs.onboardingDone)))

    suspend fun setCounty(countyCode: Int?): Boolean = update { it.copy(countyCode = countyCode) }

    suspend fun setLanguage(language: Language): Boolean = update { it.copy(language = language) }

    suspend fun setOnboardingDone(done: Boolean): Boolean = update { it.copy(onboardingDone = done) }

    private val updateLock = Mutex()

    private suspend fun update(change: (UserPreferences) -> UserPreferences): Boolean =
        updateLock.withLock { save(change(load())) }

    private companion object {
        const val KEY = "preferences"
    }
}
