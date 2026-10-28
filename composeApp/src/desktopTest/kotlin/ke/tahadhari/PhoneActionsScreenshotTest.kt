package ke.tahadhari

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.*
import ke.tahadhari.ui.*
import ke.tahadhari.ui.mobile.CrashReportCard
import ke.tahadhari.ui.mobile.ShareAlertButton
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Phone-size (412x860) screenshots of the detail screen with the Share alert button, and of the
 * crash card. The risk-alert switch is hidden on desktop (no notifications), so a preview row is
 * drawn with the same strings. Skipped unless TAHADHARI_SCREENSHOTS=1.
 */
class PhoneActionsScreenshotTest {
    private val enabled = System.getenv("TAHADHARI_SCREENSHOTS") == "1"
    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun shoot(name: String, content: @Composable () -> Unit) {
        if (!enabled) return
        ImageComposeScene(412 * 2, 860 * 2, Density(2f)) { content() }.use { scene ->
            var t = 0L
            repeat(30) { scene.render(t); t += 50_000_000L }
            File(outDir, "$name.png").writeBytes(scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    private val county = Counties.byCode(4)!!
    private val ready: LoadState.Ready = run {
        val mm = listOf(4.0, 12.0, 28.0, 63.0, 99.0, 9.0, 0.5)
        val days = mm.indices.map { DailyRain("2026-11-0${2 + it}", mm[it], listOf(35, 55, 80, 95, 85, 40, 15)[it]) }
        val forecast = CountyForecast(county, days, RiverOutlook("Tana River", 340.0, 812.0, "2026-11-06"), "2026-11-02")
        LoadState.Ready(forecast, RiskEngine().assess(forecast), 1_793_000_000_000L, fromCache = false)
    }

    @Test
    fun detailWithShareAndAlerts() = Language.entries.forEach { lang ->
        val strings = Strings.of(lang)
        shoot("phone-share-alert-${lang.name.lowercase().take(2)}-light") {
            TahadhariTheme(false) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        RiskHeroCard(county, ready.assessment, lang)
                        ShareAlertButton(county, ready, lang)
                        androidx.compose.material3.OutlinedButton(onClick = {}, Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text(strings.refresh)
                        }
                        androidx.compose.material3.OutlinedButton(onClick = {}, Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Text(strings.changeCounty)
                        }
                        PreviewAlertSwitch(strings, on = true)
                    }
                }
            }
        }
    }

    @Test
    fun crashCard() = Language.entries.forEach { lang ->
        shoot("phone-crash-card-${lang.name.lowercase().take(2)}-light") {
            TahadhariTheme(false) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
                        CrashReportCard("Tahadhari crash report\nTime: 2026-10-28 14:30 (Nairobi)", lang, onDelete = {})
                    }
                }
            }
        }
    }
}

/** Same layout as the real switch row (which only exists where notifications do). */
@Composable
private fun PreviewAlertSwitch(strings: Strings, on: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.Text(strings.riskAlertsToggle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.md))
            androidx.compose.material3.Switch(checked = on, onCheckedChange = null)
        }
        androidx.compose.material3.Text(strings.riskAlertsHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
