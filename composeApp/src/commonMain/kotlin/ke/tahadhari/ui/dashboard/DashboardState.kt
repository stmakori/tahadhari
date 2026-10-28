package ke.tahadhari.ui.dashboard

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import ke.tahadhari.AppGraph
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.data.RiskSnapshotStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskSnapshot
import ke.tahadhari.domain.SnapshotHistory
import ke.tahadhari.domain.Trend
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.loadCounties
import ke.tahadhari.ui.loadCounty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** How often the dashboard reloads by itself while open. */
const val AUTO_REFRESH_INTERVAL_MILLIS: Long = 3L * 60 * 60 * 1000

/**
 * State holder for the desktop dashboard: per-county load states, selection, search and filter.
 * Network work runs in [scope] (from `rememberCoroutineScope()`), never in composition.
 *
 * @param loader loads one county (used by [retry], and by refresh when there is no [batchLoader]);
 *   risk always comes from RiskEngine inside it. Tests pass a fake.
 * @param batchLoader loads many counties with a few batched requests (used by a full [refresh]).
 * @param clock current time in epoch millis; tests pass a fake.
 * @param autoRefreshIntervalMillis delay between automatic reloads (see [autoRefreshLoop]).
 * @param snapshotStore where the daily risk snapshots for "trend since yesterday" are kept; null (tests) keeps them in memory only.
 */
