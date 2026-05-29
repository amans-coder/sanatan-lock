package expo.modules.sadhanablocker.store

import android.content.Context
import android.content.SharedPreferences
import expo.modules.sadhanablocker.model.BlockMode
import expo.modules.sadhanablocker.model.DetectionMethod

/**
 * Persistence layer (BLOCKING_LOGIC §8). Single file `sadhana_blocker_prefs`.
 *
 * This is NOT the runtime communication channel — the module talks to the live service
 * directly (BLOCKING_LOGIC §1). SharedPreferences exists so the service can restore the
 * last-known config on a cold start after process death / reboot.
 *
 * Threading: a single process is assumed (§4). Use [commitNow] only where a write MUST
 * survive an immediate force-stop (morning-gate flag, §11 failure mode #6).
 */
class PrefsStore private constructor(private val prefs: SharedPreferences) {

    // ---- Module-written config (read by service on cold start) ----
    var blockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED, value).apply()

    var mode: BlockMode
        get() = BlockMode.from(prefs.getString(KEY_MODE, BlockMode.GENTLE.wire))
        set(value) = prefs.edit().putString(KEY_MODE, value.wire).apply()

    var selectedDeity: String
        get() = prefs.getString(KEY_DEITY, "krishna") ?: "krishna"
        set(value) = prefs.edit().putString(KEY_DEITY, value).apply()

    var overlayContentJson: String?
        get() = prefs.getString(KEY_OVERLAY_JSON, null)
        set(value) = prefs.edit().putString(KEY_OVERLAY_JSON, value).apply()

    var serviceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var detectionMethod: DetectionMethod
        get() = DetectionMethod.from(prefs.getString(KEY_DETECTION, DetectionMethod.ACCESSIBILITY.wire))
        set(value) = prefs.edit().putString(KEY_DETECTION, value.wire).apply()

    var cooldownMinutes: Int
        get() = prefs.getInt(KEY_COOLDOWN_MIN, DEFAULT_COOLDOWN_MIN)
        set(value) = prefs.edit().putInt(KEY_COOLDOWN_MIN, value).apply()

    // ---- Night-lock config (read by ScheduleManager) ----
    var nightLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_ENABLED, value).apply()

    var nightLockStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_START, DEFAULT_NIGHT_START)
        set(value) = prefs.edit().putInt(KEY_NIGHT_START, value).apply()

    var nightLockEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_END, DEFAULT_NIGHT_END)
        set(value) = prefs.edit().putInt(KEY_NIGHT_END, value).apply()

    // ---- Service-written stats (read by module) ----
    var lastPrayerTs: Long
        get() = prefs.getLong(KEY_LAST_PRAYER, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PRAYER, value).apply()

    var prayerCountToday: Int
        get() = prefs.getInt(KEY_PRAYER_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_PRAYER_COUNT, value).apply()

    /**
     * Morning-gate flag (BLOCKING_LOGIC §2). Written with commit() because it must
     * survive an immediate force-stop (§11 #6) — an apply() could be lost in the crash.
     */
    var morningPrayerDoneToday: Boolean
        get() = prefs.getBoolean(KEY_MORNING_DONE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MORNING_DONE, value).commit()
        }

    /** Reset daily counters; called by ScheduleManager at night_lock_end. */
    fun resetDailyCounters() {
        prefs.edit()
            .putInt(KEY_PRAYER_COUNT, 0)
            .putBoolean(KEY_MORNING_DONE, false)
            .commit()
    }

    companion object {
        private const val FILE = "sadhana_blocker_prefs"

        const val KEY_BLOCKED = "blocked_packages"
        const val KEY_MODE = "mode"
        const val KEY_DEITY = "selected_deity"
        const val KEY_OVERLAY_JSON = "overlay_content_json"
        const val KEY_SERVICE_ENABLED = "service_enabled"
        const val KEY_LAST_PRAYER = "last_prayer_ts"
        const val KEY_PRAYER_COUNT = "prayer_count_today"
        const val KEY_NIGHT_ENABLED = "night_lock_enabled"
        const val KEY_NIGHT_START = "night_lock_start_hour"
        const val KEY_NIGHT_END = "night_lock_end_hour"
        const val KEY_MORNING_DONE = "morning_prayer_done_today"
        const val KEY_COOLDOWN_MIN = "cooldown_minutes"
        const val KEY_DETECTION = "detection_method"

        const val DEFAULT_COOLDOWN_MIN = 15      // PROGRESS decision #6
        const val DEFAULT_NIGHT_START = 22       // 10 PM (decision #5)
        const val DEFAULT_NIGHT_END = 6          // 6 AM

        @Volatile
        private var instance: PrefsStore? = null

        fun get(context: Context): PrefsStore =
            instance ?: synchronized(this) {
                instance ?: PrefsStore(
                    context.applicationContext
                        .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
    }
}
