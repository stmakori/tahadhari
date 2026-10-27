package ke.tahadhari.data.geo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Longitude/latitude box in degrees. */
data class GeoBounds(val minLon: Double, val minLat: Double, val maxLon: Double, val maxLat: Double) {
    fun contains(lon: Double, lat: Double): Boolean = lon in minLon..maxLon && lat in minLat..maxLat

    fun union(other: GeoBounds) = GeoBounds(
        min(minLon, other.minLon), min(minLat, other.minLat), max(maxLon, other.maxLon), max(maxLat, other.maxLat),
    )
}

/**
 * Outline of one county. Each part is a closed ring (mainland or island) stored as interleaved
 * `lon, lat, lon, lat, …` degrees; the last point connects back to the first.
 */
class CountyShape(val code: Int, val parts: List<DoubleArray>) {
    val bounds: GeoBounds = parts.map { ringBounds(it) }.reduce(GeoBounds::union)

    /** Even-odd point-in-polygon test across all parts. */
    fun contains(lon: Double, lat: Double): Boolean =
        bounds.contains(lon, lat) && parts.any { ringContains(it, lon, lat) }

    /** Area-weighted centroid of the largest part, in degrees (lon to lat): a good label anchor. */
    val labelPoint: Pair<Double, Double> by lazy { ringCentroid(parts.maxBy { ringArea(it) }) }
}

/**
 * Kenya county boundaries, keyed by our county code (see `data/Counties.kt`).
 * Source: geoBoundaries gbOpen KEN ADM1 (RCMRD GeoPortal, public domain), simplified by
 * `tools/geo/build_county_shapes.py`.
 */
object CountyShapes {
    val all: Map<Int, CountyShape> by lazy {
        CountyShapeData.encoded.mapValues { (code, text) -> CountyShape(code, decode(text)) }
    }

    val bounds: GeoBounds by lazy { all.values.map { it.bounds }.reduce(GeoBounds::union) }

    /** County code at a point, or null when the point is outside every county (sea, lake, abroad). */
    fun countyAt(lon: Double, lat: Double): Int? = all.values.firstOrNull { it.contains(lon, lat) }?.code

    /** Parses "lon,lat lon,lat;lon,lat …" into rings. */
    internal fun decode(text: String): List<DoubleArray> = text.split(';').map { part ->
        val points = part.trim().split(' ')
        DoubleArray(points.size * 2).also { out ->
            points.forEachIndexed { i, p ->
                val comma = p.indexOf(',')
                out[2 * i] = p.substring(0, comma).toDouble()
                out[2 * i + 1] = p.substring(comma + 1).toDouble()
            }
        }
    }
}

/**
 * Equirectangular projection with a cos(latitude) correction at the centre of [bounds], fitted
 * into a [width] x [height] pixel box with [padding] on each side and centred. Good enough for a
 * country the size of Kenya that straddles the equator.
 */
class MapProjection(val bounds: GeoBounds, val width: Float, val height: Float, val padding: Float = 0f) {
    private val kx = cos((bounds.minLat + bounds.maxLat) / 2 * PI / 180)
    private val geoW = (bounds.maxLon - bounds.minLon) * kx
    private val geoH = bounds.maxLat - bounds.minLat
    val scale: Double = max(
        1e-9,
        min((width - 2 * padding).coerceAtLeast(1f) / geoW, (height - 2 * padding).coerceAtLeast(1f) / geoH),
    )
    private val offsetX = (width - geoW * scale) / 2
    private val offsetY = (height - geoH * scale) / 2

    fun x(lon: Double): Float = (offsetX + (lon - bounds.minLon) * kx * scale).toFloat()
    fun y(lat: Double): Float = (offsetY + (bounds.maxLat - lat) * scale).toFloat()

    fun lon(x: Float): Double = bounds.minLon + (x - offsetX) / (kx * scale)
    fun lat(y: Float): Double = bounds.maxLat - (y - offsetY) / scale
}

private fun ringBounds(r: DoubleArray): GeoBounds {
    var minLon = Double.MAX_VALUE; var minLat = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE; var maxLat = -Double.MAX_VALUE
    for (i in r.indices step 2) {
        minLon = min(minLon, r[i]); maxLon = max(maxLon, r[i])
        minLat = min(minLat, r[i + 1]); maxLat = max(maxLat, r[i + 1])
    }
    return GeoBounds(minLon, minLat, maxLon, maxLat)
}

internal fun ringContains(r: DoubleArray, x: Double, y: Double): Boolean {
    var inside = false
    val n = r.size / 2
    var j = n - 1
    for (i in 0 until n) {
        val xi = r[2 * i]; val yi = r[2 * i + 1]
        val xj = r[2 * j]; val yj = r[2 * j + 1]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

private fun ringArea(r: DoubleArray): Double {
    var s = 0.0
    val n = r.size / 2
    for (i in 0 until n) {
        val j = (i + 1) % n
        s += r[2 * i] * r[2 * j + 1] - r[2 * j] * r[2 * i + 1]
    }
    return s / 2
}

private fun ringCentroid(r: DoubleArray): Pair<Double, Double> {
    val a = ringArea(r)
    if (a == 0.0) return r[0] to r[1]
    var cx = 0.0; var cy = 0.0
    val n = r.size / 2
    for (i in 0 until n) {
        val j = (i + 1) % n
        val f = r[2 * i] * r[2 * j + 1] - r[2 * j] * r[2 * i + 1]
        cx += (r[2 * i] + r[2 * j]) * f
        cy += (r[2 * i + 1] + r[2 * j + 1]) * f
    }
    return cx / (6 * a) to cy / (6 * a)
}
