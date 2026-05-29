package expo.modules.sadhanablocker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import expo.modules.sadhanablocker.model.DetectionMethod
import expo.modules.sadhanablocker.schedule.ScheduleManager
import expo.modules.sadhanablocker.store.PrefsStore
import expo.modules.sadhanablocker.work.HealthCheckWorker
import expo.modules.sadhanablocker.work.UsageStatsForegroundService

/**
 * Re-arm the engine after reboot or app update (PLAN §3.3).
 *
 * The AccessibilityService is rebound automatically by the system if it was still enabled,
 * so here we only need to: re-schedule the daily reset alarm, re-enqueue the health check,
 * and (for the UsageStats fallback path) relaunch the polling service.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val prefs = PrefsStore.get(context)
        ScheduleManager.scheduleDailyReset(context)

        if (!prefs.serviceEnabled) return
        HealthCheckWorker.enqueue(context)

        if (prefs.detectionMethod == DetectionMethod.USAGE_STATS) {
            UsageStatsForegroundService.start(context)
        }
    }
}
