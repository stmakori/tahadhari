package ke.tahadhari.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import ke.tahadhari.shared.ai.ExplainContract
import ke.tahadhari.shared.ai.ExplainRequest
import ke.tahadhari.shared.ai.PromptBuilder
import ke.tahadhari.shared.ai.ResponseParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

@Serializable
data class ErrorBody(val error: String)

private val log = LoggerFactory.getLogger("Tahadhari")
private val explainLimit = RateLimitName("explain")

fun main() {
    val config = ServerConfig.fromEnv()
    val llm = AnthropicCompleter(config.apiKey, config.model)
    log.info("Tahadhari AI server on port ${config.port}, model ${config.model}")
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(llm, requestsPerHour = config.requestsPerHour, allowedOrigins = config.allowedOrigins, trustedProxyHops = config.trustedProxyHops)
    }.start(wait = true)
}

/**
 * The address to rate-limit by. Behind a hosting platform every request comes from its load balancer,
 * so with [trustedProxyHops] = N the client is the Nth entry from the right of X-Forwarded-For (each
 * trusted proxy appends one). Entries further left are client-supplied and ignored, so they cannot be
 * used to dodge the limit. Too few entries means the request skipped the proxies: use the socket address.
 */
fun clientKey(forwardedFor: String?, remoteHost: String, trustedProxyHops: Int): String {
    if (trustedProxyHops <= 0 || forwardedFor == null) return remoteHost
    val hops = forwardedFor.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    return hops.getOrNull(hops.size - trustedProxyHops) ?: remoteHost
}

fun Application.module(
    llm: LlmCompleter,
    requestsPerHour: Int = 60,
    allowedOrigins: List<String> = DEFAULT_ALLOWED_ORIGINS,
    trustedProxyHops: Int = 0,
) {
    val cache = ExplanationCache()

    // Browser (web build) access: only the listed origins, POST + Content-Type, no credentials.
    install(CORS) {
        allowedOrigins.forEach { origin ->
            allowHost(origin.substringAfter("://"), schemes = listOf(origin.substringBefore("://")))
        }
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = false
    }

    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(StatusPages) {
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, ErrorBody("invalid_json")) }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorBody("internal_error"))
        }
    }
    install(RateLimit) {
        register(explainLimit) {
            rateLimiter(limit = requestsPerHour, refillPeriod = 1.hours)
            requestKey { call -> clientKey(call.request.headers[HttpHeaders.XForwardedFor], call.request.origin.remoteHost, trustedProxyHops) }
        }
    }

    routing {
        get("/health") { call.respondText("ok") }

        rateLimit(explainLimit) {
            post("/v1/explain") {
                val request = call.receive<ExplainRequest>()
                ExplainContract.validateRequest(request)?.let {
                    call.respond(HttpStatusCode.BadRequest, ErrorBody(it))
                    return@post
                }

                cache.get(request)?.let {
                    call.respond(it)
                    return@post
                }

                val raw = try {
                    llm.complete(PromptBuilder.system, PromptBuilder.user(request))
                } catch (e: Exception) {
                    log.warn("LLM call failed: ${e.message}")
                    call.respond(HttpStatusCode.BadGateway, ErrorBody("ai_unavailable"))
                    return@post
                }

                val parsed = ResponseParser.parse(raw)
                if (parsed == null) {
                    log.warn("Unusable model output for ${request.countyName}")
                    call.respond(HttpStatusCode.BadGateway, ErrorBody("bad_ai_output"))
                    return@post
                }
                cache.put(request, parsed)
                call.respond(parsed)
            }
        }
    }
}
