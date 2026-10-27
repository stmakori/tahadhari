package ke.tahadhari.data.store

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Tiny string key-value store used for the forecast cache and preferences.
 * Implementations never throw for I/O problems: a failed read is `null`, a failed write returns false.
 * Callers must treat stored values as untrusted (they may be corrupt or from an older app version).
 */
interface KeyValueStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String): Boolean
    suspend fun remove(key: String)
}

/** Non-persistent store: used in tests and as a fallback when no storage directory is available. */
class InMemoryKeyValueStore : KeyValueStore {
    private val mutex = Mutex()
    private val map = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = mutex.withLock { map[key] }
    override suspend fun put(key: String, value: String): Boolean = mutex.withLock { map[key] = value; true }
    override suspend fun remove(key: String) { mutex.withLock { map.remove(key) } }
}

/**
 * One file per key in [directory] (`<key>.json`). Writes go to a temp file that is then renamed
 * over the target, so a crash mid-write leaves the previous value intact.
 * The platform only provides the raw file operations (see `PlatformFiles.kt`).
 */
class FileKeyValueStore(
    private val directory: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : KeyValueStore {
    private val writeLock = Mutex()

    override suspend fun get(key: String): String? = withContext(io) {
        safely { readTextOrNull(pathFor(key)) }
    }

    override suspend fun put(key: String, value: String): Boolean = writeLock.withLock {
        withContext(io) {
            val target = pathFor(key)
            val temp = "$target.tmp"
            safely {
                writeTextFile(temp, value)
                moveReplacing(temp, target)
                true
            } ?: run {
                safely { deleteIfExists(temp) }
                false
            }
        }
    }

    override suspend fun remove(key: String) {
        writeLock.withLock { withContext(io) { safely { deleteIfExists(pathFor(key)) } } }
    }

    private fun pathFor(key: String): String = "$directory/${sanitize(key)}.json"

    private inline fun <T> safely(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        /** Keeps file names portable: anything other than letters, digits, '-' and '_' becomes '_'. */
        fun sanitize(key: String): String =
            key.map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }.joinToString("")
    }
}

/** The app's persistent store, or an in-memory one if the platform has no storage directory yet. */
fun platformKeyValueStore(): KeyValueStore =
    appStorageDir()?.let { FileKeyValueStore(it) } ?: InMemoryKeyValueStore()
