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

actual val copiesToClipboard: Boolean = true

/** No location on desktop: the "Use my location" button is hidden. */
@Composable
actual fun rememberLocationRequester(onResult: (LocationResult) -> Unit): (() -> Unit)? = null
