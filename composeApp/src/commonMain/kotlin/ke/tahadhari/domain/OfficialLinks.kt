package ke.tahadhari.domain

/**
 * Where the app sends people for the official forecast. Pure Kotlin so it is easy to test.
 *
 * Checked 2026-10-28 (all HTTP 200, valid TLS):
 * - https://meteo.go.ke/our-products/county-forecasts/ : index of weekly county forecasts.
 * - https://meteo.go.ke/our-products/daily-forecast/, /5-day-forecast/, /monthly-forecast/ : national forecasts.
 * - https://meteo.go.ke/weather-warnings/ : official alerts (CAP).
 * Individual county pages exist but their addresses contain the validity dates of that week
 * ("nakuru-county-weekly-weather-forecast-valid-27th-october-to-2nd-november-2026"), so they cannot be
 * predicted for all 47 counties. We link the stable index page, which lists every county.
 */
object OfficialLinks {
    const val KMD_COUNTY_FORECASTS = "https://meteo.go.ke/our-products/county-forecasts/"

    /** Only `https://` links with a host may be opened. No `http`, `javascript:`, `file:`, `intent:` or `tel:`. */
    fun isOpenable(url: String): Boolean {
        if (url.length > 2048 || url.any { it.isWhitespace() || it.isISOControl() }) return false
        if (!url.startsWith("https://", ignoreCase = true)) return false
        val rest = url.substring("https://".length)
        val host = rest.substringBefore('/').substringBefore('?').substringBefore('#')
        if (host.isEmpty() || '@' in host) return false // no user-info tricks like https://meteo.go.ke@evil.example
        return true
    }
}
