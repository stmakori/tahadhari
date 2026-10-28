package ke.tahadhari.data

import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskSnapshot
import ke.tahadhari.domain.SnapshotHistory
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persists the daily risk snapshots (a few hundred bytes). Stored text is untrusted: bad dates,
 * unknown counties or unknown level names are skipped, and corrupt JSON reads as "no history".
 */
class RiskSnapshotStore(private val store: KeyValueStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Day(val date: String = "", val levels: Map<String, String> = emptyMap())

    @Serializable
    private data class Stored(val days: List<Day> = emptyList())

    /** Newest first, at most [SnapshotHistory.KEEP_DAYS] days. */
    suspend fun load(): List<RiskSnapshot> {
        val text = store.get(KEY) ?: return emptyList()
        val stored = try {
            json.decodeFromString(Stored.serializer(), text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return emptyList()
        }
        return stored.days.mapNotNull { day ->
            if (!DATE.matches(day.date)) return@mapNotNull null
            val levels = day.levels.mapNotNull { (code, name) ->
                val c = code.toIntOrNull()?.takeIf { Counties.byCode(it) != null } ?: return@mapNotNull null
                val level = RiskLevel.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
                c to level
            }.toMap()
            if (levels.isEmpty()) null else RiskSnapshot(day.date, levels)
        }.distinctBy { it.date }.sortedByDescending { it.date }.take(SnapshotHistory.KEEP_DAYS)
    }

    suspend fun save(history: List<RiskSnapshot>): Boolean {
        val stored = Stored(history.map { s -> Day(s.date, s.levels.entries.associate { it.key.toString() to it.value.name }) })
        return store.put(KEY, json.encodeToString(Stored.serializer(), stored))
    }

    private companion object {
        const val KEY = "risk_snapshots"
        val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}
