package ke.tahadhari.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.data.County
import ke.tahadhari.domain.*
import kotlin.math.roundToInt

private fun Double.oneDecimal(): String = ((this * 10).roundToInt() / 10.0).toString()

/**
 * Full details for one county: hero card, optional status line, explanation, rain chart,
 * river outlook, action checklist and the disclaimer. Does not scroll; the caller decides.
 *
 * @param statusLine slot rendered right under the hero card when data is ready, e.g. an
 *   "Updated 10:30" or "Showing saved data" line built from the [LoadState.Ready] it receives.
 * @param explainer source of the plain-language explanation (AI with template fallback by default).
 * @param onRetry called from the error state.
 */
@Composable
fun CountyDetail(
    county: County,
    state: LoadState,
    language: Language,
    modifier: Modifier = Modifier,
    statusLine: (@Composable (LoadState.Ready) -> Unit)? = { DataStatusLine(it, Strings.of(language)) },
    explainer: AlertExplainer = AppGraph.explainer,
    onRetry: () -> Unit,
) {
    val strings = Strings.of(language)
    Column(modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        when (state) {
            LoadState.Loading -> LoadingSkeleton(strings)
            is LoadState.Failed -> {
                CountyTitle(county, strings)
                val message = state.staleSinceMillis
                    ?.let { strings.savedForecastTooOld(Dates.isoDateFromEpochMillis(it)) }
                    ?: strings.failed
                ErrorState(strings, onRetry, message = message)
            }
            is LoadState.Ready -> CountyReadyContent(
                county, state.forecast, state.assessment, language, explainer,
                statusLine = statusLine?.let { slot -> { slot(state) } },
            )
        }
        Disclaimer(strings)
    }
}

@Composable
private fun CountyTitle(county: County, strings: Strings) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { heading() }) {
        Icon(AppIcons.Location, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.sm))
        Text(strings.countyLabel(county.name), style = MaterialTheme.typography.titleLarge)
    }
}

/** Everything shown once a forecast is ready. Internal so screenshot tests can render it without network. */
@Composable
internal fun CountyReadyContent(
    county: County,
    forecast: CountyForecast,
    assessment: RiskAssessment,
    language: Language,
    explainer: AlertExplainer,
    statusLine: (@Composable () -> Unit)? = null,
) {
    val strings = Strings.of(language)
    var explanation by remember(county.code, language, assessment) { mutableStateOf<Explanation?>(null) }
    LaunchedEffect(county.code, language, assessment) {
        explanation = explainer.explain(county, assessment, language)
    }

    // The hero always shows RiskEngine's level, whatever the AI writes.
    RiskHeroCard(county, assessment, language)
    statusLine?.invoke()
    ExplanationCard(explanation, strings)
    SectionCard {
        SectionTitle(AppIcons.Drop, strings.rainNext7)
        RainChart(forecast.days, language, today = forecast.fetchedAtDate)
    }
    assessment.river?.let { RiverCard(it, language) }
    explanation?.let { ActionChecklist(it.actions, strings) }
}

/**
 * The first thing a resident sees: level (name + shape mark + meter), county and the
 * main reasons behind the level, on a tint of the level colour with a water motif.
 */
@Composable
fun RiskHeroCard(county: County, assessment: RiskAssessment, language: Language, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    val level = assessment.level
    val c = level.colors()
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = c.container, contentColor = c.onContainer) {
        Column(
            Modifier
                .drawBehind { drawWaves(c.fill.copy(alpha = 0.16f), bandHeight = 26.dp.toPx()) }
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xl + 28.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Location, null, Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "${strings.countyLabel(county.name)} · ${county.town}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = strings.riskDescription(level) },
            ) {
                RiskMark(level, Modifier.size(56.dp))
                Spacer(Modifier.width(Spacing.lg))
                Column(Modifier.weight(1f)) {
                    Text(strings.floodRisk.uppercase(), style = MaterialTheme.typography.labelMedium)
                    Text(strings.risk(level), style = MaterialTheme.typography.displaySmall)
                }
                LevelMeter(level, emptyColor = c.onContainer.copy(alpha = 0.18f))
            }
            HorizontalDivider(color = c.onContainer.copy(alpha = 0.15f))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                val reasons = reasonLines(assessment, language, strings)
                if (reasons.isEmpty()) ReasonRow(AppIcons.Check, strings.reasonNone)
                reasons.forEach { (icon, text) -> ReasonRow(icon, text) }
            }
        }
    }
}

private fun reasonLines(a: RiskAssessment, language: Language, s: Strings): List<Pair<ImageVector, String>> =
    a.reasons.map { r ->
        when (r) {
            is RiskReason.HeavyDay -> AppIcons.Drop to s.reasonHeavyDay(r.mm.roundToInt(), Dates.dayName(r.date, language))
            is RiskReason.WetSpell -> AppIcons.Drop to s.reasonWetSpell(r.totalMm.roundToInt(), Dates.dayName(r.startDate, language))
            is RiskReason.RiverRising -> AppIcons.Waves to s.reasonRiver(r.riverName ?: s.nearbyRiver, r.ratio.oneDecimal())
        }
    }

