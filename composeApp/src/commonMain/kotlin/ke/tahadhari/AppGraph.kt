package ke.tahadhari

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import ke.tahadhari.ai.AiExplainer
import ke.tahadhari.ai.ProxyExplainClient
import ke.tahadhari.ai.aiProxyUrl
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.remote.OpenMeteoApi
import ke.tahadhari.domain.AlertExplainer
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.TemplateExplainer
import kotlinx.serialization.json.Json

/**
 * Simple manual dependency container. The HTTP engine is picked up automatically
 * from the classpath (OkHttp on Android, Java on desktop).
 * Swap for Koin later if the graph grows.
 */
object AppGraph {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
    }

    val repository: ForecastRepository by lazy { ForecastRepository(OpenMeteoApi(httpClient)) }
    val riskEngine: RiskEngine by lazy { RiskEngine() }
    /** AI explanations when a server URL is configured, offline templates otherwise. */
    val explainer: AlertExplainer by lazy {
        val url = aiProxyUrl()
        if (url.isBlank()) TemplateExplainer() else AiExplainer(ProxyExplainClient(httpClient, url))
    }
}
