package ke.tahadhari.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ke.tahadhari.ui.onboarding.OnboardingScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.data.Preferences
import ke.tahadhari.data.UserPreferences
import ke.tahadhari.platform.deleteCrashReport
import ke.tahadhari.platform.installCrashHandler
import ke.tahadhari.platform.onRiskAlertsChanged
import ke.tahadhari.platform.readCrashReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phone app entry point with saved preferences: loads the user's county and language
 * (a brief branded splash shows while the file is read off the main thread), then shows
 * [MobileApp] and saves changes.
 *
 * Only a chosen county is saved. Clearing it (Back to the picker, "Change county") is not, so
 * leaving the app with Back still reopens on the user's county next time.
 *
 * Use from `MainActivity` and the desktop `--mobile` preview: `MobileRoot()`.
 */
@Composable
fun MobileRoot(
    darkTheme: Boolean = isSystemInDarkTheme(),
    preferences: Preferences = AppGraph.preferences,
) {
    val prefs by produceState<UserPreferences?>(null, preferences) { value = preferences.load() }
    val scope = rememberCoroutineScope()
    // Local crash log: install the handler (idempotent) and look for a report from the last run.
    var crashReport by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val found = readCrashReport() // read before installing, so only an earlier run's crash shows
            installCrashHandler()
            found
        }.let { crashReport = it }
    }
    val loaded = prefs
    if (loaded == null) {
        Splash(darkTheme)
        return
    }
    // The intro runs once. Language picked in it is saved at once and carried into the app.
    var introDone by rememberSaveable { mutableStateOf(loaded.onboardingDone) }
    var language by rememberSaveable { mutableStateOf(loaded.language) }
    if (!introDone) {
        TahadhariTheme(darkTheme) {
            OnboardingScreen(
                language = language,
                onLanguage = { l -> language = l; scope.launch { preferences.setLanguage(l) } },
                onFinish = { introDone = true; scope.launch { preferences.setOnboardingDone(true) } },
            )
        }
        return
    }
    MobileApp(
        initialCountyCode = loaded.countyCode,
        initialLanguage = language,
        onCountyChange = { code -> if (code != null) scope.launch { preferences.setCounty(code) } },
        initialWatchCodes = loaded.watchCodes,
        onWatchChange = { list -> scope.launch { preferences.setWatchCodes(list) } },
        onLanguageChange = { l -> language = l; scope.launch { preferences.setLanguage(l) } },
        riskAlertsEnabled = loaded.riskAlertsEnabled,
        onRiskAlertsChange = { on ->
            scope.launch {
                preferences.setRiskAlertsEnabled(on)
                onRiskAlertsChanged(on) // start or stop the background check
            }
        },
        crashReport = crashReport,
        onCrashReportDelete = { crashReport = null; scope.launch(Dispatchers.Default) { deleteCrashReport() } },
        darkTheme = darkTheme,
    )
}

@Composable
private fun Splash(darkTheme: Boolean) {
    TahadhariTheme(darkTheme) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            AppMark(Modifier.size(64.dp))
        }
    }
}
