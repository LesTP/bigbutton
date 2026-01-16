package com.example.bigbutton.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.bigbutton.data.BigButtonDatabase
import com.example.bigbutton.data.CompletionEvent
import com.example.bigbutton.data.TrackingMetadata
import com.example.bigbutton.receiver.ResetAlarmScheduler
import com.example.bigbutton.util.ResetCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Action callback for marking the button as Done.
 *
 * Behavior:
 * - If reset is due: performs reset (Done->Do)
 * - If currently "Do" (isDone=false): sets isDone=true
 * - If already "Done" (isDone=true) and no reset due: does nothing
 */
class MarkDoneAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        var shouldScheduleAlarm = false

        updateAppWidgetState(context, BigButtonStateDefinition, glanceId) { prefs ->
            val isDone = prefs[BigButtonStateDefinition.Keys.IS_DONE] ?: false
            val lastChanged = prefs[BigButtonStateDefinition.Keys.LAST_CHANGED] ?: 0L
            val periodDays = prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS]
                ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS
            val resetHour = prefs[BigButtonStateDefinition.Keys.RESET_HOUR]
                ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR
            val resetMinute = prefs[BigButtonStateDefinition.Keys.RESET_MINUTE]
                ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE

            // Check if reset is due
            val resetDue = isDone && ResetCalculator.shouldReset(lastChanged, periodDays, resetHour, resetMinute)

            when {
                resetDue -> {
                    // Perform reset
                    prefs.toMutablePreferences().apply {
                        this[BigButtonStateDefinition.Keys.IS_DONE] = false
                        this[BigButtonStateDefinition.Keys.LAST_CHANGED] = System.currentTimeMillis()
                    }
                }
                !isDone -> {
                    // Mark as done
                    shouldScheduleAlarm = true
                    prefs.toMutablePreferences().apply {
                        this[BigButtonStateDefinition.Keys.IS_DONE] = true
                        this[BigButtonStateDefinition.Keys.LAST_CHANGED] = System.currentTimeMillis()
                    }
                }
                else -> {
                    // Already done, no reset due - no change
                    prefs
                }
            }
        }

        // Trigger widget update
        BigButtonWidget().update(context, glanceId)

        // Record completion event and schedule alarm if we just marked done
        if (shouldScheduleAlarm) {
            val prefs = context.dataStore.data.first()
            val periodDays = prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS]
                ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS
            val resetHour = prefs[BigButtonStateDefinition.Keys.RESET_HOUR]
                ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR
            val resetMinute = prefs[BigButtonStateDefinition.Keys.RESET_MINUTE]
                ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE

            // Record completion event in database
            val db = BigButtonDatabase.getDatabase(context)
            db.bigButtonDao().insertCompletionEvent(
                CompletionEvent(
                    timestamp = System.currentTimeMillis(),
                    periodDays = periodDays
                )
            )

            // Set tracking start date if not already set
            val existingStart = db.bigButtonDao().getMetadata(TrackingMetadata.KEY_TRACKING_START_DATE)
            if (existingStart == null) {
                db.bigButtonDao().upsertMetadata(
                    TrackingMetadata(
                        key = TrackingMetadata.KEY_TRACKING_START_DATE,
                        value = LocalDate.now().toString()
                    )
                )
            }

            ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
        }
    }
}
