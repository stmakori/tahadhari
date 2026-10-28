package ke.tahadhari

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.*
import ke.tahadhari.ui.*
import ke.tahadhari.ui.mobile.WatchSection
import ke.tahadhari.ui.mobile.WatchStates
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the main phone screens (360x640 dp) at font scales 1.0 / 1.5 / 2.0 and the dashboard detail
 * pane at 1.5, and fails if any text is visually clipped (an ellipsized line or `TextLayoutResult.didOverflowHeight`; `hasVisualOverflow` is also true for every plain wrapping text here, so it is not usable;
 * see below).
 *
 * PNGs go to `composeApp/build/large-text/` for review (always written; they are small).
 */
@OptIn(ExperimentalTestApi::class)
class LargeTextTest {
    private val outDir = File("build/large-text").apply { mkdirs() }
    private val failures = mutableListOf<String>()

    @BeforeTest fun reset() = failures.clear()

    @AfterTest fun noClippedText() = assertTrue(failures.isEmpty(), failures.joinToString(separator = NL, prefix = NL))

    private val NL = System.lineSeparator()
    private val scales = listOf(1.0f, 1.5f, 2.0f)

    private fun sample(): Triple<County, CountyForecast, RiskAssessment> {
        val county = Counties.byCode(4)!! // Tana River: river + hotspots
        val mm = listOf(4.0, 12.0, 28.0, 63.0, 41.0, 9.0, 0.5)
        val prob = listOf(35, 55, 80, 95, 85, 40, 15)
        val days = mm.indices.map { DailyRain("2026-11-0${2 + it}", mm[it], prob[it]) }
        val forecast = CountyForecast(county, days, RiverOutlook("Tana River", 340.0, 812.0, "2026-11-06"), "2026-11-02")
        val assessment = RiskEngine().assess(forecast)
        return Triple(county, forecast, assessment)
    }

    private fun check(name: String, scale: Float, widthDp: Int, heightDp: Int, language: Language = Language.ENGLISH, content: @Composable () -> Unit) {
        runSkikoComposeUiTest(Size(widthDp * 2f, heightDp * 2f), Density(2f, scale)) {
            setContent { TahadhariTheme(darkTheme = false) { content() } }
            waitForIdle()
            mainClock.advanceTimeBy(1500)
            waitForIdle()
            val bytes = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(outDir, "$name-${language.name.lowercase().take(2)}-x$scale.png").writeBytes(bytes)

            val withText = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult)).fetchSemanticsNodes()
            val clipped = withText.mapNotNull { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.firstOrNull()?.takeIf { r -> r.didOverflowHeight || (0 until r.lineCount).any { r.isLineEllipsized(it) } }?.layoutInput?.text?.text
            }
            if (clipped.isNotEmpty()) failures += "$name at x$scale ($language): clipped text: $clipped"
        }
    }

    private fun phone(name: String, tab: MobileTab = MobileTab.ALERTS, county: Int? = null, guide: String? = null, about: Boolean = false) {
        Language.entries.forEach { lang ->
            scales.forEach { s ->
                check(name, s, 360, 640, lang) {
                    MobileApp(
                        initialCountyCode = county, initialLanguage = lang, darkTheme = false,
                        initialTab = tab, initialGuideId = guide, initialShowAbout = about,
                    )
                }
            }
        }
    }

    @Test fun countyList() = phone("list")

    @Test fun safetyGuides() = phone("safety", MobileTab.SAFETY)

    @Test fun safetyGuideDetail() = phone("guide", MobileTab.SAFETY, guide = "floods")

    @Test fun emergency() = phone("emergency", MobileTab.EMERGENCY, county = 4)

    @Test fun about() = phone("about", about = true)

    @Test fun countyDetailHigh() {
        val (county, forecast, assessment) = sample()
        Language.entries.forEach { lang ->
            scales.forEach { s ->
                // Tall scene: the real screen scrolls, here we want to see and check all of it.
                check("detail", s, 360, 6500, lang) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            CountyDetail(
                                county, LoadState.Ready(forecast, assessment, updatedAtMillis = System.currentTimeMillis(), fromCache = false), lang,
                                explainer = TemplateExplainer(), heavyRainChance = null,
                            ) {}
                        }
                    }
                }
            }
        }
    }

    @Test fun dashboardDetailPane() {
        val (county, forecast, assessment) = sample()
        check("dashboard-detail", 1.5f, 520, 3200) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CountyDetail(
                        county, LoadState.Ready(forecast, assessment, updatedAtMillis = System.currentTimeMillis(), fromCache = false), Language.ENGLISH,
                        explainer = TemplateExplainer(), heavyRainChance = null,
                    ) {}
                }
            }
        }
    }

    @Test fun watchSection() {
        val (_, forecast, assessment) = sample()
        val watched = listOf(4, 30, 47).map { Counties.byCode(it)!! } // Tana River, Baringo, Nairobi
        Language.entries.forEach { lang ->
            scales.forEach { s ->
                // States are filled in up front with the same refresh key, so the section fetches nothing.
                val holder = WatchStates().apply {
                    loadedRefreshKey = 0
                    states[4] = LoadState.Ready(forecast, assessment, updatedAtMillis = System.currentTimeMillis(), fromCache = false)
                    states[30] = LoadState.Ready(forecast, assessment, updatedAtMillis = System.currentTimeMillis() - 26 * 3_600_000L, fromCache = true)
                    states[47] = LoadState.Failed("offline")
                }
                check("watch", s, 360, 1400, lang) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            WatchSection(watched, holder, refreshKey = 0, language = lang, riskAlertsOn = true, onOpen = {}, onAdd = {}, onRemove = {})
                        }
                    }
                }
            }
        }
    }
}
