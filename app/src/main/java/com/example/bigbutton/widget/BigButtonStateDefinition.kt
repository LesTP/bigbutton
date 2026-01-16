package com.example.bigbutton.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.state.GlanceStateDefinition
import java.io.File

private const val DATA_STORE_NAME = "big_button_widget_state"

/**
 * DataStore extension for accessing widget state from anywhere in the app.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)

/**
 * State definition for BigButton widget using DataStore Preferences.
 *
 * Schema:
 * - isDone: Boolean - whether the action is marked complete
 * - lastChanged: Long - timestamp (epoch ms) of last state change
 * - periodDays: Int - reset period in days (default: 1)
 * - resetHour: Int - hour of day for reset (0-23, default: 4)
 * - resetMinute: Int - minute of hour for reset (0-59, default: 0)
 */
object BigButtonStateDefinition : GlanceStateDefinition<Preferences> {

    const val DEFAULT_PERIOD_DAYS = 1
    const val MIN_PERIOD_DAYS = 1
    const val MAX_PERIOD_DAYS = 90

    const val DEFAULT_RESET_HOUR = 4  // 4:00 AM
    const val DEFAULT_RESET_MINUTE = 0

    object Keys {
        val IS_DONE = booleanPreferencesKey("is_done")
        val LAST_CHANGED = longPreferencesKey("last_changed")
        val PERIOD_DAYS = intPreferencesKey("period_days")
        val RESET_HOUR = intPreferencesKey("reset_hour")
        val RESET_MINUTE = intPreferencesKey("reset_minute")
    }

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/$DATA_STORE_NAME.preferences_pb")
    }
}
