package ke.tahadhari.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Anything that can turn a system + user prompt into text. Swappable for tests or other providers. */
fun interface LlmCompleter {
    suspend fun complete(system: String, user: String): String
}

/**
 * Calls the Anthropic Messages API, on the Claude API or Claude Platform on AWS (see [ClaudeEndpoint]).
 * Docs: https://docs.claude.com/en/api/messages
 */
class AnthropicCompleter(
    private val apiKey: String,
    private val model: String,
    private val endpoint: ClaudeEndpoint = ClaudeEndpoint.CLAUDE_API,
    private val http: HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    },
) : LlmCompleter {

    override suspend fun complete(system: String, user: String): String {
        val response = http.post("${endpoint.baseUrl}/v1/messages") {
            if (endpoint.awsWorkspaceId == null) {
                header("x-api-key", apiKey)
            } else {
                header("Authorization", "Bearer $apiKey")
                header("anthropic-workspace-id", endpoint.awsWorkspaceId)
            }
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            // Thinking off: this is a short, fully specified JSON rewrite, and the app waits at most 12 s.
            // Claude Haiku 5.5 thinks by default, which spent the token budget and cut the JSON short.
            setBody(
                MessagesRequest(
                    model, maxTokens = MAX_TOKENS, system = system, messages = listOf(Message("user", user)),
                    thinking = Thinking("disabled"),
                ),
            )
        }
        if (!response.status.isSuccess()) {
            // The error body says why (bad key, no model access, no credit). It never contains the key.
            val detail = runCatching { response.bodyAsText() }.getOrDefault("").replace(Regex("\\s+"), " ").take(300)
            error("Anthropic API returned ${response.status}: $detail")
        }
        val body = response.body<MessagesResponse>()
        // A cut-off or refused reply can't be valid JSON; say why in the log instead of "unusable output".
        check(body.stopReason != "max_tokens") { "Reply cut off at $MAX_TOKENS tokens" }
        check(body.stopReason != "refusal") { "Model declined the request" }
        return body.content
            .filter { it.type == "text" }
            .joinToString("") { it.text.orEmpty() }
    }

    private companion object {
        const val MAX_TOKENS = 1_000
    }

    @Serializable
    private data class MessagesRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String,
        val messages: List<Message>,
        val thinking: Thinking,
    )

    @Serializable
    private data class Thinking(val type: String)

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class MessagesResponse(
        val content: List<ContentBlock>,
        @SerialName("stop_reason") val stopReason: String? = null,
    )

    @Serializable
    private data class ContentBlock(val type: String, val text: String? = null)
}
