package ke.tahadhari

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import ke.tahadhari.ui.*
import ke.tahadhari.ui.mobile.LocationCardContent
import ke.tahadhari.ui.mobile.LocationUi
import ke.tahadhari.ui.onboarding.OnboardingScreen
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Screenshots for the About page, first-launch intro, location card states and 1.5x font scale
 * checks, written to `composeApp/build/screenshots/`. Skipped unless TAHADHARI_SCREENSHOTS=1:
 *   TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest --tests '*PhoneExtrasScreenshotTest*'
 */
class PhoneExtrasScreenshotTest {
    private val enabled = System.getenv("TAHADHARI_SCREENSHOTS") == "1"
    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun shoot(name: String, heightDp: Int = 860, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (!enabled) return
        ImageComposeScene(412 * 2, heightDp * 2, Density(2f, fontScale)) { content() }.use { scene ->
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

    private fun themed(dark: Boolean, content: @Composable () -> Unit): @Composable () -> Unit = {
        TahadhariTheme(dark) { Surface(color = MaterialTheme.colorScheme.surface) { content() } }
    }

    @Test
    fun about() = variants { suffix, dark, lang ->
        shoot("phone-about-$suffix") { MobileApp(initialLanguage = lang, darkTheme = dark, initialShowAbout = true) }
        shoot("phone-about-full-$suffix", heightDp = 2700, content = themed(dark) {
            AboutContent(lang, Modifier.padding(Spacing.lg))
        })
    }

    @Test
    fun intro() = variants { suffix, dark, lang ->
        (0..2).forEach { page ->
            shoot("phone-intro-${page + 1}-$suffix") {
                TahadhariTheme(dark) { OnboardingScreen(lang, onLanguage = {}, onFinish = {}, initialPage = page) }
            }
        }
    }

    @Test
    fun locationCard() = variants { suffix, dark, lang ->
        val s = Strings.of(lang)
        val states = listOf(
            LocationUi.Idle,
            LocationUi.Locating,
            LocationUi.Suggest(Counties.byCode(4)!!),
            LocationUi.Message(s.locationDenied),
            LocationUi.Message(s.locationOutsideKenya),
        )
        shoot("phone-location-card-$suffix", heightDp = 900, content = themed(dark) {
            Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                states.forEach { LocationCardContent(it, s, onRequest = {}, onConfirm = {}, onDismiss = {}) }
            }
        })
    }

    /** County detail and Emergency at 1.5x font scale, to look for clipping and overlap. */
    @Test
    fun fontScale() = listOf(false, true).forEach { dark ->
        val mode = if (dark) "dark" else "light"
        val county = Counties.byCode(4)!!
        val mm = listOf(4.0, 12.0, 28.0, 63.0, 99.0, 9.0, 0.5)
        val days = mm.indices.map { DailyRain("2026-11-0${2 + it}", mm[it], listOf(35, 55, 80, 95, 85, 40, 15)[it]) }
        val forecast = CountyForecast(county, days, RiverOutlook("Tana River", 340.0, 812.0, "2026-11-06"), "2026-11-02")
        val assessment = RiskEngine().assess(forecast)
        Language.entries.forEach { lang ->
            val l = lang.name.lowercase().take(2)
            shoot("phone-detail-font15-$l-$mode", heightDp = 2900, fontScale = 1.5f, content = themed(dark) {
                Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    CountyReadyContent(county, forecast, assessment, lang, TemplateExplainer())
                    Disclaimer(Strings.of(lang))
                }
            })
            shoot("phone-emergency-font15-$l-$mode", heightDp = 2400, fontScale = 1.5f) {
                MobileApp(initialCountyCode = 4, initialLanguage = lang, darkTheme = dark, initialTab = MobileTab.EMERGENCY)
            }
        }
        shoot("phone-picker-font15-en-$mode", fontScale = 1.5f) { MobileApp(darkTheme = dark) }
    }
}
