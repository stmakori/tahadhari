package ke.tahadhari

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import ke.tahadhari.ai.AiExplainer
import ke.tahadhari.ai.ProxyExplainClient
import ke.tahadhari.data.Counties
import ke.tahadhari.data.ForecastCache
import ke.tahadhari.data.ForecastRepository
import ke.tahadhari.data.remote.OpenMeteoApi
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.*
import ke.tahadhari.shared.ai.ExplainContract
import ke.tahadhari.shared.ai.ExplainRequest
import ke.tahadhari.shared.ai.ExplainResponse
import ke.tahadhari.shared.ai.PromptBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.test.Test

/**
 * Tries the AI prompt on real forecasts and writes `docs/ai-examples.md` for review.
 *
 * Gated: does nothing unless TAHADHARI_AI_EXAMPLES=1. Needs the network and a running AI server:
 *
 *   ANTHROPIC_API_KEY=sk-... ./gradlew :server:run          # in one terminal
 *   TAHADHARI_AI_EXAMPLES=1 ./gradlew :composeApp:desktopTest --tests "ke.tahadhari.AiExamplesTest"
 *
 * The server URL comes from TAHADHARI_PROXY_URL (default http://localhost:8080). It fetches all 47
 * counties (2 batched Open-Meteo calls), takes the 9 highest-risk and the 3 lowest-risk, and asks for
 * English and Swahili: 24 AI calls. Each example shows the exact facts the AI got, its reply, and
 * automatic checks; a person must still read every example (Swahili by a fluent speaker).
 */
class AiExamplesTest {

