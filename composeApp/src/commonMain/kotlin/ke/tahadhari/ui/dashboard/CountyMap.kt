package ke.tahadhari.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.geo.CountyShape
import ke.tahadhari.data.geo.CountyShapes
import ke.tahadhari.data.geo.MapProjection
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.LocalRiskPalette
import ke.tahadhari.ui.RiskBadge
import ke.tahadhari.ui.RiskMark
import ke.tahadhari.ui.Spacing
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.levelCounts
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Colours for the two "no level" states. Neither may ever resemble Low (green). */
private data class NoDataColors(val loadingFill: Color, val loadingHatch: Color, val failedFill: Color, val failedHatch: Color)

@Composable
private fun noDataColors(): NoDataColors {
    val s = MaterialTheme.colorScheme
    return NoDataColors(
        loadingFill = s.surfaceContainerHighest,
        loadingHatch = s.outlineVariant,
        failedFill = s.outline.copy(alpha = 0.55f),
        failedHatch = s.onSurface.copy(alpha = 0.45f),
    )
}

/**
 * Kenya county choropleth. Each county is filled with its RiskEngine level colour (and, where it
 * fits, the level's shape mark); loading counties are hatched, failed ones are grey with a
 * cross-hatch. Hover shows a tooltip, click selects. Scales to its size.
 */
@Composable
fun CountyMap(
    states: Map<Int, LoadState>,
    selectedCode: Int?,
    strings: Strings,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var hover by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    val select by rememberUpdatedState(onSelect)
    val padPx = with(LocalDensity.current) { 12.dp.toPx() }
    val projection = remember(size, padPx) {
        MapProjection(CountyShapes.bounds, size.width.toFloat(), size.height.toFloat(), padPx)
    }
    val paths = remember(projection) { CountyShapes.all.mapValues { (_, shape) -> shape.toPath(projection) } }
    val palette = LocalRiskPalette.current
    val noData = noDataColors()
    val border = MaterialTheme.colorScheme.surfaceContainerLowest
    val emphasis = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current.density

    Box(modifier) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .semantics { contentDescription = mapSummary(strings, states) }
                .pointerHoverIcon(if (hover != null) PointerIcon.Hand else PointerIcon.Default)
                .pointerInput(projection) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            if (event.type == PointerEventType.Exit) {
                                hover = null
                                continue
                            }
                            val code = CountyShapes.countyAt(projection.lon(pos.x), projection.lat(pos.y))
                            hover = code?.let { it to pos }
                            if (event.type == PointerEventType.Press && code != null) select(code)
                        }
                    }
                },
        ) {
            if (size == IntSize.Zero) return@Canvas
            val hatchGap = 6f * density
            paths.forEach { (code, path) ->
                when (val s = states[code]) {
                    is LoadState.Ready -> drawPath(path, palette[s.assessment.level].fill)
                    is LoadState.Failed -> {
                        drawPath(path, noData.failedFill)
                        hatch(path, noData.failedHatch, hatchGap, 1.2f * density, cross = true)
                    }
                    else -> {
                        drawPath(path, noData.loadingFill)
                        hatch(path, noData.loadingHatch, hatchGap, 1f * density, cross = false)
                    }
                }
            }
            paths.values.forEach { drawPath(it, border, style = Stroke(1f * density, join = StrokeJoin.Round)) }

            // Shape marks so the map doesn't rely on colour alone (only where the county is big enough).
            val markPx = 13f * density
            CountyShapes.all.forEach { (code, shape) ->
                val ready = states[code] as? LoadState.Ready ?: return@forEach
                val w = projection.x(shape.bounds.maxLon) - projection.x(shape.bounds.minLon)
                val h = projection.y(shape.bounds.minLat) - projection.y(shape.bounds.maxLat)
                if (w < markPx * 2.2f || h < markPx * 2.2f) return@forEach
                val (lon, lat) = markAnchor(code, shape)
                val c = palette[ready.assessment.level]
                drawLevelMark(ready.assessment.level, c.onFill, c.fill, Offset(projection.x(lon), projection.y(lat)), markPx)
            }

            hover?.first?.takeIf { it != selectedCode }?.let { code ->
                paths[code]?.let {
                    drawPath(it, emphasis.copy(alpha = 0.12f))
                    drawPath(it, emphasis.copy(alpha = 0.7f), style = Stroke(1.5f * density, join = StrokeJoin.Round))
                }
            }
            selectedCode?.let { paths[it] }?.let {
                drawPath(it, border, style = Stroke(5f * density, join = StrokeJoin.Round))
                drawPath(it, emphasis, style = Stroke(2.5f * density, join = StrokeJoin.Round))
            }
        }

        hover?.let { (code, pos) ->
            val county = Counties.byCode(code) ?: return@let
            Box(
                Modifier.matchParentSize().layout { measurable, constraints ->
                    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val gap = (14 * density).roundToInt()
                        var x = pos.x.roundToInt() + gap
                        var y = pos.y.roundToInt() + gap
                        if (x + p.width > constraints.maxWidth) x = pos.x.roundToInt() - gap - p.width
                        if (y + p.height > constraints.maxHeight) y = pos.y.roundToInt() - gap - p.height
                        p.place(x.coerceAtLeast(0), y.coerceAtLeast(0))
                    }
                },
            ) {
                MapTooltip(county.name, states[code], strings)
            }
        }
    }
}

