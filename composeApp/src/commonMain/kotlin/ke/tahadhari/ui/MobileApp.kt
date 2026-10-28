package ke.tahadhari.ui

import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.domain.atPoint
import ke.tahadhari.ui.mobile.PointForecastCard
import ke.tahadhari.ui.mobile.ReadAloudButton
import ke.tahadhari.ui.mobile.RoundedPointSaver
import ke.tahadhari.data.guides.SafetyGuides
import ke.tahadhari.domain.Language
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import ke.tahadhari.ui.mobile.AboutPage
import ke.tahadhari.ui.mobile.CheckInRow
import ke.tahadhari.ui.mobile.CrashReportCard
import ke.tahadhari.ui.mobile.RiskAlertsRow
import ke.tahadhari.ui.mobile.ShareAlertButton
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
 * @param riskAlertsEnabled saved state of the opt-in "notify me if risk rises" switch; [onRiskAlertsChange] saves it.
 * @param crashReport text of a crash saved by the last run, or null; [onCrashReportDelete] deletes it.
 * @param statusLine optional line under the hero card (e.g. "Updated …" / "Showing saved data").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MobileApp(
    initialCountyCode: Int? = null,
    initialLanguage: Language = Language.ENGLISH,
    onCountyChange: (Int?) -> Unit = {},
    onLanguageChange: (Language) -> Unit = {},
    riskAlertsEnabled: Boolean = false,
    onRiskAlertsChange: (Boolean) -> Unit = {},
    crashReport: String? = null,
    onCrashReportDelete: () -> Unit = {},
    statusLine: (@Composable (LoadState.Ready) -> Unit)? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    initialTab: MobileTab = MobileTab.ALERTS,
    initialGuideId: String? = null,
    initialShowAbout: Boolean = false,
) {
    TahadhariTheme(darkTheme) {
        var language by rememberSaveable { mutableStateOf(initialLanguage) }
        var countyCode by rememberSaveable { mutableStateOf(initialCountyCode?.takeIf { Counties.byCode(it) != null }) }
        var tab by rememberSaveable { mutableStateOf(initialTab) }
        var guideId by rememberSaveable { mutableStateOf(initialGuideId?.takeIf { SafetyGuides.byId(it) != null }) }
        var showAbout by rememberSaveable { mutableStateOf(initialShowAbout) }
        var riskAlerts by rememberSaveable { mutableStateOf(riskAlertsEnabled) }
        fun setRiskAlerts(on: Boolean) { riskAlerts = on; onRiskAlertsChange(on) }
        val guideTicks = remember { mutableStateMapOf<String, Boolean>() } // in memory only, by design
        val strings = Strings.of(language)
        val county = countyCode?.let { Counties.byCode(it) }
        val guide = guideId?.let { SafetyGuides.byId(it) }

        fun selectCounty(code: Int?) { countyCode = code; onCountyChange(code) }
        fun selectLanguage(l: Language) { language = l; onLanguageChange(l) }

        // Back: About -> (previous screen) | guide -> guide list -> Alerts tab -> county picker -> exit.
        BackHandler(enabled = showAbout || tab != MobileTab.ALERTS || county != null) {
            when {
                showAbout -> showAbout = false
                tab == MobileTab.SAFETY && guide != null -> guideId = null
                tab != MobileTab.ALERTS -> tab = MobileTab.ALERTS
                else -> selectCounty(null)
            }
        }
        fun selectTab(t: MobileTab) {
            if (t == MobileTab.SAFETY && tab == MobileTab.SAFETY) guideId = null // re-tap returns to the list
            tab = t
        }

        val countyBack = tab == MobileTab.ALERTS && county != null
        val guideBack = tab == MobileTab.SAFETY && guide != null
        if (showAbout) {
            AboutPage(language, ::selectLanguage, riskAlerts, ::setRiskAlerts) { showAbout = false }
            return@TahadhariTheme
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                when {
                                    countyBack -> strings.countyLabel(county!!.name)
                                    guideBack -> guide!!.content(language).title
                                    else -> strings.tagline
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (countyBack) IconButton(onClick = { selectCounty(null) }) { Icon(AppIcons.ArrowBack, strings.allCounties) }
                        else if (guideBack) IconButton(onClick = { guideId = null }) { Icon(AppIcons.ArrowBack, strings.allGuides) }
                        else AppMark(Modifier.padding(horizontal = Spacing.md).size(36.dp))
                    },
                    actions = {
                        IconButton(onClick = { showAbout = true }) { Icon(AppIcons.Info, strings.aboutPage.aboutOpen) }
                        LanguageToggle(language, ::selectLanguage, Modifier.padding(end = Spacing.sm))
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    TabItem(tab, MobileTab.ALERTS, AppIcons.Bell, strings.navAlerts, ::selectTab)
                    TabItem(tab, MobileTab.SAFETY, AppIcons.Shield, strings.navSafety, ::selectTab)
                    TabItem(tab, MobileTab.EMERGENCY, AppIcons.Phone, strings.navEmergency, ::selectTab)
                }
            },
        ) { padding ->
            val inner = Modifier.padding(padding).consumeWindowInsets(padding)
            when (tab) {
                MobileTab.ALERTS -> Column(inner) {
                    if (crashReport != null) {
                        CrashReportCard(crashReport, language, onCrashReportDelete, Modifier.padding(Spacing.lg))
                    }
                    AnimatedContent(county, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }) { c ->
                        if (c == null) CountyPickerScreen(strings, onPick = { selectCounty(it.code) })
                        else CountyScreen(c, language, statusLine, riskAlerts, ::setRiskAlerts, onChangeCounty = { selectCounty(null) })
                    }
                }
                MobileTab.SAFETY -> SafetyScreen(strings, language, guide?.id, { guideId = it }, guideTicks, inner)
                MobileTab.EMERGENCY -> EmergencyScreen(strings, language, county, onChooseCounty = { tab = MobileTab.ALERTS }, inner)
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(current: MobileTab, tab: MobileTab, icon: ImageVector, label: String, onSelect: (MobileTab) -> Unit) {
    NavigationBarItem(
        modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
        selected = current == tab,
        onClick = { onSelect(tab) },
        icon = { Icon(icon, null) },
        label = { Text(label) },
    )
}

/** Loads one county (in a LaunchedEffect, never in composition) and shows its detail. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountyScreen(
    county: County,
    language: Language,
    statusLine: (@Composable (LoadState.Ready) -> Unit)?,
    riskAlerts: Boolean,
    onRiskAlertsChange: (Boolean) -> Unit,
    onChangeCounty: () -> Unit,
) {
    val strings = Strings.of(language)
    // Optional: the user's own point (rounded to ~1 km) instead of the county HQ. Kept in memory only.
    var point by rememberSaveable(county.code, stateSaver = RoundedPointSaver) { mutableStateOf<GeoPoint?>(null) }
    val shown = point?.let { county.atPoint(it) } ?: county
    // The state resets when the point changes, so an HQ forecast is never shown under "your location".
    var state by remember(county.code, point) { mutableStateOf<LoadState>(LoadState.Loading) }
    var attempt by remember(county.code) { mutableIntStateOf(0) }
    var refreshing by remember(county.code, point) { mutableStateOf(false) }
    LaunchedEffect(county.code, point, attempt) {
        // A refresh keeps the current forecast on screen (with the pull indicator) until the new one arrives.
        if (state is LoadState.Ready) refreshing = true else state = LoadState.Loading
        state = loadCounty(shown)
        refreshing = false
    }
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { if (!refreshing) attempt++ }, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PointForecastCard(county, point, language, { point = it }, Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.lg))
            CountyDetail(shown, state, language, statusLine = statusLine ?: { DataStatusLine(it, Strings.of(language)) }) { attempt++ }
            // Sharing is offered only with a forecast on screen, never after a failure.
            (state as? LoadState.Ready)?.let { ready ->
                ReadAloudButton(shown, ready, language, Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm))
                ShareAlertButton(shown, ready, language, Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm))
            }
            // Visible alternative to pulling, for desktop, switch and screen-reader users.
            OutlinedButton(
                onClick = { attempt++ },
                enabled = !refreshing && state !is LoadState.Loading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.refresh)
            }
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = onChangeCounty,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Location, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.changeCounty)
            }
            RiskAlertsRow(riskAlerts, language, onRiskAlertsChange, Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.sm))
            CheckInRow(county, strings, Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg))
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}
