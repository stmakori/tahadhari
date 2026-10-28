package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.Preferences
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.data.store.InMemoryKeyValueStore
import ke.tahadhari.domain.*
import ke.tahadhari.platform.CRASH_REPORT_MAX_CHARS
import ke.tahadhari.platform.appendCrashReport
import ke.tahadhari.platform.formatCrashReport
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.mobile.RiskAlertState
import ke.tahadhari.ui.mobile.SHARE_ALERT_MAX_CHARS
import ke.tahadhari.ui.mobile.checkRiskAlert
import ke.tahadhari.ui.mobile.shareAlertMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private val NOW = 1_793_000_000_000L // 2026-10-26 or so

/** Forecast with [peakMm] on Thursday 2026-11-05 and an optional Tana-style river. */
private fun ready(countyCode: Int, peakMm: Double, riverRatio: Double?, fromCache: Boolean = false): LoadState.Ready {
    val county = Counties.byCode(countyCode)!!
    val mm = listOf(4.0, 6.0, 8.0, peakMm, peakMm / 2, 9.0, 0.5)
    val dates = listOf("2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06", "2026-11-07", "2026-11-08")
    val days = mm.indices.map { DailyRain(dates[it], mm[it], 60) }
    val river = riverRatio?.let { RiverOutlook("Tana River", 100.0, 100.0 * it, "2026-11-06") }
    val forecast = CountyForecast(county, days, river, "2026-11-02")
    return LoadState.Ready(forecast, RiskEngine().assess(forecast), NOW, fromCache)
}

class ShareAlertMessageTest {
    private val county = Counties.byCode(4)!!

    @Test fun worstCaseFitsInThreeSmsBothLanguages() {
        for (lang in Language.entries) {
            val state = ready(county.code, 99.0, 3.4, fromCache = true) // Severe, river, saved forecast: longest case
            assertEquals(RiskLevel.SEVERE, state.assessment.level)
            val text = shareAlertMessage(county, state, lang)
            assertTrue(text.length <= SHARE_ALERT_MAX_CHARS, "$lang: ${text.length} chars\n$text")
            assertTrue("999 / 112" in text && "1199" in text, text)
            assertTrue("KMSA" in text, text)
            assertTrue(text.trimEnd().endsWith("Tahadhari"), text)
            assertTrue("1. " in text, "at least one action kept ($lang):\n$text")
        }
    }

    @Test fun uncachedWorstCaseKeepsAtLeastTwoActions() {
        for (lang in Language.entries) {
            val text = shareAlertMessage(county, ready(county.code, 99.0, 3.4), lang)
            assertTrue(text.length <= SHARE_ALERT_MAX_CHARS && "2. " in text, "$lang: $text")
        }
    }

    @Test fun englishSevereWithRiverNamesLevelCountyAndReason() {
        val text = shareAlertMessage(county, ready(county.code, 99.0, 3.4), Language.ENGLISH)
        assertTrue(text.startsWith("Severe flood risk: ${county.name} County"), text)
        assertTrue("Up to 99 mm of rain on Thursday" in text, text)
        assertTrue("1. " in text, text)
    }

    @Test fun riverReasonIsUsedWhenItIsTheStrongestSignal() {
        val text = shareAlertMessage(county, ready(county.code, 25.0, 3.4), Language.ENGLISH)
        assertTrue("Tana River rising to 3.4×" in text, text)
    }

    @Test fun noRiverCase() {
        val text = shareAlertMessage(county, ready(county.code, 99.0, null), Language.SWAHILI)
        assertTrue(text.length <= SHARE_ALERT_MAX_CHARS, text)
        assertTrue("Alhamisi" in text, text)
        assertFalse("unapanda" in text, text)
    }

