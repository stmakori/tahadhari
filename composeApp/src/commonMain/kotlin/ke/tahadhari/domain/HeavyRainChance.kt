package ke.tahadhari.domain

import kotlin.math.roundToInt

/**
 * How many ensemble forecast runs ("members") have a day at or above the High / Moderate single-day
 * rainfall thresholds in the coming week. INFORMATION ONLY: it never feeds [RiskEngine].
 */
data class HeavyRainChance(
    val members: Int,
    val overHigh: Int,
    val overModerate: Int,
    val highMm: Double,
    val moderateMm: Double,
) {
    val highPercent: Int get() = (overHigh * 100.0 / members).roundToInt()
    val moderatePercent: Int get() = (overModerate * 100.0 / members).roundToInt()

    companion object {
        /**
         * [memberDailyMm] has one list of daily rainfall (mm) per member. A member with no usable day is
         * not counted. Returns null when no member has data, so callers hide the line instead of showing 0%.
         */
        fun fromMembers(memberDailyMm: List<List<Double?>>, moderateMm: Double, highMm: Double): HeavyRainChance? {
            val wettest = memberDailyMm.mapNotNull { days -> days.filterNotNull().maxOrNull() }
            if (wettest.isEmpty()) return null
            return HeavyRainChance(
                members = wettest.size,
                overHigh = wettest.count { it >= highMm },
                overModerate = wettest.count { it >= moderateMm },
                highMm = highMm,
                moderateMm = moderateMm,
            )
        }
    }
}
