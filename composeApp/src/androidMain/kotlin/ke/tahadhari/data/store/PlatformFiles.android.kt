package ke.tahadhari.data.store

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Holds the app's private files directory. Call [init] from `MainActivity.onCreate` before
 * anything touches `AppGraph`; until then the app falls back to an in-memory store.
 */
object AndroidStorage {
    @Volatile internal var filesDir: File? = null
        private set

    fun init(context: Context) {
        if (filesDir == null) filesDir = File(context.applicationContext.filesDir, "tahadhari")
    }
}

internal actual fun appStorageDir(): String? {
    val dir = AndroidStorage.filesDir ?: return null
    return if (dir.isDirectory || dir.mkdirs()) dir.path else null
}

internal actual fun readTextOrNull(path: String): String? =
    File(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

internal actual fun writeTextFile(path: String, text: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeText(text, Charsets.UTF_8)
}

/** rename(2) on Android's Linux file systems replaces the target atomically. (java.nio.file needs API 26.) */
internal actual fun moveReplacing(from: String, to: String) {
    if (!File(from).renameTo(File(to))) throw IOException("rename $from -> $to failed")
}

internal actual fun deleteIfExists(path: String) {
    File(path).delete()
}

internal actual val storageDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
