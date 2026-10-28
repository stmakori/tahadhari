package ke.tahadhari.platform

import ke.tahadhari.APP_VERSION
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

private fun crashFile(): File? =
    System.getProperty("user.home")?.let { File(File(it, ".tahadhari"), "crash.log") }

@Volatile private var installed = false

actual fun installCrashHandler() {
    if (installed) return
    installed = true
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        try {
            val file = crashFile()
            if (file != null) {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it, true)) }.toString()
                val entry = formatCrashReport(
                    System.currentTimeMillis(), APP_VERSION,
                    "${System.getProperty("os.name")} ${System.getProperty("os.version")}, Java ${System.getProperty("java.version")}",
                    System.getProperty("os.arch") ?: "desktop", trace,
                )
                file.parentFile?.mkdirs()
                file.writeText(appendCrashReport(file.takeIf { it.isFile }?.readText(), entry), Charsets.UTF_8)
            }
        } catch (_: Throwable) {
            // Never let crash logging hide the original crash.
        }
        previous?.uncaughtException(thread, error)
    }
}

actual fun readCrashReport(): String? = try {
    crashFile()?.takeIf { it.isFile }?.readText(Charsets.UTF_8)?.takeIf { it.isNotBlank() }
} catch (_: Exception) { null }

actual fun deleteCrashReport() {
    try { crashFile()?.delete() } catch (_: Exception) {}
}
