package ke.tahadhari

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.TemplateExplainer
import ke.tahadhari.ui.DashboardScreen
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.TahadhariTheme
import ke.tahadhari.ui.colors
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.mapSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardAccessibilityTest {
    private fun contrast(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble() + 0.05
        val l2 = b.luminance().toDouble() + 0.05
        return maxOf(l1, l2) / minOf(l1, l2)
    }

    /** Collects (label, foreground, background) pairs that carry text, for the theme in use. */
    @Composable
    private fun collect(out: MutableList<Triple<String, Color, Color>>) {
        val c = MaterialTheme.colorScheme
        out += Triple("onSurface/surface", c.onSurface, c.surface)
        out += Triple("onSurface/surfaceContainerLow", c.onSurface, c.surfaceContainerLow)
        out += Triple("onSurfaceVariant/surfaceContainerLow", c.onSurfaceVariant, c.surfaceContainerLow)
        out += Triple("onSurfaceVariant/surface", c.onSurfaceVariant, c.surface)
        out += Triple("onSurfaceVariant/surfaceContainer (disclaimer)", c.onSurfaceVariant, c.surfaceContainer)
        out += Triple("onSurfaceVariant/surfaceContainerHigh (loading pill)", c.onSurfaceVariant, c.surfaceContainerHigh)
        out += Triple("onSecondaryContainer/secondaryContainer (selected row)", c.onSecondaryContainer, c.secondaryContainer)
        out += Triple("error/surfaceContainerLow (no data text)", c.error, c.surfaceContainerLow)
        out += Triple("error/surfaceContainerLowest", c.error, c.surfaceContainerLowest)
        out += Triple("onPrimary/primary (language toggle)", c.onPrimary, c.primary)
        out += Triple("primary/surface", c.primary, c.surface)
        RiskLevel.entries.forEach { l ->
            val r = l.colors()
            out += Triple("$l onFill/fill (badge)", r.onFill, r.fill)
            out += Triple("$l onContainer/container", r.onContainer, r.container)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun textContrastMeetsWcagAaInLightAndDark() {
        listOf(false, true).forEach { dark ->
            val pairs = mutableListOf<Triple<String, Color, Color>>()
            ImageComposeScene(100, 100, Density(1f)) { TahadhariTheme(dark) { collect(pairs) } }.use { it.render(0L) }
            assertTrue(pairs.isNotEmpty())
            val failures = pairs.filter { (_, fg, bg) -> contrast(fg, bg) < 4.5 }
                .map { (n, fg, bg) -> "${if (dark) "dark" else "light"} $n = ${"%.2f".format(contrast(fg, bg))}" }
            assertTrue(failures.isEmpty(), "Below 4.5:1: $failures")
        }
    }

    private fun state(): DashboardState {
        val st = DashboardState(CoroutineScope(Dispatchers.Unconfined), loader = { error("no network") }, clock = { 0L })
        Counties.all.forEach { c ->
            st.states[c.code] = when (c.code) {
                4 -> readyState(c, Rain.severe)
                8 -> LoadState.Failed("offline")
                else -> readyState(c, Rain.low)
            }
        }
        return st
    }

    @Test
    fun mapSummaryListsCountsAndNeverCountsFailedAsLow() {
        val s = mapSummary(Strings.english, state().states)
        assertTrue(s.startsWith(Strings.english.mapDescription))
        assertTrue("Severe 1" in s, s)
        assertTrue("Low 45" in s, s)
        assertTrue("No data 1" in s, s)
        val partial = mapSummary(Strings.english, mapOf(1 to LoadState.Failed("x")))
        assertTrue("Low" !in partial, partial)
        assertTrue("Loading 46" in partial, partial)
    }

    @OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
    @Test
    fun arrowKeysMoveSelection() {
        val st = state()
        ImageComposeScene(1500, 900, Density(1f)) {
            DashboardScreen(st, false, explainer = TemplateExplainer())
        }.use { scene ->
            var t = 0L
            repeat(10) { scene.render(t); t += 50_000_000L }
            assertEquals(null, st.selectedCode)
            scene.sendKeyEvent(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            repeat(3) { scene.render(t); t += 50_000_000L }
            val first = st.selectedCode
            assertEquals(st.visible.first().code, first) // the highest-risk county (Tana River, severe)
            assertEquals(4, first)
            scene.sendKeyEvent(KeyEvent(Key.DirectionDown, KeyEventType.KeyDown))
            repeat(3) { scene.render(t); t += 50_000_000L }
            assertEquals(st.visible[1].code, st.selectedCode)
            scene.sendKeyEvent(KeyEvent(Key.DirectionUp, KeyEventType.KeyDown))
            repeat(3) { scene.render(t); t += 50_000_000L }
            assertEquals(4, st.selectedCode)
        }
    }
}
