package ke.tahadhari.ui.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.ui.*

/**
 * Welcome + county picker: a short intro, a search field and all 47 counties grouped by
 * first letter (flat list while searching). Matches county name or headquarters town.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CountyPickerScreen(strings: Strings, onPick: (County) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val filtered = remember(q) {
        Counties.all
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.town.contains(q, ignoreCase = true) }
            .sortedBy { it.name }
    }
    val groups = remember(filtered) { filtered.groupBy { it.name.first().uppercaseChar() } }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
        item(key = "welcome") { WelcomeBanner(strings, Modifier.padding(Spacing.lg)) }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { v -> query = v },
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(AppIcons.Close, strings.clearSearch) } }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                Text(
                    strings.noMatches(q),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.xl),
                )
            }
        } else {
            item(key = "heading") {
                Text(
                    strings.countiesHeading,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl, bottom = Spacing.xs)
                        .semantics { heading() },
                )
            }
            groups.forEach { (letter, counties) ->
                if (q.isEmpty()) {
                    stickyHeader(key = "h-$letter") {
                        Text(
                            letter.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = Spacing.lg + 4.dp, vertical = Spacing.xs),
                        )
                    }
                }
                items(counties, key = { it.code }) { county -> CountyRow(county, strings) { onPick(county) } }
            }
        }
    }
}

@Composable
private fun WelcomeBanner(strings: Strings, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer) {
        Column(
            Modifier.drawBehind { drawWaves(scheme.primary.copy(alpha = 0.14f), bandHeight = 28.dp.toPx()) }
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.xl, bottom = Spacing.xxl + Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppMark(Modifier.size(44.dp))
            Text(strings.welcomeTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(strings.welcomeBody, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CountyRow(county: County, strings: Strings, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Official county code (1–47), a familiar reference in Kenya.
        Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Text(county.code.toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge, color = scheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(county.name, style = MaterialTheme.typography.titleMedium)
            Text(strings.hqLabel(county.town), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        if (county.riverPoint != null) {
            Icon(AppIcons.Waves, strings.riverDataLabel, Modifier.size(20.dp), tint = scheme.primary)
            Spacer(Modifier.width(Spacing.sm))
        }
        Icon(AppIcons.ChevronRight, null, tint = scheme.onSurfaceVariant)
    }
}