@Stable
class DashboardState(
    private val scope: CoroutineScope,
    val counties: List<County> = Counties.all,
    loader: (suspend (County) -> LoadState)? = null,
    initialLanguage: Language = Language.ENGLISH,
    private val clock: () -> Long = { AppGraph.nowMillis() },
    private val autoRefreshIntervalMillis: Long = AUTO_REFRESH_INTERVAL_MILLIS,
    batchLoader: (suspend (List<County>) -> Map<Int, LoadState>)? = null,
    private val snapshotStore: RiskSnapshotStore? = null,
) {
    // With neither loader given, the app's real ones are used (batch for refresh, single for retry).
    // A custom single-county [loader] without a [batchLoader] keeps the per-county refresh (tests).
    private val loader: suspend (County) -> LoadState = loader ?: { loadCounty(it) }
    private val batchLoader: (suspend (List<County>) -> Map<Int, LoadState>)? =
        batchLoader ?: if (loader == null) ::loadCounties else null

    val states: SnapshotStateMap<Int, LoadState> = mutableStateMapOf()
    var language by mutableStateOf(initialLanguage)
    var selectedCode by mutableStateOf<Int?>(null)
    var query by mutableStateOf("")
    var filter by mutableStateOf<StatusFilter?>(null)
    /** Narrow windows only: show the map instead of the list in the left pane. */
    var showMapTab by mutableStateOf(false)
    /** Reload everything every [autoRefreshIntervalMillis] while the dashboard is open. */
    var autoRefresh by mutableStateOf(true)

    /** True while a full reload is running (the UI shows a small progress indicator, rows stay as they are). */
    var refreshing by mutableStateOf(false)
        private set
    /** Counties finished in the current reload, for "Refreshing: 12 of 47". */
    var refreshDone by mutableIntStateOf(0)
        private set

    val ranked: List<County> by derivedStateOf { rankCounties(counties, states) }
    val visible: List<County> by derivedStateOf { visibleCounties(ranked, states, query, filter, trends) }
    val counts: Map<StatusFilter, Int> by derivedStateOf { statusCounts(counties, states, trends) }
    val doneCount: Int by derivedStateOf { counties.count { states[it.code].let { s -> s != null && s !is LoadState.Loading } } }
    val oldestUpdate: Long? by derivedStateOf { oldestUpdatedAt(states) }

    /** Daily snapshots, newest first (see [SnapshotHistory]). */
    private var history by mutableStateOf<List<RiskSnapshot>>(emptyList())
    /** Most recent snapshot from an earlier Nairobi day, or null: then no trend is shown. */
    private var baseline by mutableStateOf<RiskSnapshot?>(null)
    /** Level change per county since [baseline]. Empty without a baseline. */
    val trends: Map<Int, Trend> by derivedStateOf { computeTrends(counties, states, baseline?.levels) }

    /** What is saved between runs. */
    val settings: DashboardSettings
        get() = DashboardSettings(language.name, selectedCode, DashboardSettings.filterKey(filter), autoRefresh)

    /** Applies saved [settings] (unknown values were already ignored by the accessors). */
    fun restore(settings: DashboardSettings) {
        language = settings.languageOrDefault()
        selectedCode = settings.selectedCodeOrNull()
        filter = settings.filterOrNull()
        autoRefresh = settings.autoRefresh
    }

    /** Loads the saved snapshots (call once before the first [refresh]). */
    suspend fun loadSnapshots() {
        history = snapshotStore?.load().orEmpty()
        baseline = SnapshotHistory.baseline(history, clock())
    }

    /**
     * Saves a snapshot after a COMPLETED full reload, from counties whose data is fresh (live, not cached).
     * Cached, stale, failed and still-loading counties are left out; nothing is saved if none is fresh.
     */
    private suspend fun recordSnapshot() {
        val fresh = freshLevels(counties, states)
        if (fresh.isEmpty()) return
        val now = clock()
        val stored = snapshotStore?.load() ?: history
        val updated = SnapshotHistory.record(stored, fresh, now)
        history = updated
        baseline = SnapshotHistory.baseline(updated, now)
        snapshotStore?.save(updated)
    }

    private var refreshJob: Job? = null
    private var generation = 0

    /**
     * Reloads every county. With a batch loader (the default in the app) that is about 2 HTTP calls
     * in total; otherwise one call per county, at most 6 at a time to be polite to the free API.
     * Non-blanking: rows keep their previous data and update in place as each county arrives.
     * Only counties with no state yet show "loading". A reload that falls back to the cache or
     * fails never replaces good data (see [mergeReload]).
     */
    fun refresh() {
        refreshJob?.cancel()
        val gen = ++generation
        refreshing = true
        refreshDone = 0
        counties.forEach { if (states[it.code] == null) states[it.code] = LoadState.Loading }
        refreshJob = scope.launch {
            try {
                val batch = batchLoader
                if (batch != null) refreshInBatch(batch) else refreshOneByOne()
            } finally {
                if (gen == generation) refreshing = false
            }
        }
    }

    private suspend fun refreshInBatch(batch: suspend (List<County>) -> Map<Int, LoadState>) {
        val results = try {
            batch(counties)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
        counties.forEach { county ->
            val result = results[county.code] ?: LoadState.Failed("No result")
            states[county.code] = mergeReload(states[county.code], result, clock())
            refreshDone++
        }
        recordSnapshot()
    }

    private suspend fun refreshOneByOne() {
        coroutineScope {
            val limit = Semaphore(6)
            counties.map { county ->
                launch {
                    limit.withPermit {
                        val result = loader(county)
                        states[county.code] = mergeReload(states[county.code], result, clock())
                        refreshDone++
                    }
                }
            }.forEach { it.join() }
        }
        recordSnapshot()
    }

    /**
     * Automatic reloads: waits [autoRefreshIntervalMillis], reloads, repeats, for as long as
     * [autoRefresh] is on. Run it from a `LaunchedEffect(autoRefresh)` so it restarts when toggled.
     */
    suspend fun autoRefreshLoop() {
        while (autoRefresh) {
            delay(autoRefreshIntervalMillis)
            if (autoRefresh) refresh()
        }
    }

    /** Reloads one county (from its error state), sharing nothing with a full refresh. */
    fun retry(code: Int) {
        val county = counties.firstOrNull { it.code == code } ?: return
        val previous = states[code]
        states[code] = LoadState.Loading
        scope.launch { states[code] = mergeReload(previous, loader(county), clock()) }
    }

    fun toggleFilter(f: StatusFilter) {
        filter = if (filter == f) null else f
    }

    /** Moves the selection by [delta] rows within the visible list (arrow keys). */
    fun moveSelection(delta: Int) {
        val list = visible
        if (list.isEmpty()) return
        val i = list.indexOfFirst { it.code == selectedCode }
        val next = if (i < 0) (if (delta > 0) 0 else list.lastIndex) else (i + delta).coerceIn(0, list.lastIndex)
        selectedCode = list[next].code
    }
}
