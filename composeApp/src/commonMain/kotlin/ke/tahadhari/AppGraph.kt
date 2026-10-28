package ke.tahadhari

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import ke.tahadhari.ai.AiExplainer
import ke.tahadhari.ai.ProxyExplainClient
import ke.tahadhari.ai.aiProxyUrl
import ke.tahadhari.data.EnsembleRepository
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.Preferences
import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.data.store.platformKeyValueStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import ke.tahadhari.data.remote.OpenMeteoApi
import ke.tahadhari.data.remote.OpenMeteoEnsembleApi
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

    /** Current wall-clock time in epoch millis. */
    @OptIn(ExperimentalTime::class)
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Persistent key-value store (`~/.tahadhari/` on desktop, app files dir on Android).
     * On Android, `AndroidStorage.init(context)` must run before first access, otherwise data is
     * kept in memory only.
     */
    val store: KeyValueStore by lazy { platformKeyValueStore() }
    val forecastCache: ForecastCache by lazy { ForecastCache(store) }
    val preferences: Preferences by lazy { Preferences(store) }

    val repository: ForecastRepository by lazy {
        ForecastRepository(OpenMeteoApi(httpClient), forecastCache, ::nowMillis)
    }
    val riskEngine: RiskEngine by lazy { RiskEngine() }
    /** Ensemble chance of heavy rain, one county at a time. Information only, never feeds [riskEngine]. */
    val ensemble: EnsembleRepository by lazy { EnsembleRepository(OpenMeteoEnsembleApi(httpClient)) }
    /** AI explanations when a server URL is configured, offline templates otherwise. */
    val explainer: AlertExplainer by lazy {
        val url = aiProxyUrl()
        if (url.isBlank()) TemplateExplainer() else AiExplainer(ProxyExplainClient(httpClient, url))
    }
}
