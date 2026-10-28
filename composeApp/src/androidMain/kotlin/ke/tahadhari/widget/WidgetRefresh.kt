package ke.tahadhari.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ke.tahadhari.AppGraph
import ke.tahadhari.data.Counties
import ke.tahadhari.data.store.AndroidStorage
import ke.tahadhari.platform.AndroidPlatform
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.loadCounty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * The widget can run (receiver, worker) without MainActivity ever starting, so storage must be
 * set up here too. Both inits are idempotent. Always call before touching AppGraph.
 */
internal object WidgetEnv {
    fun init(context: Context) {
        AndroidStorage.init(context)
        AndroidPlatform.init(context)
    }
}

/** Tiny SharedPreferences record of what the last refresh did. Not forecast data. */
internal object WidgetStatus {
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("tahadhari_widget", Context.MODE_PRIVATE)

    /** True when the last refresh attempt failed and nothing usable is saved. */
    fun lastFailed(context: Context): Boolean = prefs(context).getBoolean("lastFailed", false)
    fun setLastFailed(context: Context, failed: Boolean) = prefs(context).edit().putBoolean("lastFailed", failed).apply()

    /** "countyCode:language" the widget was last refreshed for. */
    fun lastSeen(context: Context): String? = prefs(context).getString("lastSeen", null)
    fun setLastSeen(context: Context, key: String) = prefs(context).edit().putString("lastSeen", key).apply()
}

internal object WidgetRefresh {
    private const val PERIODIC = "tahadhari-widget-periodic"
    private const val ONE_OFF = "tahadhari-widget-now"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every 3 hours, only with a network connection. Kept if already scheduled. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(3, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * One immediate refresh. No network constraint on purpose: offline it fails fast, so the widget
     * shows "No data" with a retry instead of "Loading" forever.
     */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC)
        wm.cancelUniqueWork(ONE_OFF)
    }

    /**
     * Called from MainActivity.onStop. If the user changed county or language while the app was
     * open, refresh the widget now. Does nothing when no widget is placed or nothing changed, so
     * leaving the app normally costs no network.
     */
    fun onAppStopped(context: Context) {
        val app = context.applicationContext
        scope.launch {
            WidgetEnv.init(app)
            if (GlanceAppWidgetManager(app).getGlanceIds(TahadhariWidget::class.java).isEmpty()) return@launch
            val prefs = AppGraph.preferences.load()
            if (seenKey(prefs.countyCode, prefs.language.name) != WidgetStatus.lastSeen(app)) {
                WidgetStatus.setLastFailed(app, false)
                refreshNow(app)
            }
        }
    }

    fun seenKey(countyCode: Int?, language: String) = "$countyCode:$language"
}

/** Downloads the saved county's forecast (which also updates the cache), then redraws every widget. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        WidgetEnv.init(context)
        val prefs = AppGraph.preferences.load()
        WidgetStatus.setLastSeen(context, WidgetRefresh.seenKey(prefs.countyCode, prefs.language.name))
        val county = prefs.countyCode?.let { Counties.byCode(it) }
        if (county != null) {
            // loadCounty falls back to the saved forecast on network errors; Failed means nothing usable.
            WidgetStatus.setLastFailed(context, loadCounty(county) is LoadState.Failed)
        }
        TahadhariWidget().updateAll(context)
        return Result.success()
    }
}
