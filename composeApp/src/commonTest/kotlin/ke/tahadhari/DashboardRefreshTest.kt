package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.mergeReload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRefreshTest {
    private val hour = 3_600_000L
    private val now = 1_793_099_100_000L
    private val kisumu = Counties.byCode(42)!!

    private fun ready(updated: Long, fromCache: Boolean = false, mm: List<Double> = Rain.moderate) =
        readyState(kisumu, mm, updatedAtMillis = updated, fromCache = fromCache)

    // ---- mergeReload ----

    @Test
    fun liveResultAlwaysWins() {
        val fresh = ready(now)
        assertSame(fresh, mergeReload(ready(now - hour), fresh, now))
        assertSame(fresh, mergeReload(null, fresh, now))
        assertSame(fresh, mergeReload(LoadState.Failed("x"), fresh, now))
    }

    @Test
    fun failedReloadKeepsGoodDataAndFlagsItSaved() {
        val old = ready(now - hour)
        val merged = mergeReload(old, LoadState.Failed("offline"), now)
        assertIs<LoadState.Ready>(merged)
        assertTrue(merged.fromCache)
        assertEquals(old.updatedAtMillis, merged.updatedAtMillis)
        assertEquals(old.assessment, merged.assessment)
    }

    @Test
    fun cachedReloadThatIsNotNewerKeepsPrevious() {
        val old = ready(now - hour)
        val merged = mergeReload(old, ready(now - 2 * hour, fromCache = true), now)
        assertEquals(old.updatedAtMillis, (merged as LoadState.Ready).updatedAtMillis)
        // A cached result that is newer than what we hold is better.
        val newer = ready(now - hour / 2, fromCache = true)
        assertSame(newer, mergeReload(old, newer, now))
    }

    @Test
    fun tooOldDataIsNotKeptWhenReloadFails() {
        val ancient = ready(now - 4 * 24 * hour)
        val failed = LoadState.Failed("offline")
        assertSame(failed, mergeReload(ancient, failed, now))
    }

    // ---- DashboardState ----

    private fun TestScopeState(
        scope: kotlinx.coroutines.CoroutineScope,
        interval: Long = 3 * hour,
        clock: () -> Long,
        loader: suspend (County) -> LoadState,
    ) = DashboardState(scope, listOf(kisumu, Counties.byCode(47)!!), loader, clock = clock, autoRefreshIntervalMillis = interval)

    @Test
    fun refreshKeepsRowsAndUpdatesInPlace() = runTest {
        var calls = 0
        val state = TestScopeState(backgroundScope, clock = { now + currentTime }) { c ->
            delay(1_000)
            calls++
            ready(now + currentTime, mm = if (calls > 2) Rain.severe else Rain.low)
        }
        state.refresh()
        assertTrue(state.refreshing)
        advanceTimeBy(1_001); runCurrent()
        assertFalse(state.refreshing)
        val firstLevels = state.states.values.map { (it as LoadState.Ready).assessment.level }

        state.refresh()
        // Rows still hold the previous data while the reload runs: nothing goes back to Loading.
        assertTrue(state.refreshing)
        assertEquals(2, state.states.values.count { it is LoadState.Ready })
        assertEquals(0, state.counts.getValue(ke.tahadhari.ui.dashboard.StatusFilter.Loading))
        advanceTimeBy(1_001); runCurrent()
        assertFalse(state.refreshing)
        assertEquals(2, state.refreshDone)
        assertTrue(state.states.values.map { (it as LoadState.Ready).assessment.level } != firstLevels)
    }

    @Test
    fun failedReloadDoesNotReplaceGoodData() = runTest {
        var fail = false
        val state = TestScopeState(backgroundScope, clock = { now + currentTime }) {
            if (fail) LoadState.Failed("offline") else ready(now)
        }
        state.refresh(); runCurrent()
        fail = true
        state.refresh(); runCurrent()
        state.states.values.forEach {
            assertIs<LoadState.Ready>(it)
            assertTrue(it.fromCache)
        }
    }

    @Test
    fun autoRefreshRunsEveryThreeHours() = runTest {
        var calls = 0
        val state = TestScopeState(backgroundScope, clock = { now + currentTime }) { calls++; ready(now) }
        backgroundScope.launch { state.autoRefreshLoop() }
        runCurrent()
        assertEquals(0, calls) // the first load is started by the app, then the timer begins
        advanceTimeBy(3 * hour - 1); runCurrent()
        assertEquals(0, calls)
        advanceTimeBy(2); runCurrent()
        assertEquals(2, calls) // two counties in this test
        advanceTimeBy(3 * hour); runCurrent()
        assertEquals(4, calls)
    }

    @Test
    fun autoRefreshStopsWhenTurnedOff() = runTest {
        var calls = 0
        val state = TestScopeState(backgroundScope, clock = { now + currentTime }) { calls++; ready(now) }
        backgroundScope.launch { state.autoRefreshLoop() }
        runCurrent()
        state.autoRefresh = false
        advanceTimeBy(10 * hour); runCurrent()
        assertEquals(0, calls)
    }

    @Test
    fun neverMoreThanSixRequestsAtOnce() = runTest {
        var running = 0
        var peak = 0
        val state = DashboardState(backgroundScope, Counties.all, { c ->
            running++; peak = maxOf(peak, running)
            delay(100)
            running--
            readyState(c, Rain.low, updatedAtMillis = now)
        }, clock = { now })
        state.refresh()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(6, peak)
        assertEquals(47, state.doneCount)
    }
}
