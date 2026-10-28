package ke.tahadhari.ui.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.*

/** Full-screen About page for the phone: back arrow, language toggle and a scrolling [AboutContent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutPage(
    language: Language,
    onLanguage: (Language) -> Unit,
    riskAlerts: Boolean = false,
    onRiskAlertsChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
) {
    val strings = Strings.of(language)
    Scaffold(
        topBar = {
            FlexibleTopBar(
                title = strings.aboutPage.aboutTitle,
                navigation = { IconButton(onClick = onBack) { Icon(AppIcons.ArrowBack, strings.aboutPage.aboutBack) } },
                actions = { LanguageToggle(language, onLanguage, Modifier.padding(end = Spacing.sm)) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            AboutContent(language, Modifier.padding(Spacing.lg))
            // Phone only; draws nothing on desktop.
            RiskAlertsRow(riskAlerts, language, onRiskAlertsChange, Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.lg))
        }
    }
}
