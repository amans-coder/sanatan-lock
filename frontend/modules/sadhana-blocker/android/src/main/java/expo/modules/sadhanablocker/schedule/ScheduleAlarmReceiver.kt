package expo.modules.sadhanablocker.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires at night_lock_end → reset daily counters + morning-gate flag, then re-arm. */
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ScheduleManager.onDailyReset(context)
    }
}
