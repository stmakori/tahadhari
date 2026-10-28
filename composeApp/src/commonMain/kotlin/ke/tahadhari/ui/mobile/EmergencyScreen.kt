package ke.tahadhari.ui.mobile

import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.data.guides.EmergencyContact
import ke.tahadhari.data.guides.EmergencyContacts
import ke.tahadhari.data.guides.EmergencyService
import ke.tahadhari.domain.Language
import ke.tahadhari.platform.copiesToClipboard
import ke.tahadhari.platform.dialNumber
import ke.tahadhari.ui.*

/**
 * Emergency tab (Dharura): verified national numbers (tap opens the dialer), the
 * "Niko salama / I'm safe" check-in, and the chosen county's known flood-prone areas.
 *
 * @param onChooseCounty sends the user to the county picker when no county is chosen yet.
 */
@Composable
fun EmergencyScreen(
    strings: Strings,
    language: Language,
    county: County?,
    onChooseCounty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var copied by remember { mutableStateOf<String?>(null) }
    Column(
        modifier.testTag("emergency_screen").fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenIntro(AppIcons.Phone, strings.emergencyTitle, strings.emergencyBody, Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm))
        SectionCard {
            SectionTitle(AppIcons.Phone, strings.emergencyNumbersTitle)
            Text(strings.emergencyCallHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            EmergencyContacts.national.forEach { contact ->
                NumberRow(contact, strings) {
                    dialNumber(contact.number)
                    copied = if (copiesToClipboard) strings.numberCopied(contact.number) else null
                }
            }
            AnimatedVisibility(copied != null) {
                Text(copied.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        CheckInCard(county, strings)
        HotspotsCard(county, strings, onChooseCounty)
        NoteRow(strings.emergencyOfficialNote, icon = AppIcons.Warning)
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun NumberRow(contact: EmergencyContact, strings: Strings, onCall: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val primaryLine = contact.service == EmergencyService.POLICE_FIRE_AMBULANCE
    val label = strings.emergencyService(contact.service)
    Surface(
        onClick = onCall,
        shape = MaterialTheme.shapes.medium,
        color = if (primaryLine) scheme.errorContainer else scheme.surfaceContainerLowest,
        contentColor = if (primaryLine) scheme.onErrorContainer else scheme.onSurface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clearAndSetSemantics {
            role = Role.Button
            contentDescription = strings.callNumber(contact.number, label)
        },
    ) {
        Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Text(
                contact.number,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.widthIn(min = 84.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    strings.emergencyServiceDetail(contact.service),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                )
            }
            Box(
                Modifier.size(Spacing.touch).clip(CircleShape)
                    .background(if (primaryLine) scheme.error else scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Phone, null, Modifier.size(22.dp), tint = if (primaryLine) scheme.onError else scheme.onPrimary)
            }
        }
    }
}

@Composable
private fun HotspotsCard(county: County?, strings: Strings, onChooseCounty: () -> Unit) {
    SectionCard {
        if (county == null) {
            SectionTitle(AppIcons.Waves, strings.hotspotsPickCounty)
            OutlinedButton(onClick = onChooseCounty, modifier = Modifier.heightIn(min = Spacing.touch)) {
                Icon(AppIcons.Location, null, Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(strings.chooseCounty)
            }
            return@SectionCard
        }
        SectionTitle(AppIcons.Waves, strings.hotspotsTitle(county.name))
        if (county.hotspots.isEmpty()) {
            Text(strings.hotspotsNone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(strings.hotspotsBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            county.hotspots.forEach { place ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Warning, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(place, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
