package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.domain.Geo
import ke.tahadhari.domain.NearestHqLocator
import ke.tahadhari.domain.nearestCounty
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeoTest {
    @Test
    fun distanceIsZeroForSamePoint() {
        val p = GeoPoint(-1.2921, 36.8219)
        assertEquals(0.0, Geo.distanceKm(p, p), 1e-9)
    }

    @Test
    fun nairobiToMombasaIsAbout440Km() {
        val d = Geo.distanceKm(GeoPoint(-1.2921, 36.8219), GeoPoint(-4.0435, 39.6682))
        assertTrue(abs(d - 440) < 10, "was $d")
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111.2, Geo.distanceKm(GeoPoint(0.0, 37.0), GeoPoint(1.0, 37.0)), 0.5)
    }

    @Test
    fun distanceIsSymmetric() {
        val a = GeoPoint(3.9373, 41.8569)
        val b = GeoPoint(-4.6, 34.0)
        assertEquals(Geo.distanceKm(a, b), Geo.distanceKm(b, a), 1e-9)
    }

    @Test
    fun everyHqMapsToItsOwnCounty() {
        Counties.all.forEach { c ->
            assertEquals(c.code, nearestCounty(c.location.latitude, c.location.longitude, Counties.all)?.code, c.name)
        }
    }

    @Test
    fun pointsNearTownsMapToTheirCounty() {
        assertEquals("Kisumu", nearestCounty(-0.10, 34.76, Counties.all)?.name) // Kisumu city
        assertEquals("Mombasa", nearestCounty(-4.05, 39.67, Counties.all)?.name) // Mombasa island
        assertEquals("Garissa", nearestCounty(-0.46, 39.65, Counties.all)?.name)
    }

    @Test
    fun outsideKenyaReturnsNull() {
        assertNull(nearestCounty(-6.79, 39.21, Counties.all)) // Dar es Salaam
        assertNull(nearestCounty(0.35, 32.58, Counties.all)) // Kampala
        assertNull(nearestCounty(51.5, -0.12, Counties.all)) // London
    }

    @Test
    fun emptyListReturnsNull() {
        assertNull(NearestHqLocator(emptyList()).locate(GeoPoint(-1.29, 36.82)))
    }
}
