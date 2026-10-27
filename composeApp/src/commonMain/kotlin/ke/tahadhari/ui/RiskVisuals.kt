package ke.tahadhari.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ke.tahadhari.domain.RiskLevel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shape-coded risk symbol so the level never relies on colour alone:
 * Low = circle with a tick, Moderate = diamond, High = triangle, Severe = octagon (all with "!").
 * Decorative: put the level name in text next to it. [inverted] draws the shape in the
 * on-colour, for use on top of a filled risk background.
 */
@Composable
fun RiskMark(level: RiskLevel, modifier: Modifier = Modifier.size(24.dp), inverted: Boolean = false) {
    val c = level.colors()
    val shapeColor = if (inverted) c.onFill else c.fill
    val glyphColor = if (inverted) c.fill else c.onFill
    Canvas(modifier) { drawRiskMark(level, shapeColor, glyphColor) }
}

private fun DrawScope.drawRiskMark(level: RiskLevel, shape: Color, glyph: Color) {
    val s = size.minDimension
    val left = (size.width - s) / 2
    val top = (size.height - s) / 2
    fun p(x: Float, y: Float) = Offset(left + x * s, top + y * s)

    val outline = Path().apply {
        when (level) {
            RiskLevel.LOW -> addOval(androidx.compose.ui.geometry.Rect(p(0.04f, 0.04f), Size(s * 0.92f, s * 0.92f)))
            RiskLevel.MODERATE -> {
                moveTo(p(0.5f, 0.02f)); lineTo(p(0.98f, 0.5f)); lineTo(p(0.5f, 0.98f)); lineTo(p(0.02f, 0.5f)); close()
            }
            RiskLevel.HIGH -> {
                moveTo(p(0.5f, 0.05f)); lineTo(p(0.98f, 0.9f)); lineTo(p(0.02f, 0.9f)); close()
            }
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
    // Soften the triangle's sharp corners a little.
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
 * Four ascending bars, filled up to [level]: a second, colour-independent cue for severity
 * (like a signal-strength meter). Decorative.
 */
@Composable
fun LevelMeter(level: RiskLevel, modifier: Modifier = Modifier.size(width = 34.dp, height = 28.dp), emptyColor: Color = MaterialTheme.colorScheme.outlineVariant) {
    val fill = level.color()
    Canvas(modifier) {
        val n = RiskLevel.entries.size
        val gap = size.width * 0.1f
        val w = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val h = size.height * (0.4f + 0.6f * i / (n - 1))
            drawRoundRect(
                color = if (i <= level.ordinal) fill else emptyColor,
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.3f),
            )
        }
    }
}

/** Compact pill showing the risk level with its shape mark. Always pass RiskEngine's level. */
@Composable
fun RiskBadge(level: RiskLevel, strings: Strings, modifier: Modifier = Modifier) {
    val c = level.colors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.fill)
            .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
            .clearAndSetSemantics { contentDescription = strings.riskDescription(level) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiskMark(level, Modifier.size(14.dp), inverted = true)
        Spacer(Modifier.width(5.dp))
        Text(strings.risk(level), color = c.onFill, style = MaterialTheme.typography.labelMedium)
    }
}

/** Two soft sine waves in a band of [bandHeight] px along the bottom of a card: the visual motif for water. */
fun DrawScope.drawWaves(color: Color, bandHeight: Float, phase: Float = 0f) {
    val base = size.height - bandHeight
    val amp = bandHeight * 0.18f
    listOf(0f to 1f, 0.45f to 0.6f).forEachIndexed { i, (offset, alpha) ->
        val path = Path().apply {
            val y0 = base + i * amp * 2.2f
            moveTo(0f, y0)
            val steps = 48
            for (k in 0..steps) {
                val x = size.width * k / steps
                val y = y0 + amp * sin((x / size.width) * 2f * PI.toFloat() * 1.5f + phase + offset * 6f)
                lineTo(x, y)
            }
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(path, color.copy(alpha = color.alpha * alpha))
    }
}
