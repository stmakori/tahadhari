package ke.tahadhari.domain

enum class RiskLevel { LOW, MODERATE, HIGH, SEVERE }

sealed interface RiskReason {
    val level: RiskLevel
    data class HeavyDay(val date: String, val mm: Double, override val level: RiskLevel) : RiskReason
    data class WetSpell(val startDate: String, val totalMm: Double, override val level: RiskLevel) : RiskReason
    /** Heavy 3-day rain in a landslide-prone county ([ke.tahadhari.data.County.landslideProne]). */
    data class LandslideRisk(val startDate: String, val totalMm: Double, override val level: RiskLevel) : RiskReason
    /**
     * The ground is already very wet (soil moisture at 9-27 cm), so rain runs off instead of soaking in.
     * Raises a rainfall-based Moderate by one step, capped at High. [level] is the level it leads to.
     */
    data class SaturatedGround(val soilMoisture: Double, override val level: RiskLevel) : RiskReason
    /**
     * Heavy rain (heavy day or wet spell at or above [RiskThresholds.upstreamTriggerLevel]) in an upstream county
     * can raise this county's river within days. [totalMm] is that county's wettest 3-day total; [level] is the
     * floor it sets for this county.
     */
    data class UpstreamRain(val countyName: String, val totalMm: Double, override val level: RiskLevel) : RiskReason
    data class RiverRising(val riverName: String?, val ratio: Double, override val level: RiskLevel) : RiskReason
}

data class RiskAssessment(
    val level: RiskLevel,
    val reasons: List<RiskReason>,
    val wettestDay: DailyRain?,
    val maxThreeDayTotalMm: Double,
    val river: RiverOutlook?,
    /** Upstream rain could not be loaded for a county that has upstream counties (see [CountyForecast.upstreamUnavailable]). */
    val upstreamUnavailable: Boolean = false,
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
    /**
     * Soil moisture (m³/m³, 9-27 cm layer) at or above which the ground counts as saturated. null (the
     * default) disables the signal: the back-test did not support it (it roughly doubled High+ county-days,
     * 7.5% to 14.8% at 0.40, for about 2 more event hits, and the value does not transfer between soil
     * datasets). See docs/validation.md section 8. The soil moisture fetch and the rule stay in the code.
     */
    val soilSaturatedM3m3: Double? = null,
    /**
     * Upstream rain: when an upstream county's rainfall-only level (heavy day / wet spell) reaches
     * [upstreamTriggerLevel], the downstream county is at least this level. null (the default) disables the
     * signal: the back-test showed it adds almost nothing beyond the river signal (4-6 extra county-days of 468 on
     * the Tana counties, no new event hits). See docs/validation.md section 9. The fetch and the rule stay in the code.
     */
    val upstreamRainFloor: RiskLevel? = null,
    val upstreamTriggerLevel: RiskLevel = RiskLevel.HIGH,
)

class RiskEngine(private val t: RiskThresholds = RiskThresholds()) {

    fun assess(forecast: CountyForecast): RiskAssessment {
        val reasons = mutableListOf<RiskReason>()
        val wettest = forecast.days.maxByOrNull { it.precipitationMm }

        wettest?.let { day ->
            levelFor(day.precipitationMm, t.dailyModerateMm, t.dailyHighMm, t.dailySevereMm)
                ?.let { reasons += RiskReason.HeavyDay(day.date, day.precipitationMm, it) }
        }

        val (bestTotal, bestStart) = wettestThreeDays(forecast.days)
        bestStart?.let { start ->
            levelFor(bestTotal, t.threeDayModerateMm, t.threeDayHighMm, t.threeDaySevereMm)
                ?.let { reasons += RiskReason.WetSpell(start, bestTotal, it) }
        }

        // Rainfall-based level only (heavy day / wet spell); river and landslide don't count for saturation.
        val rainLevel = reasons.maxOfOrNull { it.level }

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

        // Saturated ground: one step up, capped at High. Never raises Low, never lowers or creates Severe.
        val soil = forecast.soilMoistureM3m3
        val soilLimit = t.soilSaturatedM3m3
        if (soil != null && soilLimit != null && soil >= soilLimit && rainLevel != null && rainLevel >= RiskLevel.MODERATE) {
            val current = reasons.maxOf { it.level }
            val boosted = if (current < RiskLevel.HIGH) RiskLevel.entries[current.ordinal + 1] else current
            reasons += RiskReason.SaturatedGround(soil, boosted)
        }

        // Upstream rain: a floor, never a lowering. Missing upstream data (empty list) changes nothing.
        val floor = t.upstreamRainFloor
        if (floor != null) {
            upstreamTrigger(forecast.upstream)?.let { (name, total) ->
                reasons += RiskReason.UpstreamRain(name, total, floor)
            }
        }

        return RiskAssessment(
            level = reasons.maxOfOrNull { it.level } ?: RiskLevel.LOW,
            reasons = reasons.sortedByDescending { it.level },
            wettestDay = wettest,
            maxThreeDayTotalMm = bestTotal,
            // Below the baseline floor the ratio is meaningless, so don't show it either.
            river = forecast.river?.takeIf { it.baselineDischarge >= t.riverMinBaselineM3s },
            upstreamUnavailable = forecast.upstreamUnavailable && floor != null,
        )
    }

    private fun wettestThreeDays(days: List<DailyRain>): Pair<Double, String?> {
        var bestTotal = 0.0
        var bestStart: String? = null
        days.windowed(size = 3, partialWindows = false).forEach { window ->
            val total = window.sumOf { it.precipitationMm }
            if (total > bestTotal) { bestTotal = total; bestStart = window.first().date }
        }
        return bestTotal to bestStart
    }

    /** Rainfall-only level of an upstream county (heavy day / wet spell; never river or landslide) and its wettest 3-day total. */
    private fun upstreamTrigger(upstream: List<UpstreamForecast>): Pair<String, Double>? =
        upstream.mapNotNull { u ->
            val day = u.days.maxOfOrNull { it.precipitationMm } ?: return@mapNotNull null
            val (total, _) = wettestThreeDays(u.days)
            val level = maxOf(
                levelFor(day, t.dailyModerateMm, t.dailyHighMm, t.dailySevereMm) ?: RiskLevel.LOW,
                levelFor(total, t.threeDayModerateMm, t.threeDayHighMm, t.threeDaySevereMm) ?: RiskLevel.LOW,
            )
            if (level >= t.upstreamTriggerLevel) Triple(u.countyName, maxOf(total, day), level) else null
        }.maxWithOrNull(compareBy<Triple<String, Double, RiskLevel>> { it.third }.thenBy { it.second })
            ?.let { it.first to it.second }

    private fun levelFor(value: Double, moderate: Double, high: Double, severe: Double): RiskLevel? = when {
        value >= severe -> RiskLevel.SEVERE
        value >= high -> RiskLevel.HIGH
        value >= moderate -> RiskLevel.MODERATE
        else -> null
    }
}
