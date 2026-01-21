package com.movingfingerstudios.bigbutton.ui

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.movingfingerstudios.bigbutton.data.BigButtonDatabase
import com.movingfingerstudios.bigbutton.receiver.ResetAlarmScheduler
import com.movingfingerstudios.bigbutton.util.ResetCalculator
import com.movingfingerstudios.bigbutton.widget.BigButtonStateDefinition
import com.movingfingerstudios.bigbutton.widget.BigButtonWidget
import com.movingfingerstudios.bigbutton.widget.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

// Period presets
private enum class PeriodPreset(val days: Int, val label: String) {
    DAILY(1, "Daily (1 day)"),
    WEEKLY(7, "Weekly (7 days)"),
    MONTHLY(30, "Monthly (30 days)"),
    CUSTOM(-1, "Custom")
}

/**
 * Checks if the exact alarm permission warning should be shown.
 * Returns true on Android 12+ when permission is not granted.
 */
private fun shouldShowPermissionWarning(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return !alarmManager.canScheduleExactAlarms()
    }
    return false
}

/**
 * Warning banner for missing exact alarm permission.
 */
@Composable
private fun PermissionWarningBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Light amber/orange
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFE65100), // Dark orange
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFE65100)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "BigButton widget requires 'Alarms & reminders' permission to reset automatically. Enable it in system settings.",
                fontSize = 14.sp,
                color = Color(0xFF5D4037) // Brown text
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onOpenSettings,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFE65100)
                )
            ) {
                Text(
                    text = "Open Settings",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Formats hour and minute as time string using system 12h/24h preference.
 */
private fun formatTime(hour: Int, minute: Int, use24HourFormat: Boolean): String {
    return if (use24HourFormat) {
        String.format("%02d:%02d", hour, minute)
    } else {
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}

@Composable
fun SettingsScreen(onResetComplete: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Read current state from DataStore
    val isDone by context.dataStore.data
        .map { prefs -> prefs[BigButtonStateDefinition.Keys.IS_DONE] ?: false }
        .collectAsState(initial = false)

    val periodDays by context.dataStore.data
        .map { prefs -> prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS] ?: BigButtonStateDefinition.DEFAULT_PERIOD_DAYS }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_PERIOD_DAYS)

    val resetHour by context.dataStore.data
        .map { prefs -> prefs[BigButtonStateDefinition.Keys.RESET_HOUR] ?: BigButtonStateDefinition.DEFAULT_RESET_HOUR }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_RESET_HOUR)

    val resetMinute by context.dataStore.data
        .map { prefs -> prefs[BigButtonStateDefinition.Keys.RESET_MINUTE] ?: BigButtonStateDefinition.DEFAULT_RESET_MINUTE }
        .collectAsState(initial = BigButtonStateDefinition.DEFAULT_RESET_MINUTE)

    // Check system 12h/24h preference
    val is24HourFormat = DateFormat.is24HourFormat(context)

    // Determine which preset is selected (or custom)
    val selectedPreset = when (periodDays) {
        1 -> PeriodPreset.DAILY
        7 -> PeriodPreset.WEEKLY
        30 -> PeriodPreset.MONTHLY
        else -> PeriodPreset.CUSTOM
    }

    // Custom days input state
    // Use normalized key: same for all presets (0), unique for custom values
    // This prevents text reset when typing a value that matches a preset
    val customRememberKey = if (periodDays in listOf(1, 7, 30)) 0 else periodDays
    var customDaysText by remember(customRememberKey) {
        mutableStateOf(if (selectedPreset == PeriodPreset.CUSTOM) periodDays.toString() else "")
    }

    // Focus requester for custom input field
    val customFocusRequester = remember { FocusRequester() }

    // Check if permission warning should be shown (reactive on resume)
    val showPermissionWarning = shouldShowPermissionWarning(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Permission warning banner (Android 12+ only)
        if (showPermissionWarning) {
            PermissionWarningBanner(
                onOpenSettings = {
                    // Open the Alarms & reminders settings page directly
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset section
        Text(
            text = if (isDone) "Status: Done" else "Status: Not done",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    // Update all widget instances via Glance state management
                    val manager = GlanceAppWidgetManager(context)
                    val widgetIds = manager.getGlanceIds(BigButtonWidget::class.java)
                    widgetIds.forEach { glanceId ->
                        // Reset state via Glance (ensures proper widget refresh)
                        updateAppWidgetState(context, BigButtonStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[BigButtonStateDefinition.Keys.IS_DONE] = false
                                this[BigButtonStateDefinition.Keys.LAST_CHANGED] = System.currentTimeMillis()
                            }
                        }
                        BigButtonWidget().update(context, glanceId)
                    }

                    // Delete completion events from current period (undo behavior)
                    val db = BigButtonDatabase.getDatabase(context)
                    val now = System.currentTimeMillis()
                    val periodStart = ResetCalculator.calculateCurrentPeriodStart(
                        now, periodDays, resetHour, resetMinute
                    )
                    db.bigButtonDao().deleteEventsInRange(periodStart, now + 1)

                    // Close activity
                    onResetComplete()
                }
            },
            enabled = isDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Reset to \"Do\"",
                fontSize = 16.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (!isDone) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nothing to reset - widget is already in \"Do\" state",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Period selector section
        Text(
            text = "Reset Period",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                // Preset options
                PeriodPreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (preset == PeriodPreset.CUSTOM) {
                                    customFocusRequester.requestFocus()
                                } else {
                                    scope.launch {
                                        context.dataStore.edit { prefs ->
                                            prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS] = preset.days
                                        }
                                        // Reschedule reset alarm with new period
                                        ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = {
                                if (preset == PeriodPreset.CUSTOM) {
                                    customFocusRequester.requestFocus()
                                } else {
                                    scope.launch {
                                        context.dataStore.edit { prefs ->
                                            prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS] = preset.days
                                        }
                                        // Reschedule reset alarm with new period
                                        ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
                                    }
                                }
                            }
                        )

                        if (preset == PeriodPreset.CUSTOM) {
                            Text(
                                text = "Custom: ",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = customDaysText,
                                onValueChange = { newValue ->
                                    // Only allow digits
                                    val filtered = newValue.filter { it.isDigit() }
                                    customDaysText = filtered

                                    // Parse and save if valid
                                    val days = filtered.toIntOrNull()
                                    if (days != null && days in BigButtonStateDefinition.MIN_PERIOD_DAYS..BigButtonStateDefinition.MAX_PERIOD_DAYS) {
                                        scope.launch {
                                            context.dataStore.edit { prefs ->
                                                prefs[BigButtonStateDefinition.Keys.PERIOD_DAYS] = days
                                            }
                                            // Reschedule reset alarm with new period
                                            ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 56.dp, max = 88.dp)
                                    .focusRequester(customFocusRequester),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                placeholder = { Text("1-90") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "days",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = preset.label,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Show validation hint for custom
        if (selectedPreset == PeriodPreset.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a value between 1 and 90 days",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reset time section
        Text(
            text = "Reset Time",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        scope.launch {
                            context.dataStore.edit { prefs ->
                                prefs[BigButtonStateDefinition.Keys.RESET_HOUR] = selectedHour
                                prefs[BigButtonStateDefinition.Keys.RESET_MINUTE] = selectedMinute
                            }
                            // Reschedule reset alarm with new time
                            ResetAlarmScheduler.scheduleNextReset(context, selectedHour, selectedMinute)
                        }
                    },
                    resetHour,
                    resetMinute,
                    is24HourFormat
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatTime(resetHour, resetMinute, is24HourFormat),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Widget resets to \"Do\" at this time each period",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
