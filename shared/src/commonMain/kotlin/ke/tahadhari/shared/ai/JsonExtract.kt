package ke.tahadhari.shared.ai

/** Pulls the JSON object out of a model reply that may include code fences or extra text. */
object JsonExtract {
    fun objectIn(raw: String): String? {
        val cleaned = raw.replace("```json", "").replace("```", "")
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else null
    }
}
