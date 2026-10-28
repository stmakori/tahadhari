package ke.tahadhari.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskThresholds
import kotlin.math.roundToInt

/** Level of a single day's rain against the daily thresholds (used to colour chart bars). */
internal fun RiskThresholds.dailyLevel(mm: Double): RiskLevel = when {
    mm >= dailySevereMm -> RiskLevel.SEVERE
    mm >= dailyHighMm -> RiskLevel.HIGH
    mm >= dailyModerateMm -> RiskLevel.MODERATE
    else -> RiskLevel.LOW
}

/**
 * 7-day rainfall bar chart. Bars are coloured by each day's level, dashed lines mark the
 * moderate and high daily thresholds, and each column shows the mm total, weekday and
 * chance of rain. The whole chart is exposed to screen readers as one description.
 * [today] (ISO date) labels that column "Today".
 */
@Composable
fun RainChart(
    days: List<DailyRain>,
    language: Language,
    modifier: Modifier = Modifier,
    thresholds: RiskThresholds = RiskThresholds(),
    today: String? = null,
) {
    val strings = Strings.of(language)
    val palette = LocalRiskPalette.current
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val measurer = rememberTextMeasurer()
    val grow = remember(days) { Animatable(0f) }
    LaunchedEffect(days) { grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    val description = strings.rainNext7 + ". " + days.joinToString(". ") {
        strings.chartDay(Dates.dayName(it.date, language), it.precipitationMm.roundToInt(), it.probabilityPercent)
    }
    val gutter = 40.dp // right-hand space for threshold labels

    // The axis labels share 7 narrow columns, so they cannot grow with the system font; the legend below does.
    val fullDensity = LocalDensity.current
    CappedFontScale { Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(168.dp)) {
            if (days.isEmpty()) return@Canvas
            val topPad = 22.dp.toPx()
            val gutterPx = gutter.toPx()
            val plotW = size.width - gutterPx
            val plotH = size.height - topPad
            val maxMm = maxOf(days.maxOf { it.precipitationMm }, thresholds.dailyHighMm * 1.2)
            fun y(mm: Double) = topPad + plotH * (1f - (mm / maxMm).toFloat())
            val slot = plotW / days.size

            // Today's column gets a soft highlight.
            days.indexOfFirst { it.date == today }.takeIf { it >= 0 }?.let { i ->
                drawRoundRect(
                    scheme.surfaceContainerHighest.copy(alpha = 0.7f),
                    topLeft = Offset(i * slot + slot * 0.08f, 0f),
                    size = Size(slot * 0.84f, size.height),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                )
            }

            // Baseline.
            drawLine(scheme.outlineVariant, Offset(0f, size.height), Offset(plotW, size.height), 1.dp.toPx())

            // Threshold lines with labels in the gutter.
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
            listOf(thresholds.dailyModerateMm to RiskLevel.MODERATE, thresholds.dailyHighMm to RiskLevel.HIGH).forEach { (mm, level) ->
                val ly = y(mm)
                drawLine(palette[level].fill, Offset(0f, ly), Offset(plotW + 4.dp.toPx(), ly), 1.5.dp.toPx(), pathEffect = dash)
                val label = measurer.measure("${mm.roundToInt()}", typography.labelSmall.copy(color = scheme.onSurfaceVariant))
                drawText(label, topLeft = Offset(plotW + 8.dp.toPx(), ly - label.size.height / 2f))
            }

            // Bars, rounded on top, growing in on first show.
            val barW = minOf(slot * 0.56f, 36.dp.toPx())
            val radius = CornerRadius(minOf(barW / 2.5f, 8.dp.toPx()))
            days.forEachIndexed { i, day ->
                val x = i * slot + (slot - barW) / 2
                val fullTop = minOf(y(day.precipitationMm), size.height - 3.dp.toPx())
                val top = size.height - (size.height - fullTop) * grow.value
                val bar = Path().apply {
                    addRoundRect(RoundRect(Rect(x, top, x + barW, size.height), radius, radius, CornerRadius.Zero, CornerRadius.Zero))
                }
                drawPath(bar, palette[thresholds.dailyLevel(day.precipitationMm)].fill)

                val value = measurer.measure(
                    "${day.precipitationMm.roundToInt()}",
                    typography.labelMedium.copy(color = scheme.onSurface, fontWeight = FontWeight.Bold),
                )
                drawText(value, topLeft = Offset(x + barW / 2 - value.size.width / 2f, top - value.size.height - 2.dp.toPx()))
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().padding(end = gutter)) {
            days.forEach { day ->
                val isToday = day.date == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isToday) strings.today else strings.dayShort(Dates.dayName(day.date, language)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isToday) scheme.primary else scheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        // "Today" is wider than a column on small phones; let it spill into the
                        // neighbouring columns, whose short weekday labels leave room.
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                    )
                    Text(Dates.dayOfMonth(day.date), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    day.probabilityPercent?.let { p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(AppIcons.Drop, null, Modifier.size(9.dp), tint = scheme.primary.copy(alpha = 0.35f + 0.65f * p / 100f))
                            Text("$p%", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CompositionLocalProvider(LocalDensity provides fullDensity) {
            Text(
                strings.chartLegend(thresholds.dailyModerateMm.roundToInt(), thresholds.dailyHighMm.roundToInt()) + " · " + strings.chanceLegend,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    } }
}
