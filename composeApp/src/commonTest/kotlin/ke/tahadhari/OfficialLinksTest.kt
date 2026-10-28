package ke.tahadhari

import ke.tahadhari.domain.OfficialLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfficialLinksTest {
    @Test fun officialUrlIsOpenable() = assertTrue(OfficialLinks.isOpenable(OfficialLinks.KMD_COUNTY_FORECASTS))

    @Test fun onlyHttps() {
        listOf(
            "http://meteo.go.ke/", "javascript:alert(1)", "file:///etc/passwd", "intent://x#Intent;end", "tel:999",
            "meteo.go.ke", "", "ftp://meteo.go.ke", "//meteo.go.ke", " https://meteo.go.ke/", "https://", "https:///x",
            "https://meteo.go.ke@evil.example/", "https://meteo.go.ke/a b", "https://meteo.go.ke/\nx",
        ).forEach { assertFalse(OfficialLinks.isOpenable(it), "should reject: $it") }
    }

    @Test fun acceptsQueryAndUppercaseScheme() {
        assertTrue(OfficialLinks.isOpenable("HTTPS://meteo.go.ke/a?b=c#d"))
        assertTrue(OfficialLinks.isOpenable("https://meteo.go.ke?x=1"))
    }

    @Test fun userInfoOnlyCountsInHost() = assertTrue(OfficialLinks.isOpenable("https://meteo.go.ke/a?mail=x@y"))

    @Test fun rejectsHugeUrls() = assertFalse(OfficialLinks.isOpenable("https://a.co/" + "x".repeat(3000)))

    @Test fun kmdUrlIsHttpsMeteo() = assertEquals("https://meteo.go.ke/", OfficialLinks.KMD_COUNTY_FORECASTS.substringBefore("our-products"))
}
