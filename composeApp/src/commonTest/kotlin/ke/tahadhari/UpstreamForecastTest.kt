package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.data.remote.FloodResponse
import ke.tahadhari.data.remote.ForecastApi
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.data.remote.RainfallResponse
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskReason
import ke.tahadhari.domain.RiskThresholds
import ke.tahadhari.domain.TemplateExplainer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun rain(vararg mm: Double) = RainfallDaily(
    time = mm.indices.map { "2026-10-${27 + it}" },
    precipitationSum = mm.toList(),
    precipitationProbabilityMax = mm.map { 70 },
)

/** Counts HTTP-call equivalents; a batch counts once. Murang'a (upstream of the Tana) is heavy, everywhere else is dry. */
private class UpstreamApi : ForecastApi {
    var singles = 0
    var batches = 0
    val batchSizes = mutableListOf<Int>()
    var failBatches = false
    var failSingles = false
    val heavy = Counties.byCode(21)!!.location

    private fun rainAt(p: GeoPoint) = if (p == heavy) rain(0.0, 60.0, 0.0, 0.0, 0.0, 0.0, 0.0) else rain(0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse {
        singles++
        if (failSingles) throw RuntimeException("offline")
        return RainfallResponse(rainAt(point))
    }

    override suspend fun riverDischarge(point: GeoPoint, pastDays: Int, forecastDays: Int): FloodResponse =
        throw RuntimeException("no flood data")

    override suspend fun dailyRainfallBatch(points: List<GeoPoint>, forecastDays: Int): List<RainfallResponse?> {
        batches++
        batchSizes += points.size
        if (failBatches) throw RuntimeException("batch offline")
        return points.map { RainfallResponse(rainAt(it)) }
    }

    override suspend fun riverDischargeBatch(points: List<GeoPoint>, pastDays: Int, forecastDays: Int): List<FloodResponse?> =
        points.map { null }
}

class UpstreamForecastTest {
    private val t0 = 1_793_080_800_000L // 2026-10-27 09:00 Nairobi
    private var now = t0
    private val api = UpstreamApi()
    private val cache = ForecastCache(InMemoryKeyValueStore())
    private val on = ForecastRepository(api, cache, upstreamEnabled = true) { now }
    private val garissa = Counties.byCode(7)!!
    private val nairobi = Counties.byCode(47)!!
    private val engineOn = RiskEngine(RiskThresholds(upstreamRainFloor = RiskLevel.MODERATE))

    @Test fun mappingsPointAtRealCounties() {
        Counties.all.forEach { c -> c.upstreamCodes.forEach { assertTrue(Counties.byCode(it) != null && it != c.code, "${c.name} -> $it") } }
        assertEquals(setOf("Nyeri", "Murang'a", "Embu", "Meru", "Kirinyaga", "Tharaka-Nithi"), garissa.upstreamCodes.map { Counties.byCode(it)!!.name }.toSet())
        assertEquals(garissa.upstreamCodes, Counties.byCode(4)!!.upstreamCodes)
    }

    @Test fun disabledSignalCostsNoExtraCalls() = runTest {
        val repo = ForecastRepository(api, cache) { now }
        val f = repo.forecastFor(garissa).forecast
        assertEquals(1, api.singles)
        assertEquals(0, api.batches)
        assertTrue(f.upstream.isEmpty())
        assertFalse(f.upstreamUnavailable)
    }

    @Test fun phoneFetchCostsOneExtraBatchedCallForACountyWithUpstream() = runTest {
        val f = on.forecastFor(garissa).forecast
        assertEquals(1, api.singles) // its own rain
        assertEquals(1, api.batches) // all six upstream counties together
        assertEquals(listOf(6), api.batchSizes)
        assertEquals(6, f.upstream.size)
        assertFalse(f.upstreamUnavailable)
        val a = engineOn.assess(f)
        assertEquals(RiskLevel.MODERATE, a.level)
        assertEquals("Murang'a", a.reasons.filterIsInstance<RiskReason.UpstreamRain>().single().countyName)
    }

    @Test fun countyWithoutUpstreamCostsNothingExtra() = runTest {
        on.forecastFor(nairobi)
        assertEquals(1, api.singles)
        assertEquals(0, api.batches)
    }

    @Test fun upstreamFailureKeepsTheLocalForecastAndNeverClaimsUpstreamIsDry() = runTest {
        api.failBatches = true
        val f = on.forecastFor(garissa).forecast
        assertTrue(f.upstream.isEmpty())
        assertTrue(f.upstreamUnavailable)
        val a = engineOn.assess(f)
        assertEquals(RiskLevel.LOW, a.level) // local data is valid and unchanged
        assertTrue(a.upstreamUnavailable)
        val text = TemplateExplainer().build(garissa, a, Language.ENGLISH)
        assertTrue(text.body.contains("could not be checked"), text.body)
        assertFalse(text.body.contains("dry"))
    }

    @Test fun recentCachedUpstreamIsReusedWhenTheNextUpstreamFetchFails() = runTest {
        on.forecastFor(garissa)
        api.failBatches = true
        now += 3_600_000L
        val f = on.forecastFor(garissa).forecast
        assertEquals(6, f.upstream.size)
        assertFalse(f.upstreamUnavailable)
    }

    @Test fun savedForecastServedOfflineIncludesUpstream() = runTest {
        on.forecastFor(garissa)
        api.failSingles = true
        now += 3_600_000L
        val r = on.forecastFor(garissa)
        assertTrue(r.fromCache)
        assertEquals(6, r.forecast.upstream.size)
        assertEquals(RiskLevel.MODERATE, engineOn.assess(r.forecast).level)
    }

    @Test fun dashboardReusesTheBatchWithNoExtraCall() = runTest {
        val results = on.forecastsFor(Counties.all)
        assertEquals(1, api.batches)
        assertEquals(0, api.singles)
        val f = results.getValue(7).getOrThrow().forecast
        assertEquals(6, f.upstream.size)
        assertFalse(f.upstreamUnavailable)
        assertEquals(RiskLevel.MODERATE, engineOn.assess(f).level)
        assertEquals(RiskLevel.LOW, engineOn.assess(results.getValue(47).getOrThrow().forecast).level)
    }

    @Test fun dashboardSubsetWithoutUpstreamCountiesSaysNotChecked() = runTest {
        val results = on.forecastsFor(listOf(garissa))
        val f = results.getValue(7).getOrThrow().forecast
        assertTrue(f.upstream.isEmpty())
        assertTrue(f.upstreamUnavailable)
    }

    @Test fun templateMentionsHeavyUpstreamRainInBothLanguages() = runTest {
        val a = engineOn.assess(on.forecastFor(garissa).forecast)
        val en = TemplateExplainer().build(garissa, a, Language.ENGLISH).body
        val sw = TemplateExplainer().build(garissa, a, Language.SWAHILI).body
        assertTrue(en.contains("Heavy rain upstream in Murang'a"), en)
        assertTrue(sw.contains("Murang'a"), sw)
    }
}
