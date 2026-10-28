package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.*
import ke.tahadhari.ui.mobile.SPOKEN_ALERT_MAX_WORDS
import ke.tahadhari.ui.mobile.cleanForSpeech
import ke.tahadhari.ui.mobile.spokenAlert
import kotlin.test.*

private fun assessment(
    level: RiskLevel,
    reasons: List<RiskReason>,
    river: RiverOutlook? = null,
) = RiskAssessment(
    level = level,
    reasons = reasons,
    wettestDay = DailyRain("2026-11-05", 62.0, 70),
    maxThreeDayTotalMm = 95.0,
    river = river,
)

class SpokenAlertTest {
    private val nairobi = Counties.byCode(47)!!
    private val river = RiverOutlook("Tana River", 100.0, 310.0, "2026-11-06")
    private val landslideCounty: County = Counties.all.first { it.landslideProne && it.landslideAreas.isNotEmpty() }
    private val riverCounty: County = Counties.all.first { it.riverPoint != null }

    private val reasonCases: List<Pair<String, RiskAssessment>> = listOf(
        "none" to assessment(RiskLevel.LOW, emptyList()),
        "heavyDay" to assessment(RiskLevel.HIGH, listOf(RiskReason.HeavyDay("2026-11-05", 62.0, RiskLevel.HIGH))),
        "wetSpell" to assessment(RiskLevel.MODERATE, listOf(RiskReason.WetSpell("2026-11-04", 95.0, RiskLevel.MODERATE))),
        "landslide" to assessment(RiskLevel.SEVERE, listOf(RiskReason.LandslideRisk("2026-11-04", 130.0, RiskLevel.SEVERE))),
        "river" to assessment(RiskLevel.SEVERE, listOf(RiskReason.RiverRising("Tana River", 3.1, RiskLevel.SEVERE)), river),
    )

    private fun countyFor(name: String) = when (name) {
        "landslide" -> landslideCounty
        "river" -> riverCounty
        else -> nairobi
    }

    private val badChars = Regex("[^\\p{L}\\p{N} .,;:!?'\\-]")

    @Test fun everyReasonTypeInBothLanguagesIsCleanAndShort() {
        for ((name, a) in reasonCases) for (lang in Language.entries) {
            val alert = spokenAlert(countyFor(name), a, lang)
            for (text in listOf(alert.text, alert.englishFallback)) {
                assertTrue(text.isNotBlank(), "$name $lang")
                assertNull(badChars.find(text), "symbol in $name $lang: $text")
                assertFalse(Regex("\\d\\s?mm").containsMatchIn(text), "mm left in $name $lang: $text")
                assertFalse("  " in text, "double space in $name $lang")
                assertTrue(text.split(' ').size <= SPOKEN_ALERT_MAX_WORDS, "too long ($name $lang): ${text.split(' ').size} words")
            }
        }
    }

    @Test fun englishSaysLevelExplanationActionsAndDisclaimer() {
        val a = reasonCases.first { it.first == "heavyDay" }.second
        val t = spokenAlert(nairobi, a, Language.ENGLISH).text
        assertContains(t, "High flood risk in Nairobi")
        assertContains(t, "62 millimetres")
        assertContains(t, "What to do.")
        assertContains(t, "not an official warning")
    }

    @Test fun swahiliUsesSwahiliTextAndKeepsAnEnglishFallback() {
        val a = reasonCases.first { it.first == "heavyDay" }.second
        val alert = spokenAlert(nairobi, a, Language.SWAHILI)
        assertContains(alert.text, "Hatari kubwa ya mafuriko")
        assertContains(alert.text, "milimita 62")
        assertContains(alert.text, "Unachopaswa kufanya")
        assertContains(alert.englishFallback, "High flood risk")
        assertNotEquals(alert.text, alert.englishFallback)
    }

    @Test fun riverRatioIsSpokenAsWords() {
        val (name, a) = reasonCases.first { it.first == "river" }
        val t = spokenAlert(countyFor(name), a, Language.ENGLISH).text
        assertContains(t, "times its recent level")
        assertFalse('×' in t)
    }

    @Test fun emergencyNumbersAreSpelledDigitByDigit() {
        val a = reasonCases.first { it.first == "landslide" }.second
        val t = spokenAlert(nairobi, a, Language.ENGLISH).text
        assertFalse(Regex("\\b999\\b").containsMatchIn(t))
        if ("9 9 9" in t) assertContains(t, "1 1 2")
    }

    @Test fun savedForecastIsSaid() {
        val a = reasonCases.first { it.first == "heavyDay" }.second
        assertContains(spokenAlert(nairobi, a, Language.ENGLISH, fromCache = true).text, "saved forecast")
        assertContains(spokenAlert(nairobi, a, Language.SWAHILI, fromCache = true).text, "uliohifadhiwa")
        assertFalse("saved forecast" in spokenAlert(nairobi, a, Language.ENGLISH).text)
    }

    @Test fun worstCaseOverAllCountiesStaysWithinTheLimitBothLanguages() {
        val a = assessment(RiskLevel.SEVERE, listOf(
            RiskReason.RiverRising("Tana River", 3.1, RiskLevel.SEVERE),
            RiskReason.LandslideRisk("2026-11-04", 130.0, RiskLevel.SEVERE),
        ), river)
        for (c in Counties.all) for (lang in Language.entries) {
            val words = spokenAlert(c, a, lang).text.split(' ').size
            assertTrue(words <= SPOKEN_ALERT_MAX_WORDS, "${c.name} $lang: $words words")
        }
    }

    @Test fun cleanerRemovesEmojiArrowsAndUnits() {
        val t = cleanForSpeech(listOf("Rain 🌧 up to 45 mm → 3× more (Fri) & 50%"), Language.ENGLISH)
        assertEquals("Rain up to 45 millimetres, 3 times more Fri and 50 percent.", t)
    }
}
