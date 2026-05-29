package expo.modules.sadhanablocker.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import expo.modules.sadhanablocker.engine.BlockerRuntime
import expo.modules.sadhanablocker.store.PrefsStore
import expo.modules.sadhanablocker.util.NotificationHelper
import expo.modules.sadhanablocker.util.PermissionHelper
import java.util.concurrent.TimeUnit

/**
 * Resilience against OEM kills (PLAN §3.4, BLOCKING_LOGIC §11 #1). Runs periodically; if the
 * user expects blocking (service_enabled) but the engine isn't live — or accessibility was
 * silently disabled — it posts a notification nudging the user to re-enable.
 *
 * It cannot force-restart an AccessibilityService (only the user/system can), but for the
 * UsageStats fallback path it can re-launch the polling service.
 */
class HealthCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PrefsStore.get(applicationContext)
        if (!prefs.serviceEnabled) return Result.success() // user paused — nothing to guard

        val usingAccessibility =
            prefs.detectionMethod == expo.modules.sadhanablocker.model.DetectionMethod.ACCESSIBILITY
        val accessibilityOk =
            !usingAccessibility || PermissionHelper.isAccessibilityEnabled(applicationContext)

        if (BlockerRuntime.isRunning() && accessibilityOk) {
            return Result.success() // healthy
        }

        // Unhealthy: engine down or accessibility revoked.
        if (!usingAccessibility &&
            PermissionHelper.isOverlayGranted(applicationContext) &&
            PermissionHelper.isUsageAccessGranted(applicationContext)
        ) {
            // Fallback path can self-heal — relaunch the polling service. Starting an FGS
            // from a background Worker can throw ForegroundServiceStartNotAllowedException
            // on A12+; swallow it (we'll retry next cycle) rather than fail the worker.
            runCatching { UsageStatsForegroundService.start(applicationContext) }
        } else {
            NotificationHelper.notifyServiceStopped(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sadhana_health_check"

        /** Enqueue the ~15-min periodic check (WorkManager's minimum interval). */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
