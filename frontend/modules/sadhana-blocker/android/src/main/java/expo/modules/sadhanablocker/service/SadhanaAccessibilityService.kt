package expo.modules.sadhanablocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import expo.modules.sadhanablocker.engine.BlockerEngine
import expo.modules.sadhanablocker.engine.BlockerRuntime
import expo.modules.sadhanablocker.overlay.OverlayController
import expo.modules.sadhanablocker.schedule.ScheduleManager

/**
 * Primary detection path (PLAN §3.1). Listens for foreground-app changes and delegates the
 * decision to a shared [BlockerEngine] backed by a TYPE_ACCESSIBILITY_OVERLAY controller
 * (so the overlay survives the Recent Apps swipe — §6).
 *
 * The system binds/rebinds this service automatically (incl. after reboot, if still
 * enabled), so there is no manual start. We register the engine in [BlockerRuntime] on
 * connect so the JS bridge can drive it live (module-first IPC §1).
 */
class SadhanaAccessibilityService : AccessibilityService() {

    private var engine: BlockerEngine? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val overlay = OverlayController(this) // defaults to TYPE_ACCESSIBILITY_OVERLAY
        engine = BlockerEngine(this, overlay).also {
            it.loadConfig()
            BlockerRuntime.register(it)
        }
        ScheduleManager.scheduleDailyReset(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        engine?.onForeground(pkg)
    }

    override fun onInterrupt() { /* no spoken feedback */ }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        engine?.let {
            it.dismiss()
            BlockerRuntime.unregister(it)
        }
        engine = null
    }
}
