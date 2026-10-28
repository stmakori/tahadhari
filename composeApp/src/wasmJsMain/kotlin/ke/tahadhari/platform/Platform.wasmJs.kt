package ke.tahadhari.platform

import androidx.compose.runtime.Composable
import kotlinx.browser.window

/** Browsers cannot dial: the number is copied to the clipboard (needs HTTPS, as on GitHub Pages). */
actual fun dialNumber(number: String) = copy(number)

actual fun shareText(text: String, title: String) = copy(text)

actual val copiesToClipboard: Boolean = true

private fun copy(text: String) {
    try {
        window.navigator.clipboard.writeText(text)
    } catch (_: Throwable) {
        // Clipboard blocked: nothing to do.
    }
}

/** Location is not offered on the web build: the button is hidden. */
@Composable
actual fun rememberLocationRequester(onResult: (LocationResult) -> Unit): (() -> Unit)? = null

/** No notifications in the browser: the risk-alert switch is hidden. */
@Composable
actual fun rememberNotificationRequester(onResult: (allowed: Boolean) -> Unit): (() -> Unit)? = null

actual fun onRiskAlertsChanged(enabled: Boolean) {}
