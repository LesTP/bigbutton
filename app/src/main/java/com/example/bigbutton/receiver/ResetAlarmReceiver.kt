package com.example.bigbutton.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.bigbutton.data.BigButtonDatabase
import com.example.bigbutton.data.FinalizedDay
import com.example.bigbutton.data.TrackingMetadata
import com.example.bigbutton.util.ResetCalculator
import com.example.bigbutton.widget.BigButtonStateDefinition
import java.time.Instant
import java.time.ZoneId
import com.example.bigbutton.widget.BigButtonWidget
import com.example.bigbutton.widget.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles reset alarms.
 * Called by AlarmManager at the scheduled reset time.
 */
class ResetAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESET_CHECK = "com.example.bigbutton.ACTION_RESET_CHECK"
        private const val TAG = "ResetAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESET_CHECK) return

        // Use goAsync() for coroutine work in BroadcastReceiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                performResetCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun performResetCheck(context: Context) {
        // Read current state
        val prefs = context.dataStore.data.first()
        val isDone = prefs[BigButtonStateDefinition.Keys.IS_DONE] ?: false
        val lastChanged = prefs[BigButtonStateDefinition.Keys.LAST_CHANGED] ?: 0L
        val periodDays = prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS]
            ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS
        val resetHour = prefs[BigButtonStateDefinition.Keys.RESET_HOUR]
            ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR
        val resetMinute = prefs[BigButtonStateDefinition.Keys.RESET_MINUTE]
            ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE

        val shouldReset = ResetCalculator.shouldReset(lastChanged, periodDays, resetHour, resetMinute)

        // Finalize the ending period if a reset boundary was crossed
        // This happens regardless of isDone - a manually-reset period ends as "missed"
        if (shouldReset) {
            try {
                finalizePeriod(context, periodDays, resetHour, resetMinute)
            } catch (e: Exception) {
                Log.e(TAG, "Error in finalizePeriod", e)
            }
        }

        // Check if reset is due (only if currently Done)
        if (isDone && shouldReset) {
            // Update all widget instances via Glance state management
            val manager = GlanceAppWidgetManager(context)
            val widgetIds = manager.getGlanceIds(BigButtonWidget::class.java)

            widgetIds.forEach { glanceId ->
                // Reset state via Glance (ensures proper widget refresh)
                updateAppWidgetState(context, BigButtonStateDefinition, glanceId) { p ->
                    p.toMutablePreferences().apply {
                        this[BigButtonStateDefinition.Keys.IS_DONE] = false
                        this[BigButtonStateDefinition.Keys.LAST_CHANGED] = System.currentTimeMillis()
                    }
                }
                BigButtonWidget().update(context, glanceId)
            }
            Log.d(TAG, "Reset ${widgetIds.size} widgets")
        }

        // Schedule next alarm
        ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
    }

    /**
     * Finalizes the ending period by writing FinalizedDay records.
     * Called when a period boundary is crossed (shouldReset=true).
     */
    private suspend fun finalizePeriod(
        context: Context,
        periodDays: Int,
        resetHour: Int,
        resetMinute: Int
    ) {
        val db = BigButtonDatabase.getDatabase(context)
        val dao = db.bigButtonDao()

        // Only finalize if tracking has started
        val trackingStartStr = dao.getMetadata(TrackingMetadata.KEY_TRACKING_START_DATE)
            ?: return

        // Calculate the period that just ended
        // periodEnd = today's reset time, periodStart = periodEnd - periodDays
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, resetHour)
            set(java.util.Calendar.MINUTE, resetMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val periodEnd = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -periodDays)
        val periodStart = calendar.timeInMillis

        // Check if any completion events exist in this period
        val events = dao.getEventsInRange(periodStart, periodEnd)
        val completed = events.isNotEmpty()

        // Generate FinalizedDay for each day in the period
        // For periodDays=1, finalize 1 day. For periodDays=3, finalize 3 days.
        // endDate is the day BEFORE periodEnd (yesterday for a morning reset, today for evening reset)
        val zone = ZoneId.systemDefault()
        val endDate = Instant.ofEpochMilli(periodEnd - 1).atZone(zone).toLocalDate()
        val startDate = endDate.minusDays(periodDays.toLong() - 1)

        val daysToFinalize = mutableListOf<FinalizedDay>()
        var date = startDate
        while (!date.isAfter(endDate)) {
            daysToFinalize.add(FinalizedDay(date.toString(), completed))
            date = date.plusDays(1)
        }

        // Insert with IGNORE (preserves immutability of already-finalized days)
        dao.insertFinalizedDays(daysToFinalize)

        // Update last_finalized_date metadata
        dao.upsertMetadata(
            TrackingMetadata(
                key = TrackingMetadata.KEY_LAST_FINALIZED_DATE,
                value = endDate.toString()
            )
        )
    }
}
