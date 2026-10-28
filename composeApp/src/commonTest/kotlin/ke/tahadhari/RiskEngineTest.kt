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

    @Test fun riverOnNearlyDryCellIsIgnored() {
        // A grid cell that misses the channel: 0.05 -> 1.7 m³/s is a 34x "rise" but means nothing.
        val river = RiverOutlook("Tana River", baselineDischarge = 0.05, peakDischarge = 1.7, peakDate = "2026-10-29")
        val a = engine.assess(forecast(5.0, 5.0, 5.0, river = river))
        assertEquals(RiskLevel.LOW, a.level)
        assertTrue(a.reasons.none { it is RiskReason.RiverRising })
    }

    @Test fun riverAtMinimumBaselineStillCounts() {
        val t = RiskThresholds()
        val river = RiverOutlook("Nzoia", baselineDischarge = t.riverMinBaselineM3s, peakDischarge = t.riverMinBaselineM3s * 3, peakDate = "2026-10-29")
        assertEquals(RiskLevel.SEVERE, engine.assess(forecast(5.0, 5.0, 5.0, river = river)).level)
        val below = river.copy(baselineDischarge = t.riverMinBaselineM3s - 0.1, peakDischarge = t.riverMinBaselineM3s * 3)
        assertEquals(RiskLevel.LOW, engine.assess(forecast(5.0, 5.0, 5.0, river = below)).level)
    }

    @Test fun everyRiverPointHasADefinedRiverName() {
        Counties.all.filter { it.riverPoint != null }.forEach { assertTrue(it.riverName != null, it.name) }
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

    // ---- landslide signal ----

    private val murangaProne = Counties.byCode(21)!!.copy(landslideProne = true)
    private val flatCounty = Counties.byCode(21)!!.copy(landslideProne = false)

    private fun forecastIn(county: ke.tahadhari.data.County, vararg mm: Double) = CountyForecast(
        county = county,
        days = mm.mapIndexed { i, v -> DailyRain("2026-10-${27 + i}", v, null) },
        river = null,
        fetchedAtDate = "2026-10-27",
    )

    @Test fun landslideProneCountyWith3DayRainAtHighThresholdIsHigh() {
        // 3-day total 50 mm: only MODERATE from the wet-spell rule (>=40), but 50 on a slope is HIGH.
        val a = engine.assess(forecastIn(murangaProne, 17.0, 17.0, 16.0, 0.0))
        assertEquals(50.0, a.maxThreeDayTotalMm)
        assertEquals(RiskLevel.HIGH, a.level)
        val r = a.reasons.filterIsInstance<RiskReason.LandslideRisk>().single()
        assertEquals(RiskLevel.HIGH, r.level)
        assertEquals("2026-10-27", r.startDate)
        assertEquals(50.0, r.totalMm)
    }

    @Test fun sameRainInNonProneCountyIsOnlyModerate() {
        val a = engine.assess(forecastIn(flatCounty, 17.0, 17.0, 16.0, 0.0))
        assertEquals(RiskLevel.MODERATE, a.level)
        assertTrue(a.reasons.none { it is RiskReason.LandslideRisk })
    }

    @Test fun landslideBoundariesJustBelowAndAtThresholds() {
        assertEquals(RiskLevel.MODERATE, engine.assess(forecastIn(murangaProne, 16.0, 16.0, 17.9)).level) // 49.9
        assertTrue(engine.assess(forecastIn(murangaProne, 16.0, 16.0, 17.9)).reasons.none { it is RiskReason.LandslideRisk })
        assertEquals(RiskLevel.HIGH, engine.assess(forecastIn(murangaProne, 30.0, 30.0, 39.9)).level)   // 99.9 -> wet spell is HIGH too
        val severe = engine.assess(forecastIn(murangaProne, 33.0, 33.0, 34.0)) // exactly 100
        assertEquals(RiskLevel.SEVERE, severe.level)
        assertEquals(RiskLevel.SEVERE, severe.reasons.filterIsInstance<RiskReason.LandslideRisk>().single().level)
    }

    @Test fun landslideSevereBeatsWetSpellHigh() {
        // 100 mm over 3 days is only HIGH by the generic wet-spell rule (<120) and no single day >= 50.
        val flat = engine.assess(forecastIn(flatCounty, 33.0, 33.0, 34.0))
        assertEquals(RiskLevel.HIGH, flat.level)
        assertEquals(RiskLevel.SEVERE, engine.assess(forecastIn(murangaProne, 33.0, 33.0, 34.0)).level)
    }

    @Test fun landslideNeverLowersAnotherSignal() {
        // A severe river signal stays SEVERE even when the landslide signal is only HIGH.
        val river = RiverOutlook("Maragua", baselineDischarge = 10.0, peakDischarge = 35.0, peakDate = "2026-10-29")
        val f = forecastIn(murangaProne, 17.0, 17.0, 16.0).copy(river = river)
        val a = engine.assess(f)
        assertEquals(RiskLevel.SEVERE, a.level)
        assertTrue(a.reasons.any { it is RiskReason.LandslideRisk })
        assertTrue(a.reasons.any { it is RiskReason.RiverRising })
    }

    @Test fun landslideThresholdsAreConfigurable() {
        val strict = RiskEngine(RiskThresholds(landslideHighMm = 30.0, landslideSevereMm = 45.0))
        assertEquals(RiskLevel.SEVERE, strict.assess(forecastIn(murangaProne, 15.0, 15.0, 15.0)).level)
        assertEquals(50.0, RiskThresholds().landslideHighMm)
        assertEquals(100.0, RiskThresholds().landslideSevereMm)
    }

    @Test fun landslideFieldsInCountyData() {
        assertTrue(Counties.all.any { it.landslideProne })
        assertTrue(Counties.all.filter { it.landslideAreas.isNotEmpty() }.all { it.landslideProne })
        assertTrue(!Counties.byCode(47)!!.landslideProne) // Nairobi: default false
    }

    // --- Saturated ground (soil moisture) ---

    // The signal is off by default (see RiskThresholds.soilSaturatedM3m3), so tests pass an explicit threshold.
    private val soilEngine = RiskEngine(RiskThresholds(soilSaturatedM3m3 = 0.40))

    @Test fun saturatedGroundIsOffByDefault() {
        assertNull(RiskThresholds().soilSaturatedM3m3)
        val a = engine.assess(forecast(0.0, 25.0, 0.0).copy(soilMoistureM3m3 = 0.9))
        assertEquals(RiskLevel.MODERATE, a.level)
        assertTrue(a.reasons.none { it is RiskReason.SaturatedGround })
    }

    private fun wet(soil: Double?, vararg mm: Double, river: RiverOutlook? = null) =
        forecast(*mm, river = river).copy(soilMoistureM3m3 = soil)

    @Test fun saturatedGroundRaisesModerateToHigh() {
        assertEquals(RiskLevel.MODERATE, soilEngine.assess(wet(null, 0.0, 25.0, 0.0)).level)
        val a = soilEngine.assess(wet(0.45, 0.0, 25.0, 0.0))
        assertEquals(RiskLevel.HIGH, a.level)
        val reason = a.reasons.filterIsInstance<RiskReason.SaturatedGround>().single()
        assertEquals(0.45, reason.soilMoisture)
        assertEquals(RiskLevel.HIGH, reason.level)
    }

    @Test fun saturatedGroundRaisesModerateWetSpellToo() {
        // 3-day total 45 mm, no single day >= 20 mm: MODERATE from the wet spell only.
        assertEquals(RiskLevel.MODERATE, soilEngine.assess(wet(null, 15.0, 15.0, 15.0)).level)
        assertEquals(RiskLevel.HIGH, soilEngine.assess(wet(0.5, 15.0, 15.0, 15.0)).level)
    }

    @Test fun saturatedGroundNeverRaisesLow() {
        val a = soilEngine.assess(wet(0.6, 0.0, 10.0, 5.0))
        assertEquals(RiskLevel.LOW, a.level)
        assertTrue(a.reasons.isEmpty())
    }

    @Test fun saturatedGroundDoesNotCreateSevere() {
        // Rain alone is HIGH (55 mm day): wet ground must not push it to SEVERE.
        assertEquals(RiskLevel.HIGH, soilEngine.assess(wet(0.6, 0.0, 55.0, 0.0)).level)
    }

    @Test fun saturatedGroundNeverLowersSevere() {
        val a = soilEngine.assess(wet(0.6, 0.0, 90.0, 0.0))
        assertEquals(RiskLevel.SEVERE, a.level)
        assertEquals(RiskLevel.SEVERE, a.reasons.first().level)
    }

    @Test fun missingSoilDataChangesNothing() {
        val a = soilEngine.assess(wet(null, 0.0, 25.0, 0.0))
        assertEquals(RiskLevel.MODERATE, a.level)
        assertTrue(a.reasons.none { it is RiskReason.SaturatedGround })
    }

    @Test fun nullThresholdDisablesTheSignal() {
        val off = RiskEngine(RiskThresholds(soilSaturatedM3m3 = null))
        val a = off.assess(wet(0.9, 0.0, 25.0, 0.0))
        assertEquals(RiskLevel.MODERATE, a.level)
        assertTrue(a.reasons.none { it is RiskReason.SaturatedGround })
    }

    @Test fun soilThresholdBoundaryIsInclusive() {
        val e = RiskEngine(RiskThresholds(soilSaturatedM3m3 = 0.40))
        assertEquals(RiskLevel.HIGH, e.assess(wet(0.40, 0.0, 25.0, 0.0)).level)
        assertEquals(RiskLevel.MODERATE, e.assess(wet(0.399, 0.0, 25.0, 0.0)).level)
    }

    @Test fun saturatedGroundNeedsRainfallNotJustRiver() {
        // River HIGH, rain only LOW: wet ground must add nothing.
        val river = RiverOutlook("Enkare Narok", baselineDischarge = 10.0, peakDischarge = 25.0, peakDate = "2026-10-29")
        val a = soilEngine.assess(wet(0.6, 5.0, 5.0, 5.0, river = river))
        assertEquals(RiskLevel.HIGH, a.level)
        assertTrue(a.reasons.none { it is RiskReason.SaturatedGround })
    }

    @Test fun saturatedGroundIsCappedAtHighWhenRiverAlreadyHigh() {
        val river = RiverOutlook("Enkare Narok", baselineDischarge = 10.0, peakDischarge = 25.0, peakDate = "2026-10-29")
        assertEquals(RiskLevel.HIGH, soilEngine.assess(wet(0.6, 0.0, 25.0, 0.0, river = river)).level)
    }
}
