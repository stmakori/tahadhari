package ke.tahadhari.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.platform.copiesToClipboard
import ke.tahadhari.platform.rememberNotificationRequester
import ke.tahadhari.platform.shareText
import ke.tahadhari.ui.*

/** "Share alert" button: forwards a short plain-text alert. Only call with a Ready state. */
@Composable
internal fun ShareAlertButton(county: County, ready: LoadState.Ready, language: Language, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    var note by remember { mutableStateOf<String?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilledTonalButton(
            onClick = {
                shareText(shareAlertMessage(county, ready, language), strings.shareTitle)
                note = if (copiesToClipboard) strings.messageCopied else null
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
        ) {
            Icon(AppIcons.Share, null, Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(strings.shareAlert)
        }
        AnimatedVisibility(note != null) {
            Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Switch row for the opt-in "notify me if risk rises" setting. Draws nothing where the platform
 * has no notifications (desktop). Turning it on asks for permission first and stays off if refused.
 */
@Composable
internal fun RiskAlertsRow(enabled: Boolean, language: Language, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    var denied by remember { mutableStateOf(false) }
    val requestPermission = rememberNotificationRequester { allowed ->
        denied = !allowed
        if (allowed) onChange(true)
    } ?: return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.touch)
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = { on ->
                        if (on) requestPermission() else { denied = false; onChange(false) }
                    },
                )
                .semantics { stateDescription = if (enabled) strings.riskAlertsOn else strings.riskAlertsOff },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.riskAlertsToggle, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.md))
            Switch(checked = enabled, onCheckedChange = null)
        }
        Text(
            if (denied) strings.riskAlertsDenied else strings.riskAlertsHint,
            style = MaterialTheme.typography.bodySmall,
            color = if (denied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Shown on the first launch after a crash. Nothing is sent: the user shares the report or deletes it. */
@Composable
internal fun CrashReportCard(report: String, language: Language, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val strings = Strings.of(language)
    var note by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(AppIcons.Warning, null, Modifier.size(20.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(Spacing.sm))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(strings.crashTitle, style = MaterialTheme.typography.titleSmall)
                    Text(strings.crashBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(
                    onClick = {
                        shareText(report, strings.shareTitle)
                        note = if (copiesToClipboard) strings.messageCopied else null
                    },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
                ) { Text(strings.crashShare) }
                OutlinedButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                    modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
                ) { Text(strings.crashDelete) }
            }
            AnimatedVisibility(note != null) {
                Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
