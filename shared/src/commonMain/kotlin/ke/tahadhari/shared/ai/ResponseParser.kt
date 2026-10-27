package ke.tahadhari.shared.ai

import kotlinx.serialization.json.Json

object ResponseParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Parses and validates a raw model reply. Returns null if anything is wrong. */
    fun parse(raw: String): ExplainResponse? = runCatching {
        val obj = JsonExtract.objectIn(raw) ?: return null
        ExplainContract.validateResponse(json.decodeFromString(ExplainResponse.serializer(), obj))
    }.getOrNull()
}
