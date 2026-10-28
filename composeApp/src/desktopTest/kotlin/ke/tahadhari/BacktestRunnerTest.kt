package ke.tahadhari

import ke.tahadhari.data.Counties
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.RiskEngine
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskThresholds
import ke.tahadhari.domain.RiverOutlook
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.LocalDate
import kotlin.test.Test

/**
 * Backtest of the real [RiskEngine] on the March-May 2024 Kenya floods.
 *
 * Gated: does nothing unless TAHADHARI_BACKTEST=1. Never touches the network; it only reads the
 * committed data in tools/backtest/data (made by tools/backtest/fetch.py). Writes
 * build/backtest/results.csv, which tools/backtest/analyze.py turns into docs/validation.md numbers.
 *
 *   TAHADHARI_BACKTEST=1 ./gradlew :composeApp:desktopTest --tests "ke.tahadhari.BacktestRunnerTest"
 */
class BacktestRunnerTest {

    /** Named threshold sets. "default" uses the shipped defaults; the others are what-if scenarios. */
    private val scenarios: List<Pair<String, RiskThresholds>> = listOf(
        "default" to RiskThresholds(),
        "rain_lower" to RiskThresholds(
            dailyModerateMm = 15.0, dailyHighMm = 35.0, dailySevereMm = 60.0,
            threeDayModerateMm = 30.0, threeDayHighMm = 60.0, threeDaySevereMm = 100.0,
        ),
        "rain_lower_river_x3" to RiskThresholds(
            dailyModerateMm = 15.0, dailyHighMm = 35.0, dailySevereMm = 60.0,
            threeDayModerateMm = 30.0, threeDayHighMm = 60.0, threeDaySevereMm = 100.0,
            riverHighRatio = 3.0, riverSevereRatio = 5.0,
        ),
    )

    @Test
    fun runBacktest() {
        if (System.getenv("TAHADHARI_BACKTEST") != "1") return

        val dataDir = findDataDir()
        val models = listOf("ecmwf_ifs025", "gfs_seamless")
        val forecasts = models.associateWith { parseForecast(File(dataDir, "forecast_$it.json")) }
        val era5 = parseDaily(File(dataDir, "era5.json"), "era5")
        val flood = Json.parseToJsonElement(File(dataDir, "flood.json").readText())
            .jsonObject.getValue("flood").jsonObject
            .mapValues { (_, v) -> v.jsonObject.getValue("daily").jsonObject.mapValues { it.value.jsonPrimitive.doubleOrNull } }
        val locIds = (forecasts.getValue(models[0]).keys).toList()
        val locMeta = Json.parseToJsonElement(File(dataDir, "forecast_${models[0]}.json").readText())
            .jsonObject.getValue("locations").jsonArray.map { it.jsonObject }
            .associateBy { it.getValue("id").jsonPrimitive.content }

        val engines = scenarios.map { (name, t) -> name to RiskEngine(t) }
        val out = StringBuilder()
        out.appendLine("source,loc,county,issue,wettest_mm,max3_mm,river_ratio," + engines.joinToString(",") { "${it.first}_level,${it.first}_rainonly" })

        val first = LocalDate.parse("2024-03-15"); val last = LocalDate.parse("2024-05-31")
        val sources = models + "era5"
        for (source in sources) for (id in locIds) {
            val meta = locMeta.getValue(id)
            val county = Counties.all.first { it.name == meta.getValue("county").jsonPrimitive.content }
            var d = first
            while (!d.isAfter(last)) {
                val issue = d.toString()
                val days = (0..6).map { k ->
                    val date = d.plusDays(k.toLong()).toString()
                    val mm = if (source == "era5") era5.getValue(id)[date]
                    else forecasts.getValue(source).getValue(id).getValue(k.toString())[date]
                    DailyRain(date, mm ?: error("missing $source $id $date"), null)
                }
                val river = flood[id]?.let { series ->
                    val dates = (-30..6).map { d.plusDays(it.toLong()).toString() }
                    RiverOutlook.fromSeries(county.riverName, dates, dates.map { series[it] }, today = issue)
                }
                val withRiver = CountyForecast(county, days, river, issue)
                val rainOnly = withRiver.copy(river = null)
                val base = engines[0].second.assess(withRiver)
                out.append("$source,$id,${meta.getValue("name").jsonPrimitive.content.replace(",", " ")},$issue,")
                out.append("${base.wettestDay?.precipitationMm},${"%.1f".format(base.maxThreeDayTotalMm)},")
                out.append(river?.let { "%.2f".format(it.riseRatio) } ?: "")
                for ((_, e) in engines) {
                    out.append(",${e.assess(withRiver).level.ordinal},${e.assess(rainOnly).level.ordinal}")
                }
                out.appendLine()
                d = d.plusDays(1)
            }
        }
        val target = File("build/backtest/results.csv")
        target.parentFile.mkdirs()
        target.writeText(out.toString())
        println("Backtest wrote ${target.absolutePath} (levels: " + RiskLevel.entries.joinToString { "${it.ordinal}=${it.name}" } + ")")
    }

    private fun findDataDir(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "tools/backtest/data")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile
        }
        error("tools/backtest/data not found; run tools/backtest/fetch.py first")
    }

    // id -> lead -> date -> mm
    private fun parseForecast(f: File): Map<String, Map<String, Map<String, Double?>>> =
        Json.parseToJsonElement(f.readText()).jsonObject.getValue("forecast").jsonObject.mapValues { (_, byLead) ->
            byLead.jsonObject.mapValues { (_, days) -> days.jsonObject.mapValues { (it.value as? JsonPrimitive)?.doubleOrNull } }
        }

    private fun parseDaily(f: File, key: String): Map<String, Map<String, Double?>> =
        (Json.parseToJsonElement(f.readText()) as JsonObject).getValue(key).jsonObject.mapValues { (_, days) ->
            days.jsonObject.mapValues { (it.value as? JsonPrimitive)?.doubleOrNull }
        }
}
