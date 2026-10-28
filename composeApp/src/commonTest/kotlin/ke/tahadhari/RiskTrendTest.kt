package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.RiskSnapshotStore
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskSnapshot
import ke.tahadhari.domain.SnapshotHistory
import ke.tahadhari.domain.Trend
import ke.tahadhari.domain.trendFor
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.StatusFilter
import ke.tahadhari.ui.dashboard.computeTrends
import ke.tahadhari.ui.dashboard.freshLevels
import ke.tahadhari.ui.dashboard.statusCounts
import ke.tahadhari.ui.dashboard.visibleCounties
import ke.tahadhari.ui.dashboard.rankCounties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RiskTrendTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    // 2026-10-27T14:05+03:00
    private val now = 1_793_099_100_000L
    private val nowDate = "2026-10-27"

    private val L = RiskLevel.LOW
    private val M = RiskLevel.MODERATE
    private val H = RiskLevel.HIGH

    @Test
    fun trendForComparesLevels() {
        assertEquals(Trend.Rising(M), trendFor(H, M))
        assertEquals(Trend.Falling(H), trendFor(L, H))
        assertEquals(Trend.Same, trendFor(M, M))
        assertEquals(Trend.Unknown, trendFor(M, null))
        assertEquals(Trend.Unknown, trendFor(null, M))
        assertEquals(Trend.Unknown, trendFor(null, null))
    }

    @Test
    fun baselineIsLatestSnapshotFromAnEarlierDay() {
        val history = listOf(
            RiskSnapshot("2026-10-27", mapOf(1 to H)),
            RiskSnapshot("2026-10-26", mapOf(1 to M)),
            RiskSnapshot("2026-10-25", mapOf(1 to L)),
        )
        assertEquals("2026-10-26", SnapshotHistory.baseline(history, now)?.date)
    }

    @Test
    fun baselineIgnoresSameDayAndNoBaselineIsNull() {
        assertNull(SnapshotHistory.baseline(emptyList(), now))
        assertNull(SnapshotHistory.baseline(listOf(RiskSnapshot(nowDate, mapOf(1 to H))), now))
        // A snapshot dated in the future (clock change) is not a baseline either.
        assertNull(SnapshotHistory.baseline(listOf(RiskSnapshot("2026-10-28", mapOf(1 to H))), now))
        // Too old to be "since yesterday".
        assertNull(SnapshotHistory.baseline(listOf(RiskSnapshot("2026-10-20", mapOf(1 to H))), now))
    }

    @Test
    fun dateBoundaryUsesNairobiTimeNotUtc() {
        // 22:30 UTC on the 27th is already 01:30 on the 28th in Nairobi.
        val lateUtc = 1_793_140_200_000L
        val history = listOf(RiskSnapshot("2026-10-27", mapOf(1 to M)))
        assertEquals("2026-10-27", SnapshotHistory.baseline(history, lateUtc)?.date)
        // 20:59 UTC on the 27th = 23:59 in Nairobi: still the same day, so no baseline.
        assertNull(SnapshotHistory.baseline(history, 1_793_134_740_000L))
    }

    @Test
    fun recordKeepsOnePerDayAndPrunesToThreeDays() {
        var h = emptyList<RiskSnapshot>()
        for (i in 4 downTo 0) h = SnapshotHistory.record(h, mapOf(1 to L), now - i * day)
        assertEquals(listOf("2026-10-27", "2026-10-26", "2026-10-25"), h.map { it.date })
        // A second refresh on the same day replaces that day's levels rather than adding a snapshot.
        h = SnapshotHistory.record(h, mapOf(1 to H), now + hour)
        assertEquals(3, h.size)
        assertEquals(H, h.first().levels[1])
    }

    @Test
    fun recordMergesSameDayAndIgnoresEmptyFreshMap() {
        val h = SnapshotHistory.record(emptyList(), mapOf(1 to L, 2 to M), now)
        val h2 = SnapshotHistory.record(h, mapOf(2 to H), now + hour)
        assertEquals(mapOf(1 to L, 2 to H), h2.single().levels)
        assertEquals(h2, SnapshotHistory.record(h2, emptyMap(), now + 2 * hour))
        assertTrue(SnapshotHistory.record(emptyList(), emptyMap(), now).isEmpty())
    }

    @Test
    fun recordDropsFutureSnapshots() {
        val h = SnapshotHistory.record(listOf(RiskSnapshot("2027-01-01", mapOf(1 to H))), mapOf(1 to L), now)
        assertEquals(listOf(nowDate), h.map { it.date })
    }

    // ---- freshness and trends over dashboard states ----

    private val kisumu = Counties.byCode(42)!!
    private val nairobi = Counties.byCode(47)!!
    private val mombasa = Counties.byCode(1)!!
    private val all = listOf(kisumu, nairobi, mombasa)

    @Test
    fun onlyFreshReadyCountiesAreSnapshotted() {
        val states = mapOf(
            42 to readyState(kisumu, Rain.high, updatedAtMillis = now),
            47 to readyState(nairobi, Rain.high, updatedAtMillis = now, fromCache = true),
            1 to LoadState.Failed("offline"),
        )
        assertEquals(mapOf(42 to H), freshLevels(all, states))
        assertTrue(freshLevels(all, emptyMap()).isEmpty())
    }

    private fun trendStates() = mapOf(
        42 to readyState(kisumu, Rain.high, updatedAtMillis = now),
        47 to readyState(nairobi, Rain.low, updatedAtMillis = now),
        1 to LoadState.Failed("offline"),
    )

    @Test
    fun trendsPerCountyAndUnknownWithoutBaselineOrData() {
        val t = computeTrends(all, trendStates(), mapOf(42 to M, 47 to M, 1 to M))
        assertEquals(Trend.Rising(M), t[42])
        assertEquals(Trend.Falling(M), t[47])
        assertEquals(Trend.Unknown, t[1]) // failed: never a trend
        assertTrue(computeTrends(all, trendStates(), null).isEmpty())
        assertEquals(Trend.Unknown, computeTrends(all, trendStates(), mapOf(42 to M))[47])
    }

    @Test
    fun risingFilterAndCount() {
        val states = trendStates()
        val trends = computeTrends(all, states, mapOf(42 to M, 47 to M))
        val counts = statusCounts(all, states, trends)
        assertEquals(1, counts[StatusFilter.Rising])
        val shown = visibleCounties(rankCounties(all, states), states, "", StatusFilter.Rising, trends)
        assertEquals(listOf(42), shown.map { it.code })
        assertEquals(0, statusCounts(all, states)[StatusFilter.Rising])
    }

    // ---- store ----

    @Test
    fun storeRoundTripsAndToleratesCorruptData() = runTest {
        val kv = InMemoryKeyValueStore()
        val store = RiskSnapshotStore(kv)
        assertTrue(store.load().isEmpty())
        val h = listOf(RiskSnapshot("2026-10-27", mapOf(42 to H)), RiskSnapshot("2026-10-26", mapOf(42 to M)))
        store.save(h)
        assertEquals(h, store.load())
        kv.put("risk_snapshots", "not json")
        assertTrue(store.load().isEmpty())
        kv.put("risk_snapshots", """{"days":[{"date":"bad","levels":{"42":"HIGH"}},{"date":"2026-10-27","levels":{"42":"NOPE","9999":"LOW","x":"LOW","1":"SEVERE"}}]}""")
        assertEquals(listOf(RiskSnapshot("2026-10-27", mapOf(1 to RiskLevel.SEVERE))), store.load())
    }

    // ---- DashboardState end to end ----

    @Test
    fun refreshSavesSnapshotAndNextDayShowsTrend() = runTest {
        val store = RiskSnapshotStore(InMemoryKeyValueStore())
        var clock = now
        var rain = Rain.moderate
        var fail = false
        val state = DashboardState(
            backgroundScope, listOf(kisumu, nairobi),
            loader = { c -> if (fail) LoadState.Failed("offline") else readyState(c, rain, updatedAtMillis = clock) },
            clock = { clock }, snapshotStore = store,
        )
        state.loadSnapshots()
        state.refresh(); runCurrent()
        assertEquals(nowDate, store.load().single().date)
        assertTrue(state.trends.isEmpty()) // no earlier day yet

        // Same day, higher rain: still no baseline (not the previous 3-hourly refresh).
        rain = Rain.severe; clock += 3 * hour
        state.refresh(); runCurrent()
        assertTrue(state.trends.isEmpty())
        assertEquals(1, store.load().size)

        // Next day: compared with the latest snapshot of the earlier day (SEVERE), rain drops.
        rain = Rain.low; clock += day
        state.refresh(); runCurrent()
        assertEquals(Trend.Falling(RiskLevel.SEVERE), state.trends[42])
        assertEquals(2, store.load().size)

        // A failed reload keeps cached data and saves nothing new (today's snapshot stays LOW).
        fail = true; clock += 3 * hour
        state.refresh(); runCurrent()
        assertEquals(RiskLevel.LOW, store.load().first().levels[42])
    }

    @Test
    fun failedOrCachedRefreshNeverSavesASnapshot() = runTest {
        val store = RiskSnapshotStore(InMemoryKeyValueStore())
        val state = DashboardState(
            backgroundScope, listOf(kisumu, nairobi),
            loader = { c -> if (c.code == 42) LoadState.Failed("x") else readyState(c, Rain.high, updatedAtMillis = now, fromCache = true) },
            clock = { now }, snapshotStore = store,
        )
        state.refresh(); runCurrent()
        assertTrue(store.load().isEmpty())
    }
}
