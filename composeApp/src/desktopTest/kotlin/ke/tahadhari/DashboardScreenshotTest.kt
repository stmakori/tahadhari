package ke.tahadhari

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiverOutlook
import ke.tahadhari.domain.TemplateExplainer
import ke.tahadhari.ui.DashboardScreen
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.dashboard.CsvExportResult
import ke.tahadhari.ui.dashboard.DashboardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Dashboard PNGs in `composeApp/build/screenshots/` with fake data (no network). Skipped unless
 * TAHADHARI_SCREENSHOTS=1:
 *   TAHADHARI_SCREENSHOTS=1 ./gradlew :composeApp:desktopTest --tests '*DashboardScreenshotTest*'
 */
class DashboardScreenshotTest {
    private val enabled = System.getenv("TAHADHARI_SCREENSHOTS") == "1"
    private val outDir = File("build/screenshots").apply { mkdirs() }

    /** A plausible El Niño week: wet west and Tana basin, a few counties loading or failed. */
    private fun fakeState(language: Language = Language.ENGLISH): DashboardState {
        val now = System.currentTimeMillis()
        val severe = setOf(4, 7, 40)
        val high = setOf(3, 5, 41, 42, 43)
        val moderate = setOf(1, 2, 6, 12, 13, 14, 15, 17, 37, 38, 39, 44, 46, 20)
        val loading = setOf(10, 36)
        val failed = setOf(8, 45)
        val state = DashboardState(CoroutineScope(Dispatchers.Unconfined), loader = { error("no network in tests") }, initialLanguage = language)
        Counties.all.forEach { c ->
            val mm = when (c.code) {
                in severe -> Rain.severe.map { it * (1 + c.code % 5 * 0.04) }
                in high -> Rain.high.map { it * (1 + c.code % 4 * 0.01) }
                in moderate -> Rain.moderate.map { it * (1 + c.code % 3 * 0.05) }
                else -> Rain.low
            }
            val river = when (c.code) {
                4, 7 -> RiverOutlook("Tana River", 310.0, 1_020.0, "2026-11-06")
                40, 41 -> RiverOutlook("Nzoia River", 140.0, 330.0, "2026-11-05")
                42 -> RiverOutlook("Nyando River", 22.0, 41.0, "2026-11-05")
                else -> null
            }
            state.states[c.code] = when (c.code) {
                in loading -> LoadState.Loading
                in failed -> LoadState.Failed("offline")
                else -> readyState(c, mm, river, now - 25 * 60_000L, fromCache = c.code == 3)
            }
        }
        return state
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun shoot(name: String, w: Int, h: Int, dark: Boolean, hoverAt: Offset? = null, content: @Composable () -> Unit) {
        if (!enabled) return
        ImageComposeScene(w * 2, h * 2, Density(2f)) { content() }.use { scene ->
            var t = 0L
            repeat(30) { scene.render(t); t += 50_000_000L }
            hoverAt?.let {
                scene.sendPointerEvent(PointerEventType.Enter, it * 2f)
                scene.sendPointerEvent(PointerEventType.Move, it * 2f)
                repeat(5) { scene.render(t); t += 50_000_000L }
            }
            val png = scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(outDir, "$name-${if (dark) "dark" else "light"}.png").writeBytes(png)
        }
    }

    @Test
    fun wide() = listOf(false, true).forEach { dark ->
        val state = fakeState().apply { selectedCode = 4 }
        shoot("dashboard-wide", 1500, 900, dark) { DashboardScreen(state, dark, explainer = TemplateExplainer(), onExportCsv = { CsvExportResult.Cancelled }) }
    }

    @Test
    fun wideFilteredSwahili() {
        val state = fakeState(Language.SWAHILI).apply {
            filter = ke.tahadhari.ui.dashboard.StatusFilter.Level(ke.tahadhari.domain.RiskLevel.HIGH)
        }
        shoot("dashboard-wide-sw-filtered", 1500, 900, false, hoverAt = Offset(330f, 420f)) {
            DashboardScreen(state, false, explainer = TemplateExplainer())
        }
    }

    @Test
    fun narrow() = listOf(false, true).forEach { dark ->
        val list = fakeState().apply { selectedCode = 42 }
        shoot("dashboard-narrow-list", 1100, 800, dark) { DashboardScreen(list, dark, explainer = TemplateExplainer()) }
        val map = fakeState().apply { selectedCode = 8; showMapTab = true }
        shoot("dashboard-narrow-map", 1100, 800, dark) { DashboardScreen(map, dark, explainer = TemplateExplainer()) }
    }
}
