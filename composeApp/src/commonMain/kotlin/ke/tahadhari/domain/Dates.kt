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
}
