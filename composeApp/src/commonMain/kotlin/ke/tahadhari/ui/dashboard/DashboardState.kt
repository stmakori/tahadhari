package ke.tahadhari.ui.dashboard

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.loadCounty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * State holder for the desktop dashboard: per-county load states, selection, search and filter.
 * Network work runs in [scope] (from `rememberCoroutineScope()`), never in composition.
 *
 * @param loader loads one county; risk always comes from RiskEngine inside it. Tests pass a fake.
 */
@Stable
class DashboardState(
    private val scope: CoroutineScope,
    val counties: List<County> = Counties.all,
    private val loader: suspend (County) -> LoadState = ::loadCounty,
    initialLanguage: Language = Language.ENGLISH,
) {
    val states: SnapshotStateMap<Int, LoadState> = mutableStateMapOf()
    var language by mutableStateOf(initialLanguage)
    var selectedCode by mutableStateOf<Int?>(null)
    var query by mutableStateOf("")
    var filter by mutableStateOf<StatusFilter?>(null)
    /** Narrow windows only: show the map instead of the list in the left pane. */
    var showMapTab by mutableStateOf(false)

    val ranked: List<County> by derivedStateOf { rankCounties(counties, states) }
    val visible: List<County> by derivedStateOf { visibleCounties(ranked, states, query, filter) }
    val counts: Map<StatusFilter, Int> by derivedStateOf { statusCounts(counties, states) }
    val doneCount: Int by derivedStateOf { counties.count { states[it.code].let { s -> s != null && s !is LoadState.Loading } } }
    val oldestUpdate: Long? by derivedStateOf { oldestUpdatedAt(states) }

    private var refreshJob: Job? = null

    /** Reloads every county, at most 6 requests at a time to be polite to the free API. */
    fun refresh() {
        refreshJob?.cancel()
        states.clear()
        counties.forEach { states[it.code] = LoadState.Loading }
        refreshJob = scope.launch {
            val limit = Semaphore(6)
            counties.forEach { county ->
                launch { limit.withPermit { states[county.code] = loader(county) } }
            }
        }
    }

    /** Reloads one county (from its error state), sharing nothing with a full refresh. */
    fun retry(code: Int) {
        val county = counties.firstOrNull { it.code == code } ?: return
        states[code] = LoadState.Loading
        scope.launch { states[code] = loader(county) }
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