@Composable
private fun MapTooltip(name: String, state: LoadState?, strings: Strings) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(strings.countyLabel(name), style = MaterialTheme.typography.titleSmall)
            when (state) {
                is LoadState.Ready -> RiskBadge(state.assessment.level, strings)
                is LoadState.Failed -> Text(strings.noDataShort, style = MaterialTheme.typography.labelMedium)
                else -> Text(strings.loadingShort, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Legend: one entry per level (shape mark + name), plus the loading and no-data patterns. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapLegend(strings: Strings, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        RiskLevel.entries.forEach { level ->
            LegendItem(strings.risk(level)) { RiskMark(level, Modifier.size(16.dp)) }
        }
        LegendItem(strings.loadingShort) { PatternSwatch(failed = false) }
        LegendItem(strings.noDataLegend) { PatternSwatch(failed = true) }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small square showing the loading (hatched) or no-data (grey cross-hatched) map fill. */
@Composable
fun PatternSwatch(failed: Boolean, modifier: Modifier = Modifier.size(16.dp)) {
    val c = noDataColors()
    Canvas(modifier.clip(RoundedCornerShape(3.dp))) {
        val path = Path().apply { addRect(Rect(Offset.Zero, size)) }
        drawPath(path, if (failed) c.failedFill else c.loadingFill)
        hatch(path, if (failed) c.failedHatch else c.loadingHatch, size.width / 3f, 1.2f * density, cross = failed)
    }
}

/** Spinner-sized loading indicator for chips and rows. */
@Composable
internal fun SmallSpinner() = CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)

private fun CountyShape.toPath(p: MapProjection) = Path().apply {
    parts.forEach { r ->
        moveTo(p.x(r[0]), p.y(r[1]))
        for (i in 2 until r.size step 2) lineTo(p.x(r[i]), p.y(r[i + 1]))
        close()
    }
}

/** Diagonal stripes (and the opposite diagonal when [cross]) clipped to [path]. */
private fun DrawScope.hatch(path: Path, color: Color, gap: Float, width: Float, cross: Boolean) {
    clipPath(path) {
        val b = path.getBounds()
        val span = b.width + b.height
        var d = 0f
        while (d <= span) {
            drawLine(color, Offset(b.left + d, b.top), Offset(b.left + d - b.height, b.bottom), width)
            if (cross) drawLine(color, Offset(b.left + d - b.height, b.top), Offset(b.left + d, b.bottom), width)
            d += gap
        }
    }
}

/** Where to draw a county's mark: the centroid of its largest part if inside, else the HQ town. */
private fun markAnchor(code: Int, shape: CountyShape): Pair<Double, Double> {
    val c = shape.labelPoint
    if (shape.contains(c.first, c.second)) return c
    val hq = Counties.byCode(code)?.location ?: return c
    return hq.longitude to hq.latitude
}

/**
 * Same shapes as [RiskMark] (circle+tick, diamond, triangle, octagon with "!"), drawn at [center]
 * inside a Canvas. Private copy because RiskMark's drawing helper is private to RiskVisuals.kt.
 */
private fun DrawScope.drawLevelMark(level: RiskLevel, shape: Color, glyph: Color, center: Offset, s: Float) {
    val left = center.x - s / 2
    val top = center.y - s / 2
    fun p(x: Float, y: Float) = Offset(left + x * s, top + y * s)
    val outline = Path().apply {
        when (level) {
            RiskLevel.LOW -> addOval(Rect(p(0.04f, 0.04f), Size(s * 0.92f, s * 0.92f)))
            RiskLevel.MODERATE -> {
                moveTo(p(0.5f, 0.02f)); lineTo(p(0.98f, 0.5f)); lineTo(p(0.5f, 0.98f)); lineTo(p(0.02f, 0.5f)); close()
            }
            RiskLevel.HIGH -> { moveTo(p(0.5f, 0.05f)); lineTo(p(0.98f, 0.9f)); lineTo(p(0.02f, 0.9f)); close() }
            RiskLevel.SEVERE -> {
                for (i in 0 until 8) {
                    val a = (PI / 8 + i * PI / 4).toFloat()
                    val pt = p(0.5f + 0.48f * cos(a), 0.5f + 0.48f * sin(a))
                    if (i == 0) moveTo(pt) else lineTo(pt)
                }
                close()
            }
        }
    }
    drawPath(outline, shape)
    if (level == RiskLevel.HIGH) drawPath(outline, shape, style = Stroke(s * 0.06f, join = StrokeJoin.Round))
    val stroke = s * 0.11f
    if (level == RiskLevel.LOW) {
        val tick = Path().apply { moveTo(p(0.29f, 0.52f)); lineTo(p(0.44f, 0.66f)); lineTo(p(0.71f, 0.37f)) }
        drawPath(tick, glyph, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        val (y0, y1, dot) = if (level == RiskLevel.HIGH) Triple(0.4f, 0.6f, 0.75f) else Triple(0.28f, 0.56f, 0.72f)
        drawLine(glyph, p(0.5f, y0), p(0.5f, y1), stroke, StrokeCap.Round)
        drawCircle(glyph, stroke * 0.62f, p(0.5f, dot))
    }
}

private fun Path.moveTo(o: Offset) = moveTo(o.x, o.y)
private fun Path.lineTo(o: Offset) = lineTo(o.x, o.y)

/**
 * Screen-reader summary of the map: the description plus how many counties are at each level,
 * so the information in the colours is available as text. Counties without data are counted
 * as "no data", never as Low.
 */
internal fun mapSummary(strings: Strings, states: Map<Int, LoadState>): String {
    val total = CountyShapes.all.size
    val levels = states.filterKeys { it in CountyShapes.all }.levelCounts()
    val failed = states.filterKeys { it in CountyShapes.all }.values.count { it is LoadState.Failed }
    val parts = RiskLevel.entries.reversed().mapNotNull { l -> levels[l]?.takeIf { it > 0 }?.let { "${strings.risk(l)} $it" } }
    val loading = total - levels.values.sum() - failed
    return (listOf(strings.mapDescription) +
        listOfNotNull(parts.takeIf { it.isNotEmpty() }?.joinToString(", "), failed.takeIf { it > 0 }?.let { "${strings.noDataShort} $it" },
            loading.takeIf { it > 0 }?.let { "${strings.loadingShort} $it" })).joinToString(". ")
}
