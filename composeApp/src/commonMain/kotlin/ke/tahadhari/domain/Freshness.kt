package ke.tahadhari.domain

/** How old a forecast is, bucketed for display ("5 min ago", "3 h ago", "on Tue 27 Oct"). */
sealed interface DataAge {
    data object JustNow : DataAge
    data class Minutes(val minutes: Int) : DataAge
    data class Hours(val hours: Int) : DataAge
    /** A day or older: show the Nairobi calendar date (ISO yyyy-MM-dd). */
    data class OnDate(val isoDate: String) : DataAge
}

/** Pure rules about forecast age. No clock here: callers pass "now" in, so this is easy to test. */
object Freshness {
    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    /**
     * Cached forecasts older than this are not shown at all. A 7-day forecast fetched more than
     * 3 days ago has lost most of its window, so its risk level could be dangerously wrong.
     */
    const val MAX_SHOWABLE_AGE_MILLIS: Long = 3 * DAY

    /** True when data fetched at [fetchedAtMillis] is too old to show a risk level. */
    fun isTooOldToShow(
        fetchedAtMillis: Long,
        nowMillis: Long,
        maxAgeMillis: Long = MAX_SHOWABLE_AGE_MILLIS,
    ): Boolean = nowMillis - fetchedAtMillis > maxAgeMillis

    /** Buckets the age of data for display. Clock skew (a time in the future) counts as "just now". */
    fun ageOf(updatedAtMillis: Long, nowMillis: Long): DataAge {
        val age = nowMillis - updatedAtMillis
        return when {
            age < MINUTE -> DataAge.JustNow
            age < HOUR -> DataAge.Minutes((age / MINUTE).toInt())
            age < DAY -> DataAge.Hours((age / HOUR).toInt())
            else -> DataAge.OnDate(Dates.isoDateFromEpochMillis(updatedAtMillis))
        }
    }
}
