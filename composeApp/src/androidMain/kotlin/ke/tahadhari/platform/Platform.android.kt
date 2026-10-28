package ke.tahadhari.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import ke.tahadhari.widget.WidgetRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Holds the application context for intents. Call [init] from `MainActivity.onCreate`. */
object AndroidPlatform {
    @Volatile internal var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

private fun startSafely(intent: Intent) {
    val context = AndroidPlatform.appContext ?: return
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No dialer / share target (e.g. some tablets): nothing sensible to do.
    }
}

actual fun dialNumber(number: String) = startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))

actual fun shareText(text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startSafely(Intent.createChooser(send, title))
}

actual val copiesToClipboard: Boolean = false

@Composable
actual fun rememberLocationRequester(onResult: (LocationResult) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    fun fetch() = scope.launch { callback(currentLocation(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetch() else callback(LocationResult.PermissionDenied)
    }
    return remember(context) {
        {
            val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (granted) fetch() else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}

/** One coarse fix from the platform LocationManager (no Play Services). Needs the permission granted. */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): LocationResult = withContext(Dispatchers.Default) {
    val lm = context.getSystemService(LocationManager::class.java) ?: return@withContext LocationResult.Unavailable
    try {
        // Only providers this app may use and that are switched on.
        val providers = lm.getProviders(true)
        if (providers.isEmpty()) return@withContext LocationResult.Unavailable
        var fix: Location? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // "fused" is LocationManager.FUSED_PROVIDER (API 31); the plain string is fine on API 30.
            val provider = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .firstOrNull { it in providers } ?: providers.first()
            fix = withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
                }
            }
        }
        // Fallback (and the only path below API 30): freshest last known fix.
        if (fix == null) {
            fix = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        }
        fix?.let { LocationResult.Found(it.latitude, it.longitude) } ?: LocationResult.Unavailable
    } catch (_: SecurityException) {
        LocationResult.PermissionDenied
    } catch (_: Exception) {
        LocationResult.Unavailable
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true

@Composable
actual fun rememberNotificationRequester(onResult: (allowed: Boolean) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        callback(granted && notificationsAllowed(context))
    }
    return remember(context) {
        {
            val needsAsking = Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsAsking) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else callback(notificationsAllowed(context))
        }
    }
}

actual fun onRiskAlertsChanged(enabled: Boolean) {
    val context = AndroidPlatform.appContext ?: return
    WidgetRefresh.syncSchedule(context, riskAlerts = enabled)
}
