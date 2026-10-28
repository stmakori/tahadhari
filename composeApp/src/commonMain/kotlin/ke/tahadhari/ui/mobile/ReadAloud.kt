package ke.tahadhari.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.platform.SpeechEvent
import ke.tahadhari.platform.isSpeechAvailable
import ke.tahadhari.platform.speakAloud
import ke.tahadhari.platform.stopSpeaking
import ke.tahadhari.ui.AppIcons
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Spacing
import ke.tahadhari.ui.Strings

/**
 * "Read aloud" button for a ready forecast. Pressing it again stops. Draws nothing where the
 * platform has no speech. Speech uses the template text only (see [spokenAlert]), and stops when
 * the button leaves the screen or the alert changes.
 */
@Composable
fun ReadAloudButton(county: County, ready: LoadState.Ready, language: Language, modifier: Modifier = Modifier) {
    if (!remember { isSpeechAvailable() }) return
    val strings = Strings.of(language)
    var speaking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val currentStrings by rememberUpdatedState(strings)

    // Leaving the screen, or a different alert/language, must silence the old one.
    DisposableEffect(county, ready.assessment, language) {
        onDispose { stopSpeaking(); speaking = false }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilledTonalButton(
            onClick = {
                if (speaking) {
                    stopSpeaking()
                    speaking = false
                } else {
                    note = null
                    speaking = true
                    val alert = spokenAlert(county, ready.assessment, language, ready.fromCache)
                    speakAloud(alert.text, language, alert.englishFallback) { event ->
                        when (event) {
                            SpeechEvent.STARTED -> {}
                            SpeechEvent.STARTED_ENGLISH_FALLBACK -> note = currentStrings.speechSwahiliMissing
                            SpeechEvent.FINISHED -> speaking = false
                            SpeechEvent.FAILED -> { speaking = false; note = currentStrings.speechFailed }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
        ) {
            Icon(AppIcons.VolumeUp, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(if (speaking) strings.stopReading else strings.readAloud)
        }
        AnimatedVisibility(note != null) {
            Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
