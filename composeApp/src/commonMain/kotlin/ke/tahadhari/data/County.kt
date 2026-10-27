package ke.tahadhari.data

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * One of Kenya's 47 counties.
 *
 * [location] is the county headquarters and is used for rainfall forecasts.
 * [riverPoint] is set only for counties with a major flood-prone river; it is used
 * for river discharge forecasts. Hotspots are known flood/landslide areas and must
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
)
