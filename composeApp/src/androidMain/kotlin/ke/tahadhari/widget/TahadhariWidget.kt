package ke.tahadhari.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ke.tahadhari.AppGraph
import ke.tahadhari.MainActivity
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.widget.WidgetContent
import ke.tahadhari.ui.widget.WidgetUiState
import ke.tahadhari.ui.widget.loadSavedForWidget
import ke.tahadhari.ui.widget.toContent
import ke.tahadhari.ui.widget.widgetUiState

private val SmallSize = DpSize(110.dp, 110.dp)
private val MediumSize = DpSize(250.dp, 110.dp)

// Duplicated from ui/Theme.kt (LightRisk fill/onFill and the light scheme), because Theme's
// palette is private and @Composable. Keep in sync if the app palette changes.
private val Surface = Color(0xFFF5FAFA)
private val OnSurface = Color(0xFF171D1E)
private val OnSurfaceVariant = Color(0xFF3F4949)
private val NeutralFill = Color(0xFFDAE4E5) // surfaceVariant: "no level" pill
private fun fillOf(level: RiskLevel?): Color = when (level) {
    RiskLevel.LOW -> Color(0xFF2E7D32)
    RiskLevel.MODERATE -> Color(0xFFF5B700)
    RiskLevel.HIGH -> Color(0xFFC2410C)
    RiskLevel.SEVERE -> Color(0xFFB3261E)
    null -> NeutralFill
}
private fun onFillOf(level: RiskLevel?): Color = when (level) {
    RiskLevel.LOW, RiskLevel.HIGH, RiskLevel.SEVERE -> Color.White
    RiskLevel.MODERATE -> Color(0xFF231A00)
    null -> OnSurface
}

/** Shape glyph per level so colour is not the only signal (the label is always shown too). */
private fun glyphOf(level: RiskLevel?): String = when (level) {
    RiskLevel.LOW -> "● "
    RiskLevel.MODERATE -> "◆ "
    RiskLevel.HIGH -> "▲ "
    RiskLevel.SEVERE -> "■ "
    null -> ""
}

class TahadhariWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SmallSize, MediumSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetEnv.init(context)
        // Draw from preferences + saved forecast only; the worker does the downloading.
        val prefs = AppGraph.preferences.load()
        val now = AppGraph.nowMillis()
        val county = prefs.countyCode?.let { Counties.byCode(it) }
        val loadState = county?.let { loadSavedForWidget(it, AppGraph.forecastCache, AppGraph.riskEngine, now) }
        val ui = if (county != null && loadState == null && WidgetStatus.lastFailed(context)) {
            WidgetUiState.NoData(county.name, savedSinceMillis = null)
        } else {
            widgetUiState(prefs, loadState)
        }
        val content = ui.toContent(Strings.of(prefs.language), now)
        provideContent { WidgetBody(content) }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(c: WidgetContent) {
    val medium = LocalSize.current.width >= MediumSize.width
    val open = if (c.tapRetries) actionRunCallback<RetryCallback>() else actionStartActivity(android.content.Intent(LocalContext.current, MainActivity::class.java))
    Column(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Surface)).cornerRadius(16.dp)
            .padding(12.dp).clickable(open),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            c.title,
            style = TextStyle(color = ColorProvider(OnSurface), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            maxLines = if (c.level == null && c.levelLabel == null) 3 else 1,
        )
        if (c.levelLabel != null) {
            Spacer(GlanceModifier.height(4.dp))
            Box(
                modifier = GlanceModifier.background(ColorProvider(fillOf(c.level))).cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 3.dp).wrapContentSize(),
            ) {
                Text(
                    glyphOf(c.level) + c.levelLabel,
                    style = TextStyle(color = ColorProvider(onFillOf(c.level)), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        if (medium && c.reason != null) small(c.reason, 2)
        // The saved-forecast marker replaces nothing: it is shown with the updated line.
        if (c.updated != null) small(c.updated, 1)
        if (c.savedMarker != null) small("• " + c.savedMarker, 1, bold = true)
        Spacer(GlanceModifier.height(2.dp))
        Text(
            c.footer,
            style = TextStyle(color = ColorProvider(OnSurfaceVariant), fontSize = 10.sp),
            maxLines = 1,
        )
    }
}

@androidx.compose.runtime.Composable
private fun small(text: String, lines: Int, bold: Boolean = false) {
    Text(
        text,
        modifier = GlanceModifier.padding(top = 2.dp),
        style = TextStyle(
            color = ColorProvider(OnSurfaceVariant), fontSize = 11.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        ),
        maxLines = lines,
    )
}

/** "No data" tap: try again now, and redraw so it shows loading. */
class RetryCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetEnv.init(context)
        WidgetStatus.setLastFailed(context, false)
        WidgetRefresh.refreshNow(context)
        TahadhariWidget().update(context, glanceId)
    }
}

class TahadhariWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TahadhariWidget()

    override fun onReceive(context: Context, intent: android.content.Intent) {
        WidgetEnv.init(context)
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefresh.schedulePeriodic(context)
        WidgetRefresh.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefresh.cancelAll(context)
    }
}
