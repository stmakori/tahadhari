package ke.tahadhari.domain

/** Minimal date helpers so we don't need a date-time library for ISO yyyy-MM-dd strings. */
object Dates {
    /** 0 = Sunday … 6 = Saturday (Sakamoto's algorithm). */
    fun dayOfWeek(isoDate: String): Int {
        val (y0, m, d) = isoDate.split("-").map { it.toInt() }
        val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val y = if (m < 3) y0 - 1 else y0
        return (y + y / 4 - y / 100 + y / 400 + offsets[m - 1] + d) % 7
    }

    private val englishDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val swahiliDays = listOf("Jumapili", "Jumatatu", "Jumanne", "Jumatano", "Alhamisi", "Ijumaa", "Jumamosi")

    fun dayName(isoDate: String, language: Language): String {
        val i = dayOfWeek(isoDate)
        return if (language == Language.SWAHILI) swahiliDays[i] else englishDays[i]
    }

    /** Day of month as a label for compact charts, e.g. "2026-10-27" -> "27". */
    fun dayOfMonth(isoDate: String): String = isoDate.substringAfterLast("-").trimStart('0')

    /** Kenya (Africa/Nairobi) is UTC+3 all year, with no daylight saving. */
    const val NAIROBI_UTC_OFFSET_MINUTES: Int = 180

    /** Epoch millis -> ISO yyyy-MM-dd in Nairobi local time (civil-from-days algorithm). */
    fun isoDateFromEpochMillis(epochMillis: Long, utcOffsetMinutes: Int = NAIROBI_UTC_OFFSET_MINUTES): String {
        val localMillis = epochMillis + utcOffsetMinutes * 60_000L
        val z = localMillis.floorDiv(86_400_000L) + 719_468L
        val era = (if (z >= 0) z else z - 146_096L) / 146_097L
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val day = doy - (153L * mp + 2L) / 5L + 1L
        val month = if (mp < 10L) mp + 3L else mp - 9L
        val year = yoe + era * 400L + if (month <= 2L) 1L else 0L
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private val englishShortDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val englishShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val swahiliMonths = listOf(
        "Januari", "Februari", "Machi", "Aprili", "Mei", "Juni",
        "Julai", "Agosti", "Septemba", "Oktoba", "Novemba", "Desemba",
    )

    /**
     * Compact date for "updated on" text: "Tue 27 Oct" in English, "Jumanne, 27 Oktoba" in Swahili
     * (Swahili day and month names are not usually abbreviated).
     */
    fun shortDate(isoDate: String, language: Language): String {
        val month = isoDate.split("-")[1].toInt()
        val day = dayOfMonth(isoDate)
        return if (language == Language.SWAHILI) {
            "${dayName(isoDate, language)}, $day ${swahiliMonths[month - 1]}"
        } else {
            "${englishShortDays[dayOfWeek(isoDate)]} $day ${englishShortMonths[month - 1]}"
        }
    }
}
