package ke.tahadhari.ui.mobile

import ke.tahadhari.data.County
import ke.tahadhari.data.store.KeyValueStore
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskAssessment
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.decideRiskAlert
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings

/** Text of the "risk rose" notification. */
data class RiskAlertNotification(val title: String, val text: String)

/** "Severe flood risk: Baringo County" + "Up to 99 mm of rain on Thursday. Not an official warning." */
fun riskAlertNotification(county: County, assessment: RiskAssessment, language: Language): RiskAlertNotification {
    val strings = Strings.of(language)
    return RiskAlertNotification(
        title = "${strings.riskHeadline(assessment.level)}: ${strings.countyLabel(county.name)}",
        text = "${mainReasonText(assessment, language, strings)}. ${strings.notifNotOfficial}",
    )
}

/**
 * The level last notified, kept in its own small key (not in `UserPreferences`).
 * One entry per county, stored as "4:HIGH,33:SEVERE" (the older single "4:HIGH" form reads the same).
 * Each county's level is separate. A county without an entry, or a corrupt entry, reads as "nothing notified".
 */
class RiskAlertState(private val store: KeyValueStore) {
    private suspend fun read(): Map<Int, RiskLevel> {
        val text = store.get(KEY) ?: return emptyMap()
        return text.split(",").mapNotNull { pair ->
            val parts = pair.split(":").takeIf { it.size == 2 } ?: return@mapNotNull null
            val code = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
            val level = RiskLevel.entries.firstOrNull { it.name == parts[1].trim() } ?: return@mapNotNull null
            code to level
        }.toMap()
    }

    private suspend fun write(map: Map<Int, RiskLevel>) {
        if (map.isEmpty()) store.remove(KEY) else store.put(KEY, map.entries.joinToString(",") { "${it.key}:${it.value.name}" })
    }

    suspend fun lastNotified(county: County): RiskLevel? = read()[county.code]

    suspend fun save(county: County, level: RiskLevel?) {
        val map = read().toMutableMap()
        if (level == null) map.remove(county.code) else map[county.code] = level
        write(map)
    }

    /** Forgets counties that are no longer followed, so following one again later starts fresh. */
    suspend fun retainOnly(codes: Set<Int>) {
        val map = read()
        val kept = map.filterKeys { it in codes }
        if (kept.size != map.size) write(kept)
    }

    suspend fun clear() = store.remove(KEY)

    private companion object { const val KEY = "risk-alert-state" }
}

/**
 * Runs the notification rules for one background refresh and updates the remembered level.
 * Returns the notification to show, or null. Only a freshly downloaded forecast can notify;
 * cached or failed loads change nothing.
 */
suspend fun checkRiskAlert(county: County, loaded: LoadState, language: Language, state: RiskAlertState): RiskAlertNotification? {
    val ready = loaded as? LoadState.Ready
    val before = state.lastNotified(county)
    val decision = decideRiskAlert(before, ready?.assessment?.level, fresh = ready != null && !ready.fromCache)
    if (decision.lastNotified != before) state.save(county, decision.lastNotified)
    return if (decision.notify && ready != null) riskAlertNotification(county, ready.assessment, language) else null
}

/**
 * Runs the rules for several counties (own county + watched ones) after one background refresh.
 * Each county keeps its own remembered level; returns one notification per county that rose.
 * Counties missing from [loaded] are treated as failed (no notification, memory unchanged).
 */
suspend fun checkRiskAlerts(
    counties: List<County>,
    loaded: Map<Int, LoadState>,
    language: Language,
    state: RiskAlertState,
): List<Pair<County, RiskAlertNotification>> {
    state.retainOnly(counties.map { it.code }.toSet())
    return counties.mapNotNull { county ->
        val n = checkRiskAlert(county, loaded[county.code] ?: LoadState.Failed("no data"), language, state)
        n?.let { county to it }
    }
}
