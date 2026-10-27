package ke.tahadhari.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

/** Calls the Anthropic Messages API. Docs: https://docs.claude.com/en/api/messages */
class AnthropicCompleter(
    private val apiKey: String,
    private val model: String,
    private val http: HttpClient = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
        install(HttpTimeout) { requestTimeoutMillis = 20_000 }
    },
) : LlmCompleter {

    override suspend fun complete(system: String, user: String): String {
        val response = http.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(MessagesRequest(model, maxTokens = 600, system = system, messages = listOf(Message("user", user))))
        }
        check(response.status.isSuccess()) { "Anthropic API returned ${response.status}" }
        return response.body<MessagesResponse>().content
            .filter { it.type == "text" }
            .joinToString("") { it.text.orEmpty() }
    }

    @Serializable
    private data class MessagesRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String,
        val messages: List<Message>,
    )

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class MessagesResponse(val content: List<ContentBlock>)

    @Serializable
    private data class ContentBlock(val type: String, val text: String? = null)
}
