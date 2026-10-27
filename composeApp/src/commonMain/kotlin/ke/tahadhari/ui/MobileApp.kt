package ke.tahadhari.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language

/**
 * Phone layout: pick a county, then see its forecast and advice.
 * Only the selected county is fetched, to save data on mobile.
 * TODO(week 5): remember the user's county, use GPS, add the "I'm safe" check-in.
 */
@Composable
fun MobileApp() {
    TahadhariTheme {
        var language by remember { mutableStateOf(Language.ENGLISH) }
        var selected by remember { mutableStateOf<County?>(null) }
        val strings = Strings.of(language)

        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column {
                AppHeader("Tahadhari", strings.tagline, language) { language = it }
                val county = selected
                if (county == null) {
                    CountyPicker(strings) { selected = it }
                } else {
                    var state by remember(county) { mutableStateOf<LoadState>(LoadState.Loading) }
                    var attempt by remember(county) { mutableStateOf(0) }
                    LaunchedEffect(county, attempt) {
                        state = LoadState.Loading
                        state = loadCounty(county)
                    }
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        TextButton(onClick = { selected = null }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(strings.back)
                        }
                        CountyDetail(county, state, language) { attempt++ }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountyPicker(strings: Strings, onPick: (County) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        Counties.all.filter { it.name.contains(query, ignoreCase = true) || it.town.contains(query, ignoreCase = true) }
    }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(strings.searchHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(filtered, key = { it.code }) { county ->
                ListItem(
                    headlineContent = { Text(county.name) },
                    supportingContent = { Text(county.town) },
                    modifier = Modifier.clickable { onPick(county) },
                )
                HorizontalDivider()
            }
        }
    }
}
