package ke.tahadhari.data.store

// Thin platform file operations. All logic (atomic writes, error handling) lives in FileKeyValueStore.
// These may throw; FileKeyValueStore catches everything.

/** Directory for app data, created if needed, or null if storage is not available (yet). */
internal expect fun appStorageDir(): String?

/** File contents as UTF-8, or null if the file does not exist. */
internal expect fun readTextOrNull(path: String): String?

/** Writes UTF-8 text, creating parent directories. */
internal expect fun writeTextFile(path: String, text: String)

/** Renames [from] to [to], replacing [to] if it exists (atomically where the platform allows). */
internal expect fun moveReplacing(from: String, to: String)

internal expect fun deleteIfExists(path: String)

/** Dispatcher for file I/O: Dispatchers.IO on Android and desktop; the browser has no blocking I/O. */
internal expect val storageDispatcher: kotlinx.coroutines.CoroutineDispatcher
