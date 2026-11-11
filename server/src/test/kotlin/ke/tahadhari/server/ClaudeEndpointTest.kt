package ke.tahadhari.server

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClaudeEndpointTest {
    private val reply = """{"content":[{"type":"text","text":"ok"}]}"""

    private fun capture(endpoint: ClaudeEndpoint, status: HttpStatusCode = HttpStatusCode.OK, body: String = reply):
        Pair<AnthropicCompleter, MutableList<io.ktor.client.request.HttpRequestData>> {
        val seen = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val http = HttpClient(MockEngine { request ->
            seen += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) } }
        return AnthropicCompleter("key-123", "claude-haiku-5-5", endpoint, http) to seen
    }

    @Test fun claudeApiUsesXApiKey() = runBlocking {
        val (llm, seen) = capture(ClaudeEndpoint.CLAUDE_API)
        assertEquals("ok", llm.complete("s", "u"))
        val r = seen.single()
        assertEquals("https://api.anthropic.com/v1/messages", r.url.toString())
        assertEquals("key-123", r.headers["x-api-key"])
        assertNull(r.headers[HttpHeaders.Authorization])
        assertNull(r.headers["anthropic-workspace-id"])
    }

    @Test fun awsPlatformUsesBearerAndWorkspace() = runBlocking {
        val (llm, seen) = capture(ClaudeEndpoint.awsPlatform("eu-central-1", "wrkspc_01AbC"))
        assertEquals("ok", llm.complete("s", "u"))
        val r = seen.single()
        assertEquals("https://aws-external-anthropic.eu-central-1.api.aws/v1/messages", r.url.toString())
        assertEquals("Bearer key-123", r.headers[HttpHeaders.Authorization])
        assertEquals("wrkspc_01AbC", r.headers["anthropic-workspace-id"])
        assertNull(r.headers["x-api-key"])
    }

    @Test fun sendsThinkingDisabledAndReportsCutOffReply() = runBlocking {
        val (llm, seen) = capture(ClaudeEndpoint.CLAUDE_API)
        llm.complete("s", "u")
        val sent = (seen.single().body as io.ktor.http.content.TextContent).text
        assertTrue(""""thinking":{"type":"disabled"}""" in sent, sent)
        val (cut, _) = capture(ClaudeEndpoint.CLAUDE_API, body = """{"content":[{"type":"text","text":"{\"head"}],"stop_reason":"max_tokens"}""")
        val e = assertFailsWith<IllegalStateException> { cut.complete("s", "u") }
        assertTrue("cut off" in e.message.orEmpty(), e.message)
    }

    @Test fun errorIncludesReasonFromBody() = runBlocking {
        val (llm, _) = capture(ClaudeEndpoint.CLAUDE_API, HttpStatusCode.Unauthorized,
            """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}""")
        val e = assertFailsWith<IllegalStateException> { llm.complete("s", "u") }
        assertTrue("invalid x-api-key" in e.message.orEmpty(), e.message)
    }

    @Test fun configSelectsAwsPlatformWhenWorkspaceIsSet() {
        val cfg = ServerConfig.fromEnv(mapOf(
            "ANTHROPIC_API_KEY" to " aws-key\n", "ANTHROPIC_AWS_WORKSPACE_ID" to "wrkspc_01AbC", "AWS_REGION" to "eu-central-1",
        ))
        assertEquals("aws-key", cfg.apiKey)
        assertEquals("https://aws-external-anthropic.eu-central-1.api.aws", cfg.endpoint.baseUrl)
        assertEquals("claude-haiku-5-5", cfg.model)
        val own = ServerConfig.fromEnv(mapOf(
            "ANTHROPIC_AWS_API_KEY" to "a", "ANTHROPIC_API_KEY" to "b",
            "ANTHROPIC_AWS_WORKSPACE_ID" to "wrkspc_1", "AWS_REGION" to "us-west-2",
        ))
        assertEquals("a", own.apiKey)
        assertEquals(ClaudeEndpoint.CLAUDE_API, ServerConfig.fromEnv(mapOf("ANTHROPIC_API_KEY" to "k")).endpoint)
    }

    @Test fun configRejectsBadAwsSettings() {
        val base = mapOf("ANTHROPIC_API_KEY" to "k", "ANTHROPIC_AWS_WORKSPACE_ID" to "wrkspc_1")
        assertFailsWith<IllegalStateException> { ServerConfig.fromEnv(base) } // no region
        assertFailsWith<IllegalArgumentException> { ServerConfig.fromEnv(base + ("AWS_REGION" to "evil.com/x")) }
        assertFailsWith<IllegalArgumentException> {
            ServerConfig.fromEnv(base + ("AWS_REGION" to "eu-central-1") + ("ANTHROPIC_AWS_WORKSPACE_ID" to "x/y"))
        }
    }
}
