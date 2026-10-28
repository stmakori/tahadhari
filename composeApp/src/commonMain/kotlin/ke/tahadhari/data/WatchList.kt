package ke.tahadhari.data

/**
 * Rules for the "Counties I watch" list: known county codes only, no duplicates, never the
 * user's own county, at most [MAX] entries, order kept. Anything read from storage goes through [sanitize].
 */
object WatchList {
    const val MAX = 3

    fun sanitize(codes: Iterable<Int>, ownCode: Int?): List<Int> =
        codes.asSequence()
            .filter { Counties.byCode(it) != null && it != ownCode }
            .distinct()
            .take(MAX)
            .toList()

    fun canAdd(list: List<Int>, code: Int, ownCode: Int?): Boolean =
        list.size < MAX && code != ownCode && code !in list && Counties.byCode(code) != null

    /** The list with [code] added at the end, or unchanged when it cannot be added. */
    fun add(list: List<Int>, code: Int, ownCode: Int?): List<Int> =
        if (canAdd(list, code, ownCode)) sanitize(list + code, ownCode) else sanitize(list, ownCode)

    fun remove(list: List<Int>, code: Int): List<Int> = list.filter { it != code }

    /** Parses "4,33,7" (the in-memory saved form). Garbage entries are skipped. */
    fun parse(text: String, ownCode: Int?): List<Int> =
        sanitize(text.split(",").mapNotNull { it.trim().toIntOrNull() }, ownCode)

    fun encode(list: List<Int>): String = list.joinToString(",")
}
