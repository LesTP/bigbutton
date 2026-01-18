package com.example.bigbutton.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.bigbutton.util.ResetCalculator

/**
 * Utility object for scheduling reset alarms using AlarmManager.
 * Uses setExactAndAllowWhileIdle() for precise timing even in Doze mode.
 */
object ResetAlarmScheduler {

    private const val TAG = "ResetAlarmScheduler"
    private const val REQUEST_CODE = 1001

    /**
     * Schedules an exact alarm for the next occurrence of reset time.
     * Returns true if alarm was scheduled, false if permission denied.
     */
    fun scheduleNextReset(context: Context, resetHour: Int, resetMinute: Int): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
                return false
            }
        }

        val intent = createAlarmIntent(context)
        val triggerTime = ResetCalculator.calculateNextResetTimeFromNow(resetHour, resetMinute)

        Log.d(TAG, "Scheduling reset alarm for: $triggerTime (in ${(triggerTime - System.currentTimeMillis()) / 1000}s)")

        // Use setExactAndAllowWhileIdle for precise timing in Doze mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intent
        )
        return true
    }

    /**
     * Schedules an immediate alarm (for initial check on app/widget start).
     * Returns true if alarm was scheduled, false if permission denied.
     */
    fun scheduleImmediateCheck(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule immediate check - exact alarm permission not granted")
                return false
            }
        }

        val intent = createAlarmIntent(context)

        // Schedule for 1 second from now
        val triggerTime = System.currentTimeMillis() + 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intent
        )
        return true
    }

    /**
     * Cancels any scheduled reset alarm.
     */
    fun cancelScheduledReset(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createAlarmIntent(context)
        alarmManager.cancel(intent)
    }

    private fun createAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, ResetAlarmReceiver::class.java).apply {
            action = ResetAlarmReceiver.ACTION_RESET_CHECK
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
