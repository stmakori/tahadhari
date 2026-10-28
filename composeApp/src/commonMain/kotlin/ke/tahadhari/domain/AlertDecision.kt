package ke.tahadhari.domain

/**
 * What the background check should do after loading a county.
 * [notify]: show one notification now. [lastNotified]: the level to remember for next time
 * (null = nothing notified, i.e. the next rise to High or Severe notifies).
 */
data class AlertDecision(val notify: Boolean, val lastNotified: RiskLevel?)

/**
 * Pure rules for the opt-in "risk rose to High or Severe" notification.
 *
 * - Notify only when the new level is High or Severe and higher than the last *notified* level
 *   (so Moderate -> High notifies, High -> Severe notifies, High -> High does not).
 * - When the level falls back to Moderate or Low the memory is cleared, so a later rise notifies again.
 * - When it eases from Severe to High there is no notification, but the memory follows it down,
 *   so a later return to Severe notifies again.
 * - Cached or failed data never notifies and never changes the memory: it says nothing about
 *   the present. (Failure must never be read as "Low", so it must not reset either.)
 *
 * @param lastNotified the level last notified for this county, or null.
 * @param newLevel the level just computed by `RiskEngine`, or null when loading failed.
 * @param fresh true only for a forecast downloaded just now (not from the saved cache).
 */
fun decideRiskAlert(lastNotified: RiskLevel?, newLevel: RiskLevel?, fresh: Boolean): AlertDecision {
    if (newLevel == null || !fresh) return AlertDecision(notify = false, lastNotified = lastNotified)
    if (newLevel < RiskLevel.HIGH) return AlertDecision(notify = false, lastNotified = null)
    val rose = lastNotified == null || newLevel > lastNotified
    return AlertDecision(notify = rose, lastNotified = newLevel)
}
