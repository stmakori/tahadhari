package ke.tahadhari.domain

import ke.tahadhari.data.County
import ke.tahadhari.data.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Small geography helpers. Pure Kotlin, no platform code. */
object Geo {
    private const val EARTH_RADIUS_KM = 6371.0088

    /** Great-circle distance in km (haversine). */
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = a.latitude.toRadians()
        val lat2 = b.latitude.toRadians()
        val dLat = lat2 - lat1
        val dLon = (b.longitude - a.longitude).toRadians()
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(h)))
    }

    /**
     * Rough box around Kenya (it reaches about 4.7°S–5.0°N and 33.9°E–41.9°E), with a small margin.
     * Points outside it are not matched to any county.
     */
    fun isInKenyaBox(p: GeoPoint): Boolean = p.latitude in -4.9..5.2 && p.longitude in 33.7..42.1

    private fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0
}

/**
 * Finds the county a point is in. Today: the county whose headquarters is nearest
 * ([NearestHqLocator]). That can be wrong near borders, so the UI must ask the user to confirm.
 * A polygon-based locator (point-in-polygon on county boundaries) can replace it later.
 */
fun interface CountyLocator {
    fun locate(point: GeoPoint): County?
}

/** Nearest county headquarters by haversine distance. Null outside Kenya or for an empty list. */
class NearestHqLocator(private val counties: List<County>) : CountyLocator {
    override fun locate(point: GeoPoint): County? {
        if (!Geo.isInKenyaBox(point)) return null
        return counties.minByOrNull { Geo.distanceKm(point, it.location) }
    }
}

/** Convenience wrapper: county whose HQ is nearest to ([latitude], [longitude]), or null outside Kenya. */
fun nearestCounty(latitude: Double, longitude: Double, counties: List<County>): County? =
    NearestHqLocator(counties).locate(GeoPoint(latitude, longitude))

/**
 * Rules for using the user's own point instead of the county HQ. Pure, so they are testable.
 * Privacy: coordinates are rounded to 2 decimals (about 1 km) before any network call or storage.
 */
object UserPoint {
    /** The point must be within this distance of the county's main town, or the option is not offered. */
    const val MAX_DISTANCE_KM = 100.0

    /** Rounds both coordinates to 2 decimals (about 1.1 km north-south). */
    fun round(p: GeoPoint): GeoPoint = GeoPoint(round2(p.latitude), round2(p.longitude))

    /**
     * The rounded point to forecast for, or null when it should not be offered: outside Kenya, or
     * more than [MAX_DISTANCE_KM] from [county]'s HQ (the user is probably not in that county).
     */
    fun forCounty(raw: GeoPoint, county: County): GeoPoint? {
        if (!Geo.isInKenyaBox(raw)) return null
        val rounded = round(raw)
        return rounded.takeIf { Geo.distanceKm(it, county.location) <= MAX_DISTANCE_KM }
    }

    private fun round2(x: Double): Double = kotlin.math.round(x * 100.0) / 100.0 + 0.0 // + 0.0 turns -0.0 into 0.0
}

/** The same county, forecast for [point] instead of its HQ. River point, hotspots and landslide data are kept. */
fun County.atPoint(point: GeoPoint): County = copy(location = point)
