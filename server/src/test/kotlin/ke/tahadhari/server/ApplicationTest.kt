package ke.tahadhari.server

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
}