@Composable
private fun ReasonRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Plain-language explanation with a label saying whether the AI or the offline template wrote it. */
@Composable
fun ExplanationCard(explanation: Explanation?, strings: Strings, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        if (explanation == null) {
            val brush = shimmerBrush()
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.preparing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SkeletonBlock(Modifier.fillMaxWidth(0.7f).height(20.dp), brush)
            SkeletonBlock(Modifier.fillMaxWidth().height(14.dp), brush)
            SkeletonBlock(Modifier.fillMaxWidth(0.85f).height(14.dp), brush)
        } else {
            val ai = explanation.source == ExplanationSource.AI
            InfoChip(if (ai) AppIcons.Sparkle else AppIcons.Document, if (ai) strings.aiLabel else strings.offlineLabel)
            Text(explanation.headline, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(explanation.body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * River discharge outlook: ratio of forecast peak to the recent baseline, on a gauge with
 * the high (2×) and severe (3×) marks, plus the peak and baseline in m³/s.
 */
@Composable
fun RiverCard(river: RiverOutlook, language: Language, modifier: Modifier = Modifier, thresholds: RiskThresholds = RiskThresholds()) {
    val strings = Strings.of(language)
    val ratio = river.riseRatio
    val level = when {
        ratio >= thresholds.riverSevereRatio -> RiskLevel.SEVERE
        ratio >= thresholds.riverHighRatio -> RiskLevel.HIGH
        else -> null
    }
    val palette = LocalRiskPalette.current
    val scheme = MaterialTheme.colorScheme
    val fill = level?.let { palette[it].fill } ?: scheme.primary
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = scheme.onSurfaceVariant)

    SectionCard(modifier) {
        SectionTitle(AppIcons.Waves, strings.riverTitle + (river.riverName?.let { " · $it" } ?: ""))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${ratio.oneDecimal()}×", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.riverRatioLabel, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            level?.let { Spacer(Modifier.weight(1f)); RiskBadge(it, strings, Modifier.padding(bottom = 4.dp)) }
        }
        val scaleMax = maxOf(4.0, ratio * 1.1)
        Canvas(
            Modifier.fillMaxWidth().height(34.dp).clearAndSetSemantics { contentDescription = strings.riverGauge(ratio.oneDecimal()) },
        ) {
            val barH = 12.dp.toPx()
            val r = CornerRadius(barH / 2)
            drawRoundRect(scheme.surfaceContainerHighest, Offset.Zero, Size(size.width, barH), r)
            drawRoundRect(fill, Offset.Zero, Size((size.width * (ratio / scaleMax)).toFloat().coerceAtLeast(barH), barH), r)
            listOf(thresholds.riverHighRatio, thresholds.riverSevereRatio).forEach { mark ->
                val x = (size.width * (mark / scaleMax)).toFloat()
                drawLine(scheme.onSurface, Offset(x, -2.dp.toPx()), Offset(x, barH + 2.dp.toPx()), 2.dp.toPx())
                val label = measurer.measure("${mark.roundToInt()}×", labelStyle)
                drawText(label, topLeft = Offset(x - label.size.width / 2f, barH + 4.dp.toPx()))
            }
        }
        Text(
            strings.riverPeakLine(river.peakDischarge.roundToInt(), Dates.dayName(river.peakDate, language)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(strings.riverBaselineLine(river.baselineDischarge.roundToInt()), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

/** "What to do" list. Each step can be ticked off; ticks are kept only while the screen is open. */
@Composable
fun ActionChecklist(actions: List<String>, strings: Strings, modifier: Modifier = Modifier) {
    val done = remember(actions) { mutableStateListOf(*Array(actions.size) { false }) }
    SectionCard(modifier) {
        SectionTitle(AppIcons.Check, strings.whatToDo)
        Text(strings.checklistHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            actions.forEachIndexed { i, action ->
                ChecklistRow(i + 1, action, done[i], strings) { done[i] = it }
            }
        }
    }
}

@Composable
private fun ChecklistRow(number: Int, text: String, checked: Boolean, strings: Strings, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val circle by animateColorAsState(if (checked) scheme.primary else scheme.surfaceContainerLow)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touch)
            .clip(MaterialTheme.shapes.small)
            .toggleable(checked, role = Role.Checkbox, onValueChange = onChange)
            .semantics { stateDescription = strings.actionState(checked) }
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(circle).border(2.dp, scheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(AppIcons.Check, null, Modifier.size(18.dp), tint = scheme.onPrimary)
            else Text("$number", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
