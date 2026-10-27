package ke.tahadhari.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ke.tahadhari.domain.Language

/** Pill toggle between English and Kiswahili. Each option is a 48dp touch target. */
@Composable
fun LanguageToggle(language: Language, onChange: (Language) -> Unit, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .height(Spacing.touch)
            .padding(vertical = 6.dp)
            .clip(CircleShape)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .semantics { contentDescription = strings.languageLabel }
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Language.entries.forEach { lang ->
            val selected = lang == language
            Box(
                Modifier
                    .fillMaxHeight()
                    .widthIn(min = Spacing.touch)
                    .background(if (selected) scheme.primary else scheme.surface)
                    .selectable(selected = selected, role = Role.RadioButton) { onChange(lang) }
                    .semantics { contentDescription = strings.languageName(lang) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    lang.name.take(2), // "EN" / "SW": language codes, the same in both languages
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }
    }
}

/** Small round app mark: a water drop on the primary colour. */
@Composable
fun AppMark(modifier: Modifier = Modifier.size(40.dp)) {
    Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Icon(AppIcons.Drop, null, Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Header row used by the desktop dashboard: mark, title, subtitle and language toggle. */
@Composable
fun AppHeader(title: String, subtitle: String, language: Language, onLanguage: (Language) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark()
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LanguageToggle(language, onLanguage)
    }
}

/** Rounded tonal container used for every section of the detail screen. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(Spacing.lg + 4.dp), verticalArrangement = Arrangement.spacedBy(Spacing.md), content = content)
    }
}

/** Section heading with a leading icon. */
@Composable
fun SectionTitle(icon: ImageVector, text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Icon-in-a-circle, title and body: the opening block of a simple screen (Safety, Emergency…). */
@Composable
fun ScreenIntro(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Animated gradient used to paint loading placeholders. */
@Composable
fun shimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val shine = MaterialTheme.colorScheme.surfaceContainerHighest
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
    )
    val w = 600f
    return Brush.linearGradient(listOf(base, shine, base), start = Offset(t * w, 0f), end = Offset(t * w + w, w / 3))
}

/** Grey placeholder block that shimmers while content loads. */
@Composable
fun SkeletonBlock(modifier: Modifier, brush: Brush = shimmerBrush()) {
    Box(modifier.clip(MaterialTheme.shapes.medium).background(brush))
}

/** Placeholder with the shape of the detail screen (hero, chart, text) while the forecast loads. */
@Composable
fun LoadingSkeleton(strings: Strings, modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = strings.loading },
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SkeletonBlock(Modifier.fillMaxWidth().height(196.dp).clip(MaterialTheme.shapes.extraLarge), brush)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.loading, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(120.dp), brush)
        SkeletonBlock(Modifier.fillMaxWidth().height(220.dp), brush)
    }
}

/**
 * Clear failure state with a retry button. Deliberately shows no risk level:
 * we never fall back to "Low" when data failed to load.
 */
@Composable
fun ErrorState(strings: Strings, onRetry: () -> Unit, modifier: Modifier = Modifier, message: String = strings.failed) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(AppIcons.CloudOff, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                strings.errorTitle, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.semantics { heading() },
            )
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(strings.errorNoRisk, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.retry)
            }
        }
    }
}

/** "Not an official warning" notice. Must appear on every screen that shows a risk level. */
@Composable
fun Disclaimer(strings: Strings, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(AppIcons.Info, null, Modifier.size(18.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(strings.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small rounded chip used for labels such as the explanation source. */
@Composable
fun InfoChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
