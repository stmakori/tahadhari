package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.data.remote.FloodDaily
import ke.tahadhari.data.remote.FloodResponse
import ke.tahadhari.data.remote.ForecastApi
import ke.tahadhari.data.remote.MAX_POINTS_PER_CALL
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.data.remote.RainfallResponse
import ke.tahadhari.data.remote.parseBatch
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.loadCounties
import ke.tahadhari.ui.loadCounty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Counts every HTTP-call-equivalent. A batch call counts once, however many points it carries. */
private class CountingApi : ForecastApi {
    var singleRain = 0
    var singleFlood = 0
    var rainBatches = 0
    var floodBatches = 0
    val rainBatchSizes = mutableListOf<Int>()

    /** Rainfall as a function of the point; the point's latitude identifies the county. */
    var rainFor: (GeoPoint) -> RainfallDaily? = { week(1.0) }
    var floodFor: (GeoPoint) -> FloodDaily? = { null }
    var failRainBatch = false
    var failFloodBatch = false
    var failSingles = false

    val totalCalls get() = singleRain + singleFlood + rainBatches + floodBatches

    override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse {
        singleRain++
        if (failSingles) throw RuntimeException("offline")
        return RainfallResponse(rainFor(point) ?: throw RuntimeException("no data"))
    }

    override suspend fun riverDischarge(point: GeoPoint, pastDays: Int, forecastDays: Int): FloodResponse {
        singleFlood++
        if (failSingles) throw RuntimeException("offline")
        return FloodResponse(floodFor(point) ?: throw RuntimeException("no flood data"))
    }

    override suspend fun dailyRainfallBatch(points: List<GeoPoint>, forecastDays: Int): List<RainfallResponse?> {
        rainBatches++
        rainBatchSizes += points.size
        if (failRainBatch) throw RuntimeException("batch offline")
        return points.map { p -> rainFor(p)?.let { RainfallResponse(it) } }
    }

    override suspend fun riverDischargeBatch(points: List<GeoPoint>, pastDays: Int, forecastDays: Int): List<FloodResponse?> {
        floodBatches++
        if (failFloodBatch) throw RuntimeException("flood batch offline")
        return points.map { p -> floodFor(p)?.let { FloodResponse(it) } }
    }
}

