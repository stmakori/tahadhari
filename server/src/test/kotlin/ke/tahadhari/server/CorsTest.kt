package ke.tahadhari.server

import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CorsTest {
    private val requestJson = """
        {"language":"en","countyName":"Narok","town":"Narok","riskLevel":"HIGH",
         "wettestDayName":"Wednesday","wettestDayMm":62.0,"threeDayTotalMm":101.0,
         "hotspots":["Mosiro"],"reasons":["HEAVY_DAY"]}
    """.trimIndent()
    private val goodReply = """{"headline":"Heavy rain in Narok","body":"About 62 mm of rain is expected. Low areas like Mosiro may flood.","actions":["Plan where to go if water rises","Do not cross flooded roads"]}"""
    private val pages = "https://user.github.io"

    @Test fun allowedOriginGetsCorsHeader() = testApplication {
        application { module(llm = { _, _ -> goodReply }, allowedOrigins = listOf(pages)) }
        val res = client.post("/v1/explain") {
            header(HttpHeaders.Origin, pages)
            contentType(ContentType.Application.Json); setBody(requestJson)
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(pages, res.headers[HttpHeaders.AccessControlAllowOrigin])
        assertNull(res.headers[HttpHeaders.AccessControlAllowCredentials])
    }

    @Test fun disallowedOriginGetsNoCorsHeader() = testApplication {
        application { module(llm = { _, _ -> goodReply }, allowedOrigins = listOf(pages)) }
        val res = client.post("/v1/explain") {
            header(HttpHeaders.Origin, "https://evil.example")
            contentType(ContentType.Application.Json); setBody(requestJson)
        }
        assertNull(res.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test fun preflightIsAnsweredForAllowedOrigin() = testApplication {
        application { module(llm = { _, _ -> goodReply }, allowedOrigins = listOf(pages)) }
        val res = client.options("/v1/explain") {
            header(HttpHeaders.Origin, pages)
            header(HttpHeaders.AccessControlRequestMethod, "POST")
            header(HttpHeaders.AccessControlRequestHeaders, "Content-Type")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals(pages, res.headers[HttpHeaders.AccessControlAllowOrigin])
        // POST is a CORS "simple" method, so Ktor does not list it in Allow-Methods; the preflight still succeeds.
        val allowed = res.headers[HttpHeaders.AccessControlAllowHeaders]
        assertTrue(allowed.orEmpty().contains("Content-Type", ignoreCase = true), "Allow-Headers was: $allowed")
    }

    @Test fun preflightIsRefusedForDisallowedOrigin() = testApplication {
        application { module(llm = { _, _ -> goodReply }, allowedOrigins = listOf(pages)) }
        val res = client.options("/v1/explain") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.AccessControlRequestMethod, "POST")
        }
        assertNull(res.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test fun defaultAllowsOnlyLocalDevServer() = testApplication {
        application { module(llm = { _, _ -> goodReply }) }
        val local = client.post("/v1/explain") {
            header(HttpHeaders.Origin, "http://localhost:8080")
            contentType(ContentType.Application.Json); setBody(requestJson)
        }
        assertEquals("http://localhost:8080", local.headers[HttpHeaders.AccessControlAllowOrigin])
        val other = client.post("/v1/explain") {
            header(HttpHeaders.Origin, pages)
            contentType(ContentType.Application.Json); setBody(requestJson)
        }
        assertNull(other.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test fun configParsesOriginsAndModelDefault() {
        val base = mapOf("ANTHROPIC_API_KEY" to "k")
        assertEquals(DEFAULT_ALLOWED_ORIGINS, ServerConfig.fromEnv(base).allowedOrigins)
        assertEquals("claude-haiku-5-5", ServerConfig.fromEnv(base).model)
        val cfg = ServerConfig.fromEnv(base + ("TAHADHARI_ALLOWED_ORIGINS" to " https://a.github.io/ , ,http://localhost:8080"))
        assertEquals(listOf("https://a.github.io", "http://localhost:8080"), cfg.allowedOrigins)
        assertEquals(DEFAULT_ALLOWED_ORIGINS, ServerConfig.fromEnv(base + ("TAHADHARI_ALLOWED_ORIGINS" to "  ")).allowedOrigins)
    }

    @Test fun configTrimsPastedApiKey() {
        // Keys pasted into a host dashboard often carry a trailing space or newline, which Anthropic rejects (401).
        assertEquals("sk-test", ServerConfig.fromEnv(mapOf("ANTHROPIC_API_KEY" to "  sk-test\n")).apiKey)
    }
}
