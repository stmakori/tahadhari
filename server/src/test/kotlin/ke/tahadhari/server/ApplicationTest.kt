package ke.tahadhari.server

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    private val requestJson = """
        {"language":"en","countyName":"Narok","town":"Narok","riskLevel":"HIGH",
         "wettestDayName":"Wednesday","wettestDayMm":62.0,"threeDayTotalMm":101.0,
         "hotspots":["Mosiro"],"reasons":["HEAVY_DAY"]}
    """.trimIndent()

    private val goodReply = """```json
        {"headline":"Heavy rain in Narok on Wednesday","body":"About 62 mm of rain is expected on Wednesday. Low areas like Mosiro may flood.","actions":["Plan where to go if water rises","Do not cross flooded roads"]}
        ```"""

    @Test fun returnsParsedExplanationAndCachesIt() = testApplication {
        var calls = 0
        application { module(llm = { _, _ -> calls++; goodReply }) }
        repeat(2) {
            val res = client.post("/v1/explain") { contentType(ContentType.Application.Json); setBody(requestJson) }
            assertEquals(HttpStatusCode.OK, res.status)
            assertTrue(res.bodyAsText().contains("Mosiro"))
        }
        assertEquals(1, calls)
    }

    @Test fun rejectsInvalidRequest() = testApplication {
        application { module(llm = { _, _ -> goodReply }) }
        val res = client.post("/v1/explain") {
            contentType(ContentType.Application.Json); setBody(requestJson.replace("\"en\"", "\"fr\""))
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test fun badModelOutputIsBadGateway() = testApplication {
        application { module(llm = { _, _ -> "Sorry, I can't help with that." }) }
        val res = client.post("/v1/explain") { contentType(ContentType.Application.Json); setBody(requestJson) }
        assertEquals(HttpStatusCode.BadGateway, res.status)
    }

    @Test fun clientKeyUsesSocketAddressUnlessProxiesAreTrusted() {
        assertEquals("10.0.0.1", clientKey("1.2.3.4", "10.0.0.1", trustedProxyHops = 0))
        assertEquals("10.0.0.1", clientKey(null, "10.0.0.1", trustedProxyHops = 1))
        assertEquals("1.2.3.4", clientKey("1.2.3.4", "10.0.0.1", trustedProxyHops = 1))
        // A client-supplied X-Forwarded-For value is to the left of what the proxy appends: ignored.
        assertEquals("1.2.3.4", clientKey("6.6.6.6, 1.2.3.4", "10.0.0.1", trustedProxyHops = 1))
        assertEquals("1.2.3.4", clientKey("6.6.6.6, 1.2.3.4, 172.16.0.9", "10.0.0.1", trustedProxyHops = 2))
        assertEquals("10.0.0.1", clientKey("1.2.3.4", "10.0.0.1", trustedProxyHops = 2))
    }

    @Test fun spoofedForwardedForDoesNotDodgeTheLimit() = testApplication {
        application { module(llm = { _, _ -> goodReply }, requestsPerHour = 2, trustedProxyHops = 1) }
        val codes = (1..3).map { i ->
            client.post("/v1/explain") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.XForwardedFor, "9.9.9.$i, 1.2.3.4")
                setBody(requestJson)
            }.status
        }
        assertEquals(listOf(HttpStatusCode.OK, HttpStatusCode.OK, HttpStatusCode.TooManyRequests), codes)
    }

    @Test fun acceptsLandslideReasonCode() = testApplication {
        var prompt = ""
        application { module(llm = { system, user -> prompt = system + user; goodReply }) }
        val res = client.post("/v1/explain") {
            contentType(ContentType.Application.Json)
            setBody(requestJson.replace("\"HEAVY_DAY\"", "\"WET_SPELL\",\"LANDSLIDE\""))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(prompt.contains("Reasons: WET_SPELL, LANDSLIDE"))
    }
}
