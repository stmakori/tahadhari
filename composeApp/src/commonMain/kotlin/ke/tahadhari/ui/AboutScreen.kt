package ke.tahadhari.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.tahadhari.APP_VERSION
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.RiskThresholds
import kotlin.math.roundToInt

/**
 * "About and how it works": what the app does, how risk is calculated (numbers read from
 * [RiskThresholds], never hardcoded), data sources, AI, privacy, disclaimer and version.
 * Does not scroll itself and has no mobile-only dependencies: the caller scrolls it
 * (phone page, or a dashboard dialog).
 */
@Composable
fun AboutContent(language: Language, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        SectionCard {
            SectionTitle(AppIcons.Drop, strings.aboutPage.aboutWhatTitle)
            BodyText(strings.aboutPage.aboutWhatBody)
        }
        RiskTableCard(strings)
        SectionCard {
            SectionTitle(AppIcons.Map, strings.aboutPage.aboutDataTitle)
            Bullet(strings.aboutPage.aboutDataWeather)
            Bullet(strings.aboutPage.aboutDataRiver)
            Bullet(strings.aboutPage.aboutDataEnsemble)
            Bullet(strings.aboutPage.aboutDataBoundaries)
            Bullet(strings.aboutPage.aboutDataGuides)
        }
        SectionCard {
            SectionTitle(AppIcons.Sparkle, strings.aboutPage.aboutAiTitle)
            BodyText(strings.aboutPage.aboutAiBody)
            BodyText(strings.aboutPage.aboutAiOffline)
        }
        SectionCard {
            SectionTitle(AppIcons.Shield, strings.aboutPage.aboutPrivacyTitle)
            Bullet(strings.aboutPage.aboutPrivacyNoAccounts)
            Bullet(strings.aboutPage.aboutPrivacyStored)
            Bullet(strings.aboutPage.aboutPrivacyLocation)
            Bullet(strings.aboutPage.aboutPrivacyPoint)
            Bullet(strings.aboutPage.aboutPrivacySent)
            Bullet(strings.aboutPage.aboutPrivacyCheckIn)
            Bullet(strings.aboutPage.aboutPrivacyCrash)
            Bullet(strings.aboutPage.aboutPrivacyNotify)
        }
        Disclaimer(strings)
        Column(
            Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            val style = MaterialTheme.typography.bodySmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(strings.appName + " · " + strings.aboutPage.aboutVersion(APP_VERSION), style = MaterialTheme.typography.labelLarge, color = color, textAlign = TextAlign.Center)
            Text(strings.aboutPage.aboutLicence, style = style, color = color, textAlign = TextAlign.Center)
            Text(strings.aboutPage.aboutContest, style = style, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/** Number as shown in the table: "20", or "2.5" for a fraction. */
private fun Double.clean(): String = if (this == roundToInt().toDouble()) roundToInt().toString() else toString()

@Composable
private fun RiskTableCard(strings: Strings, thresholds: RiskThresholds = RiskThresholds()) {
    val scheme = MaterialTheme.colorScheme
    val levels = listOf(RiskLevel.MODERATE, RiskLevel.HIGH, RiskLevel.SEVERE)
    val none = "–" // the river signal starts at High
    SectionCard {
        SectionTitle(AppIcons.Waves, strings.aboutPage.aboutRiskTitle)
        BodyText(strings.aboutPage.aboutRiskBody)
        Column {
            Row(Modifier.fillMaxWidth().padding(bottom = Spacing.sm), verticalAlignment = Alignment.Bottom) {
                Text(strings.aboutPage.aboutSignalColumn, Modifier.weight(SIGNAL_WEIGHT), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                levels.forEach { level ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        RiskMark(level, Modifier.size(16.dp))
                        CappedFontScale {
                            Text(strings.risk(level), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) // no line cap: narrow fonts wrap "Kubwa sana" to 3 lines
                        }
                    }
                }
            }
            TableRow(
                strings.aboutPage.aboutSignalDay,
                listOf(thresholds.dailyModerateMm, thresholds.dailyHighMm, thresholds.dailySevereMm).map { "≥ ${it.clean()}" },
            )
            TableRow(
                strings.aboutPage.aboutSignalThreeDay,
                listOf(thresholds.threeDayModerateMm, thresholds.threeDayHighMm, thresholds.threeDaySevereMm).map { "≥ ${it.clean()}" },
            )
            TableRow(
                strings.aboutPage.aboutSignalRiver,
                listOf(none, "≥ ${thresholds.riverHighRatio.clean()}×", "≥ ${thresholds.riverSevereRatio.clean()}×"),
            )
        }
        BodyText(strings.aboutPage.aboutRiskRule)
        Text(strings.aboutPage.aboutRiskHeuristic, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}

private const val SIGNAL_WEIGHT = 1.7f

@Composable
private fun TableRow(signal: String, cells: List<String>) {
    Column {
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Text(signal, Modifier.weight(SIGNAL_WEIGHT).padding(end = Spacing.sm), style = MaterialTheme.typography.bodyMedium)
            cells.forEach { cell ->
                Text(cell, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            }
        }
    }
}
