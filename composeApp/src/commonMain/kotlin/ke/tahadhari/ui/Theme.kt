package ke.tahadhari.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.tahadhari.domain.RiskLevel

// Deep teal "water" palette. Neutrals carry a slight teal tint so surfaces feel related to the primary.
private val Light = lightColorScheme(
    primary = Color(0xFF0E6670), onPrimary = Color.White,
    primaryContainer = Color(0xFFB5ECF2), onPrimaryContainer = Color(0xFF002023),
    secondary = Color(0xFF4A6366), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8EA), onSecondaryContainer = Color(0xFF051F22),
    tertiary = Color(0xFF4D5F7C), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5E3FF), onTertiaryContainer = Color(0xFF071C36),
    background = Color(0xFFF5FAFA), onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF5FAFA), onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDAE4E5), onSurfaceVariant = Color(0xFF3F4949),
    outline = Color(0xFF6F7979), outlineVariant = Color(0xFFBEC8C9),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEFF5F5),
    surfaceContainer = Color(0xFFE9EFEF), surfaceContainerHigh = Color(0xFFE3E9E9),
    surfaceContainerHighest = Color(0xFFDDE4E4),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF82D3DD), onPrimary = Color(0xFF00363C),
    primaryContainer = Color(0xFF004F56), onPrimaryContainer = Color(0xFF9EF0FA),
    secondary = Color(0xFFB1CBCE), onSecondary = Color(0xFF1B3437),
    secondaryContainer = Color(0xFF324B4E), onSecondaryContainer = Color(0xFFCCE8EA),
    tertiary = Color(0xFFB5C7E9), onTertiary = Color(0xFF1F314C),
    tertiaryContainer = Color(0xFF354764), onTertiaryContainer = Color(0xFFD5E3FF),
    background = Color(0xFF0E1415), onBackground = Color(0xFFDDE4E4),
    surface = Color(0xFF0E1415), onSurface = Color(0xFFDDE4E4),
    surfaceVariant = Color(0xFF3F4949), onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899393), outlineVariant = Color(0xFF3F4949),
    surfaceContainerLowest = Color(0xFF090F10), surfaceContainerLow = Color(0xFF171D1E),
    surfaceContainer = Color(0xFF1B2122), surfaceContainerHigh = Color(0xFF252B2C),
    surfaceContainerHighest = Color(0xFF303637),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Colours for one risk level.
 * [fill]/[onFill] are strong (badges, chart bars, marks); text on fill meets WCAG AA (4.5:1).
 * [container]/[onContainer] are soft tints for large areas such as the hero card.
 */
@Immutable
data class RiskColors(val fill: Color, val onFill: Color, val container: Color, val onContainer: Color)

@Immutable
class RiskPalette(private val low: RiskColors, private val moderate: RiskColors, private val high: RiskColors, private val severe: RiskColors) {
    operator fun get(level: RiskLevel): RiskColors = when (level) {
        RiskLevel.LOW -> low
        RiskLevel.MODERATE -> moderate
        RiskLevel.HIGH -> high
        RiskLevel.SEVERE -> severe
    }
}

// Hues are spaced green / yellow / orange / red and every level also has its own shape (see RiskMark).
private val LightRisk = RiskPalette(
    low = RiskColors(Color(0xFF2E7D32), Color.White, Color(0xFFDDF2DF), Color(0xFF0F3D14)),
    moderate = RiskColors(Color(0xFFF5B700), Color(0xFF231A00), Color(0xFFFFF1C2), Color(0xFF4A3800)),
    high = RiskColors(Color(0xFFC2410C), Color.White, Color(0xFFFFE2D2), Color(0xFF5C1F00)),
    severe = RiskColors(Color(0xFFB3261E), Color.White, Color(0xFFFFDAD6), Color(0xFF5F0F0A)),
)

private val DarkRisk = RiskPalette(
    low = RiskColors(Color(0xFF7FD48A), Color(0xFF00390E), Color(0xFF183A1D), Color(0xFFB4F1BC)),
    moderate = RiskColors(Color(0xFFFFD25A), Color(0xFF3B2D00), Color(0xFF3A3000), Color(0xFFFFE591)),
    high = RiskColors(Color(0xFFFF9A4D), Color(0xFF431700), Color(0xFF4A2410), Color(0xFFFFD0B8)),
    severe = RiskColors(Color(0xFFFF6E6E), Color(0xFF3D0000), Color(0xFF5A1A17), Color(0xFFFFDAD6)),
)

val LocalRiskPalette = staticCompositionLocalOf { LightRisk }

/** Spacing scale. Use these instead of ad-hoc dp values so screens share one rhythm. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Minimum touch target size. */
    val touch = 48.dp
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun appTypography(): Typography {
    val base = Typography()
    return base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
        bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    )
}

/**
 * App theme: Material 3 with the Tahadhari palette, type scale and shapes, plus [LocalRiskPalette].
 * [darkTheme] defaults to the system setting; pass it explicitly for previews and screenshots.
 */
@Composable
fun TahadhariTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRiskPalette provides if (darkTheme) DarkRisk else LightRisk) {
        MaterialTheme(
            colorScheme = if (darkTheme) Dark else Light,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}

/** All colours for this level in the current theme. */
@Composable
@ReadOnlyComposable
fun RiskLevel.colors(): RiskColors = LocalRiskPalette.current[this]

/** Strong fill colour for this level (badges, bars). */
@Composable
@ReadOnlyComposable
fun RiskLevel.color(): Color = colors().fill

/** Text/icon colour on top of [color]. */
@Composable
@ReadOnlyComposable
fun RiskLevel.onColor(): Color = colors().onFill
