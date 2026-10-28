package ke.tahadhari.ui.dashboard

import ke.tahadhari.data.County
import ke.tahadhari.domain.Freshness
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.Trend
import ke.tahadhari.domain.trendFor
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.levelCounts

/** A summary-bar chip that filters the ranked list. */
sealed interface StatusFilter {
    data class Level(val level: RiskLevel) : StatusFilter
    data object Loading : StatusFilter
    data object Failed : StatusFilter
    /** Counties whose level rose since the baseline day (not saved between runs). */
    data object Rising : StatusFilter
}

/** Status of a county for filtering. A missing state counts as loading. */
fun LoadState?.matches(filter: StatusFilter, trend: Trend = Trend.Unknown): Boolean = when (filter) {
    is StatusFilter.Level -> this is LoadState.Ready && assessment.level == filter.level
    StatusFilter.Loading -> this == null || this is LoadState.Loading
    StatusFilter.Failed -> this is LoadState.Failed
    StatusFilter.Rising -> this is LoadState.Ready && trend is Trend.Rising
}

/** Number of counties per filter, for the summary bar. */
fun statusCounts(
    counties: List<County>,
    states: Map<Int, LoadState>,
    trends: Map<Int, Trend> = emptyMap(),
): Map<StatusFilter, Int> {
    val codes = counties.map { it.code }.toSet()
    val levels = states.filterKeys { it in codes }.levelCounts()
    return RiskLevel.entries.associate<RiskLevel, StatusFilter, Int> { StatusFilter.Level(it) to (levels[it] ?: 0) } +
        mapOf(
            StatusFilter.Loading to counties.count { states[it.code].matches(StatusFilter.Loading) },
            StatusFilter.Failed to counties.count { states[it.code].matches(StatusFilter.Failed) },
            StatusFilter.Rising to counties.count { states[it.code].matches(StatusFilter.Rising, trends[it.code] ?: Trend.Unknown) },
        )
}

/**
 * Triage order: counties with data first, highest level then wettest 3-day total; then failed
 * counties (no data is a reason to look, never "Low"); then those still loading. Name breaks ties.
 */
fun rankCounties(counties: List<County>, states: Map<Int, LoadState>): List<County> =
    counties.sortedWith(
        compareBy<County> {
            when (states[it.code]) {
                is LoadState.Ready -> 0
                is LoadState.Failed -> 1
                else -> 2
            }
        }
            .thenByDescending { (states[it.code] as? LoadState.Ready)?.assessment?.level?.ordinal ?: -1 }
            .thenByDescending { (states[it.code] as? LoadState.Ready)?.assessment?.maxThreeDayTotalMm ?: 0.0 }
            .thenBy { it.name },
    )

/** Case- and punctuation-insensitive match on county name, HQ town or code. Blank matches all. */
fun County.matchesQuery(query: String): Boolean {
    val q = normalise(query)
    if (q.isEmpty()) return true
    return normalise(name).contains(q) || normalise(town).contains(q) || code.toString() == query.trim()
}

private fun normalise(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

/** Ranked counties that match the search [query] and optional [filter]. */
fun visibleCounties(
    ranked: List<County>,
    states: Map<Int, LoadState>,
    query: String,
    filter: StatusFilter?,
    trends: Map<Int, Trend> = emptyMap(),
): List<County> = ranked.filter {
    it.matchesQuery(query) && (filter == null || states[it.code].matches(filter, trends[it.code] ?: Trend.Unknown))
}

/** Oldest download time across loaded counties: the honest "updated" time for the whole dashboard. */
fun oldestUpdatedAt(states: Map<Int, LoadState>): Long? =
    states.values.filterIsInstance<LoadState.Ready>().minOfOrNull { it.updatedAtMillis }

/** 1-based rank among counties with data, or null when the county has no data. */
fun rankOf(code: Int, ranked: List<County>, states: Map<Int, LoadState>): Int? {
    if (states[code] !is LoadState.Ready) return null
    return ranked.filter { states[it.code] is LoadState.Ready }.indexOfFirst { it.code == code } + 1
}

/**
 * Combines a county's [previous] state with the result of a reload ([incoming]) so a reload never
 * makes the dashboard worse. A fresh live result always wins. If the reload fell back to the saved
 * cache or failed, we keep the good data we already have (flagged `fromCache`, with its original
 * update time) unless it is too old to show (see `Freshness`), in which case the honest result wins.
 */
fun mergeReload(previous: LoadState?, incoming: LoadState, nowMillis: Long): LoadState {
    if (previous !is LoadState.Ready) return incoming
    val keepPrevious = previous.copy(fromCache = true)
    val previousUsable = !Freshness.isTooOldToShow(previous.updatedAtMillis, nowMillis)
    return when {
        incoming is LoadState.Ready && !incoming.fromCache -> incoming
        incoming is LoadState.Ready ->
            if (incoming.updatedAtMillis > previous.updatedAtMillis || !previousUsable) incoming else keepPrevious
        incoming is LoadState.Failed -> if (previousUsable) keepPrevious else incoming
        else -> incoming
    }
}

/** Trend per county: current level (only for counties with data) against the [baseline] snapshot. */
fun computeTrends(counties: List<County>, states: Map<Int, LoadState>, baseline: Map<Int, RiskLevel>?): Map<Int, Trend> {
    if (baseline == null) return emptyMap()
    return counties.associate { c ->
        c.code to trendFor((states[c.code] as? LoadState.Ready)?.assessment?.level, baseline[c.code])
    }
}

/** Levels of counties whose data is fresh (live download, not the saved cache). Never includes failed or cached ones. */
fun freshLevels(counties: List<County>, states: Map<Int, LoadState>): Map<Int, RiskLevel> =
    counties.mapNotNull { c ->
        (states[c.code] as? LoadState.Ready)?.takeIf { !it.fromCache }?.let { c.code to it.assessment.level }
    }.toMap()
