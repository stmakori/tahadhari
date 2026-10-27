package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.geo.CountyShapes
import ke.tahadhari.data.geo.GeoBounds
import ke.tahadhari.data.geo.MapProjection
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountyShapesTest {
    @Test
    fun everyCountyHasAShape() {
        assertEquals(Counties.all.map { it.code }.toSet(), CountyShapes.all.keys)
        CountyShapes.all.values.forEach { s -> assertTrue(s.parts.all { it.size >= 6 }, "county ${s.code}") }
    }

    @Test
    fun boundsCoverKenya() {
        val b = CountyShapes.bounds
        assertTrue(b.minLon in 33.5..34.5 && b.maxLon in 41.5..42.5, "$b")
        assertTrue(b.minLat in -5.2..-4.3 && b.maxLat in 4.5..5.5, "$b")
    }

    @Test
    fun countyHeadquartersHitTestToTheirOwnCounty() {
        // HQ coordinates are approximate and some towns sit on a county border, so allow a few misses.
        val misses = Counties.all.filter { CountyShapes.countyAt(it.location.longitude, it.location.latitude) != it.code }
        assertTrue(misses.size <= 3, "HQs outside their county: ${misses.map { it.name }}")
        assertEquals(47, CountyShapes.countyAt(36.8172, -1.2864)) // Nairobi CBD
        assertEquals(42, CountyShapes.countyAt(34.7680, -0.0917)) // Kisumu
        assertEquals(5, CountyShapes.countyAt(40.90, -2.27))      // Lamu island (MultiPolygon part)
    }

    @Test
    fun outsideKenyaIsNoCounty() {
        assertNull(CountyShapes.countyAt(45.0, 0.0))   // Indian Ocean / Somalia
        assertNull(CountyShapes.countyAt(33.0, -1.0))  // Lake Victoria, Uganda/Tanzania side
    }

    @Test
    fun projectionRoundTripsAndKeepsNorthUp() {
        val p = MapProjection(GeoBounds(34.0, -5.0, 42.0, 5.0), 800f, 600f, 10f)
        val x = p.x(37.5); val y = p.y(1.25)
        assertTrue(abs(p.lon(x) - 37.5) < 1e-6 && abs(p.lat(y) - 1.25) < 1e-6)
        assertTrue(p.y(4.0) < p.y(-4.0), "north is up")
        assertTrue(p.x(41.0) > p.x(35.0), "east is right")
        // Fits inside the padded box.
        assertTrue(p.x(34.0) >= 9.99f && p.x(42.0) <= 790.01f && p.y(5.0) >= 9.99f && p.y(-5.0) <= 590.01f)
    }
}
