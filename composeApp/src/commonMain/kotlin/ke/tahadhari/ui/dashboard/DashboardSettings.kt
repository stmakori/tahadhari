package ke.tahadhari.ui.dashboard

import ke.tahadhari.data.Counties
import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the dashboard remembers between runs. Stored as JSON under [DashboardSettingsStore.KEY].
 * Fields are plain strings and numbers so an older or corrupt file can never crash the app:
 * anything unrecognised falls back to the default (see the `...OrDefault` accessors).
 */
@Serializable
data class DashboardSettings(
    /** [Language] name, e.g. "ENGLISH". */
    val language: String = Language.ENGLISH.name,
    val selectedCode: Int? = null,
    /** "low", "moderate", "high", "severe" or "failed". The transient "loading" filter is never saved. */
    val filter: String? = null,
    val autoRefresh: Boolean = true,
) {
    fun languageOrDefault(): Language = Language.entries.firstOrNull { it.name == language } ?: Language.ENGLISH

    fun filterOrNull(): StatusFilter? = when (filter) {
        null -> null
        "failed" -> StatusFilter.Failed
        else -> RiskLevel.entries.firstOrNull { it.name.lowercase() == filter }?.let { StatusFilter.Level(it) }
    }

    fun selectedCodeOrNull(): Int? = selectedCode?.takeIf { Counties.byCode(it) != null }

    companion object {
        fun filterKey(filter: StatusFilter?): String? = when (filter) {
            is StatusFilter.Level -> filter.level.name.lowercase()
            StatusFilter.Failed -> "failed"
            StatusFilter.Loading, null -> null
        }
    }
}

/** Loads and saves [DashboardSettings] in a [KeyValueStore]. Never throws on bad data. */
class DashboardSettingsStore(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Saved settings, or defaults when nothing is saved or the stored text is corrupt. */
    suspend fun load(): DashboardSettings {
        val text = store.get(KEY) ?: return DashboardSettings()
        return try {
            json.decodeFromString(DashboardSettings.serializer(), text)
        } catch (e: IllegalArgumentException) { // SerializationException is a subclass
            DashboardSettings()
        }
    }

    suspend fun save(settings: DashboardSettings): Boolean =
        store.put(KEY, json.encodeToString(DashboardSettings.serializer(), settings))

    companion object {
        const val KEY = "dashboard_settings"
    }
}
