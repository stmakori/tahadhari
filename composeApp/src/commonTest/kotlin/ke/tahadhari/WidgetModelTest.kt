package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.widget.WidgetReason
import ke.tahadhari.ui.widget.WidgetUiState
import ke.tahadhari.ui.widget.toContent
import ke.tahadhari.ui.widget.widgetUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WidgetModelTest {
    private val county = Counties.all.first()
    private val prefs = UserPreferences(countyCode = county.code, language = Language.ENGLISH)
    private val now = 1_793_000_000_000L

    @Test fun noCountyShowsPrompt() {
        val ui = widgetUiState(UserPreferences(), null)
        assertEquals(WidgetUiState.NoCounty, ui)
        val c = ui.toContent(Strings.english, now)
        assertEquals("Open Tahadhari to choose your county", c.title)
        assertNull(c.level)
    }

    @Test fun failedIsNoDataNeverLow() {
        val ui = widgetUiState(prefs, LoadState.Failed("boom"))
        assertIs<WidgetUiState.NoData>(ui)
        val c = ui.toContent(Strings.english, now)
        assertNull(c.level)
        assertEquals("No data", c.levelLabel)
        assertTrue(c.tapRetries)
        assertEquals("Tap to retry", c.footer)
    }

    @Test fun tooOldShowsSavedDate() {
        val ui = widgetUiState(prefs, LoadState.Failed("old", staleSinceMillis = now - 5 * 86_400_000L))
        val c = ui.toContent(Strings.english, now)
        assertNull(c.level)
        assertNotNull(c.updated)
        assertTrue(c.updated!!.startsWith("Saved forecast from"))
    }

    @Test fun loadingHasNoLevel() {
        val c = widgetUiState(prefs, null).toContent(Strings.english, now)
        assertNull(c.level)
        assertEquals("Loading", c.levelLabel)
        assertEquals(widgetUiState(prefs, LoadState.Loading), widgetUiState(prefs, null))
    }

    @Test fun readyUsesEngineLevelAndReason() {
        val ready = readyState(county, Rain.high, updatedAtMillis = now - 60_000)
        val ui = widgetUiState(prefs, ready)
        assertIs<WidgetUiState.Ready>(ui)
        assertEquals(ready.assessment.level, ui.level)
        assertEquals(RiskLevel.HIGH, ui.level)
        assertIs<WidgetReason>(ui.reason)
        val c = ui.toContent(Strings.english, now)
        assertEquals("High", c.levelLabel)
        assertNull(c.savedMarker)
        assertEquals("Not an official warning", c.footer)
        assertEquals("Updated 1 min ago", c.updated)
    }

    @Test fun cachedHasSavedMarker() {
        val ui = widgetUiState(prefs, readyState(county, Rain.moderate, fromCache = true))
        val c = ui.toContent(Strings.english, now)
        assertEquals("Saved forecast", c.savedMarker)
    }

    @Test fun swahiliIsUsed() {
        val c = widgetUiState(prefs, readyState(county, Rain.severe)).toContent(Strings.swahili, now)
        assertEquals("Si tahadhari rasmi", c.footer)
        assertEquals(Strings.swahili.risk(RiskLevel.SEVERE), c.levelLabel)
    }

    @Test fun unknownCountyCodeIsNoCounty() {
        assertEquals(WidgetUiState.NoCounty, widgetUiState(UserPreferences(countyCode = 999), null))
    }
}
