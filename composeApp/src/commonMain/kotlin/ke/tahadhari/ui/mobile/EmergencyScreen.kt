package ke.tahadhari.ui.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.AppIcons
import ke.tahadhari.ui.ScreenIntro
import ke.tahadhari.ui.Spacing
import ke.tahadhari.ui.Strings

/**
 * Emergency tab (Dharura). Placeholder: intro only.
 * TODO(week 5): emergency numbers (999, 112, 1199 Kenya Red Cross; verify against official
 * sources first) as 48dp+ call buttons, and county contacts. Add them below the intro.
 */
@Composable
fun EmergencyScreen(strings: Strings, language: Language, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        ScreenIntro(AppIcons.Phone, strings.emergencyTitle, strings.emergencyBody)
    }
}
