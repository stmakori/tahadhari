package ke.tahadhari.platform

import androidx.compose.runtime.Composable

// Thin platform features. All decisions (what to say, which county) stay in common code.

/**
 * Opens the phone dialer with [number] filled in. The user still presses call, so no
 * CALL_PHONE permission is needed. On desktop the number is copied to the clipboard instead.
 */
expect fun dialNumber(number: String)

/**
 * Shares [text] through the platform share sheet ([title] is the chooser title on Android).
 * On desktop the text is copied to the clipboard instead.
 */
expect fun shareText(text: String, title: String)

/** True where [dialNumber] and [shareText] copy to the clipboard, so the UI should say "copied". */
expect val copiesToClipboard: Boolean

/** Outcome of one "Use my location" request. */
sealed interface LocationResult {
    data class Found(val latitude: Double, val longitude: Double) : LocationResult
    data object PermissionDenied : LocationResult
    /** Location is off, no provider gave a fix in time, or something else failed. */
    data object Unavailable : LocationResult
}

/**
 * Returns a function that asks for permission if needed, gets one coarse location and calls
 * [onResult] once. Returns null where location is not supported (desktop), so callers can hide
 * the button.
 */
@Composable
expect fun rememberLocationRequester(onResult: (LocationResult) -> Unit): (() -> Unit)?

/**
 * Returns a function that makes sure the app may post notifications (asks on Android 13+) and calls
 * [onResult] once with true (allowed) or false (blocked). Returns null where risk notifications are
 * not offered (desktop), so callers can hide the switch.
 */
@Composable
expect fun rememberNotificationRequester(onResult: (allowed: Boolean) -> Unit): (() -> Unit)?

/**
 * Called after the saved "risk alerts" setting changed, so the platform can start or stop the
 * background refresh that checks the forecast. No-op on desktop.
 */
expect fun onRiskAlertsChanged(enabled: Boolean)
