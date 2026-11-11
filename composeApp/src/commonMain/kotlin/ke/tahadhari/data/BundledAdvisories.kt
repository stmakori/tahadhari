package ke.tahadhari.data

import ke.tahadhari.domain.Advisory

/**
 * Official advisories shipped with the app (KMSA/KMD has no public API, see README "Official advisories").
 *
 * To update (weekly in the rains):
 * 1. Open https://meteo.go.ke/our-products/county-forecasts/ and the county's page for this week.
 * 2. Copy the text word for word (at most 2,000 characters); never reword, summarise or translate it.
 * 3. Add an [Advisory] with the page's exact validity dates and its URL, and remove expired entries.
 * 4. Run `./gradlew :composeApp:desktopTest` (AdvisoriesTest checks every entry) and release.
 *
 * Expired entries are ignored automatically, so an old list never shows stale advice.
 * Example (county codes are those in [Counties]):
 *
 *     Advisory(
 *         issuer = "Kenya Meteorological Department",
 *         title = "Nakuru County weekly weather forecast",
 *         validFrom = "2026-10-27", validTo = "2026-11-02",
 *         sourceUrl = "https://meteo.go.ke/...",
 *         text = "...",
 *         countyCodes = setOf(32),
 *     )
 */
object BundledAdvisories {
    val all: List<Advisory> = emptyList()
}
