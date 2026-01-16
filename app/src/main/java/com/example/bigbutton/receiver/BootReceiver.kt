package com.example.bigbutton.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.bigbutton.widget.BigButtonStateDefinition
import com.example.bigbutton.widget.BigButtonWidget
import com.example.bigbutton.widget.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles device boot completion.
 * Reschedules the reset alarm and refreshes widgets after device restart.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Read reset time from DataStore and reschedule alarm
                val prefs = context.dataStore.data.first()
                val resetHour = prefs[BigButtonStateDefinition.Keys.RESET_HOUR]
                    ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR
                val resetMinute = prefs[BigButtonStateDefinition.Keys.RESET_MINUTE]
                    ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE

                ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)

                // Refresh all widget instances
                val manager = GlanceAppWidgetManager(context)
                val widgetIds = manager.getGlanceIds(BigButtonWidget::class.java)

                widgetIds.forEach { glanceId ->
                    BigButtonWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during boot refresh", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
