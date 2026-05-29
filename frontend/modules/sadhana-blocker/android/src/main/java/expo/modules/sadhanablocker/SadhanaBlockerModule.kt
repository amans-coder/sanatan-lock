package expo.modules.sadhanablocker

import android.content.Context
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.sadhanablocker.engine.BlockerRuntime
import expo.modules.sadhanablocker.model.BlockMode
import expo.modules.sadhanablocker.model.DetectionMethod
import expo.modules.sadhanablocker.model.OverlayState
import expo.modules.sadhanablocker.schedule.ScheduleManager
import expo.modules.sadhanablocker.store.PrefsStore
import expo.modules.sadhanablocker.util.AppListHelper
import expo.modules.sadhanablocker.util.NotificationHelper
import expo.modules.sadhanablocker.util.PermissionHelper
import expo.modules.sadhanablocker.work.HealthCheckWorker
import expo.modules.sadhanablocker.work.UsageStatsForegroundService
import org.json.JSONObject

/**
 * Expo Module bridge (PLAN §3.8, §4.2). The ONLY surface JS talks to.
 *
 * Pattern: every set*() writes config to PrefsStore (crash-recovery backup) AND pushes it
 * into the live engine via BlockerRuntime (module-first IPC §1). Events flow back through a
 * [BlockerEventSink] we install on create.
 */
class SadhanaBlockerModule : Module() {

    private val context: Context
        get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

    private val prefs: PrefsStore
        get() = PrefsStore.get(context)

    override fun definition() = ModuleDefinition {
        Name("SadhanaBlocker")

        Events("onOverlayShown", "onPrayerCompleted", "onServiceStateChanged")

        OnCreate {
            BlockerRuntime.eventSink = object : BlockerEventSink {
                override fun onOverlayShown(packageName: String, timestamp: Long) {
                    sendEvent("onOverlayShown", mapOf("packageName" to packageName, "timestamp" to timestamp))
                }
                override fun onPrayerCompleted(packageName: String, durationMs: Long, timestamp: Long) {
                    sendEvent(
                        "onPrayerCompleted",
                        mapOf("packageName" to packageName, "durationMs" to durationMs, "timestamp" to timestamp)
                    )
                }
                override fun onServiceStateChanged(isRunning: Boolean) {
                    sendEvent("onServiceStateChanged", mapOf("isRunning" to isRunning))
                }
            }
        }

        OnDestroy { BlockerRuntime.eventSink = null }

        // ---- Permissions ----
        AsyncFunction("requestAccessibilityPermission") { PermissionHelper.openAccessibilitySettings(context) }
        AsyncFunction("isAccessibilityEnabled") { PermissionHelper.isAccessibilityEnabled(context) }
        AsyncFunction("requestOverlayPermission") { PermissionHelper.requestOverlay(context) }
        AsyncFunction("isOverlayPermissionGranted") { PermissionHelper.isOverlayGranted(context) }
        AsyncFunction("requestIgnoreBatteryOptimization") { PermissionHelper.requestIgnoreBatteryOptimization(context) }
        AsyncFunction("isBatteryOptimizationIgnored") { PermissionHelper.isBatteryOptimizationIgnored(context) }

        // ---- App discovery ----
        AsyncFunction("getInstalledApps") {
            AppListHelper.getInstalledApps(context).map {
                mapOf(
                    "packageName" to it.packageName,
                    "appName" to it.appName,
                    "iconBase64" to null,          // paginated — fetch via getAppIcon (§3)
                    "isSystem" to it.isSystem
                )
            }
        }
        AsyncFunction("getAppIcon") { packageName: String ->
            AppListHelper.getAppIcon(context, packageName)
        }

        // ---- Configuration ----
        AsyncFunction("setBlockedApps") { packages: List<String> ->
            // Defence in depth: never persist a denylisted package (§7, §11 #8).
            prefs.blockedPackages = packages.filterNot {
                expo.modules.sadhanablocker.model.ProtectedPackages.isProtected(context, it)
            }.toSet()
            BlockerRuntime.activeEngine?.loadConfig()
        }
        AsyncFunction("getBlockedApps") { prefs.blockedPackages.toList() }

        AsyncFunction("setBlockingMode") { mode: String ->
            prefs.mode = BlockMode.from(mode)
            BlockerRuntime.activeEngine?.loadConfig()
        }
        AsyncFunction("getBlockingMode") { prefs.mode.wire }

        // ---- Content handoff ----
        AsyncFunction("setOverlayContent") { content: Map<String, Any?> ->
            prefs.overlayContentJson = JSONObject(content).toString()
            BlockerRuntime.activeEngine?.loadConfig()
        }
        AsyncFunction("setSelectedDeity") { deityId: String ->
            prefs.selectedDeity = deityId
        }

        // ---- Morning gate (decision #8/#16) ----
        AsyncFunction("markMorningPrayerDone") {
            prefs.morningPrayerDoneToday = true // commit() — survives force-stop (§11 #6)
            // If a morning-gate overlay is currently up, tear it down so the user is freed
            // immediately; the next foreground event will pass the (now-cleared) gate.
            BlockerRuntime.activeEngine?.let { engine ->
                if (engine.state == OverlayState.MORNING_GATE) engine.dismiss()
            }
        }

        // ---- Lifecycle ----
        AsyncFunction("startBlockerService") {
            prefs.serviceEnabled = true
            NotificationHelper.ensureChannel(context)
            HealthCheckWorker.enqueue(context)
            ScheduleManager.scheduleDailyReset(context)
            if (prefs.detectionMethod == DetectionMethod.USAGE_STATS) {
                UsageStatsForegroundService.start(context)
            }
            BlockerRuntime.activeEngine?.loadConfig()
        }
        AsyncFunction("stopBlockerService") {
            prefs.serviceEnabled = false
            HealthCheckWorker.cancel(context)
            UsageStatsForegroundService.stop(context)
            BlockerRuntime.activeEngine?.let {
                it.loadConfig()
                it.dismiss()
            }
        }
        AsyncFunction("getBlockerState") {
            mapOf(
                "isAccessibilityEnabled" to PermissionHelper.isAccessibilityEnabled(context),
                "isOverlayPermissionGranted" to PermissionHelper.isOverlayGranted(context),
                "isBatteryOptimizationIgnored" to PermissionHelper.isBatteryOptimizationIgnored(context),
                "blockedPackages" to prefs.blockedPackages.toList(),
                "mode" to prefs.mode.wire,
                "selectedDeity" to prefs.selectedDeity
            )
        }

        // ---- Manual trigger (testing) ----
        AsyncFunction("debugShowOverlay") {
            val engine = BlockerRuntime.activeEngine ?: throw EngineNotRunningException()
            engine.showOverlayNow()
        }
    }
}

/**
 * Thrown by debugShowOverlay when no detection path is live. Uses the single-arg
 * CodedException constructor (always public); the JS error code is derived from the class
 * name → "ERR_ENGINE_NOT_RUNNING".
 */
class EngineNotRunningException : CodedException(
    "Blocker engine is not running. Enable the Accessibility service (or the UsageStats " +
        "fallback) before calling debugShowOverlay()."
)
