package com.example.bigbutton.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.bigbutton.receiver.ResetAlarmScheduler

class BigButtonWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BigButtonWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule reset check when first widget is added
        // Wrapped in try-catch to prevent widget initialization failure
        try {
            ResetAlarmScheduler.scheduleImmediateCheck(context)
        } catch (e: Exception) {
            // Alarm scheduling failed (likely permission issue on Android 12+)
            // Widget will still work, just without automatic resets until permission granted
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel scheduled alarms when last widget is removed
        try {
            ResetAlarmScheduler.cancelScheduledReset(context)
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
}
