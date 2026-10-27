package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.StatusFilter
import ke.tahadhari.ui.dashboard.matchesQuery
import ke.tahadhari.ui.dashboard.oldestUpdatedAt
import ke.tahadhari.ui.dashboard.rankCounties
import ke.tahadhari.ui.dashboard.statusCounts
import ke.tahadhari.ui.dashboard.visibleCounties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DashboardLogicTest {
    private val c = { code: Int -> Counties.byCode(code)!! }
    private val counties = listOf(c(1), c(4), c(21), c(42), c(47), c(9))
    private val states: Map<Int, LoadState> = mapOf(
        1 to readyState(c(1), Rain.low, updatedAtMillis = 3_000),
        4 to readyState(c(4), Rain.severe, updatedAtMillis = 1_000),
        21 to readyState(c(21), Rain.high, updatedAtMillis = 2_000),
        42 to LoadState.Failed("offline"),
        47 to readyState(c(47), Rain.high.map { it * 1.05 }, updatedAtMillis = 5_000),
        9 to LoadState.Loading,
    )

    @Test
    fun ranksByLevelThenThreeDayTotalThenFailedThenLoading() {
        assertEquals(listOf(4, 47, 21, 1, 42, 9), rankCounties(counties, states).map { it.code })
    }

    @Test
    fun failedCountyIsNeverCountedAsLow() {
        val counts = statusCounts(counties, states)
        assertEquals(1, counts[StatusFilter.Level(RiskLevel.LOW)])
        assertEquals(2, counts[StatusFilter.Level(RiskLevel.HIGH)])
        assertEquals(1, counts[StatusFilter.Level(RiskLevel.SEVERE)])
        assertEquals(0, counts[StatusFilter.Level(RiskLevel.MODERATE)])
        assertEquals(1, counts[StatusFilter.Failed])
        assertEquals(1, counts[StatusFilter.Loading])
    }

    @Test
    fun missingStateCountsAsLoading() {
        val counts = statusCounts(counties, emptyMap())
        assertEquals(counties.size, counts[StatusFilter.Loading])
        assertEquals(0, counts[StatusFilter.Level(RiskLevel.LOW)])
    }

    @Test
    fun filtersBySearchAndChip() {
        val ranked = rankCounties(counties, states)
        assertEquals(listOf(21), visibleCounties(ranked, states, "muranga", null).map { it.code })
        assertEquals(listOf(4), visibleCounties(ranked, states, "hola", null).map { it.code }) // HQ town
        assertEquals(listOf(47, 21), visibleCounties(ranked, states, "", StatusFilter.Level(RiskLevel.HIGH)).map { it.code })
        assertEquals(listOf(42), visibleCounties(ranked, states, "", StatusFilter.Failed).map { it.code })
        assertTrue(visibleCounties(ranked, states, "kisumu", StatusFilter.Level(RiskLevel.LOW)).isEmpty())
        assertTrue(c(6).matchesQuery("taita taveta"))
        assertTrue(c(43).matchesQuery("HOMA"))
    }

    @Test
    fun oldestUpdateIgnoresFailedAndLoading() {
        assertEquals(1_000L, oldestUpdatedAt(states))
        assertNull(oldestUpdatedAt(mapOf(42 to LoadState.Failed("x"))))
    }
}
