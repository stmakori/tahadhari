package ke.tahadhari.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.mobile.CountyPickerScreen
import ke.tahadhari.ui.mobile.EmergencyScreen
import ke.tahadhari.ui.mobile.SafetyScreen

/** Bottom navigation tabs of the phone app. */
enum class MobileTab { ALERTS, SAFETY, EMERGENCY }

/**
 * Phone app: bottom navigation (Alerts / Safety / Emergency), language toggle in the top bar.
 * Alerts shows a county picker until a county is chosen, then that county's detail.
 * Only the selected county is fetched, to save data on mobile.
 *
 * State is hoisted for a preferences layer: pass the saved county/language in and persist
 * them from the callbacks. Back (Android) returns from another tab to Alerts, then from
 * county detail to the picker, then exits.
 *
 * @param darkTheme defaults to the system setting; set it for previews and screenshots.
 * @param statusLine optional line under the hero card (e.g. "Updated …" / "Showing saved data").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MobileApp(
    initialCountyCode: Int? = null,
    initialLanguage: Language = Language.ENGLISH,
    onCountyChange: (Int?) -> Unit = {},
    onLanguageChange: (Language) -> Unit = {},
    statusLine: (@Composable (LoadState.Ready) -> Unit)? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    TahadhariTheme(darkTheme) {
        var language by rememberSaveable { mutableStateOf(initialLanguage) }
        var countyCode by rememberSaveable { mutableStateOf(initialCountyCode?.takeIf { Counties.byCode(it) != null }) }
        var tab by rememberSaveable { mutableStateOf(MobileTab.ALERTS) }
        val strings = Strings.of(language)
        val county = countyCode?.let { Counties.byCode(it) }

        fun selectCounty(code: Int?) { countyCode = code; onCountyChange(code) }
        fun selectLanguage(l: Language) { language = l; onLanguageChange(l) }

        BackHandler(enabled = tab != MobileTab.ALERTS || county != null) {
            if (tab != MobileTab.ALERTS) tab = MobileTab.ALERTS else selectCounty(null)
        }

        val showBack = tab == MobileTab.ALERTS && county != null
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.appName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                if (showBack) strings.countyLabel(county!!.name) else strings.tagline,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    },
                    navigationIcon = {
                        if (showBack) IconButton(onClick = { selectCounty(null) }) { Icon(AppIcons.ArrowBack, strings.allCounties) }
                        else AppMark(Modifier.padding(horizontal = Spacing.md).size(36.dp))
                    },
                    actions = { LanguageToggle(language, ::selectLanguage, Modifier.padding(end = Spacing.sm)) },
                )
            },
            bottomBar = {
                NavigationBar {
                    TabItem(tab, MobileTab.ALERTS, AppIcons.Bell, strings.navAlerts) { tab = it }
                    TabItem(tab, MobileTab.SAFETY, AppIcons.Shield, strings.navSafety) { tab = it }
                    TabItem(tab, MobileTab.EMERGENCY, AppIcons.Phone, strings.navEmergency) { tab = it }
                }
            },
        ) { padding ->
            val inner = Modifier.padding(padding).consumeWindowInsets(padding)
            when (tab) {
                MobileTab.ALERTS -> AnimatedContent(county, inner, transitionSpec = { fadeIn() togetherWith fadeOut() }) { c ->
                    if (c == null) CountyPickerScreen(strings, onPick = { selectCounty(it.code) })
                    else CountyScreen(c, language, statusLine, onChangeCounty = { selectCounty(null) })
                }
                MobileTab.SAFETY -> SafetyScreen(strings, language, inner)
                MobileTab.EMERGENCY -> EmergencyScreen(strings, language, inner)
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(current: MobileTab, tab: MobileTab, icon: ImageVector, label: String, onSelect: (MobileTab) -> Unit) {
    NavigationBarItem(
        selected = current == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, null) },
        label = { Text(label) },
    )
}

/** Loads one county (in a LaunchedEffect, never in composition) and shows its detail. */
@Composable
private fun CountyScreen(
    county: County,
    language: Language,
    statusLine: (@Composable (LoadState.Ready) -> Unit)?,
    onChangeCounty: () -> Unit,
) {
    val strings = Strings.of(language)
    var state by remember(county.code) { mutableStateOf<LoadState>(LoadState.Loading) }
    var attempt by remember(county.code) { mutableIntStateOf(0) }
    LaunchedEffect(county.code, attempt) {
        state = LoadState.Loading
        state = loadCounty(county)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CountyDetail(county, state, language, statusLine = statusLine) { attempt++ }
        OutlinedButton(
            onClick = onChangeCounty,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).heightIn(min = Spacing.touch),
        ) {
            Icon(AppIcons.Location, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.changeCounty)
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
