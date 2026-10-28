package ke.tahadhari.data.store

import kotlinx.browser.localStorage

// Browser "files" are localStorage entries keyed by the full path. If localStorage is blocked
// (private mode, disabled site data) every operation falls back to an in-memory map.

private val memory = mutableMapOf<String, String>()

private inline fun <T> storage(block: () -> T, fallback: () -> T): T =
    try { block() } catch (_: Throwable) { fallback() }

internal actual fun appStorageDir(): String? = "tahadhari"

internal actual fun readTextOrNull(path: String): String? =
    storage({ localStorage.getItem(path) }, { memory[path] })

internal actual fun writeTextFile(path: String, text: String) {
    storage({ localStorage.setItem(path, text) }, { memory[path] = text })
}

internal actual fun moveReplacing(from: String, to: String) {
    val text = readTextOrNull(from) ?: return
    writeTextFile(to, text)
    deleteIfExists(from)
}

internal actual fun deleteIfExists(path: String) {
    storage({ localStorage.removeItem(path) }, { })
    memory.remove(path)
}
