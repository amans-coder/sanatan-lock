package expo.modules.sadhanablocker.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import expo.modules.sadhanablocker.R

/** Single status channel for the health nudge + the fallback foreground service. */
object NotificationHelper {

    const val CHANNEL_ID = "sadhana_status"
    const val ID_SERVICE_STOPPED = 9101
    const val ID_FOREGROUND = 9102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sadhana_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.sadhana_channel_desc) }
        mgr.createNotificationChannel(channel)
    }

    /** Tapping opens the host app so the user can re-enable the lock. */
    private fun openAppIntent(context: Context): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, launch, flags)
    }

    fun serviceStoppedNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(context.getString(R.string.sadhana_service_stopped_title))
            .setContentText(context.getString(R.string.sadhana_service_stopped_text))
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()

    fun foregroundNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(context.getString(R.string.sadhana_accessibility_label))
            .setContentText(context.getString(R.string.sadhana_accessibility_summary))
            .setOngoing(true)
            .build()

    fun notifyServiceStopped(context: Context) {
        ensureChannel(context)
        context.getSystemService(NotificationManager::class.java)
            .notify(ID_SERVICE_STOPPED, serviceStoppedNotification(context))
    }
}
