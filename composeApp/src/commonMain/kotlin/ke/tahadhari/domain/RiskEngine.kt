package ke.tahadhari.domain

enum class RiskLevel { LOW, MODERATE, HIGH, SEVERE }

sealed interface RiskReason {
    val level: RiskLevel
    data class HeavyDay(val date: String, val mm: Double, override val level: RiskLevel) : RiskReason
    data class WetSpell(val startDate: String, val totalMm: Double, override val level: RiskLevel) : RiskReason
    /** Heavy 3-day rain in a landslide-prone county ([ke.tahadhari.data.County.landslideProne]). */
    data class LandslideRisk(val startDate: String, val totalMm: Double, override val level: RiskLevel) : RiskReason
    data class RiverRising(val riverName: String?, val ratio: Double, override val level: RiskLevel) : RiskReason
}

data class RiskAssessment(
    val level: RiskLevel,
    val reasons: List<RiskReason>,
    val wettestDay: DailyRain?,
    val maxThreeDayTotalMm: Double,
    val river: RiverOutlook?,
)

/**
 * Heuristic thresholds. These are NOT official KMSA criteria; they are a starting point
 * and should be tuned and documented in the README.
 */
data class RiskThresholds(
    val dailyModerateMm: Double = 20.0,
    val dailyHighMm: Double = 50.0,
    val dailySevereMm: Double = 80.0,
    val threeDayModerateMm: Double = 40.0,
    val threeDayHighMm: Double = 80.0,
    val threeDaySevereMm: Double = 120.0,
    val riverHighRatio: Double = 2.0,
    val riverSevereRatio: Double = 3.0,
    /**
     * Ignore the river signal when the recent baseline is below this (m³/s): a grid cell that misses
     * the channel shows huge, meaningless ratios (see docs/validation.md, Tana River and Garissa).
     */
    val riverMinBaselineM3s: Double = 5.0,
    /** Wettest 3-day total at which a landslide-prone county is at least High / Severe. */
    val landslideHighMm: Double = 50.0,
    val landslideSevereMm: Double = 100.0,
)

class RiskEngine(private val t: RiskThresholds = RiskThresholds()) {

    fun assess(forecast: CountyForecast): RiskAssessment {
        val reasons = mutableListOf<RiskReason>()
        val wettest = forecast.days.maxByOrNull { it.precipitationMm }

        wettest?.let { day ->
            levelFor(day.precipitationMm, t.dailyModerateMm, t.dailyHighMm, t.dailySevereMm)
                ?.let { reasons += RiskReason.HeavyDay(day.date, day.precipitationMm, it) }
        }

        var bestTotal = 0.0
        var bestStart: String? = null
        forecast.days.windowed(size = 3, partialWindows = false).forEach { window ->
            val total = window.sumOf { it.precipitationMm }
            if (total > bestTotal) { bestTotal = total; bestStart = window.first().date }
        }
        bestStart?.let { start ->
            levelFor(bestTotal, t.threeDayModerateMm, t.threeDayHighMm, t.threeDaySevereMm)
                ?.let { reasons += RiskReason.WetSpell(start, bestTotal, it) }
        }

        if (forecast.county.landslideProne) {
            val level = when {
                bestTotal >= t.landslideSevereMm -> RiskLevel.SEVERE
                bestTotal >= t.landslideHighMm -> RiskLevel.HIGH
                else -> null
            }
            val start = bestStart
            if (level != null && start != null) reasons += RiskReason.LandslideRisk(start, bestTotal, level)
        }

        forecast.river?.takeIf { it.baselineDischarge >= t.riverMinBaselineM3s }?.let { river ->
            val level = when {
                river.riseRatio >= t.riverSevereRatio -> RiskLevel.SEVERE
                river.riseRatio >= t.riverHighRatio -> RiskLevel.HIGH
                else -> null
            }
            level?.let { reasons += RiskReason.RiverRising(river.riverName, river.riseRatio, it) }
        }

        return RiskAssessment(
            level = reasons.maxOfOrNull { it.level } ?: RiskLevel.LOW,
            reasons = reasons.sortedByDescending { it.level },
            wettestDay = wettest,
            maxThreeDayTotalMm = bestTotal,
            // Below the baseline floor the ratio is meaningless, so don't show it either.
            river = forecast.river?.takeIf { it.baselineDischarge >= t.riverMinBaselineM3s },
        )
    }

    private fun levelFor(value: Double, moderate: Double, high: Double, severe: Double): RiskLevel? = when {
        value >= severe -> RiskLevel.SEVERE
        value >= high -> RiskLevel.HIGH
        value >= moderate -> RiskLevel.MODERATE
        else -> null
    }
}
