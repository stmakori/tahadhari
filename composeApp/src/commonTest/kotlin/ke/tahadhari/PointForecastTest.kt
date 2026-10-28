package ke.tahadhari

import ke.tahadhari.data.geo.CountyShapes
import ke.tahadhari.data.Counties
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.data.remote.FloodResponse
import ke.tahadhari.data.remote.ForecastApi
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.data.remote.RainfallResponse
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.UserPoint
import ke.tahadhari.domain.atPoint
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private class RecordingApi : ForecastApi {
    val rainPoints = mutableListOf<GeoPoint>()
    var mm = 5.0

    override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse {
        rainPoints += point
        return RainfallResponse(RainfallDaily(
            time = (27..33).map { "2026-10-$it" },
            precipitationSum = List(7) { mm },
            precipitationProbabilityMax = List(7) { 50 },
        ))
    }

    override suspend fun riverDischarge(point: GeoPoint, pastDays: Int, forecastDays: Int): FloodResponse =
        throw RuntimeException("no river in this test")
}

class PointForecastTest {
    private val nairobi = Counties.byCode(47)!!
    private val t0 = 1_793_080_800_000L

    @Test fun roundsToTwoDecimals() {
        assertEquals(GeoPoint(-1.29, 36.82), UserPoint.round(GeoPoint(-1.28621, 36.82198)))
        assertEquals(GeoPoint(-1.28, 36.83), UserPoint.round(GeoPoint(-1.2849, 36.8251)))
        assertEquals(0.0, UserPoint.round(GeoPoint(-0.001, 0.0)).latitude) // not -0.0
    }

    @Test fun nearbyPointIsOfferedRounded() {
        val p = UserPoint.forCounty(GeoPoint(-1.2301234, 36.8801234), nairobi)
        assertEquals(GeoPoint(-1.23, 36.88), p)
    }

    @Test fun outsideKenyaIsNotOffered() {
        assertNull(UserPoint.forCounty(GeoPoint(51.5, -0.12), nairobi)) // London
        assertNull(UserPoint.forCounty(GeoPoint(-6.8, 39.28), nairobi)) // Dar es Salaam
    }

    @Test fun farFromTheCountyIsNotOffered() {
        val mombasa = Counties.all.first { it.name.contains("Mombasa") }
        assertNull(UserPoint.forCounty(mombasa.location, nairobi)) // ~440 km away
        // Just inside and just outside 100 km (1 degree of latitude is about 111 km).
        assertNotNull(UserPoint.forCounty(GeoPoint(nairobi.location.latitude + 0.8, nairobi.location.longitude), nairobi))
        assertNull(UserPoint.forCounty(GeoPoint(nairobi.location.latitude + 1.0, nairobi.location.longitude), nairobi))
    }

    @Test fun farPointInsideALargeCountyIsOffered() {
        val turkana = Counties.all.first { it.name.contains("Turkana") }
        val lokichogio = GeoPoint(4.2, 34.35) // north-west Turkana, roughly 230 km from Lodwar
        assertEquals(turkana.code, CountyShapes.countyAt(lokichogio.longitude, lokichogio.latitude))
        assertNull(UserPoint.forCounty(lokichogio, turkana))
        assertEquals(GeoPoint(4.2, 34.35), UserPoint.forCounty(lokichogio, turkana, insideCounty = true))
        // Inside the county but outside Kenya's box is still refused.
        assertNull(UserPoint.forCounty(GeoPoint(51.5, -0.12), turkana, insideCounty = true))
    }

    @Test fun atPointKeepsRiverHotspotsAndLandslideData() {
        val c = Counties.all.first { it.riverPoint != null && it.hotspots.isNotEmpty() }
        val moved = c.atPoint(GeoPoint(0.5, 37.0))
        assertEquals(GeoPoint(0.5, 37.0), moved.location)
        assertEquals(c.riverPoint, moved.riverPoint)
        assertEquals(c.hotspots, moved.hotspots)
        assertEquals(c.landslideProne, moved.landslideProne)
        assertEquals(c.code, moved.code)
    }

    @Test fun cacheKeysNeverMix() {
        val p = GeoPoint(-1.29, 36.82)
        assertEquals("forecast-47", ForecastCache.keyFor(47))
        assertNotEquals(ForecastCache.keyFor(47), ForecastCache.keyFor(47, p))
        assertNull(ForecastCache.pointOf(nairobi))
        assertEquals(p, ForecastCache.pointOf(nairobi.atPoint(p)))
    }

    @Test fun pointAndHqForecastsAreCachedSeparately() = runTest {
        val api = RecordingApi()
        val cache = ForecastCache(InMemoryKeyValueStore())
        val repo = ForecastRepository(api, cache) { t0 }
        val p = GeoPoint(-1.31, 36.9)

        api.mm = 5.0
        repo.forecastFor(nairobi)
        api.mm = 60.0
        repo.forecastFor(nairobi.atPoint(p))

        assertEquals(5.0, assertNotNull(cache.read(47)).rainfall.precipitationSum.first())
        assertEquals(60.0, assertNotNull(cache.read(47, p)).rainfall.precipitationSum.first())
        assertNull(cache.read(47, GeoPoint(-1.31, 36.91))) // a different point is a miss
    }

    @Test fun offlineServesTheMatchingCacheOnly() = runTest {
        val api = RecordingApi()
        val cache = ForecastCache(InMemoryKeyValueStore())
        val repo = ForecastRepository(api, cache) { t0 }
        val p = GeoPoint(-1.31, 36.9)
        api.mm = 5.0
        repo.forecastFor(nairobi) // only the HQ forecast is saved

        val failing = ForecastRepository(object : ForecastApi by api {
            override suspend fun dailyRainfall(point: GeoPoint, forecastDays: Int): RainfallResponse = throw RuntimeException("offline")
        }, cache) { t0 }
        assertFailsWith<RuntimeException> { failing.forecastFor(nairobi.atPoint(p)) } // no point cache: error, not HQ data
        assertTrue(failing.forecastFor(nairobi).fromCache)
    }

    @Test fun networkOnlySeesTheRoundedPoint() = runTest {
        val api = RecordingApi()
        val repo = ForecastRepository(api, ForecastCache(InMemoryKeyValueStore())) { t0 }
        repo.forecastFor(nairobi.atPoint(UserPoint.round(GeoPoint(-1.3123456, 36.9087654))))
        assertEquals(listOf(GeoPoint(-1.31, 36.91)), api.rainPoints)
        repo.forecastFor(nairobi)
        assertEquals(nairobi.location, api.rainPoints.last())
    }
}
