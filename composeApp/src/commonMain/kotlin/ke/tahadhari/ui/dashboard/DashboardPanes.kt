package ke.tahadhari.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.domain.RiskLevel
import ke.tahadhari.domain.Trend
import ke.tahadhari.ui.AppIcons
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.RiskBadge
import ke.tahadhari.ui.RiskMark
import ke.tahadhari.ui.Spacing
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.color
import kotlin.math.roundToInt

/** Counts per level (most severe first), loading and no-data, each a chip that filters the list. */
@Composable
fun SummaryBar(
    counts: Map<StatusFilter, Int>,
    filter: StatusFilter?,
    strings: Strings,
    onToggle: (StatusFilter) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiskLevel.entries.reversed().forEach { level ->
            val f = StatusFilter.Level(level)
            CountChip(strings.risk(level), counts[f] ?: 0, filter == f, strings, { onToggle(f) }) {
                RiskMark(level, Modifier.size(18.dp))
            }
        }
        val rising = counts[StatusFilter.Rising] ?: 0
        if (rising > 0 || filter == StatusFilter.Rising) {
            CountChip(strings.trendRisingShort, rising, filter == StatusFilter.Rising, strings, { onToggle(StatusFilter.Rising) },
                description = strings.filterCount(strings.trendRisingLong, rising)) {
                Icon(AppIcons.ArrowUpward, null, Modifier.size(18.dp))
            }
        }
        val loading = counts[StatusFilter.Loading] ?: 0
        if (loading > 0 || filter == StatusFilter.Loading) {
            CountChip(strings.loadingShort, loading, filter == StatusFilter.Loading, strings, { onToggle(StatusFilter.Loading) }) {
                SmallSpinner()
            }
        }
        CountChip(strings.noDataShort, counts[StatusFilter.Failed] ?: 0, filter == StatusFilter.Failed, strings, { onToggle(StatusFilter.Failed) }) {
            PatternSwatch(failed = true)
        }
        if (filter != null) {
            TextButton(onClick = onClear) {
                Icon(AppIcons.Close, null, Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text(strings.clearFilter)
            }
        }
    }
}

@Composable
private fun CountChip(
    label: String,
    count: Int,
    selected: Boolean,
    strings: Strings,
    onClick: () -> Unit,
    description: String = strings.filterCount(label, count),
    leading: @Composable () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = count > 0 || selected,
        leadingIcon = leading,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                Spacer(Modifier.width(Spacing.sm))
                Text(count.toString(), fontWeight = FontWeight.Bold)
            }
        },
        modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = description },
    )
}

/** Search field over the ranked list. */
@Composable
fun CountySearchField(query: String, onQuery: (String) -> Unit, strings: Strings, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(strings.searchHint) },
        leadingIcon = { Icon(AppIcons.Search, null) },
        trailingIcon = if (query.isEmpty()) null else {
            { IconButton(onClick = { onQuery("") }) { Icon(AppIcons.Close, strings.clearSearch) } }
        },
        shape = RoundedCornerShape(50),
    )
}

