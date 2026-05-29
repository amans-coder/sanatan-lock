package expo.modules.sadhanablocker.work

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import expo.modules.sadhanablocker.engine.BlockerEngine
import expo.modules.sadhanablocker.engine.BlockerRuntime
import expo.modules.sadhanablocker.overlay.OverlayController
import expo.modules.sadhanablocker.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UsageStatsManager fallback (BLOCKING_LOGIC §4 / §8). Used only when AccessibilityService
 * is unavailable (Google rejection, or user disabled it). Polls the foreground app on a
 * battery-friendly ~1.8s interval (decision #12) and drives the SAME [BlockerEngine] as the
 * accessibility path, but with a TYPE_APPLICATION_OVERLAY window (no a11y context here).
 */
class UsageStatsForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var usage: UsageStatsManager
    private var engine: BlockerEngine? = null
    private var lastForeground: String? = null

    override fun onCreate() {
        super.onCreate()
        usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val overlay = OverlayController(this, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        engine = BlockerEngine(this, overlay).also {
            it.loadConfig()
            BlockerRuntime.register(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannel(this)
        val notif = NotificationHelper.foregroundNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NotificationHelper.ID_FOREGROUND, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NotificationHelper.ID_FOREGROUND, notif)
        }
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                pollForegroundApp()?.let { pkg ->
                    if (pkg != lastForeground) {
                        lastForeground = pkg
                        engine?.onForeground(pkg)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** Latest package that moved to foreground in the recent window, or null. */
    private fun pollForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(now - LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var latestPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                latestPkg = event.packageName
            }
        }
        return latestPkg
    }

    override fun onDestroy() {
        engine?.let {
            it.dismiss()
            BlockerRuntime.unregister(it)
        }
        engine = null
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val POLL_INTERVAL_MS = 1_800L // decision #12 (~1.5–2s)
        private const val LOOKBACK_MS = 10_000L

        fun start(context: Context) {
            val intent = Intent(context, UsageStatsForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageStatsForegroundService::class.java))
        }
    }
}
