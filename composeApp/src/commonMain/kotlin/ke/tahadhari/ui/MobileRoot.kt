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
import kotlinx.coroutines.launch

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
        onLanguageChange = { l -> language = l; scope.launch { preferences.setLanguage(l) } },
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
