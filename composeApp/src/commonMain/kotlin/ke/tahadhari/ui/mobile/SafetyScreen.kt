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
 * Safety tab (Usalama). Placeholder: intro only.
 * TODO(week 5): offline safety guides (floods, landslides, cholera, livestock) in EN/SW and the
 * "Niko salama / I'm safe" check-in. Add guide content below the intro.
 */
@Composable
fun SafetyScreen(strings: Strings, language: Language, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        ScreenIntro(AppIcons.Shield, strings.safetyTitle, strings.safetyBody)
    }
}
