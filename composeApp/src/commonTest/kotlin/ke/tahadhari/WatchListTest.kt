package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.Preferences
import ke.tahadhari.data.WatchList
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.decideRiskAlert
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.mobile.RiskAlertState
import ke.tahadhari.ui.mobile.checkRiskAlerts
import ke.tahadhari.ui.mobile.watchCodesToLoad
import ke.tahadhari.ui.mobile.watchStateOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private const val T0 = 1_793_000_000_000L

private fun readyState(code: Int, peakMm: Double, fromCache: Boolean = false): LoadState.Ready {
    val county = Counties.byCode(code)!!
    val mm = listOf(4.0, 6.0, 8.0, peakMm, peakMm / 2, 9.0, 0.5)
    val dates = listOf("2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06", "2026-11-07", "2026-11-08")
    val forecast = CountyForecast(county, mm.indices.map { DailyRain(dates[it], mm[it], 60) }, null, "2026-11-02")
    return LoadState.Ready(forecast, RiskEngine().assess(forecast), T0, fromCache)
}

class WatchListTest {
    @Test fun sanitizeDropsUnknownDuplicatesOwnAndCapsAtThree() {
        assertEquals(listOf(4, 7, 33), WatchList.sanitize(listOf(4, 999, 4, 0, -1, 12, 7, 33, 5), ownCode = 12))
    }

    @Test fun addRespectsCapOwnAndDuplicates() {
        var list = emptyList<Int>()
        list = WatchList.add(list, 4, ownCode = 1)
        list = WatchList.add(list, 4, ownCode = 1) // duplicate
        list = WatchList.add(list, 1, ownCode = 1) // own county
        list = WatchList.add(list, 500, ownCode = 1) // unknown
        assertEquals(listOf(4), list)
        list = WatchList.add(WatchList.add(WatchList.add(list, 5, 1), 6, 1), 7, 1) // 4th is refused
        assertEquals(listOf(4, 5, 6), list)
        assertFalse(WatchList.canAdd(list, 7, 1))
        assertEquals(listOf(5, 6), WatchList.remove(list, 4))
        assertTrue(WatchList.canAdd(WatchList.remove(list, 4), 7, 1))
    }

    @Test fun parseSkipsGarbage() {
        assertEquals(listOf(4, 33), WatchList.parse("4, x,,33,4", null))
        assertEquals(emptyList(), WatchList.parse("", null))
        assertEquals("4,33", WatchList.encode(listOf(4, 33)))
    }
}

class WatchPreferencesTest {
    @Test fun roundTripsAndKeepsOtherChoices() = runTest {
        val prefs = Preferences(InMemoryKeyValueStore())
        prefs.setCounty(1)
        prefs.setLanguage(Language.SWAHILI)
        prefs.setWatchCodes(listOf(4, 33))
        val loaded = prefs.load()
        assertEquals(listOf(4, 33), loaded.watchCodes)
        assertEquals(1, loaded.countyCode)
        assertEquals(Language.SWAHILI, loaded.language)
    }

    @Test fun storedTextIsUntrusted() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("preferences", """{"countyCode":4,"watchCodes":[4,999,"7",33,33,null,2.5,12,13,14]}""")
        assertEquals(listOf(33, 12, 13), Preferences(store).load().watchCodes) // no own, unknown, string, null, duplicate; max 3
    }

    @Test fun wrongTypeOrMissingMeansEmptyAndKeepsCounty() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("preferences", """{"countyCode":4,"watchCodes":"4,5"}""")
        assertEquals(UserPrefsCheck(4, emptyList()), Preferences(store).load().let { UserPrefsCheck(it.countyCode, it.watchCodes) })
        store.put("preferences", """{"countyCode":4}""") // saved by an older version
        assertEquals(emptyList(), Preferences(store).load().watchCodes)
    }

    @Test fun makingAWatchedCountyYourOwnRemovesItFromTheList() = runTest {
        val prefs = Preferences(InMemoryKeyValueStore())
        prefs.setWatchCodes(listOf(4, 33))
        prefs.setCounty(33)
        assertEquals(listOf(4), prefs.load().watchCodes)
        prefs.setWatchCodes(listOf(33, 5)) // own county can't be added
        assertEquals(listOf(5), prefs.load().watchCodes)
    }

    private data class UserPrefsCheck(val county: Int?, val watch: List<Int>)
}

class MultiCountyAlertTest {
    private val own = Counties.byCode(1)!!
    private val a = Counties.byCode(4)!!
    private val b = Counties.byCode(33)!!

