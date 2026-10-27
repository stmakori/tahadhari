package ke.tahadhari.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import kotlinx.coroutines.delay

/** Current time in epoch millis, refreshed every minute so "Updated 5 min ago" stays true. */
@Composable
fun rememberNowMillis(): Long {
    val now by produceState(AppGraph.nowMillis()) {
        while (true) {
            delay(60_000)
            value = AppGraph.nowMillis()
        }
    }
    return now
}

/**
 * "Updated 5 min ago", or a warning that saved data is shown because the network failed.
 * Default content of [CountyDetail]'s status line slot.
 */
@Composable
fun DataStatusLine(state: LoadState.Ready, strings: Strings, modifier: Modifier = Modifier) {
    val updated = strings.updatedText(state.updatedAtMillis, rememberNowMillis())
    if (state.fromCache) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.CloudOff, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "${strings.showingSavedForecast} $updated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        Text(updated, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
