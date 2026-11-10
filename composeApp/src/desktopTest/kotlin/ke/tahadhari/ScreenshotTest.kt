package ke.tahadhari

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders key screens to PNGs in `composeApp/build/screenshots/` for visual review and README images.
 * Skipped unless TAHADHARI_SCREENSHOTS=1, so normal test runs stay fast:
 *   TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest --tests '*ScreenshotTest*'
 */
class ScreenshotTest {
    private val enabled = System.getenv("TAHADHARI_SCREENSHOTS") == "1"
    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun sampleForecast(): Pair<CountyForecast, RiskAssessment> {
        val county = Counties.byCode(4)!! // Tana River: has river data and hotspots
        val mm = listOf(4.0, 12.0, 28.0, 63.0, 41.0, 9.0, 0.5)
        val prob = listOf(35, 55, 80, 95, 85, 40, 15)
        val days = mm.indices.map { DailyRain("2026-11-0${2 + it}", mm[it], prob[it]) }
        val forecast = CountyForecast(county, days, RiverOutlook("Tana River", 340.0, 812.0, "2026-11-06"), "2026-11-02")
        return forecast to RiskEngine().assess(forecast)
    }

    private fun shoot(name: String, dark: Boolean, heightDp: Int = 900, content: @Composable () -> Unit) {
        if (!enabled) return
        ImageComposeScene(412 * 2, heightDp * 2, Density(2f)) {
            TahadhariTheme(darkTheme = dark) { content() }
        }.use { scene ->
            var t = 0L
            repeat(30) { scene.render(t); t += 50_000_000L } // let animations settle (1.5 s)
            val png = scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(outDir, "$name-${if (dark) "dark" else "light"}.png").writeBytes(png)
        }
    }

    @Test
    fun detail() = listOf(false, true).forEach { dark ->
        listOf(Language.ENGLISH, Language.SWAHILI).forEach { lang ->
            val (forecast, assessment) = sampleForecast()
            shoot("detail-${lang.name.lowercase()}", dark, heightDp = 1900) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        CountyReadyContent(forecast.county, forecast, assessment, lang, TemplateExplainer())
                        Disclaimer(Strings.of(lang))
                    }
                }
            }
        }
    }

    /** Layout check only: the advisory below is placeholder text, not a real KMD advisory. */
    @Test
    fun advisoryCard() = listOf(false, true).forEach { dark ->
        listOf(Language.ENGLISH, Language.SWAHILI).forEach { lang ->
            val sample = Advisory(
                issuer = "Kenya Meteorological Department",
                title = "Sample county weekly forecast",
                validFrom = "2026-11-02", validTo = "2026-11-08",
                sourceUrl = "https://meteo.go.ke/our-products/county-forecasts/",
                text = "Placeholder advisory text for the layout check. ".repeat(8).trim(),
            )
            shoot("advisory-${lang.name.lowercase()}", dark, heightDp = 420) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxSize().padding(Spacing.lg)) { AdvisoryCard(sample, lang) }
                }
            }
        }
    }

    @Test
    fun heroLevels()= listOf(false, true).forEach { dark ->
        shoot("hero-levels", dark, heightDp = 1150) {
            val county = Counties.byCode(42)!!
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    RiskLevel.entries.forEach { level ->
                        RiskHeroCard(county, RiskAssessment(level, emptyList(), null, 0.0, null), Language.ENGLISH)
                    }
                }
            }
        }
    }

    @Test
    fun states() = listOf(false, true).forEach { dark ->
        shoot("states", dark, heightDp = 1200) {
            val county = Counties.byCode(42)!!
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CountyDetail(county, LoadState.Failed("offline"), Language.ENGLISH, explainer = TemplateExplainer()) {}
                    CountyDetail(county, LoadState.Loading, Language.ENGLISH, explainer = TemplateExplainer()) {}
                }
            }
        }
    }

    @Test
    fun picker() = listOf(false, true).forEach { dark ->
        shoot("picker", dark) { MobileApp(darkTheme = dark) }
    }
}