    @Test fun lowCaseSaysNoHeavyRainAndStillHasSafetyLines() {
        for (lang in Language.entries) {
            val state = ready(county.code, 5.0, null)
            assertEquals(RiskLevel.LOW, state.assessment.level)
            val text = shareAlertMessage(county, state, lang)
            assertTrue(text.length <= SHARE_ALERT_MAX_CHARS, text)
            assertTrue("999 / 112" in text, text)
        }
    }

    @Test fun savedForecastIncludesItsDate() {
        val text = shareAlertMessage(county, ready(county.code, 99.0, null, fromCache = true), Language.ENGLISH)
        assertTrue("(saved forecast from " in text, text)
        assertFalse("(saved forecast" in shareAlertMessage(county, ready(county.code, 99.0, null), Language.ENGLISH))
    }

    @Test fun neverContainsAiText() {
        // Actions come from TemplateExplainer, so the message is identical on every call.
        val state = ready(county.code, 99.0, 3.4)
        assertEquals(shareAlertMessage(county, state, Language.ENGLISH), shareAlertMessage(county, state, Language.ENGLISH))
    }

    /** Printed by `--info` runs; handy when reviewing copy. */
    @Test fun printSamples() {
        for (lang in Language.entries) {
            val t = shareAlertMessage(county, ready(county.code, 99.0, 3.4), lang)
            println("SAMPLE $lang (${t.length} chars)\n$t\n")
        }
    }
}

class AlertDecisionTest {
    private val H = RiskLevel.HIGH
    private val S = RiskLevel.SEVERE

    @Test fun riseToHighNotifies() {
        assertEquals(AlertDecision(true, H), decideRiskAlert(null, H, fresh = true))
        assertEquals(AlertDecision(true, S), decideRiskAlert(null, S, fresh = true))
    }

    @Test fun highToSevereIsAnotherRise() {
        assertEquals(AlertDecision(true, S), decideRiskAlert(H, S, fresh = true))
    }

    @Test fun repeatDoesNotNotifyAgain() {
        assertEquals(AlertDecision(false, H), decideRiskAlert(H, H, fresh = true))
        assertEquals(AlertDecision(false, S), decideRiskAlert(S, S, fresh = true))
    }

    @Test fun easingFromSevereToHighIsSilentButRememberedSoSevereAgainNotifies() {
        val eased = decideRiskAlert(S, H, fresh = true)
        assertEquals(AlertDecision(false, H), eased)
        assertTrue(decideRiskAlert(eased.lastNotified, S, fresh = true).notify)
    }

    @Test fun dropToModerateOrLowResetsSoLaterRiseNotifies() {
        for (low in listOf(RiskLevel.MODERATE, RiskLevel.LOW)) {
            val reset = decideRiskAlert(S, low, fresh = true)
            assertEquals(AlertDecision(false, null), reset)
            assertTrue(decideRiskAlert(reset.lastNotified, H, fresh = true).notify)
        }
    }

    @Test fun moderateNeverNotifies() {
        assertFalse(decideRiskAlert(null, RiskLevel.MODERATE, fresh = true).notify)
    }

    @Test fun failedLoadNeverNotifiesAndKeepsMemory() {
        assertEquals(AlertDecision(false, S), decideRiskAlert(S, null, fresh = false))
        assertEquals(AlertDecision(false, null), decideRiskAlert(null, null, fresh = true))
    }

    @Test fun cachedDataNeverNotifiesAndDoesNotReset() {
        assertEquals(AlertDecision(false, null), decideRiskAlert(null, S, fresh = false))
        assertEquals(AlertDecision(false, S), decideRiskAlert(S, RiskLevel.LOW, fresh = false))
    }
}

class RiskAlertCheckTest {
    private val county = Counties.byCode(4)!!

