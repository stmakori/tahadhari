package ke.tahadhari.server

import ke.tahadhari.shared.ai.ExplainRequest
import ke.tahadhari.shared.ai.ExplainResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Many people in the same county ask for the same explanation. Caching identical requests
 * for an hour keeps AI costs low and responses fast.
 */
class ExplanationCache(
    private val ttlMillis: Long = 60 * 60 * 1000L,
    private val maxEntries: Int = 2_000,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(val value: ExplainResponse, val storedAt: Long)
    private val map = ConcurrentHashMap<ExplainRequest, Entry>()

    fun get(key: ExplainRequest): ExplainResponse? {
        val e = map[key] ?: return null
        if (now() - e.storedAt > ttlMillis) { map.remove(key); return null }
        return e.value
    }

    fun put(key: ExplainRequest, value: ExplainResponse) {
        if (map.size >= maxEntries) map.clear() // simple and safe for a small service
        map[key] = Entry(value, now())
    }
}
