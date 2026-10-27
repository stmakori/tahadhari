package ke.tahadhari.domain

import ke.tahadhari.data.County

data class DailyRain(
    val date: String,              // ISO yyyy-MM-dd, Africa/Nairobi
    val precipitationMm: Double,
    val probabilityPercent: Int?,
)

data class RiverOutlook(
    val riverName: String?,
    val baselineDischarge: Double, // m³/s, mean of recent past days
    val peakDischarge: Double,     // m³/s, highest forecast value
    val peakDate: String,
) {
    /** How many times higher the forecast peak is than the recent baseline. */
    val riseRatio: Double
        get() = if (baselineDischarge > 0.0) peakDischarge / baselineDischarge else 1.0

    companion object {
        /**
         * Builds an outlook from Open-Meteo flood API arrays.
         * Values dated before [today] form the baseline; values from [today] on are the forecast.
         * Returns null when there is not enough data.
         */
        fun fromSeries(riverName: String?, dates: List<String>, discharge: List<Double?>, today: String): RiverOutlook? {
            val pairs = dates.zip(discharge).mapNotNull { (d, v) -> v?.let { d to it } }
            val past = pairs.filter { it.first < today }.map { it.second }
            val future = pairs.filter { it.first >= today }
            if (future.isEmpty()) return null
            val baseline = if (past.isNotEmpty()) past.average() else future.first().second
            val peak = future.maxBy { it.second }
            return RiverOutlook(riverName, baseline, peak.second, peak.first)
        }
    }
}

data class CountyForecast(
    val county: County,
    val days: List<DailyRain>,
    val river: RiverOutlook?,
    val fetchedAtDate: String,
)
