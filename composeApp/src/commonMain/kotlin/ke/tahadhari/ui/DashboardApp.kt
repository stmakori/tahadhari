package ke.tahadhari.ui

import androidx.compose.foundation.focusable
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.data.RiskSnapshotStore
import ke.tahadhari.domain.AlertExplainer
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.ui.dashboard.CountyMap
import ke.tahadhari.ui.dashboard.CountySearchField
import ke.tahadhari.ui.dashboard.BriefingExport
import ke.tahadhari.ui.dashboard.CsvExport
import ke.tahadhari.ui.dashboard.DashboardSettingsStore
import ke.tahadhari.ui.dashboard.DashboardState
import ke.tahadhari.ui.dashboard.FileExportRequest
import ke.tahadhari.ui.dashboard.FileExportResult
import ke.tahadhari.ui.dashboard.MapLegend
import ke.tahadhari.ui.dashboard.RankedList
import ke.tahadhari.ui.dashboard.SummaryBar
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Width at which map, list and detail fit side by side. */
private val ThreePaneMinWidth = 1400.dp

/**
 * Desktop dashboard for county disaster teams, Kenya Red Cross volunteers and NGOs: loads all 47
 * counties (at most 6 requests at a time), shows them on a risk map and a ranked list, with the
 * selected county's details alongside.
 *
 * Remembers language, selected county, filter and the auto-refresh toggle between runs, and reloads
 * everything every 3 hours while open (when the toggle is on).
 *
 * @param onExport saves an exported file, CSV or HTML briefing (desktop: a file dialog). Null hides the export menu.
 */
@Composable
fun DashboardApp(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onExport: (suspend (FileExportRequest) -> FileExportResult)? = null,
) {
    val scope = rememberCoroutineScope()
    val state = remember { DashboardState(scope, snapshotStore = RiskSnapshotStore(AppGraph.store)) }
    var showAbout by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        val settingsStore = DashboardSettingsStore(AppGraph.store)
        state.restore(settingsStore.load())
        state.loadSnapshots()
        state.refresh()
        // Save whenever a remembered setting changes (the first value is what we just loaded).
        snapshotFlow { state.settings }.drop(1).collect { settingsStore.save(it) }
    }
    LaunchedEffect(state, state.autoRefresh) { state.autoRefreshLoop() }
    DashboardScreen(state, darkTheme, onExport = onExport, onAbout = { showAbout = true })
    if (showAbout) {
        TahadhariTheme(darkTheme) { AboutDialog(state.language) { showAbout = false } }
    }
}

/** About / how it works, shared with the phone app's About page. */
@Composable
private fun AboutDialog(language: ke.tahadhari.domain.Language, onDismiss: () -> Unit) {
    val strings = Strings.of(language)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.aboutTitle, modifier = Modifier.semantics { heading() }) },
        text = {
            Box(Modifier.widthIn(max = 720.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                AboutContent(language)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.aboutBack) } },
        modifier = Modifier.widthIn(max = 760.dp),
    )
}

/** The dashboard UI for a given [state]. Screenshot tests call this with fake data and no network. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardState,
    darkTheme: Boolean = isSystemInDarkTheme(),
    explainer: AlertExplainer = AppGraph.explainer,
    onExport: (suspend (FileExportRequest) -> FileExportResult)? = null,
    onAbout: () -> Unit = {},
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

        fun exporter(build: (nowMillis: Long) -> FileExportRequest): (() -> Unit)? = onExport?.let { save ->
            {
                scope.launch {
                    when (val r = save(build(AppGraph.nowMillis()))) {
                        is FileExportResult.Saved -> snackbar.showSnackbar(strings.exportSaved(r.fileName))
                        is FileExportResult.Failed -> snackbar.showSnackbar(strings.exportFailed)
                        FileExportResult.Cancelled -> Unit
                    }
                }
            }
        }
        val exportCsv = exporter { t ->
            FileExportRequest(
                content = CsvExport.build(state.ranked, state.states, t, state.trends),
                suggestedFileName = CsvExport.fileName(t),
                dialogTitle = strings.exportCsv,
                extension = "csv",
                utf8Bom = true,
            )
        }
        val exportBriefing = exporter { t ->
            FileExportRequest(
                content = BriefingExport.build(state.counties, state.states, state.language, t, state.trends),
                suggestedFileName = BriefingExport.fileName(t),
                dialogTitle = strings.exportBriefingDialog,
                extension = "html",
            )
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
                DashboardHeader(state, strings, now, exportCsv, exportBriefing, onAbout)
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
private fun DashboardHeader(
    state: DashboardState,
    strings: Strings,
    now: Long,
    onExportCsv: (() -> Unit)?,
    onExportBriefing: (() -> Unit)?,
    onAbout: () -> Unit,
) {
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
                if (state.refreshing) {
                    // Small progress indicator: rows keep their data and update in place.
                    CircularProgressIndicator(
                        Modifier.size(14.dp).semantics { contentDescription = strings.refreshingLabel },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(AppIcons.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(Spacing.xs))
                val updated = state.oldestUpdate?.let { strings.updatedText(it, now) } ?: strings.notUpdatedYet
                Text(
                    if (state.refreshing) "$updated · ${strings.refreshingProgress(state.refreshDone, state.counties.size)}"
                    else "$updated · ${strings.loaded(state.doneCount, state.counties.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        if (onExportCsv != null || onExportBriefing != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { open = true }, enabled = state.doneCount > 0, modifier = Modifier.heightIn(min = 40.dp)) {
                    Icon(AppIcons.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(strings.export)
                    Icon(AppIcons.ArrowDropDown, null, Modifier.size(18.dp))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    onExportBriefing?.let {
                        DropdownMenuItem(text = { Text(strings.exportBriefing) }, onClick = { open = false; it() },
                            leadingIcon = { Icon(AppIcons.Document, null, Modifier.size(18.dp)) })
                    }
                    onExportCsv?.let {
                        DropdownMenuItem(text = { Text(strings.exportCsv) }, onClick = { open = false; it() },
                            leadingIcon = { Icon(AppIcons.Download, null, Modifier.size(18.dp)) })
                    }
                }
            }
            Spacer(Modifier.width(Spacing.sm))
        }
        FilledTonalButton(onClick = state::refresh, modifier = Modifier.heightIn(min = 40.dp)) {
            Icon(AppIcons.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.refresh)
        }
        Spacer(Modifier.width(Spacing.xs))
        TextButton(onClick = onAbout, modifier = Modifier.heightIn(min = 40.dp)) {
            Icon(AppIcons.Info, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(strings.about)
        }
        SettingsMenu(state, strings)
        Spacer(Modifier.width(Spacing.xs))
        LanguageToggle(state.language, { state.language = it })
    }
}

/** Overflow menu with the dashboard settings (for now: automatic refresh). [open] is exposed for screenshots. */
@Composable
internal fun SettingsMenu(state: DashboardState, strings: Strings, initiallyOpen: Boolean = false) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Box {
        IconButton(onClick = { open = true }) { Icon(AppIcons.MoreVert, strings.settings) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(strings.autoRefresh) },
                onClick = { state.autoRefresh = !state.autoRefresh },
                leadingIcon = { Checkbox(checked = state.autoRefresh, onCheckedChange = null) },
                modifier = Modifier.semantics { selected = state.autoRefresh },
            )
        }
    }
}

@Composable
private fun MapPane(state: DashboardState, strings: Strings, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.floodRisk, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
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
                trends = state.trends,
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
