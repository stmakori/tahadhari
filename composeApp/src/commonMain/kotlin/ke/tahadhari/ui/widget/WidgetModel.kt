package ke.tahadhari.ui.widget

import ke.tahadhari.data.Counties
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskReason
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.updatedText
import kotlin.math.roundToInt

/** Why the widget shows this level. Taken from the RiskEngine assessment, never invented. */
sealed interface WidgetReason {
    data class WettestDay(val mm: Int) : WidgetReason
    data class WetSpell(val mm: Int) : WidgetReason
    data class RiverRising(val ratio: String) : WidgetReason
    /** No signal reached Moderate. */
    data object Quiet : WidgetReason
}

/** What the widget is in. Pure data: the Glance UI and the tests both use it. */
sealed interface WidgetUiState {
    /** The user has not chosen a county yet. */
    data object NoCounty : WidgetUiState

    data class Loading(val countyName: String) : WidgetUiState

    /** [fromCache] is true when the data is a saved forecast rather than a fresh download. */
    data class Ready(
        val countyName: String,
        val level: RiskLevel,
        val reason: WidgetReason,
        val updatedAtMillis: Long,
        val fromCache: Boolean,
    ) : WidgetUiState

    /** Nothing safe to show. There is deliberately no risk level here: never show "Low" without data. */
    data class NoData(val countyName: String, val savedSinceMillis: Long?) : WidgetUiState
}

/**
 * Maps saved preferences and a load result to the widget state.
 * [loadState] null means "no result yet" (still loading).
 */
fun widgetUiState(prefs: UserPreferences, loadState: LoadState?): WidgetUiState {
    val county = prefs.countyCode?.let { Counties.byCode(it) } ?: return WidgetUiState.NoCounty
    return when (loadState) {
        null, LoadState.Loading -> WidgetUiState.Loading(county.name)
        is LoadState.Failed -> WidgetUiState.NoData(county.name, loadState.staleSinceMillis)
        is LoadState.Ready -> WidgetUiState.Ready(
            countyName = county.name,
            level = loadState.assessment.level,
            reason = reasonOf(loadState),
            updatedAtMillis = loadState.updatedAtMillis,
            fromCache = loadState.fromCache,
        )
    }
}

private fun reasonOf(state: LoadState.Ready): WidgetReason =
    when (val top = state.assessment.reasons.firstOrNull()) {
        is RiskReason.HeavyDay -> WidgetReason.WettestDay(top.mm.roundToInt())
        is RiskReason.WetSpell -> WidgetReason.WetSpell(top.totalMm.roundToInt())
        is RiskReason.RiverRising -> WidgetReason.RiverRising(oneDecimal(top.ratio))
        null -> state.assessment.wettestDay?.let { WidgetReason.WettestDay(it.precipitationMm.roundToInt()) }
            ?: WidgetReason.Quiet
    }

private fun oneDecimal(x: Double): String {
    val tenths = (x * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** Ready-to-draw text for one widget state in one language. */
data class WidgetContent(
    val title: String,
    /** Null when there is no risk level to show (never defaults to Low). */
    val level: RiskLevel?,
    val levelLabel: String?,
    val reason: String?,
    val updated: String?,
    /** "Saved forecast" marker, only for cached data. */
    val savedMarker: String?,
    val footer: String,
    /** True when tapping should retry the refresh instead of just opening the app. */
    val tapRetries: Boolean,
)

fun WidgetUiState.toContent(strings: Strings, nowMillis: Long): WidgetContent = when (this) {
    WidgetUiState.NoCounty -> WidgetContent(
        title = strings.widgetNoCounty, level = null, levelLabel = null, reason = null,
        updated = null, savedMarker = null, footer = strings.widgetNotOfficial, tapRetries = false,
    )
    is WidgetUiState.Loading -> WidgetContent(
        title = countyName, level = null, levelLabel = strings.loadingShort, reason = null,
        updated = null, savedMarker = null, footer = strings.widgetNotOfficial, tapRetries = false,
    )
    is WidgetUiState.NoData -> WidgetContent(
        title = countyName, level = null, levelLabel = strings.noDataShort,
        reason = strings.widgetNoDataBody,
        updated = savedSinceMillis?.let { strings.widgetSavedOn(Dates.isoDateFromEpochMillis(it)) },
        savedMarker = null, footer = strings.widgetTapToRetry, tapRetries = true,
    )
    is WidgetUiState.Ready -> WidgetContent(
        title = countyName, level = level, levelLabel = strings.risk(level),
        reason = when (val r = reason) {
            is WidgetReason.WettestDay -> strings.wettestShort(r.mm)
            is WidgetReason.WetSpell -> strings.threeDayShort(r.mm)
            is WidgetReason.RiverRising -> strings.widgetRiverRising(r.ratio)
            WidgetReason.Quiet -> null
        },
        updated = strings.updatedText(updatedAtMillis, nowMillis),
        savedMarker = if (fromCache) strings.widgetSavedForecast else null,
        footer = strings.widgetNotOfficial, tapRetries = false,
    )
}

/**
 * Cache-only load used when drawing the widget (no network on the draw path; the worker
 * downloads). Applies the same freshness rule as the app: a forecast older than
 * [Freshness.MAX_SHOWABLE_AGE_MILLIS] becomes [LoadState.Failed] with its date, never a level.
 * Returns null when nothing is saved for the county.
 *
 * [fromCache] is set when the data is older than [SAVED_MARKER_AFTER_MILLIS]: the worker runs
 * every 3 hours, so older data means at least one refresh was missed (offline, say).
 */
suspend fun loadSavedForWidget(
    county: ke.tahadhari.data.County,
    cache: ke.tahadhari.data.ForecastCache,
    riskEngine: ke.tahadhari.domain.RiskEngine,
    nowMillis: Long,
): LoadState? {
    val saved = cache.read(county.code) ?: return null
    if (ke.tahadhari.domain.Freshness.isTooOldToShow(saved.fetchedAtMillis, nowMillis)) {
        return LoadState.Failed("Saved forecast is too old", staleSinceMillis = saved.fetchedAtMillis)
    }
    val today = Dates.isoDateFromEpochMillis(nowMillis)
    val forecast = ke.tahadhari.data.ForecastRepository.buildForecast(county, saved.rainfall, saved.flood, today)
    if (forecast.days.isEmpty()) return LoadState.Failed("Saved forecast has no days left", saved.fetchedAtMillis)
    return LoadState.Ready(
        forecast = forecast.copy(fetchedAtDate = Dates.isoDateFromEpochMillis(saved.fetchedAtMillis)),
        assessment = riskEngine.assess(forecast),
        updatedAtMillis = saved.fetchedAtMillis,
        fromCache = nowMillis - saved.fetchedAtMillis > SAVED_MARKER_AFTER_MILLIS,
    )
}

const val SAVED_MARKER_AFTER_MILLIS: Long = 6 * 60 * 60 * 1000L
