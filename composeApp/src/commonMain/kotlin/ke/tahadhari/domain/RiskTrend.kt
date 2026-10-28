package ke.tahadhari.domain

/** How a county's risk level changed since the baseline day (usually yesterday). */
sealed interface Trend {
    data class Rising(val from: RiskLevel) : Trend
    data class Falling(val from: RiskLevel) : Trend
    data object Same : Trend
    /** No baseline for this county, or no current level: nothing is shown. */
    data object Unknown : Trend
}

/** Compares [current] with [baseline]. Either being null (no data / no snapshot) is [Trend.Unknown]. */
fun trendFor(current: RiskLevel?, baseline: RiskLevel?): Trend = when {
    current == null || baseline == null -> Trend.Unknown
    current > baseline -> Trend.Rising(baseline)
    current < baseline -> Trend.Falling(baseline)
    else -> Trend.Same
}

/** Risk levels per county code, taken on [date] (an Africa/Nairobi calendar day, `yyyy-MM-dd`). */
data class RiskSnapshot(val date: String, val levels: Map<Int, RiskLevel>)

/**
 * Pure rules for the daily snapshots behind "trend since yesterday".
 * One snapshot per Nairobi calendar day (the latest), at most [KEEP_DAYS] days kept.
 */
object SnapshotHistory {
    const val KEEP_DAYS = 3

    /** A baseline older than this many days is not "since yesterday" any more and is ignored. */
    const val MAX_BASELINE_AGE_DAYS = 2

    private const val DAY_MILLIS = 86_400_000L

    /**
     * Adds [fresh] levels (only counties whose data was fresh, never cached or failed ones) taken at
     * [nowMillis] to [history]. Counties already in today's snapshot but absent from [fresh] keep their
     * earlier value from today. Snapshots dated after today are dropped, then only the newest
     * [KEEP_DAYS] days are kept. Returns [history] unchanged when [fresh] is empty. Newest first.
     */
    fun record(history: List<RiskSnapshot>, fresh: Map<Int, RiskLevel>, nowMillis: Long): List<RiskSnapshot> {
        if (fresh.isEmpty()) return history
        val today = Dates.isoDateFromEpochMillis(nowMillis)
        val existingToday = history.firstOrNull { it.date == today }?.levels.orEmpty()
        val todays = RiskSnapshot(today, existingToday + fresh)
        return (history.filter { it.date < today }.distinctBy { it.date } + todays)
            .sortedByDescending { it.date }
            .take(KEEP_DAYS)
    }

    /**
     * The most recent snapshot from a calendar day EARLIER than today (Nairobi), no more than
     * [MAX_BASELINE_AGE_DAYS] days old; null when there is none. Earlier refreshes on the same day are never used.
     */
    fun baseline(history: List<RiskSnapshot>, nowMillis: Long): RiskSnapshot? {
        val today = Dates.isoDateFromEpochMillis(nowMillis)
        val oldest = Dates.isoDateFromEpochMillis(nowMillis - MAX_BASELINE_AGE_DAYS * DAY_MILLIS)
        return history.filter { it.date < today && it.date >= oldest }.maxByOrNull { it.date }
    }
}