    @Test fun decisionsAreIndependentPerCounty() {
        // Each county carries its own "last notified" level into decideRiskAlert.
        val da = decideRiskAlert(null, RiskLevel.HIGH, fresh = true)
        val db = decideRiskAlert(RiskLevel.SEVERE, RiskLevel.SEVERE, fresh = true)
        assertTrue(da.notify)
        assertFalse(db.notify)
    }

    @Test fun oneNotificationPerCountyPerRiseWithCountyName() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        val all = listOf(own, a, b)
        val loaded = mapOf(own.code to readyState(own.code, 5.0), a.code to readyState(a.code, 60.0), b.code to readyState(b.code, 99.0))
        val first = checkRiskAlerts(all, loaded, Language.ENGLISH, state)
        assertEquals(listOf(a.code, b.code), first.map { it.first.code })
        assertTrue(first.all { (c, n) -> c.name in n.title }, first.toString())
        assertTrue(checkRiskAlerts(all, loaded, Language.ENGLISH, state).isEmpty()) // same levels: nothing again
        // a rises High -> Severe: only a notifies; b stays quiet
        val worse = loaded + (a.code to readyState(a.code, 99.0))
        assertEquals(listOf(a.code), checkRiskAlerts(all, worse, Language.ENGLISH, state).map { it.first.code })
        // b drops to Low (memory cleared), then rises again: b notifies, a does not
        val lowB = worse + (b.code to readyState(b.code, 5.0))
        assertTrue(checkRiskAlerts(all, lowB, Language.ENGLISH, state).isEmpty())
        assertEquals(listOf(b.code), checkRiskAlerts(all, worse, Language.ENGLISH, state).map { it.first.code })
    }

    @Test fun failedCachedOrMissingNeverNotifyNorResetOtherCounties() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        val all = listOf(own, a, b)
        checkRiskAlerts(all, mapOf(a.code to readyState(a.code, 60.0)), Language.ENGLISH, state)
        assertEquals(RiskLevel.HIGH, state.lastNotified(a))
        val result = checkRiskAlerts(
            all,
            mapOf(a.code to LoadState.Failed("offline"), b.code to readyState(b.code, 99.0, fromCache = true)), // own is missing
            Language.ENGLISH, state,
        )
        assertTrue(result.isEmpty())
        assertEquals(RiskLevel.HIGH, state.lastNotified(a)) // failure did not reset
        assertNull(state.lastNotified(b)) // cached data did not set
        assertNull(state.lastNotified(own))
    }

    @Test fun unfollowedCountyIsForgottenSoFollowingAgainStartsFresh() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        val loaded = mapOf(a.code to readyState(a.code, 60.0), own.code to readyState(own.code, 5.0))
        assertEquals(1, checkRiskAlerts(listOf(own, a), loaded, Language.ENGLISH, state).size)
        checkRiskAlerts(listOf(own), loaded, Language.ENGLISH, state) // a no longer watched
        assertNull(state.lastNotified(a))
        assertEquals(1, checkRiskAlerts(listOf(own, a), loaded, Language.ENGLISH, state).size)
    }

    @Test fun olderSingleCountyStateStillReads() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("risk-alert-state", "4:HIGH")
        assertEquals(RiskLevel.HIGH, RiskAlertState(store).lastNotified(a))
        store.put("risk-alert-state", "garbage,4:NOPE,x:HIGH")
        assertNull(RiskAlertState(store).lastNotified(a))
    }
}

class WatchStateLogicTest {
    @Test fun missingResultIsFailedNeverALevel() {
        assertIs<LoadState.Failed>(watchStateOf(4, emptyMap()))
        val ready = readyState(4, 5.0)
        assertEquals(ready, watchStateOf(4, mapOf(4 to ready)))
    }

    @Test fun refreshReloadsAllOtherwiseOnlyWhatIsMissing() {
        val ready = readyState(4, 5.0)
        val states = mapOf(4 to ready, 5 to LoadState.Loading)
        assertEquals(listOf(4, 5, 6), watchCodesToLoad(listOf(4, 5, 6), states, refreshKey = 1, loadedRefreshKey = 0))
        assertEquals(listOf(5, 6), watchCodesToLoad(listOf(4, 5, 6), states, refreshKey = 1, loadedRefreshKey = 1))
        assertEquals(listOf(4, 5, 6), watchCodesToLoad(listOf(4, 5, 6), emptyMap(), refreshKey = 0, loadedRefreshKey = -1))
    }

    @Test fun failedRowIsNotReloadedUntilRetryOrRefresh() {
        val states = mapOf<Int, LoadState>(4 to LoadState.Failed("x"))
        assertTrue(watchCodesToLoad(listOf(4), states, 1, 1).isEmpty())
    }
}
