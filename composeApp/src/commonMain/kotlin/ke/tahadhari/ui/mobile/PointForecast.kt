package ke.tahadhari.ui.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ke.tahadhari.data.County
import ke.tahadhari.data.GeoPoint
import ke.tahadhari.domain.Language
import ke.tahadhari.domain.UserPoint
import ke.tahadhari.platform.LocationResult
import ke.tahadhari.platform.rememberLocationRequester
import ke.tahadhari.ui.*

/** Saves only the rounded point (about 1 km) across rotation; never a precise fix. */
internal val RoundedPointSaver = listSaver<GeoPoint?, Double>(
    save = { p -> p?.let { listOf(it.latitude, it.longitude) } ?: emptyList() },
    restore = { if (it.size == 2) GeoPoint(it[0], it[1]) else null },
)

/**
 * Says whose forecast is on screen ("Forecast for your location (approx.)" or "Forecast for X HQ")
 * and lets the user switch. Draws nothing where location is unsupported (desktop, web).
 * [hqCounty] must be the county with its HQ point; [onPoint] gets the rounded point or null for HQ.
 */
@Composable
internal fun PointForecastCard(
    hqCounty: County,
    point: GeoPoint?,
    language: Language,
    onPoint: (GeoPoint?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = Strings.of(language)
    var message by remember(hqCounty.code) { mutableStateOf<String?>(null) }
    var locating by remember(hqCounty.code) { mutableStateOf(false) }
    val currentStrings by rememberUpdatedState(strings)
    val request = rememberLocationRequester { result ->
        locating = false
        val s = currentStrings
        when (result) {
            is LocationResult.Found -> {
                val p = UserPoint.forCounty(GeoPoint(result.latitude, result.longitude), hqCounty)
                if (p != null) { message = null; onPoint(p) } else message = s.pointTooFar(hqCounty.name)
            }
            LocationResult.PermissionDenied -> message = s.locationDenied
            LocationResult.Unavailable -> message = s.locationUnavailable
        }
    } ?: return

    SectionCard(modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        SectionTitle(AppIcons.MyLocation, if (point != null) strings.pointForYou else strings.pointForHq(hqCounty.name))
        if (point != null) {
            Text(strings.pointRounded, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { message = null; onPoint(null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
            ) { Text(strings.pointBackToHq(hqCounty.name)) }
        } else {
            FilledTonalButton(
                onClick = { message = null; locating = true; request() },
                enabled = !locating,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
            ) {
                if (locating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(strings.locating)
                } else {
                    Text(strings.pointUse)
                }
            }
            Text(
                message ?: strings.pointRounded,
                style = MaterialTheme.typography.bodySmall,
                color = if (message != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
