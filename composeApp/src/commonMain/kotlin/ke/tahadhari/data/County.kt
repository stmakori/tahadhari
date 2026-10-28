package ke.tahadhari.data

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * One of Kenya's 47 counties.
 *
 * [location] is the county headquarters and is used for rainfall forecasts.
 * [riverPoint] is set only for counties with a major flood-prone river; it is used
 * for river discharge forecasts. [landslideProne] marks counties with documented landslide history. Hotspots are known flood/landslide areas and must
 * be checked against current county and KMSA advisories before release.
 */
data class County(
    val code: Int,
    val name: String,
    val town: String,
    val location: GeoPoint,
    val riverName: String? = null,
    val riverPoint: GeoPoint? = null,
    val hotspots: List<String> = emptyList(),
    /** True when steep, landslide-prone terrain makes heavy rain especially dangerous (see Counties.kt for sources). */
    val landslideProne: Boolean = false,
    /** Specific landslide-prone areas, shown in advice. Only meaningful when [landslideProne]. */
    val landslideAreas: List<String> = emptyList(),
    /**
     * Codes of counties whose heavy rain feeds this county's river within days (see Counties.kt for sources).
     * Used by the upstream-rain signal ([ke.tahadhari.domain.RiskThresholds.upstreamRainFloor], off by default).
     */
    val upstreamCodes: List<Int> = emptyList(),
)
