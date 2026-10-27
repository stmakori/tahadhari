package ke.tahadhari.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ke.tahadhari.shared.ai.ExplainRequest
import ke.tahadhari.shared.ai.ExplainResponse

fun interface ExplainClient {
    suspend fun explain(request: ExplainRequest): ExplainResponse
}

/** Talks to the Tahadhari server, which holds the AI key. */
class ProxyExplainClient(private val http: HttpClient, private val baseUrl: String) : ExplainClient {
    override suspend fun explain(request: ExplainRequest): ExplainResponse {
        val res = http.post("${baseUrl.trimEnd('/')}/v1/explain") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(res.status.isSuccess()) { "AI server returned ${res.status}" }
        return res.body()
    }
}
