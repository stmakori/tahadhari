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
}
