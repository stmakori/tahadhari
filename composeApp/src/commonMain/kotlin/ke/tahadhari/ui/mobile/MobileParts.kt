package ke.tahadhari.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.tahadhari.AppGraph
import ke.tahadhari.data.County
import ke.tahadhari.platform.copiesToClipboard
import ke.tahadhari.platform.shareText
import ke.tahadhari.ui.*

/**
 * A tickable guide step. Same look as the alert checklist (numbered circle that fills when done),
 * kept separate so guide screens can evolve without touching the shared detail components.
 */
@Composable
internal fun StepRow(number: Int, text: String, checked: Boolean, strings: Strings, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val circle by animateColorAsState(if (checked) scheme.primary else scheme.surfaceContainerLow)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touch)
            .clip(MaterialTheme.shapes.small)
            .toggleable(checked, role = Role.Checkbox, onValueChange = onChange)
            .semantics { stateDescription = strings.actionState(checked) }
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(circle).border(2.dp, scheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(AppIcons.Check, null, Modifier.size(18.dp), tint = scheme.onPrimary)
            else Text("$number", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f).padding(top = 2.dp),
        )
    }
}

/** Small info row in the style of the disclaimer, for "follow official advice" notes. */
@Composable
internal fun NoteRow(text: String, modifier: Modifier = Modifier, icon: ImageVector = AppIcons.Info) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, Modifier.size(18.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Round tinted icon used by guide rows and headers. */
@Composable
internal fun IconBubble(icon: ImageVector, container: Color, content: Color, size: Int = 48, modifier: Modifier = Modifier) {
    Box(modifier.size(size.dp).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size / 2).dp), tint = content)
    }
}

/** Builds and shares a check-in message; returns what to show afterwards (desktop clipboard note) or null. */
internal fun shareCheckIn(kind: CheckInKind, county: County?, strings: Strings): String? {
    shareText(checkInMessage(kind, county?.name, AppGraph.nowMillis(), strings), strings.shareTitle)
    return if (copiesToClipboard) strings.messageCopied else null
}

/** "Niko salama / I'm safe" card with two buttons that open the share sheet. */
@Composable
internal fun CheckInCard(county: County?, strings: Strings, modifier: Modifier = Modifier) {
    var note by remember { mutableStateOf<String?>(null) }
    SectionCard(modifier) {
        SectionTitle(AppIcons.Share, strings.checkInTitle)
        Text(strings.checkInBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Stacked full width so long labels ("Nahitaji msaada") never get cut off.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = { note = shareCheckIn(CheckInKind.SAFE, county, strings) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.imSafe)
            }
            FilledTonalButton(
                onClick = { note = shareCheckIn(CheckInKind.NEED_HELP, county, strings) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Warning, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.needHelp)
            }
        }
        AnimatedVisibility(note != null) {
            Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Compact check-in row for the bottom of the Alerts detail. */
@Composable
internal fun CheckInRow(county: County, strings: Strings, modifier: Modifier = Modifier) {
    var note by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(strings.checkInShort, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = { note = shareCheckIn(CheckInKind.SAFE, county, strings) },
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Share, null, Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.imSafe, textAlign = TextAlign.Center)
            }
            OutlinedButton(
                onClick = { note = shareCheckIn(CheckInKind.NEED_HELP, county, strings) },
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
            ) {
                Icon(AppIcons.Share, null, Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.needHelp, textAlign = TextAlign.Center)
            }
        }
        AnimatedVisibility(note != null) {
            Text(note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