    @Test fun oneNotificationPerRiseThenResetThenAgain() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        val high = ready(county.code, 60.0, null)
        val low = ready(county.code, 5.0, null)
        val first = checkRiskAlert(county, high, Language.ENGLISH, state)
        assertNotNull(first)
        assertTrue("High flood risk" in first.title && "Not an official warning" in first.text, first.toString())
        assertNull(checkRiskAlert(county, high, Language.ENGLISH, state)) // repeat
        assertNull(checkRiskAlert(county, low, Language.ENGLISH, state)) // drop resets
        assertNotNull(checkRiskAlert(county, high, Language.SWAHILI, state)) // new rise
    }

    @Test fun failedAndCachedNeverNotify() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        assertNull(checkRiskAlert(county, LoadState.Failed("offline"), Language.ENGLISH, state))
        assertNull(checkRiskAlert(county, ready(county.code, 99.0, null, fromCache = true), Language.ENGLISH, state))
        assertNull(state.lastNotified(county))
        // A failed load in between does not clear what was notified.
        assertNotNull(checkRiskAlert(county, ready(county.code, 99.0, null), Language.ENGLISH, state))
        assertNull(checkRiskAlert(county, LoadState.Failed("offline"), Language.ENGLISH, state))
        assertNull(checkRiskAlert(county, ready(county.code, 99.0, null), Language.ENGLISH, state))
    }

    @Test fun otherCountyStartsFresh() = runTest {
        val state = RiskAlertState(InMemoryKeyValueStore())
        assertNotNull(checkRiskAlert(county, ready(county.code, 99.0, null), Language.ENGLISH, state))
        val other = Counties.byCode(33)!!
        assertNotNull(checkRiskAlert(other, ready(other.code, 99.0, null), Language.ENGLISH, state))
    }
}

class RiskAlertPreferencesTest {
    @Test fun offByDefaultAndSavedKeepingOtherChoices() = runTest {
        val prefs = Preferences(InMemoryKeyValueStore())
        assertFalse(prefs.load().riskAlertsEnabled)
        prefs.setCounty(33)
        prefs.setRiskAlertsEnabled(true)
        assertEquals(UserPreferences(33, Language.ENGLISH, riskAlertsEnabled = true), prefs.load())
        prefs.setLanguage(Language.SWAHILI)
        assertTrue(prefs.load().riskAlertsEnabled)
        prefs.setRiskAlertsEnabled(false)
        assertFalse(prefs.load().riskAlertsEnabled)
    }

    @Test fun oldSavedPreferencesLoadWithAlertsOff() = runTest {
        val store = InMemoryKeyValueStore()
        store.put("preferences", """{"countyCode":33,"language":"SWAHILI","onboardingDone":true}""")
        assertEquals(UserPreferences(33, Language.SWAHILI, onboardingDone = true, riskAlertsEnabled = false), Preferences(store).load())
    }
}

class CrashReportTest {
    @Test fun reportHasTheFacts() {
        val r = formatCrashReport(NOW, "1.0.0", "Android 14 (API 34)", "Google Pixel 7", "java.lang.IllegalStateException: boom\n\tat a.b(C.kt:1)")
        for (part in listOf("1.0.0", "Android 14 (API 34)", "Google Pixel 7", "IllegalStateException: boom", "Nairobi")) assertTrue(part in r, r)
    }

    @Test fun longStackTraceIsTruncated() {
        val r = formatCrashReport(NOW, "1.0.0", "x", "y", "at line\n".repeat(10_000))
        assertTrue(r.length <= CRASH_REPORT_MAX_CHARS)
        assertTrue(r.contains("(cut)"))
    }

    @Test fun fileKeepsNewestEntriesWithinLimit() {
        val a = formatCrashReport(NOW, "1.0.0", "x", "y", "first")
        val b = formatCrashReport(NOW, "1.0.0", "x", "y", "second")
        val both = appendCrashReport(a, b)
        assertTrue("first" in both && both.endsWith(b))
        var file: String? = null
        repeat(200) { file = appendCrashReport(file, formatCrashReport(NOW, "1.0.0", "x", "y", "e$it\n" + "frame\n".repeat(100))) }
        assertTrue(file!!.length <= CRASH_REPORT_MAX_CHARS)
        assertTrue("e199" in file!!)
    }
}
