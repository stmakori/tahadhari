package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RiskEngineTest {
    private val engine = RiskEngine()
    private val narok = Counties.byCode(33)!!

    private fun forecast(vararg mm: Double, river: RiverOutlook? = null) = CountyForecast(
        county = narok,
        days = mm.mapIndexed { i, v -> DailyRain("2026-10-${27 + i}", v, null) },
        river = river,
        fetchedAtDate = "2026-10-27",
    )

    @Test fun dryWeekIsLow() {
        assertEquals(RiskLevel.LOW, engine.assess(forecast(0.0, 2.0, 5.0, 1.0, 0.0)).level)
    }

    @Test fun singleHeavyDayIsHigh() {
        val a = engine.assess(forecast(0.0, 55.0, 0.0, 0.0))
        assertEquals(RiskLevel.HIGH, a.level)
        assertEquals("2026-10-28", a.wettestDay?.date)
    }

    @Test fun prolongedWetSpellRaisesRiskEvenWithoutOneHugeDay() {
        // 3-day total 135 mm, no single day above 50 mm
        val a = engine.assess(forecast(45.0, 45.0, 45.0, 0.0))
        assertEquals(RiskLevel.SEVERE, a.level)
        assertEquals(135.0, a.maxThreeDayTotalMm)
    }

    @Test fun risingRiverAloneCanBeSevere() {
        val river = RiverOutlook("Enkare Narok", baselineDischarge = 10.0, peakDischarge = 35.0, peakDate = "2026-10-29")
        val a = engine.assess(forecast(5.0, 5.0, 5.0, river = river))
        assertEquals(RiskLevel.SEVERE, a.level)
        assertTrue(a.reasons.first() is RiskReason.RiverRising)
    }

    @Test fun riverOutlookUsesPastDaysAsBaseline() {
        val o = RiverOutlook.fromSeries(
            "Nzoia",
            dates = listOf("2026-10-25", "2026-10-26", "2026-10-27", "2026-10-28"),
            discharge = listOf(10.0, 20.0, 30.0, 45.0),
            today = "2026-10-27",
        )!!
        assertEquals(15.0, o.baselineDischarge)
        assertEquals(45.0, o.peakDischarge)
        assertEquals("2026-10-28", o.peakDate)
        assertEquals(3.0, o.riseRatio)
    }

    @Test fun riverOutlookIsNullWithoutForecastValues() {
        assertNull(RiverOutlook.fromSeries(null, listOf("2026-10-20"), listOf(5.0), "2026-10-27"))
    }
}
