package ke.tahadhari.platform

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Desktop has no dialer or share sheet: both copy to the clipboard (ignored when headless). */
private fun copy(text: String) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    } catch (_: Exception) {
        // Headless or clipboard busy: nothing to do.
    }
}

actual fun dialNumber(number: String) = copy(number)

actual fun shareText(text: String, title: String) = copy(text)

/** Opens the default browser; where that is not supported (or fails) the address is copied instead. */
actual fun openUrl(url: String) {
    try {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            return
        }
    } catch (_: Exception) {
        // Fall through to the clipboard.
    }
    copy(url)
}

actual val copiesToClipboard: Boolean = true

/** No location on desktop: the "Use my location" button is hidden. */
@Composable
actual fun rememberLocationRequester(onResult: (LocationResult) -> Unit): (() -> Unit)? = null

/** No notifications on desktop: the risk-alert switch is hidden. */
@Composable
actual fun rememberNotificationRequester(onResult: (allowed: Boolean) -> Unit): (() -> Unit)? = null

actual fun onRiskAlertsChanged(enabled: Boolean) {}
