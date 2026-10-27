package ke.tahadhari.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.data.County
import ke.tahadhari.domain.*
import kotlin.math.roundToInt

@Composable
fun RiskBadge(level: RiskLevel, strings: Strings, modifier: Modifier = Modifier) {
    Text(
        text = strings.risk(level),
        color = level.onColor(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(level.color())
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun LanguageToggle(language: Language, onChange: (Language) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Language.entries.forEach { lang ->
            val label = if (lang == Language.ENGLISH) "EN" else "SW"
            if (lang == language) Button(onClick = {}) { Text(label) }
            else OutlinedButton(onClick = { onChange(lang) }) { Text(label) }
        }
    }
}

@Composable
fun AppHeader(title: String, subtitle: String, language: Language, onLanguage: (Language) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LanguageToggle(language, onLanguage)
    }
}

@Composable
fun RainChart(days: List<DailyRain>, thresholds: RiskThresholds = RiskThresholds()) {
    val maxMm = maxOf(days.maxOfOrNull { it.precipitationMm } ?: 0.0, thresholds.dailyHighMm)
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val level = when {
                day.precipitationMm >= thresholds.dailySevereMm -> RiskLevel.SEVERE
                day.precipitationMm >= thresholds.dailyHighMm -> RiskLevel.HIGH
                day.precipitationMm >= thresholds.dailyModerateMm -> RiskLevel.MODERATE
                else -> RiskLevel.LOW
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("${day.precipitationMm.roundToInt()}", style = MaterialTheme.typography.labelSmall)
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                        .height((100 * (day.precipitationMm / maxMm)).coerceAtLeast(2.0).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(level.color())
                )
                Text(Dates.dayOfMonth(day.date), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun Disclaimer(strings: Strings) {
    Text(
        strings.disclaimer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** Full details for one county: risk, chart, river outlook, explanation and actions. */
@Composable
fun CountyDetail(county: County, state: LoadState, language: Language, onRetry: () -> Unit) {
    val strings = Strings.of(language)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${county.name} · ${county.town}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        when (state) {
            LoadState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp)); Text(strings.loading)
            }
            is LoadState.Failed -> Column {
                Text(strings.failed, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(strings.retry) }
            }
            is LoadState.Ready -> ReadyDetail(county, state, language, strings)
        }
        Disclaimer(strings)
    }
}

@Composable
private fun ReadyDetail(county: County, state: LoadState.Ready, language: Language, strings: Strings) {
    val a = state.assessment
    var explanation by remember(county.code, language, a) { mutableStateOf<Explanation?>(null) }
    LaunchedEffect(county.code, language, a) {
        explanation = AppGraph.explainer.explain(county, a, language)
    }

    // The badge always reflects RiskEngine's level, whatever the AI writes.
    RiskBadge(a.level, strings)

    val e = explanation
    if (e == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(strings.preparing, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        Card(colors = CardDefaults.cardColors(containerColor = a.level.color().copy(alpha = 0.12f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (e.source == ExplanationSource.AI) strings.aiLabel else strings.offlineLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(e.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(e.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Text(strings.rainNext7, style = MaterialTheme.typography.titleSmall)
    RainChart(state.forecast.days)

    a.river?.let { r ->
        val ratio = ((r.riseRatio * 10).roundToInt() / 10.0).toString()
        Text(
            strings.riverPeak(r.riverName ?: "River", r.peakDischarge.roundToInt(), Dates.dayName(r.peakDate, language), ratio),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    explanation?.let { e ->
        Text(strings.whatToDo, style = MaterialTheme.typography.titleSmall)
        e.actions.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}
