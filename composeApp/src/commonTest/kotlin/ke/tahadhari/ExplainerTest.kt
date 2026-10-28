package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import kotlin.test.Test
import kotlin.test.assertTrue

class ExplainerTest {
    private val explainer = TemplateExplainer()
    private val narok = Counties.byCode(33)!!

    private fun assess(vararg mm: Double) = RiskEngine().assess(
        CountyForecast(narok, mm.mapIndexed { i, v -> DailyRain("2026-10-${27 + i}", v, null) }, null, "2026-10-27")
    )

    @Test fun englishHighRiskNamesDayAndHotspots() {
        val e = explainer.build(narok, assess(0.0, 60.0, 10.0), Language.ENGLISH)
        assertTrue(e.headline.contains("Narok"))
        assertTrue(e.body.contains("Wednesday"))
        assertTrue(e.body.contains("Mosiro"))
        assertTrue(e.actions.isNotEmpty())
    }

    @Test fun swahiliUsesSwahiliDayNames() {
        val e = explainer.build(narok, assess(0.0, 60.0, 10.0), Language.SWAHILI)
        assertTrue(e.body.contains("Jumatano"))
        assertTrue(e.headline.startsWith("Hatari"))
    }

    @Test fun lowRiskHasNoHotspotWarning() {
        val e = explainer.build(narok, assess(1.0, 2.0), Language.ENGLISH)
        assertTrue(!e.body.contains("Mosiro"))
    }

    private val muranga = Counties.byCode(21)!!
    private fun assessIn(c: ke.tahadhari.data.County, vararg mm: Double) = RiskEngine().assess(
        CountyForecast(c, mm.mapIndexed { i, v -> DailyRain("2026-10-${27 + i}", v, null) }, null, "2026-10-27")
    )

    @Test fun landslideAdviceAppearsForProneCountyInEnglish() {
        val e = explainer.build(muranga, assessIn(muranga, 17.0, 17.0, 16.0), Language.ENGLISH)
        assertTrue(e.body.contains("Landslides are possible"))
        assertTrue(e.body.contains("Kangema"))
        assertTrue(e.actions.any { it.contains("cracks") && it.contains("tilting") })
        assertTrue(e.actions.any { it.contains("steep slopes") && it.contains("night") && it.contains("Don't return") })
        assertTrue(e.actions.size <= 5)
    }

    @Test fun landslideAdviceAppearsInSwahili() {
        val e = explainer.build(muranga, assessIn(muranga, 17.0, 17.0, 16.0), Language.SWAHILI)
        assertTrue(e.body.contains("Maporomoko ya ardhi"))
        assertTrue(e.actions.any { it.contains("nyufa") })
        assertTrue(e.actions.any { it.contains("usiku") })
    }

    @Test fun noLandslideAdviceForNonProneCountyWithSameRain() {
        val flat = muranga.copy(landslideProne = false)
        val e = explainer.build(flat, assessIn(flat, 17.0, 17.0, 16.0), Language.ENGLISH)
        assertTrue(!e.body.contains("Landslides are possible"))
        assertTrue(e.actions.none { it.contains("cracks") })
    }
}
