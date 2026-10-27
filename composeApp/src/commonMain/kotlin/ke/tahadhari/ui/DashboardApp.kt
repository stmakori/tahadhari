package ke.tahadhari.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.RiskLevel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Desktop layout for county disaster teams, Red Cross and NGOs:
 * loads all 47 counties, ranks them by risk, and shows details side by side.
 * TODO(week 4): replace the left list with an interactive Kenya map.
 */
@Composable
fun DashboardApp() {
    TahadhariTheme {
        var language by remember { mutableStateOf(Language.ENGLISH) }
        var selectedCode by remember { mutableStateOf<Int?>(null) }
        var refreshKey by remember { mutableStateOf(0) }
        val states = remember { mutableStateMapOf<Int, LoadState>() }
        val strings = Strings.of(language)

        LaunchedEffect(refreshKey) {
            states.clear()
            val limit = Semaphore(6) // be polite to the free API
            Counties.all.forEach { county ->
                states[county.code] = LoadState.Loading
                launch { limit.withPermit { states[county.code] = loadCounty(county) } }
            }
        }

        val ranked = Counties.all.sortedWith(
            compareByDescending<ke.tahadhari.data.County> { (states[it.code] as? LoadState.Ready)?.assessment?.level?.ordinal ?: -1 }
                .thenByDescending { (states[it.code] as? LoadState.Ready)?.assessment?.maxThreeDayTotalMm ?: 0.0 }
        )
        val done = states.values.count { it !is LoadState.Loading }

        Surface(Modifier.fillMaxSize()) {
            Column {
                AppHeader(strings.dashboardTitle, strings.loaded(done, Counties.all.size), language) { language = it }
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.width(340.dp).fillMaxHeight()) {
                        item {
                            TextButton(onClick = { refreshKey++ }, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text("⟳ ${strings.retry}")
                            }
                        }
                        items(ranked, key = { it.code }) { county ->
                            val state = states[county.code]
                            val isSelected = county.code == selectedCode
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedCode = county.code }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(county.name, Modifier.weight(1f))
                                when (state) {
                                    is LoadState.Ready -> RiskBadge(state.assessment.level, strings)
                                    is LoadState.Failed -> Text("!", color = MaterialTheme.colorScheme.error)
                                    else -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                    VerticalDivider()
                    Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        val county = selectedCode?.let { Counties.byCode(it) }
                        if (county == null) {
                            Text(strings.selectCounty, Modifier.padding(24.dp))
                        } else {
                            CountyDetail(county, states[county.code] ?: LoadState.Loading, language) { refreshKey++ }
                        }
                    }
                }
            }
        }
    }
}

/** Counts of counties per risk level, handy for a summary bar later. */
fun Map<Int, LoadState>.levelCounts(): Map<RiskLevel, Int> =
    values.filterIsInstance<LoadState.Ready>().groupingBy { it.assessment.level }.eachCount()
