package expo.modules.sadhanablocker.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import expo.modules.sadhanablocker.store.PrefsStore
import java.util.Calendar

/**
 * Night Lock + Morning Gate scheduling (BLOCKING_LOGIC §2, decision #5/#8).
 *
 * Night lock is evaluated live on every foreground change (time comparison — no alarm
 * needed). The only alarm we set is the daily reset at `night_lock_end`, which clears the
 * day's prayer count and the morning-gate flag. We use setExactAndAllowWhileIdle so Doze
 * cannot defer it (§11 #3).
 */
object ScheduleManager {

    private const val REQ_DAILY_RESET = 7001
    private const val ACTION_DAILY_RESET = "expo.modules.sadhanablocker.DAILY_RESET"

    /**
     * Is night lock currently in effect? Uses device-local time (decision #5, no clock
     * manipulation protection). Handles windows that wrap past midnight (22 → 6).
     */
    fun isNightLockActive(prefs: PrefsStore): Boolean {
        if (!prefs.nightLockEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = prefs.nightLockStartHour
        val end = prefs.nightLockEndHour
        if (start == end) return false
        return if (start < end) hour in start until end else (hour >= start || hour < end)
    }

    fun scheduleDailyReset(context: Context) {
        val prefs = PrefsStore.get(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrenceOfHour(prefs.nightLockEndHour)
        val pi = resetPendingIntent(context)

        // On A12+ exact alarms may be unavailable; degrade to a (possibly Doze-deferred)
        // inexact alarm rather than crashing.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun onDailyReset(context: Context) {
        PrefsStore.get(context).resetDailyCounters()
        scheduleDailyReset(context) // re-arm for tomorrow
    }

    private fun nextOccurrenceOfHour(hour: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun resetPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
            .setAction(ACTION_DAILY_RESET)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags = flags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQ_DAILY_RESET, intent, flags)
    }
}
