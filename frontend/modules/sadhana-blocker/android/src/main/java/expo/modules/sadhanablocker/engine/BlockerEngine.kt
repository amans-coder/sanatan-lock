package expo.modules.sadhanablocker.engine

import android.content.Context
import expo.modules.sadhanablocker.model.BlockMode
import expo.modules.sadhanablocker.model.OverlayContent
import expo.modules.sadhanablocker.model.OverlayState
import expo.modules.sadhanablocker.model.ProtectedPackages
import expo.modules.sadhanablocker.overlay.OverlayController
import expo.modules.sadhanablocker.schedule.ScheduleManager
import expo.modules.sadhanablocker.store.PrefsStore
import org.json.JSONObject

/**
 * The overlay state machine (BLOCKING_LOGIC §2), extracted so BOTH detection paths drive
 * identical behaviour:
 *  - SadhanaAccessibilityService (primary) → engine with a TYPE_ACCESSIBILITY_OVERLAY controller
 *  - UsageStatsForegroundService (fallback §4) → engine with a TYPE_APPLICATION_OVERLAY controller
 *
 *      IDLE → CHECK_TIME → { NIGHT_LOCK | MORNING_GATE | PRAYER_SHOWN } → COOLDOWN → IDLE
 */
class BlockerEngine(
    private val context: Context,
    private val overlay: OverlayController
) {
    private val prefs = PrefsStore.get(context)

    @Volatile var state: OverlayState = OverlayState.IDLE; private set
    private var currentTarget: String? = null
    private var overlayShownAt = 0L

    // Hot-path config mirror, refreshed by [loadConfig] on cold start and every JS set*().
    @Volatile private var enabled = false
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var mode: BlockMode = BlockMode.GENTLE
    @Volatile private var content: OverlayContent? = null

    // Per-package cooldown expiry, wall-clock ms (decision #6).
    private val cooldownUntil = HashMap<String, Long>()

    /** Process the current foreground package through the state machine. */
    fun onForeground(pkg: String) {
        if (pkg == context.packageName) return // ignore our own UI / overlay window

        if (!enabled) { dismiss(); return }

        // Left the target (home / app switch) → tear the overlay down.
        if (pkg != currentTarget && state != OverlayState.IDLE) dismiss()

        if (pkg !in blocked || ProtectedPackages.isProtected(context, pkg)) return

        // Active cooldown → let the user in.
        if (System.currentTimeMillis() < (cooldownUntil[pkg] ?: 0L)) {
            currentTarget = pkg
            state = OverlayState.COOLDOWN
            return
        }

        // Already prompting for THIS app — ignore the chatty repeat window events that real
        // apps fire (dialogs, tab switches, animations). Without this the strict-mode timer
        // resets forever (Om never enables), events spam JS, and durationMs is wrong.
        // Note: COOLDOWN is intentionally NOT guarded so cooldown expiry re-shows; a switch
        // to a different blocked app hits the pkg != currentTarget dismiss path above.
        if (pkg == currentTarget &&
            (state == OverlayState.PRAYER_SHOWN ||
                state == OverlayState.NIGHT_LOCK ||
                state == OverlayState.MORNING_GATE)
        ) {
            return
        }

        // Night lock — hard block, highest priority.
        if (ScheduleManager.isNightLockActive(prefs)) {
            currentTarget = pkg
            state = OverlayState.NIGHT_LOCK
            overlay.showNightLock()
            BlockerRuntime.eventSink?.onOverlayShown(pkg, System.currentTimeMillis())
            return
        }

        // Morning gate — block until today's first in-app prayer (decision #8). Cleared by
        // JS calling markMorningPrayerDone() (decision #16); reset nightly by ScheduleManager.
        // Gated behind the schedule feature (nightLockEnabled) so it's opt-in.
        if (prefs.nightLockEnabled && !prefs.morningPrayerDoneToday) {
            currentTarget = pkg
            state = OverlayState.MORNING_GATE
            overlay.showMorningGate()
            BlockerRuntime.eventSink?.onOverlayShown(pkg, System.currentTimeMillis())
            return
        }

        // Prayer overlay.
        currentTarget = pkg
        state = OverlayState.PRAYER_SHOWN
        overlayShownAt = System.currentTimeMillis()
        try {
            overlay.showPrayer(content, mode) { onPrayerDone(pkg) }
            BlockerRuntime.eventSink?.onOverlayShown(pkg, overlayShownAt)
        } catch (t: Throwable) {
            // addView threw — overlay permission likely revoked mid-session (§11 #2).
            // Fail open so we never trap the user; HealthCheckWorker nudges re-grant.
            state = OverlayState.IDLE
            currentTarget = null
        }
    }

    private fun onPrayerDone(pkg: String) {
        val now = System.currentTimeMillis()
        prefs.lastPrayerTs = now
        prefs.prayerCountToday = prefs.prayerCountToday + 1
        cooldownUntil[pkg] = now + prefs.cooldownMinutes * 60_000L
        overlay.dismiss()
        state = OverlayState.COOLDOWN
        BlockerRuntime.eventSink?.onPrayerCompleted(pkg, now - overlayShownAt, now)
    }

    /** Re-read config from prefs (cold start + after every JS mutation). */
    fun loadConfig() {
        enabled = prefs.serviceEnabled
        blocked = prefs.blockedPackages
        mode = prefs.mode
        content = parseContent(prefs.overlayContentJson)
        if (!enabled) dismiss()
    }

    /** debugShowOverlay — force the prayer overlay now, bypassing all checks. */
    fun showOverlayNow() {
        currentTarget = context.packageName
        state = OverlayState.PRAYER_SHOWN
        overlayShownAt = System.currentTimeMillis()
        overlay.showPrayer(content, mode) {
            overlay.dismiss()
            state = OverlayState.IDLE
        }
    }

    fun dismiss() {
        if (overlay.isShowing) overlay.dismiss()
        state = OverlayState.IDLE
        currentTarget = null
    }

    private fun parseContent(json: String?): OverlayContent? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val o = JSONObject(json)
            OverlayContent(
                deity = o.optString("deity"),
                mantraDevanagari = o.optString("mantraDevanagari"),
                mantraTransliteration = o.optString("mantraTransliteration"),
                mantraMeaning = o.optString("mantraMeaning"),
                bgColorHex = o.optString("bgColorHex", "#1A1033"),
                deityImageBase64 = o.optString("deityImageBase64")
            )
        }.getOrNull()
    }
}
