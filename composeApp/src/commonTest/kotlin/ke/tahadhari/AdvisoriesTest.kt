package ke.tahadhari

import ke.tahadhari.ai.AiExplainer
import ke.tahadhari.data.BundledAdvisories
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import ke.tahadhari.shared.ai.ExplainContract
import ke.tahadhari.shared.ai.ExplainRequest
import ke.tahadhari.shared.ai.ExplainResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdvisoriesTest {
    private val nakuru = Counties.byCode(32)!!
    private val known = Counties.all.map { it.code }.toSet()

    private fun advisory(
        from: String = "2026-10-27",
        to: String = "2026-11-02",
        counties: Set<Int> = setOf(32),
        text: String = "Heavy rainfall expected over parts of the county.",
        url: String = "https://meteo.go.ke/our-products/county-forecasts/",
    ) = Advisory("Kenya Meteorological Department", "Weekly forecast", from, to, url, text, counties)

    @Test fun bundledAdvisoriesAreAllValid() {
        BundledAdvisories.all.forEach { a ->
            assertEquals(emptyList(), Advisories.problems(a, known), "${a.title} ${a.validFrom}")
        }
    }

    @Test fun lengthLimitMatchesTheAiContract() {
        assertEquals(ExplainContract.MAX_ADVISORY_CHARS, Advisories.MAX_TEXT_CHARS)
    }

    @Test fun usedOnlyWhileValidBothDatesInclusive() {
        val a = advisory()
        assertNull(Advisories.current(listOf(a), 32, "2026-10-26"))
        assertEquals(a, Advisories.current(listOf(a), 32, "2026-10-27"))
        assertEquals(a, Advisories.current(listOf(a), 32, "2026-11-02"))
        assertNull(Advisories.current(listOf(a), 32, "2026-11-03"))
    }

    @Test fun onlyForItsCountiesUnlessNational() {
        assertNull(Advisories.current(listOf(advisory()), 1, "2026-10-28"))
        val national = advisory(counties = emptySet())
        assertEquals(national, Advisories.current(listOf(national), 1, "2026-10-28"))
    }

    @Test fun countyAdvisoryWinsOverNationalThenNewest() {
        val national = advisory(from = "2026-10-28", counties = emptySet())
        val older = advisory(from = "2026-10-20")
        val newer = advisory(from = "2026-10-27")
        assertEquals(newer, Advisories.current(listOf(national, older, newer), 32, "2026-10-28"))
    }

    @Test fun problemsCatchBadEntries() {
        assertTrue(Advisories.problems(advisory(), known).isEmpty())
        assertTrue(Advisories.problems(advisory(url = "http://meteo.go.ke/x"), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(url = "https://example.com/x"), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(url = "https://meteo.go.ke.evil.example/x"), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(from = "2026-11-03"), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(to = "2 Nov"), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(counties = setOf(99)), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(text = " "), known).isNotEmpty())
        assertTrue(Advisories.problems(advisory(text = "x".repeat(2_001)), known).isNotEmpty())
    }

    @Test fun promptTextNamesSourceAndValidityAndFitsTheLimit() {
        val t = Advisories.promptText(advisory())
        assertTrue(t.startsWith("Kenya Meteorological Department: Weekly forecast (valid 2026-10-27 to 2026-11-02)"))
        assertTrue(t.contains("Heavy rainfall expected"))
        assertEquals(Advisories.MAX_TEXT_CHARS, Advisories.promptText(advisory(text = "x".repeat(2_000))).length)
    }

    private fun assess(vararg mm: Double) = RiskEngine().assess(
        CountyForecast(nakuru, mm.mapIndexed { i, v -> DailyRain("2026-10-${27 + i}", v, null) }, null, "2026-10-27")
    )

    @Test fun requestCarriesAdvisoryAndStaysValid() {
        val r = AiExplainer.toRequest(nakuru, assess(0.0, 60.0), Language.ENGLISH, advisory(text = "x".repeat(2_000)))
        assertTrue(r.advisoryText!!.contains("Kenya Meteorological Department"))
        assertNull(ExplainContract.validateRequest(r))
        assertNull(AiExplainer.toRequest(nakuru, assess(0.0, 60.0), Language.ENGLISH).advisoryText)
    }

    @Test fun explainerSendsTheCountysAdvisoryButNeverChangesTheLevel() = runTest {
        var sent: ExplainRequest? = null
        val explainer = AiExplainer(
            client = { req -> sent = req; ExplainResponse("Heavy rain on Wednesday", "Rain is expected in Nakuru on Wednesday, so take care.", listOf("Keep away from rivers")) },
            advisoryFor = { if (it.code == 32) advisory() else null },
        )
        val a = assess(0.0, 60.0)
        val e = explainer.explain(nakuru, a, Language.ENGLISH)
        assertEquals(ExplanationSource.AI, e.source)
        assertTrue(sent!!.advisoryText!!.contains("Heavy rainfall expected"))
        assertEquals(a.level.name, sent!!.riskLevel)
    }
}
