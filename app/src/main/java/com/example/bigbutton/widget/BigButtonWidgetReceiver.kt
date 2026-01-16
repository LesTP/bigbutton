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
        ResetAlarmScheduler.scheduleImmediateCheck(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel scheduled alarms when last widget is removed
        ResetAlarmScheduler.cancelScheduledReset(context)
    }
}
