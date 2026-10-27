package ke.tahadhari.ui.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.Counties
import ke.tahadhari.data.County
import ke.tahadhari.domain.nearestCounty
import ke.tahadhari.platform.LocationResult
import ke.tahadhari.platform.rememberLocationRequester
import ke.tahadhari.ui.*

/** What the location card shows. */
internal sealed interface LocationUi {
    data object Idle : LocationUi
    data object Locating : LocationUi
    data class Suggest(val county: County) : LocationUi
    data class Message(val text: String) : LocationUi
}

/** Maps a platform result to what the card shows. Pure, so the decision is testable. */
internal fun locationUiFor(result: LocationResult, strings: Strings, counties: List<County> = Counties.all): LocationUi =
    when (result) {
        is LocationResult.Found -> nearestCounty(result.latitude, result.longitude, counties)
            ?.let { LocationUi.Suggest(it) }
            ?: LocationUi.Message(strings.locationOutsideKenya)
        LocationResult.PermissionDenied -> LocationUi.Message(strings.locationDenied)
        LocationResult.Unavailable -> LocationUi.Message(strings.locationUnavailable)
    }

/**
 * "Use my location" button that suggests the county whose HQ is nearest, and asks the user to
 * confirm (nearest HQ can be wrong near borders). Renders nothing where location is unsupported.
 */
@Composable
internal fun LocationCard(strings: Strings, onPick: (County) -> Unit, modifier: Modifier = Modifier) {
    var ui by remember { mutableStateOf<LocationUi>(LocationUi.Idle) }
    val currentStrings by rememberUpdatedState(strings)
    val request = rememberLocationRequester { result -> ui = locationUiFor(result, currentStrings) } ?: return

    AnimatedContent(ui, modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) { state ->
        when (state) {
            LocationUi.Idle, is LocationUi.Message -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilledTonalButton(
                    onClick = { ui = LocationUi.Locating; request() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                ) {
                    Icon(AppIcons.MyLocation, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(strings.useMyLocation)
                }
                val text = (state as? LocationUi.Message)?.text ?: strings.locationPrivacy
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state is LocationUi.Message) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.xs),
                )
            }
            LocationUi.Locating -> Row(
                Modifier.fillMaxWidth().heightIn(min = Spacing.touch).padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.md))
                Text(strings.locating, style = MaterialTheme.typography.bodyMedium)
            }
            is LocationUi.Suggest -> SectionCard {
                SectionTitle(AppIcons.MyLocation, strings.confirmCounty(state.county.name))
                Text(strings.confirmCountyNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Button(
                        onClick = { ui = LocationUi.Idle; onPick(state.county) },
                        modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
                    ) { Text(strings.confirmYes, textAlign = TextAlign.Center) }
                    OutlinedButton(
                        onClick = { ui = LocationUi.Idle },
                        modifier = Modifier.weight(1f).heightIn(min = Spacing.touch),
                    ) { Text(strings.confirmNo, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}
