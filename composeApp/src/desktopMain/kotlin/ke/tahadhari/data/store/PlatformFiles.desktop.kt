package ke.tahadhari.data.store

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** `~/.tahadhari/`, following the `user.home` system property. */
internal actual fun appStorageDir(): String? {
    val home = System.getProperty("user.home") ?: return null
    val dir = File(home, ".tahadhari")
    return if (dir.isDirectory || dir.mkdirs()) dir.path else null
}

internal actual fun readTextOrNull(path: String): String? =
    File(path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

internal actual fun writeTextFile(path: String, text: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeText(text, Charsets.UTF_8)
}

internal actual fun moveReplacing(from: String, to: String) {
    val source = File(from).toPath()
    val target = File(to).toPath()
    try {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (e: AtomicMoveNotSupportedException) {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}

internal actual fun deleteIfExists(path: String) {
    File(path).delete()
}

internal actual val storageDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