    @Test
    fun writeExamples() {
        if (System.getenv("TAHADHARI_AI_EXAMPLES") != "1") return
        val serverUrl = System.getenv("TAHADHARI_PROXY_URL") ?: "http://localhost:8080"
        val http = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 30_000 }
        }
        val engine = RiskEngine()
        val repo = ForecastRepository(OpenMeteoApi(http), ForecastCache(InMemoryKeyValueStore()), nowMillis = System::currentTimeMillis)
        val client = ProxyExplainClient(http, serverUrl)

        val assessed = runBlocking { repo.forecastsFor(Counties.all) }
            .mapNotNull { (code, result) -> result.getOrNull()?.let { Counties.byCode(code)!! to engine.assess(it.forecast) } }
        check(assessed.size >= 12) { "Only ${assessed.size} counties loaded; check the network" }
        val ranked = assessed.sortedWith(compareByDescending<Pair<*, RiskAssessment>> { it.second.level }.thenByDescending { it.second.maxThreeDayTotalMm })
        val picked = (ranked.take(9) + ranked.takeLast(3)).distinctBy { it.first.code }

        val examples = picked.map { (county, assessment) ->
            val results = Language.entries.associateWith { lang ->
                val request = AiExplainer.toRequest(county, assessment, lang)
                request to runBlocking { runCatching { client.explain(request) } }
            }
            Triple(county, assessment, results)
        }

        val today = LocalDate.now(ZoneId.of("Africa/Nairobi"))
        val md = buildString {
            appendLine("# AI explanation examples")
            appendLine()
            appendLine("Generated $today by `AiExamplesTest` from live Open-Meteo forecasts, through the AI server at `$serverUrl`")
            appendLine("(model: the server's `TAHADHARI_MODEL`, default `claude-haiku-5-5`). Regenerate with:")
            appendLine()
            appendLine("```bash")
            appendLine("ANTHROPIC_API_KEY=sk-... ./gradlew :server:run")
            appendLine("TAHADHARI_AI_EXAMPLES=1 ./gradlew :composeApp:desktopTest --tests \"ke.tahadhari.AiExamplesTest\"")
            appendLine("```")
            appendLine()
            appendLine("The risk level always comes from `RiskEngine`; the AI only writes the words. \"Numbers not in the facts\"")
            appendLine("flags any number in the reply that the app did not send, so a reviewer can check it was not invented.")
            appendLine("Tick each review box only after reading the example; Swahili needs a fluent speaker.")
            appendLine()
            appendLine("| County | Level | Reasons | English | Swahili |")
            appendLine("|---|---|---|---|---|")
            examples.forEach { (county, a, results) ->
                val status = { lang: Language -> statusOf(results.getValue(lang).first, results.getValue(lang).second) }
                appendLine("| ${county.name} | ${a.level} | ${results.getValue(Language.ENGLISH).first.reasons.joinToString().ifEmpty { "–" }} | ${status(Language.ENGLISH)} | ${status(Language.SWAHILI)} |")
            }
            examples.forEach { (county, a, results) ->
                appendLine()
                appendLine("## ${county.name}: ${a.level}")
                appendLine()
                appendLine("Facts sent to the AI (English request):")
                appendLine()
                appendLine("```text")
                appendLine(PromptBuilder.user(results.getValue(Language.ENGLISH).first))
                appendLine("```")
                results.forEach { (lang, pair) ->
                    val (request, outcome) = pair
                    appendLine()
                    appendLine("### ${if (lang == Language.SWAHILI) "Kiswahili" else "English"}")
                    appendLine()
                    val valid = outcome.getOrNull()?.let(ExplainContract::validateResponse)
                    when {
                        outcome.isFailure -> appendLine("Server error: `${outcome.exceptionOrNull()?.message}`. The app would show the offline template.")
                        valid == null -> appendLine("Reply failed validation. The app would show the offline template.")
                        else -> {
                            appendLine("**${valid.headline}**")
                            appendLine()
                            appendLine(valid.body)
                            appendLine()
                            valid.actions.forEach { appendLine("- $it") }
                            appendLine()
                            val extra = numbersNotInFacts(request, valid)
                            appendLine(if (extra.isEmpty()) "Automatic check: every number is in the facts." else "Automatic check: **numbers not in the facts: ${extra.joinToString()}**")
                        }
                    }
                    appendLine()
                    appendLine("- [ ] Reviewed${if (lang == Language.SWAHILI) " by a fluent Swahili speaker" else ""}: accurate, calm, matches the level, no invented facts")
                }
            }
        }

        val docs = findDocsDir()
        File(docs, "ai-examples.md").writeText(md)
        println("Wrote ${File(docs, "ai-examples.md").absolutePath} (${examples.size} counties)")
    }

    private fun statusOf(request: ExplainRequest, outcome: Result<ExplainResponse>): String {
        val valid = outcome.getOrNull()?.let(ExplainContract::validateResponse) ?: return "fallback"
        return if (numbersNotInFacts(request, valid).isEmpty()) "AI" else "AI, check numbers"
    }

    /** Whole numbers in the reply that are not among the facts sent (rounded mm, %, ratio, dates, emergency numbers). */
    private fun numbersNotInFacts(r: ExplainRequest, reply: ExplainResponse): Set<String> {
        val allowed = buildSet {
            r.wettestDayMm?.let { add(it.roundToInt().toString()) }
            r.wettestDayProbability?.let { add(it.toString()) }
            add(r.threeDayTotalMm.roundToInt().toString())
            r.riverRiseRatio?.let { val one = (it * 10).roundToInt() / 10.0; add(one.toString()); add(it.roundToInt().toString()) }
            r.wettestDayDate?.split("-")?.forEach { add(it); add(it.trimStart('0')) }
            addAll(listOf("999", "112", "1199", "3", "7")) // emergency numbers; "3-day", "7 days" in the facts' wording
        }
        val text = (listOf(reply.headline, reply.body) + reply.actions).joinToString(" ")
        return Regex("""\d+(?:\.\d+)?""").findAll(text).map { it.value }.filter { it !in allowed }.toSet()
    }

    private fun findDocsDir(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "docs")
            if (File(dir, "settings.gradle.kts").isFile && candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("docs/ not found")
    }
}
