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
 * Stored as "<countyCode>:<LEVEL>". A different county or a corrupt value reads as "nothing notified".
 */
class RiskAlertState(private val store: KeyValueStore) {
    suspend fun lastNotified(county: County): RiskLevel? {
        val (code, level) = (store.get(KEY) ?: return null).split(":").takeIf { it.size == 2 } ?: return null
        if (code != county.code.toString()) return null
        return RiskLevel.entries.firstOrNull { it.name == level }
    }

    suspend fun save(county: County, level: RiskLevel?) {
        if (level == null) store.remove(KEY) else store.put(KEY, "${county.code}:${level.name}")
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
