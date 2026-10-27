package ke.tahadhari

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.MobileApp
import ke.tahadhari.ui.MobileTab
import ke.tahadhari.ui.Spacing
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.TahadhariTheme
import ke.tahadhari.ui.mobile.CheckInRow
import ke.tahadhari.data.Counties
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Phone-size (412×860 dp) screenshots of the Safety and Emergency tabs, light/dark and EN/SW,
 * written to `composeApp/build/screenshots/`. Tall variants show the whole scrolling screen.
 * Skipped unless TAHADHARI_SCREENSHOTS=1:
 *   TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest --tests '*MobileScreensScreenshotTest*'
 */
class MobileScreensScreenshotTest {
    private val enabled = System.getenv("TAHADHARI_SCREENSHOTS") == "1"
    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun shoot(name: String, heightDp: Int = 860, content: @Composable () -> Unit) {
        if (!enabled) return
        ImageComposeScene(412 * 2, heightDp * 2, Density(2f)) { content() }.use { scene ->
            var t = 0L
            repeat(30) { scene.render(t); t += 50_000_000L } // let animations settle (1.5 s)
            File(outDir, "$name.png").writeBytes(scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    private fun variants(block: (suffix: String, dark: Boolean, lang: Language) -> Unit) =
        listOf(false, true).forEach { dark ->
            Language.entries.forEach { lang ->
                block("${lang.name.lowercase().take(2)}-${if (dark) "dark" else "light"}", dark, lang)
            }
        }

    @Test
    fun safetyList() = variants { suffix, dark, lang ->
        shoot("phone-safety-$suffix") { MobileApp(initialLanguage = lang, darkTheme = dark, initialTab = MobileTab.SAFETY) }
    }

    @Test
    fun guideDetail() = variants { suffix, dark, lang ->
        shoot("phone-guide-floods-$suffix") {
            MobileApp(initialLanguage = lang, darkTheme = dark, initialTab = MobileTab.SAFETY, initialGuideId = "floods")
        }
    }

    @Test
    fun emergency() = variants { suffix, dark, lang ->
        shoot("phone-emergency-$suffix") {
            MobileApp(initialCountyCode = 4, initialLanguage = lang, darkTheme = dark, initialTab = MobileTab.EMERGENCY)
        }
    }

    /** The check-in row under the Alerts detail (the detail itself needs network). */
    @Test
    fun alertsCheckInRow() = listOf(false, true).forEach { dark ->
        shoot("phone-alerts-checkin-sw-${if (dark) "dark" else "light"}", heightDp = 160) {
            TahadhariTheme(dark) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    CheckInRow(Counties.byCode(4)!!, Strings.swahili, Modifier.padding(Spacing.lg))
                }
            }
        }
    }

    /** Whole screens (taller than a phone) to review content below the fold. */
    @Test
    fun tallReview() = listOf(false, true).forEach { dark ->
        val mode = if (dark) "dark" else "light"
        shoot("phone-emergency-full-$mode", heightDp = 1500) {
            MobileApp(initialCountyCode = 4, darkTheme = dark, initialTab = MobileTab.EMERGENCY)
        }
        shoot("phone-emergency-nocounty-sw-$mode", heightDp = 1500) {
            MobileApp(initialLanguage = Language.SWAHILI, darkTheme = dark, initialTab = MobileTab.EMERGENCY)
        }
        shoot("phone-guide-cholera-sw-full-$mode", heightDp = 1900) {
            MobileApp(initialLanguage = Language.SWAHILI, darkTheme = dark, initialTab = MobileTab.SAFETY, initialGuideId = "cholera")
        }
    }
}
