package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.data.Preferences
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.data.remote.FloodDaily
import ke.tahadhari.data.remote.FloodResponse
import ke.tahadhari.data.remote.ForecastApi
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.data.remote.RainfallResponse
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.loadCounty
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeApi : ForecastApi {
    var rain: RainfallDaily? = null
    var flood: FloodDaily? = null
    var rainCalls = 0

    override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse {
        rainCalls++
        return RainfallResponse(rain ?: throw RuntimeException("offline"))
    }

    override suspend fun riverDischarge(point: GeoPoint, pastDays: Int, forecastDays: Int): FloodResponse =
        FloodResponse(flood ?: throw RuntimeException("flood offline"))
}

private fun week(start: Int, vararg mm: Double) = RainfallDaily(
    time = mm.indices.map { "2026-10-${start + it}" },
    precipitationSum = mm.toList(),
    precipitationProbabilityMax = mm.map { 80 },
)

class ForecastCacheTest {
    /** 2026-10-27 09:00 Nairobi. */
    private val t0 = 1_793_080_800_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    private val nairobi = Counties.byCode(47)!!
    private val tanaRiver = Counties.all.first { it.riverPoint != null }
    private val engine = RiskEngine()

    private var now = t0
    private val api = FakeApi()
    private val store = InMemoryKeyValueStore()
    private val cache = ForecastCache(store)
    private val repo = ForecastRepository(api, cache) { now }

    @Test fun missWithoutCacheRethrows() = runTest {
        assertFailsWith<RuntimeException> { repo.forecastFor(nairobi) }
        val state = loadCounty(nairobi, repo, engine, now)
        assertIs<LoadState.Failed>(state)
        assertNull(state.staleSinceMillis)
    }

    @Test fun freshFetchIsSavedAndNotFromCache() = runTest {
        api.rain = week(27, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val result = repo.forecastFor(nairobi)
        assertFalse(result.fromCache)
        assertEquals(t0, result.fetchedAtMillis)
        assertEquals(7, result.forecast.days.size)
        val saved = assertNotNull(cache.read(nairobi.code))
        assertEquals(t0, saved.fetchedAtMillis)

        val state = assertIs<LoadState.Ready>(loadCounty(nairobi, repo, engine, now))
        assertEquals(RiskLevel.HIGH, state.assessment.level)
        assertFalse(state.fromCache)
        assertEquals(t0, state.updatedAtMillis)
    }

    @Test fun networkFailureServesCacheMarkedStaleWithItsTime() = runTest {
        api.rain = week(27, 0.0, 55.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        repo.forecastFor(nairobi)

        api.rain = null
        now = t0 + 5 * hour
        val state = assertIs<LoadState.Ready>(loadCounty(nairobi, repo, engine, now))
        assertTrue(state.fromCache)
        assertEquals(t0, state.updatedAtMillis)
        assertEquals(RiskLevel.HIGH, state.assessment.level) // recomputed from cached raw data
    }

    @Test fun staleCacheDropsDaysAlreadyPast() = runTest {
        api.rain = week(27, 90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        repo.forecastFor(nairobi)

        api.rain = null
        now = t0 + 2 * day // 29 Oct: the 90 mm day on the 27th is history, not a forecast
        val state = assertIs<LoadState.Ready>(loadCounty(nairobi, repo, engine, now))
        assertEquals("2026-10-29", state.forecast.days.first().date)
        assertEquals(5, state.forecast.days.size)
        assertEquals(RiskLevel.LOW, state.assessment.level)
        assertEquals("2026-10-27", state.forecast.fetchedAtDate)
    }

    @Test fun cacheOlderThanPolicyBecomesFailedWithDate() = runTest {
        api.rain = week(27, 90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        repo.forecastFor(nairobi)

        api.rain = null
        now = t0 + 3 * day + hour
        val state = assertIs<LoadState.Failed>(loadCounty(nairobi, repo, engine, now))
        assertEquals(t0, state.staleSinceMillis)
        assertTrue("2026-10-27" in state.message)
    }

    @Test fun corruptCacheIsIgnored() = runTest {
        store.put("forecast-${nairobi.code}", "{not json")
        assertNull(cache.read(nairobi.code))
        assertIs<LoadState.Failed>(loadCounty(nairobi, repo, engine, now))

        // A good fetch overwrites the corrupt entry.
        api.rain = week(27, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        assertIs<LoadState.Ready>(loadCounty(nairobi, repo, engine, now))
        assertNotNull(cache.read(nairobi.code))
    }

    @Test fun entryForAnotherCountyOrVersionIsIgnored() = runTest {
        api.rain = week(27, 1.0, 1.0, 1.0)
        repo.forecastFor(nairobi)
        val text = assertNotNull(store.get("forecast-${nairobi.code}"))
        store.put("forecast-1", text) // county code inside says 47
        assertNull(cache.read(1))
        store.put("forecast-${nairobi.code}", text.replace("\"version\":1", "\"version\":99"))
        assertNull(cache.read(nairobi.code))
    }

    @Test fun riverFailureReusesRecentCachedRiverData() = runTest {
        // Past days at 100 m³/s, then 7 forecast days peaking at 350 (3.5x = SEVERE).
        val floodDates = (1..26).map { "2026-10-${it.toString().padStart(2, '0')}" } +
            (27..31).map { "2026-10-$it" } + listOf("2026-11-01", "2026-11-02")
        val discharge = floodDates.map { d -> if (d < "2026-10-27") 100.0 else if (d == "2026-10-29") 350.0 else 120.0 }
        api.rain = week(27, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        api.flood = FloodDaily(floodDates, discharge)
        val first = repo.forecastFor(tanaRiver)
        assertNotNull(first.forecast.river)
        assertEquals(RiskLevel.SEVERE, engine.assess(first.forecast).level)

        api.flood = null
        now = t0 + hour
        val second = repo.forecastFor(tanaRiver)
        assertFalse(second.fromCache)
        assertNotNull(second.forecast.river)
        assertEquals(RiskLevel.SEVERE, engine.assess(second.forecast).level)
        assertEquals(t0, cache.read(tanaRiver.code)?.floodFetchedAtMillis)
    }

    @Test fun storeThatFailsToWriteStillReturnsFreshData() = runTest {
        val broken = object : KeyValueStore {
            override suspend fun get(key: String): String? = null
            override suspend fun put(key: String, value: String) = false
            override suspend fun remove(key: String) {}
        }
        api.rain = week(27, 1.0, 1.0)
        val r = ForecastRepository(api, ForecastCache(broken)) { now }.forecastFor(nairobi)
        assertFalse(r.fromCache)
    }

    @Test fun preferencesRoundTripAndDefaults() = runTest {
        val prefs = Preferences(InMemoryKeyValueStore())
        assertEquals(UserPreferences(), prefs.load())
        prefs.setCounty(33)
        prefs.setLanguage(Language.SWAHILI)
        assertEquals(UserPreferences(33, Language.SWAHILI), prefs.load())
        prefs.setCounty(null)
        assertEquals(UserPreferences(null, Language.SWAHILI), prefs.load())
    }

    @Test fun preferencesIgnoreCorruptOrUnknownValues() = runTest {
        val s = InMemoryKeyValueStore()
        val prefs = Preferences(s)
        s.put("preferences", "garbage")
        assertEquals(UserPreferences(), prefs.load())
        s.put("preferences", """{"countyCode":999,"language":"KLINGON","extra":1}""")
        assertEquals(UserPreferences(), prefs.load())
    }
}
