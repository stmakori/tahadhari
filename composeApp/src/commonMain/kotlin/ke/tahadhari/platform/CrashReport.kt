package ke.tahadhari.platform

import ke.tahadhari.domain.Dates

/** The crash file never grows past about 20 KB (characters, the text is nearly all ASCII). */
const val CRASH_REPORT_MAX_CHARS = 20_000

private const val TRUNCATED = "\n… (cut)\n"

/**
 * One short crash entry. Pure, so it can be tested: the platform handlers collect the facts and
 * call this. Nothing here is ever sent anywhere; the user may share the file by hand.
 *
 * @param system e.g. "Android 14 (API 34)" or "Windows 11, Java 21".
 * @param device e.g. "Google Pixel 7", or the machine's OS name on desktop.
 */
fun formatCrashReport(epochMillis: Long, appVersion: String, system: String, device: String, stackTrace: String): String {
    val iso = Dates.isoDateFromEpochMillis(epochMillis)
    val minuteOfDay = (epochMillis + Dates.NAIROBI_UTC_OFFSET_MINUTES * 60_000L).floorDiv(60_000L).mod(1440L).toInt()
    val hhmm = "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"
    val entry = "Tahadhari crash report\n" +
        "Time: $iso $hhmm (Nairobi)\n" +
        "App version: $appVersion\n" +
        "System: $system\n" +
        "Device: $device\n\n" +
        stackTrace.trim() + "\n"
    return if (entry.length <= CRASH_REPORT_MAX_CHARS) entry
    else entry.take(CRASH_REPORT_MAX_CHARS - TRUNCATED.length) + TRUNCATED
}

/** Appends [entry] to the existing file text and keeps the newest [CRASH_REPORT_MAX_CHARS] characters. */
fun appendCrashReport(existing: String?, entry: String): String {
    val all = if (existing.isNullOrEmpty()) entry else existing.trimEnd() + "\n\n----\n\n" + entry
    return if (all.length <= CRASH_REPORT_MAX_CHARS) all else all.takeLast(CRASH_REPORT_MAX_CHARS)
}

/**
 * Installs an uncaught-exception handler that appends a crash report to a local file (Android:
 * `files/crash.log`; desktop: `~/.tahadhari/crash.log`) and then calls the previous handler.
 * Safe to call more than once. On Android, `AndroidPlatform.init` must have run first.
 */
expect fun installCrashHandler()

/** The saved crash report text, or null if there is none. */
expect fun readCrashReport(): String?

/** Deletes the saved crash report. */
expect fun deleteCrashReport()
