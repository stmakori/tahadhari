package ke.tahadhari.ui.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.data.WatchList
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** In-memory forecasts of the watched counties. Lives in `MobileApp`, so they survive opening a county and coming back. */
class WatchStates {
    val states = mutableStateMapOf<Int, LoadState>()
    /** The refresh key the list was last loaded for (not state: only read inside effects). */
    var loadedRefreshKey: Int = -1
}

/**
 * Which watched counties to fetch now: all of them on a new refresh, otherwise only those with
 * nothing on screen yet (newly added, or a load that was interrupted while still Loading).
 */
fun watchCodesToLoad(codes: List<Int>, states: Map<Int, LoadState>, refreshKey: Int, loadedRefreshKey: Int): List<Int> =
    if (refreshKey != loadedRefreshKey) codes else codes.filter { states[it].let { s -> s == null || s == LoadState.Loading } }

/** A missing result is a failure, never a level. */
fun watchStateOf(code: Int, results: Map<Int, LoadState>): LoadState = results[code] ?: LoadState.Failed("no data")

/**
 * "Counties I watch": a compact row per county, an add button and the disclaimer.
 * Fetches only the watched counties, in one batched call (see `loadCounties`), inside an effect.
 * [refreshKey] changes when the user refreshes the main county, which refreshes these too.
 */
@Composable
fun WatchSection(
    watched: List<County>,
    holder: WatchStates,
    refreshKey: Int,
    language: Language,
    riskAlertsOn: Boolean,
    onOpen: (County) -> Unit,
    onAdd: () -> Unit,
    onRemove: (County) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = Strings.of(language)
    val w = strings.watch
    val codes = watched.map { it.code }
    LaunchedEffect(codes, refreshKey) {
        val toLoad = watchCodesToLoad(codes, holder.states, refreshKey, holder.loadedRefreshKey)
        holder.loadedRefreshKey = refreshKey
        holder.states.keys.toList().filter { it !in codes }.forEach { holder.states.remove(it) }
        if (toLoad.isEmpty()) return@LaunchedEffect
        // Keep what is on screen while refreshing; show Loading only for rows with nothing yet.
        toLoad.forEach { if (holder.states[it] == null) holder.states[it] = LoadState.Loading }
        val results = try {
            loadCounties(watched.filter { it.code in toLoad })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
        toLoad.forEach { holder.states[it] = watchStateOf(it, results) }
    }
    val now = rememberNowMillis()
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxWidth().testTag("watch_section"), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(w.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (watched.isEmpty()) {
            Text(w.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        watched.forEach { county ->
            val state = holder.states[county.code] ?: LoadState.Loading
            WatchRow(county, state, now, language, onOpen = { onOpen(county) }, onRemove = { onRemove(county) }, onRetry = {
                scope.launch {
                    holder.states[county.code] = LoadState.Loading
                    holder.states[county.code] = loadCounty(county)
                }
            })
        }
        if (watched.any { holder.states[it.code] is LoadState.Ready }) Disclaimer(strings)
        if (riskAlertsOn && watched.isNotEmpty()) {
            Text(w.notifyNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (watched.size < WatchList.MAX) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("watch_add")) {
                Text(w.add)
            }
        } else {
            Text(w.limitReached, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WatchRow(
    county: County,
    state: LoadState,
    now: Long,
    language: Language,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val strings = Strings.of(language)
    val w = strings.watch
    val name = strings.countyLabel(county.name)
    val scheme = MaterialTheme.colorScheme
    val description = when (state) {
        LoadState.Loading -> w.rowLoadingDescription(name)
        is LoadState.Failed -> w.rowFailedDescription(name)
        is LoadState.Ready -> w.rowReady(
            name, strings.riskDescription(state.assessment.level), mainReasonText(state.assessment, language, strings),
            strings.updatedText(state.updatedAtMillis, now), state.fromCache,
        )
    }
    Surface(shape = MaterialTheme.shapes.medium, color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth().testTag("watch_row_${county.code}")) {
        Row(Modifier.padding(start = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            // The tappable part is one merged element with a full description (name + level + reason + age).
            Row(
                Modifier.weight(1f).heightIn(min = 64.dp)
                    .clickable(enabled = state is LoadState.Ready, role = Role.Button, onClick = onOpen)
                    .padding(vertical = Spacing.sm)
                    .clearAndSetSemantics { contentDescription = description; if (state is LoadState.Ready) role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    when (state) {
                        LoadState.Loading -> Text(w.loadingRow, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        is LoadState.Failed -> Text(w.failedRow, style = MaterialTheme.typography.bodySmall, color = scheme.error)
                        is LoadState.Ready -> {
                            Text(mainReasonText(state.assessment, language, strings), style = MaterialTheme.typography.bodySmall)
                            val updated = strings.updatedText(state.updatedAtMillis, now)
                            Text(
                                if (state.fromCache) "${w.savedRow}. $updated" else updated,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.fromCache) scheme.error else scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (state is LoadState.Ready) {
                    Spacer(Modifier.width(Spacing.sm))
                    RiskBadge(state.assessment.level, strings)
                    Icon(AppIcons.ChevronRight, null, tint = scheme.onSurfaceVariant)
                }
            }
            if (state is LoadState.Failed) TextButton(onClick = onRetry) { Text(strings.retry) }
            IconButton(onClick = onRemove) { Icon(AppIcons.Close, w.remove(name)) }
        }
    }
}
