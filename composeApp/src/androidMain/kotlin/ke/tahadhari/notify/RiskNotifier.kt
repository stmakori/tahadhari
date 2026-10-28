package ke.tahadhari.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ke.tahadhari.AppGraph
import ke.tahadhari.MainActivity
import ke.tahadhari.R
import ke.tahadhari.data.County
import ke.tahadhari.domain.Language
import ke.tahadhari.ui.LoadState
import ke.tahadhari.ui.Strings
import ke.tahadhari.ui.mobile.RiskAlertNotification
import ke.tahadhari.ui.mobile.RiskAlertState
import ke.tahadhari.ui.mobile.checkRiskAlerts

/** Posts the opt-in "flood risk rose to High or Severe" notification. All decisions live in commonMain. */
internal object RiskNotifier {
    private const val CHANNEL_ID = "flood_risk_alerts"
    private const val NOTIFICATION_ID = 4100 // + county code: one slot per county, a newer rise replaces that county's older notification

    /** True when notifications may be shown right now (permission on Android 13+, and not switched off). */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true
    }

    /**
     * Applies the rules to a refresh result and posts at most one notification.
     * Without permission nothing is checked, so the level is not marked as notified.
     */
    suspend fun checkAndNotify(context: Context, counties: List<County>, loaded: Map<Int, LoadState>, language: Language) {
        if (!canPost(context)) return
        for ((county, notification) in checkRiskAlerts(counties, loaded, language, RiskAlertState(AppGraph.store))) {
            post(context, notification, language, NOTIFICATION_ID + county.code)
        }
    }

    private fun post(context: Context, content: RiskAlertNotification, language: Language, id: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val strings = Strings.of(language)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, strings.notifChannelName, NotificationManager.IMPORTANCE_HIGH)
            channel.description = strings.notifChannelDesc
            manager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL_ID)
        else Notification.Builder(context).setPriority(Notification.PRIORITY_HIGH)
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(Notification.BigTextStyle().bigText(content.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and now: nothing to do.
        }
    }
}
