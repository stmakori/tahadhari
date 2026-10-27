package ke.tahadhari.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ke.tahadhari.domain.RiskLevel

private val Light = lightColorScheme(primary = Color(0xFF0B5D7A), secondary = Color(0xFF2E7D32))
private val Dark = darkColorScheme(primary = Color(0xFF7FC8E3), secondary = Color(0xFF81C784))

@Composable
fun TahadhariTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> Color(0xFF2E7D32)
    RiskLevel.MODERATE -> Color(0xFFF9A825)
    RiskLevel.HIGH -> Color(0xFFEF6C00)
    RiskLevel.SEVERE -> Color(0xFFC62828)
}

fun RiskLevel.onColor(): Color = if (this == RiskLevel.MODERATE) Color.Black else Color.White