private fun week(vararg mm: Double): RainfallDaily {
    val values = if (mm.size == 1) List(7) { mm[0] } else mm.toList()
    return RainfallDaily(
        time = values.indices.map { "2026-10-${27 + it}" },
        precipitationSum = values,
        precipitationProbabilityMax = values.map { 70 },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class BatchForecastTest {
    private val t0 = 1_793_080_800_000L // 2026-10-27 09:00 Nairobi
    private val hour = 3_600_000L
    private var now = t0
    private val api = CountingApi()
    private val cache = ForecastCache(InMemoryKeyValueStore())
    private val repo = ForecastRepository(api, cache) { now }
    private val all = Counties.all

    private val nairobi = Counties.byCode(47)!!
    private val kisumu = Counties.byCode(42)!!
    private val mombasa = Counties.byCode(1)!!

    @Test fun batchSuccessMapsEachCountyToItsOwnResponseInOrder() = runTest {
        // A distinct total per county, derived from its location, so any mix-up is caught.
        val mmByLocation = all.associate { it.location to it.code.toDouble() }
        api.rainFor = { p -> week(mmByLocation.getValue(p)) }
        val results = repo.forecastsFor(all)
        assertEquals(all.map { it.code }, results.keys.toList())
        all.forEach { county ->
            val r = results.getValue(county.code).getOrThrow()
            assertFalse(r.fromCache)
            assertEquals(county, r.forecast.county)
            assertTrue(r.forecast.days.all { it.precipitationMm == county.code.toDouble() })
            assertNotNull(cache.read(county.code)) // saved
        }
    }

    @Test fun riverDataIsMappedToTheRightCountiesAndOnlyRiverCountiesAreAsked() = runTest {
        val riverByPoint = all.filter { it.riverPoint != null }.associate { it.riverPoint!! to it.code.toDouble() }
        api.floodFor = { p ->
            val v = riverByPoint.getValue(p)
            FloodDaily(
                time = listOf("2026-10-25", "2026-10-26", "2026-10-27", "2026-10-28"),
                riverDischarge = listOf(v, v, v, v * 4), // 4x peak => SEVERE river signal
            )
        }
        val results = repo.forecastsFor(all)
        assertEquals(1, api.floodBatches)
        all.forEach { county ->
            val f = results.getValue(county.code).getOrThrow().forecast
            if (county.riverPoint != null) assertEquals(4.0, f.river?.riseRatio) else assertNull(f.river)
        }
    }

    @Test fun partialNullsInArraysAndMissingCountiesAreHandledPerCounty() = runTest {
        api.rainFor = { p ->
            when (p) {
                nairobi.location -> RainfallDaily(
                    time = (27..33).map { "2026-10-$it" },
                    precipitationSum = listOf(null, 60.0, null, 0.0, 0.0, 0.0, 0.0),
                    precipitationProbabilityMax = emptyList(),
                )
                mombasa.location -> null // this point has no data
                else -> week(1.0)
            }
        }
        val results = repo.forecastsFor(listOf(nairobi, mombasa, kisumu))
        val nbo = results.getValue(47).getOrThrow().forecast
        assertEquals(0.0, nbo.days[0].precipitationMm) // null -> 0, same as the single-county path
        assertEquals(60.0, nbo.days[1].precipitationMm)
        assertEquals(RiskLevel.HIGH, RiskEngine().assess(nbo).level)
        assertTrue(results.getValue(1).isFailure) // no data and no cache: a clear failure, not Low risk
        assertTrue(results.getValue(42).isSuccess)
    }

    @Test fun wholeBatchFailureFallsBackToCacheForEveryCounty() = runTest {
        api.rainFor = { week(55.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }
        repo.forecastsFor(listOf(nairobi, kisumu))
        api.failRainBatch = true
        now = t0 + 5 * hour
        val results = repo.forecastsFor(listOf(nairobi, kisumu, mombasa))
        listOf(47, 42).forEach {
            val r = results.getValue(it).getOrThrow()
            assertTrue(r.fromCache)
            assertEquals(t0, r.fetchedAtMillis)
        }
        assertTrue(results.getValue(1).isFailure) // never fetched: nothing to show
        val state = loadCounties(listOf(nairobi, mombasa), repo, RiskEngine(), now)
        assertIs<LoadState.Ready>(state.getValue(47))
        assertIs<LoadState.Failed>(state.getValue(1))
    }

    @Test fun cachedDaysInThePastAreDroppedAndTooOldCacheIsFailedLikeSingleCounty() = runTest {
        api.rainFor = { week(90.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }
        repo.forecastsFor(listOf(nairobi))
        api.failRainBatch = true
        now = t0 + 2 * 24 * hour
        val r = repo.forecastsFor(listOf(nairobi)).getValue(47).getOrThrow()
        assertEquals("2026-10-29", r.forecast.days.first().date)
        assertEquals(RiskLevel.LOW, RiskEngine().assess(r.forecast).level)

        now = t0 + 3 * 24 * hour + hour
        val state = loadCounties(listOf(nairobi), repo, RiskEngine(), now).getValue(47)
        assertIs<LoadState.Failed>(state)
        assertEquals(t0, state.staleSinceMillis)
    }

    @Test fun failedRiverBatchKeepsRecentCachedRiverDataAndFreshRain() = runTest {
        val river = FloodDaily(
            time = listOf("2026-10-25", "2026-10-26", "2026-10-27", "2026-10-28"),
            riverDischarge = listOf(10.0, 10.0, 10.0, 35.0),
        )
        api.rainFor = { week(1.0) }
        api.floodFor = { river }
        repo.forecastsFor(listOf(kisumu))

        api.failFloodBatch = true
        now = t0 + hour
        val r = repo.forecastsFor(listOf(kisumu)).getValue(42).getOrThrow()
        assertFalse(r.fromCache)
        assertEquals(3.5, r.forecast.river?.riseRatio)
    }

    @Test fun matchesSingleCountyPathResults() = runTest {
        api.rainFor = { week(5.0, 12.0, 25.0, 10.0, 3.0, 0.0, 1.0) }
        val single = repo.forecastFor(nairobi)
        val batch = repo.forecastsFor(listOf(nairobi)).getValue(47).getOrThrow()
        assertEquals(single.forecast, batch.forecast)
        assertEquals(single.fromCache, batch.fromCache)
    }

    @Test fun pointsAreChunkedAtTheLimit() = runTest {
        val many = List(120) { nairobi.copy(code = 1000 + it) }
        repo.forecastsFor(many)
        assertEquals(listOf(MAX_POINTS_PER_CALL, MAX_POINTS_PER_CALL, 20), api.rainBatchSizes.sortedDescending())
    }

    // ---- measurement ----

    @Test fun httpCallsForOneFullDashboardRefreshBeforeAndAfter() = runTest {
        // Before: one call per county for rainfall, plus one per river point.
        all.forEach { loadCounty(it, repo, RiskEngine(), now) }
        val before = api.totalCalls
        val riverCounties = all.count { it.riverPoint != null }
        assertEquals(all.size + riverCounties, before)
        println("HTTP calls per full refresh, before (per county): $before")

        // After: batched.
        val api2 = CountingApi().also { it.floodFor = { null } }
        val repo2 = ForecastRepository(api2, ForecastCache(InMemoryKeyValueStore())) { now }
        val states = loadCounties(all, repo2, RiskEngine(), now)
        assertEquals(47, states.size)
        println("HTTP calls per full refresh, after (batched): ${api2.totalCalls}")
        assertEquals(1, api2.rainBatches)
        assertEquals(1, api2.floodBatches)
        assertEquals(2, api2.totalCalls)
        assertEquals(0, api2.singleRain + api2.singleFlood)
    }

    // ---- DashboardState uses the batch loader ----

    @Test fun dashboardRefreshUsesOneBatchCallAndRetryStillUsesSingleLoader() = runTest {
        var batchCalls = 0
        var singleCalls = 0
        val counties = listOf(kisumu, nairobi)
        val state = DashboardState(
            backgroundScope, counties,
            loader = { c -> singleCalls++; readyState(c, Rain.low, updatedAtMillis = now) },
            clock = { now },
            batchLoader = { cs -> batchCalls++; cs.associate { it.code to readyState(it, Rain.moderate, updatedAtMillis = now) } },
        )
        state.refresh(); runCurrent()
        assertEquals(1, batchCalls)
        assertEquals(0, singleCalls)
        assertEquals(2, state.doneCount)
        assertFalse(state.refreshing)
        state.retry(42); runCurrent()
        assertEquals(1, batchCalls)
        assertEquals(1, singleCalls)
    }

    @Test fun dashboardBatchFailureKeepsPreviousDataAsSaved() = runTest {
        var fail = false
        val counties = listOf(kisumu, nairobi)
        val state = DashboardState(
            backgroundScope, counties, clock = { now },
            batchLoader = { cs ->
                if (fail) throw RuntimeException("offline")
                cs.associate { it.code to readyState(it, Rain.high, updatedAtMillis = now) }
            },
            loader = { error("unused") },
        )
        state.refresh(); runCurrent()
        fail = true
        state.refresh(); runCurrent()
        state.states.values.forEach {
            assertIs<LoadState.Ready>(it)
            assertTrue(it.fromCache)
        }
    }

    // ---- JSON shape ----

    @Test fun parseBatchHandlesArrayAndSingleObjectAndBadElements() {
        val json = Json { ignoreUnknownKeys = true }
        val obj = """{"daily":{"time":["2026-10-27"],"precipitation_sum":[1.5,null]}}"""
        val serializer = RainfallResponse.serializer()
        val arr = parseBatch(json, "[$obj,{\"error\":true},$obj]", 3, serializer)
        assertEquals(3, arr.size)
        assertNotNull(arr[0]); assertNull(arr[1]); assertNotNull(arr[2])
        assertEquals(listOf(1.5, null), arr[0]!!.daily.precipitationSum)
        // A single location comes back as a bare object.
        assertEquals(1, parseBatch(json, obj, 1, serializer).size)
        // A count mismatch must never be mapped onto counties.
        var failed = false
        try { parseBatch(json, "[$obj]", 2, serializer) } catch (e: IllegalStateException) { failed = true }
        assertTrue(failed)
    }
}
