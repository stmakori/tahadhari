package ke.tahadhari.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.domain.AlertExplainer
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.dashboard.CountyMap
import ke.tahadhari.ui.dashboard.CountySearchField
import ke.tahadhari.ui.dashboard.CsvExport
import ke.tahadhari.ui.dashboard.CsvExportRequest
import ke.tahadhari.ui.dashboard.CsvExportResult
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.MapLegend
import ke.tahadhari.ui.dashboard.RankedList
import ke.tahadhari.ui.dashboard.SummaryBar
import kotlinx.coroutines.launch

/** Width at which map, list and detail fit side by side. */
private val ThreePaneMinWidth = 1400.dp

/**
 * Desktop dashboard for county disaster teams, Kenya Red Cross volunteers and NGOs: loads all 47
 * counties (at most 6 requests at a time), shows them on a risk map and a ranked list, with the
 * selected county's details alongside.
 *
 * @param onExportCsv saves the CSV export (desktop: a file dialog). Null hides the export button.
 */
@Composable
fun DashboardApp(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onExportCsv: (suspend (CsvExportRequest) -> CsvExportResult)? = null,
) {
    val scope = rememberCoroutineScope()
    val state = remember { DashboardState(scope) }
    LaunchedEffect(state) { state.refresh() }
    DashboardScreen(state, darkTheme, onExportCsv = onExportCsv)
}

/** The dashboard UI for a given [state]. Screenshot tests call this with fake data and no network. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    darkTheme: Boolean = isSystemInDarkTheme(),
    explainer: AlertExplainer = AppGraph.explainer,
    onExportCsv: (suspend (CsvExportRequest) -> CsvExportResult)? = null,
) {
    TahadhariTheme(darkTheme) {
        val strings = Strings.of(state.language)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        val listState = rememberLazyListState()
        val focus = remember { FocusRequester() }
        val now = rememberNowMillis()

        // Keep the selected row in view when it changes (keyboard or map click).
        LaunchedEffect(state.selectedCode) {
            val index = state.visible.indexOfFirst { it.code == state.selectedCode }
            if (index >= 0) {
                val shown = listState.layoutInfo.visibleItemsInfo
                val fullyVisible = shown.size > 2 && index > shown.first().index && index < shown.last().index
                if (!fullyVisible) listState.animateScrollToItem((index - 2).coerceAtLeast(0))
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        val export: (() -> Unit)? = onExportCsv?.let { save ->
            {
                scope.launch {
                    val request = CsvExportRequest(
                        csv = CsvExport.build(state.ranked, state.states, now),
                        suggestedFileName = CsvExport.fileName(now),
                        dialogTitle = strings.exportCsv,
                    )
                    when (val r = save(request)) {
                        is CsvExportResult.Saved -> snackbar.showSnackbar(strings.exportSaved(r.fileName))
                        is CsvExportResult.Failed -> snackbar.showSnackbar(strings.exportFailed)
                        CsvExportResult.Cancelled -> Unit
                    }
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> { state.moveSelection(1); true }
                        Key.DirectionUp -> { state.moveSelection(-1); true }
                        else -> false
                    }
                },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                DashboardHeader(state, strings, now, export)
                SummaryBar(
                    counts = state.counts,
                    filter = state.filter,
                    strings = strings,
                    onToggle = state::toggleFilter,
                    onClear = { state.filter = null },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(bottom = Spacing.md),
                )
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.lg)) {
                    if (maxWidth >= ThreePaneMinWidth) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            MapPane(state, strings, Modifier.weight(1.05f).fillMaxHeight())
                            ListPane(state, strings, listState, Modifier.width(400.dp).fillMaxHeight())
                            DetailPane(state, explainer, Modifier.weight(1f).fillMaxHeight())
                        }
                    } else {
                        val leftWidth = if (maxWidth < 900.dp) maxWidth * 0.48f else 420.dp
                        val showMap = state.showMapTab
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            Column(Modifier.width(leftWidth).fillMaxHeight()) {
                                PrimaryTabRow(selectedTabIndex = if (showMap) 1 else 0, containerColor = MaterialTheme.colorScheme.surface) {
                                    Tab(selected = !showMap, onClick = { state.showMapTab = false }, text = { Text(strings.listTab) },
                                        icon = { Icon(AppIcons.List, null, Modifier.size(18.dp)) })
                                    Tab(selected = showMap, onClick = { state.showMapTab = true }, text = { Text(strings.mapTab) },
                                        icon = { Icon(AppIcons.Map, null, Modifier.size(18.dp)) })
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                if (showMap) MapPane(state, strings, Modifier.weight(1f).fillMaxWidth())
                                else ListPane(state, strings, listState, Modifier.weight(1f).fillMaxWidth())
                            }
                            DetailPane(state, explainer, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
                // Risk is visible on every view of the dashboard, so the disclaimer always is too.
                Disclaimer(strings, Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
            }
        }
    }
}

@Composable
private fun DashboardHeader(state: DashboardState, strings: Strings, now: Long, onExport: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark()
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                strings.dashboardTitle, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(Spacing.xs))
                val updated = state.oldestUpdate?.let { strings.updatedText(it, now) } ?: strings.notUpdatedYet
                Text(
                    "$updated · ${strings.loaded(state.doneCount, state.counties.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        if (onExport != null) {
            OutlinedButton(onClick = onExport, enabled = state.doneCount > 0, modifier = Modifier.heightIn(min = 40.dp)) {
                Icon(AppIcons.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.exportCsv)
            }
            Spacer(Modifier.width(Spacing.sm))
        }
        FilledTonalButton(onClick = state::refresh, modifier = Modifier.heightIn(min = 40.dp)) {
            Icon(AppIcons.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.refresh)
        }
        Spacer(Modifier.width(Spacing.md))
        LanguageToggle(state.language, { state.language = it })
    }
}

@Composable
private fun MapPane(state: DashboardState, strings: Strings, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.floodRisk, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(strings.mapHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CountyMap(
                states = state.states,
                selectedCode = state.selectedCode,
                strings = strings,
                onSelect = { state.selectedCode = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            MapLegend(strings)
            Text(
                strings.mapAttribution, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ListPane(state: DashboardState, strings: Strings, listState: LazyListState, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column {
            CountySearchField(state.query, { state.query = it }, strings, Modifier.padding(Spacing.md))
            Text(
                strings.countiesShown(state.visible.size, state.counties.size),
                Modifier.padding(horizontal = Spacing.lg),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RankedList(
                rows = state.visible,
                ranked = state.ranked,
                states = state.states,
                selectedCode = state.selectedCode,
                query = state.query,
                strings = strings,
                listState = listState,
                onSelect = { state.selectedCode = it },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailPane(state: DashboardState, explainer: AlertExplainer, modifier: Modifier) {
    val strings = Strings.of(state.language)
    val county = state.selectedCode?.let { code -> state.counties.firstOrNull { it.code == code } }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        if (county == null) {
            Column(
                Modifier.fillMaxSize().padding(Spacing.xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScreenIntro(AppIcons.Map, strings.selectCounty, strings.selectCountyBody, Modifier.widthIn(max = 420.dp))
            }
        } else {
            key(county.code) {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CountyDetail(
                        county, state.states[county.code] ?: LoadState.Loading, state.language,
                        explainer = explainer,
                    ) { state.retry(county.code) }
                }
            }
        }
    }
}

/** Counts of counties per risk level (counties without data are not counted). Used by the summary bar. */
fun Map<Int, LoadState>.levelCounts(): Map<RiskLevel, Int> =
    values.filterIsInstance<LoadState.Ready>().groupingBy { it.assessment.level }.eachCount()
