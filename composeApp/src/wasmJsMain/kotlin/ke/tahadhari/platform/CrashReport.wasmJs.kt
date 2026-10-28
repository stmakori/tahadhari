package ke.tahadhari.platform

// The browser has its own error console and there is no file to keep a report in, so the
// crash log is a no-op on the web.
actual fun installCrashHandler() {}

actual fun readCrashReport(): String? = null

actual fun deleteCrashReport() {}