/** Ranked counties: rank, name, level, key numbers and cached/failed state per row. */
@Composable
fun RankedList(
    rows: List<County>,
    ranked: List<County>,
    states: Map<Int, LoadState>,
    selectedCode: Int?,
    query: String,
    strings: Strings,
    listState: LazyListState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trends: Map<Int, Trend> = emptyMap(),
) {
    // Rank is the position among counties with data, so it doesn't change while filtering.
    val ranks = remember(ranked, states) {
        ranked.filter { states[it.code] is LoadState.Ready }.mapIndexed { i, c -> c.code to i + 1 }.toMap()
    }
    if (rows.isEmpty()) {
        Text(
            if (query.isNotBlank()) strings.noMatches(query) else strings.countiesShown(0, ranked.size),
            modifier.padding(Spacing.lg),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(vertical = Spacing.xs)) {
        items(rows, key = { it.code }) { county ->
            CountyRow(county, ranks[county.code], states[county.code], trends[county.code] ?: Trend.Unknown, county.code == selectedCode, strings) { onSelect(county.code) }
        }
    }
}

@Composable
private fun CountyRow(county: County, rank: Int?, state: LoadState?, trend: Trend, selected: Boolean, strings: Strings, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accent = when (state) {
        is LoadState.Ready -> state.assessment.level.color()
        is LoadState.Failed -> scheme.outline
        else -> scheme.outlineVariant
    }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            // Clear focus ring for keyboard users (Tab to a row, Enter/Space to select).
            .then(if (focused) Modifier.border(2.dp, scheme.primary, MaterialTheme.shapes.small) else Modifier)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(40.dp).clip(CircleShape).background(accent))
        Text(
            rank?.toString() ?: "–",
            Modifier.width(32.dp).clearAndSetSemantics { rank?.let { contentDescription = strings.rankLabel(it) } },
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    county.name, style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (state is LoadState.Ready && state.fromCache) {
                    Spacer(Modifier.width(6.dp))
                    Icon(AppIcons.CloudOff, strings.savedDataShort, Modifier.size(14.dp), tint = scheme.error)
                }
            }
            Text(
                metricsLine(county, state, strings),
                style = MaterialTheme.typography.bodySmall,
                color = if (state is LoadState.Failed) scheme.error else scheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (state is LoadState.Ready) TrendNote(trend, strings)
        }
        Spacer(Modifier.width(Spacing.sm))
        StatusPill(state, strings)
    }
}

/** Arrow plus "was Moderate" for counties whose level changed; nothing for same or unknown. Not colour-only. */
@Composable
private fun TrendNote(trend: Trend, strings: Strings) {
    val (icon, from, description) = when (trend) {
        is Trend.Rising -> Triple(AppIcons.ArrowUpward, trend.from, strings.trendRoseDescription(strings.risk(trend.from)))
        is Trend.Falling -> Triple(AppIcons.ArrowDownward, trend.from, strings.trendFellDescription(strings.risk(trend.from)))
        Trend.Same, Trend.Unknown -> return
    }
    Row(Modifier.clearAndSetSemantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.xs))
        Text(strings.trendWas(strings.risk(from)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun metricsLine(county: County, state: LoadState?, strings: Strings): String = when (state) {
    is LoadState.Ready -> {
        val a = state.assessment
        listOfNotNull(
            a.wettestDay?.let { strings.wettestShort(it.precipitationMm.roundToInt()) },
            strings.threeDayShort(a.maxThreeDayTotalMm.roundToInt()),
            a.river?.let { strings.riverShort(it.riverName ?: county.riverName ?: "", CsvExport.decimal(it.riseRatio, 1)) },
        ).joinToString(" · ")
    }
    is LoadState.Failed -> strings.errorTitle
    else -> strings.loading
}

/** Level badge, or a neutral "Loading" / outlined "No data" pill. Never a level without data. */
@Composable
fun StatusPill(state: LoadState?, strings: Strings) {
    val scheme = MaterialTheme.colorScheme
    when (state) {
        is LoadState.Ready -> RiskBadge(state.assessment.level, strings)
        is LoadState.Failed -> Row(
            Modifier.clip(RoundedCornerShape(50)).border(1.dp, scheme.error, RoundedCornerShape(50))
                .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.CloudOff, null, Modifier.size(14.dp), tint = scheme.error)
            Spacer(Modifier.width(5.dp))
            Text(strings.noDataShort, style = MaterialTheme.typography.labelMedium, color = scheme.error)
        }
        else -> Row(
            Modifier.clip(RoundedCornerShape(50)).background(scheme.surfaceContainerHigh)
                .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallSpinner()
            Spacer(Modifier.width(5.dp))
            Text(strings.loadingShort, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
    }
}
