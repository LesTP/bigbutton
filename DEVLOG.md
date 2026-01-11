# BigButton Development Log

This document contains the chronological implementation history, issues encountered, and lessons learned.

For product vision, requirements, and design specifications, see [DEVPLAN.md](DEVPLAN.md).

---

## Development Progress

### Phase 0: Project Setup

#### Step 0.1: Initial Project Creation
**Date:** 2026-01-07

Created basic Android project with Kotlin and Jetpack Compose. App builds and runs with intro screen showing "Welcome to BigButton!" message.

#### Step 0.2: Fixed Launcher Icon Issue
**Date:** 2026-01-08

Resolved AAPT build error for missing `mipmap/ic_launcher_foreground`. Created vector drawable and updated adaptive icon XML files.

#### Step 0.3: Documentation
**Date:** 2026-01-08

Created initial README.md and DEVELOPMENT.md.

---

### Phase 1: MVP Visual Design ✅

#### Step 1.1: Widget Implementation Decision
**Date:** 2026-01-09

Decided on Jetpack Glance. Updated minSdk from 24 to 26.

#### Step 1.2: Project Configuration
**Date:** 2026-01-09

Added Glance dependencies:
- `androidx.glance:glance-appwidget:1.1.0`
- `androidx.glance:glance-material3:1.1.0`

#### Step 1.3: Widget Provider and Registration
**Date:** 2026-01-09

Created:
- `BigButtonWidget.kt` - GlanceAppWidget implementation
- `BigButtonWidgetReceiver.kt` - Broadcast receiver
- `big_button_widget_info.xml` - Widget metadata
- `widget_loading.xml` - Loading placeholder

Registered receiver in AndroidManifest.xml.

#### Step 1.4: Visual Design Implementation
**Date:** 2026-01-09

Implemented widget UI with button, border ring, and settings icon. Initial sizing for 2x1 widget.

#### Step 1.5: Visual Polish
**Date:** 2026-01-09

Iterative refinements based on testing:
- Changed to 1x1 widget size
- Added radial gradients for 3D effect
- Adjusted button to 52dp with 60dp border ring
- Settings icon: 16dp at 70% opacity, 8dp padding
- Removed explicit corner radius (system handles it)

**Files created:**
- `button_do_gradient.xml`
- `button_done_gradient.xml`

---

### Phase 2: Basic Interactivity ✅

#### Step 2.1: State Definition
**Date:** 2026-01-09

Created `BigButtonStateDefinition.kt` using DataStore Preferences:
- `isDone: Boolean` - whether action is marked complete
- `lastChanged: Long` - timestamp for future auto-reset

#### Step 2.2: Mark Done Action
**Date:** 2026-01-09

Created `MarkDoneAction.kt` ActionCallback:
- Sets isDone=true only if currently false (one-way toggle)
- Updates lastChanged timestamp
- Triggers widget refresh

#### Step 2.3: Widget Integration
**Date:** 2026-01-09

Updated `BigButtonWidget.kt`:
- Added stateDefinition override
- Reads isDone from DataStore preferences
- Button (border ring) is clickable, triggers MarkDoneAction
- Settings icon not clickable (reserved for Phase 3)

Added DataStore dependency to `build.gradle.kts`.

#### Step 2.4: Testing
**Date:** 2026-01-09

Verified all test cases:
- ✅ Tap "Do" → changes to "Done"
- ✅ Tap "Done" → nothing happens
- ✅ Settings icon → nothing happens
- ✅ State persists across restarts (minor anomalies on first restart, resolved)

---

### Phase 3a: Settings Activity + Manual Reset ✅

#### Step 3a.1: Settings Screen
**Date:** 2026-01-09

Created `ui/SettingsScreen.kt`:
- Displays app title and "Settings" header
- Shows current widget status (Done/Not done)
- Reset button (enabled only when isDone=true)
- Reset sets isDone=false and closes activity

#### Step 3a.2: Open Settings Action
**Date:** 2026-01-09

Created `widget/OpenSettingsAction.kt`:
- ActionCallback that launches MainActivity
- Uses FLAG_ACTIVITY_NEW_TASK for widget context

#### Step 3a.3: Widget Integration
**Date:** 2026-01-09

Updated `BigButtonWidget.kt`:
- Settings icon now clickable with OpenSettingsAction

Updated `BigButtonStateDefinition.kt`:
- Exposed dataStore extension for app-wide access

#### Step 3a.4: MainActivity Update
**Date:** 2026-01-09

Updated `MainActivity.kt`:
- Replaced IntroScreen with SettingsScreen
- Closes activity on reset completion (auto-close behavior)

#### Step 3a.5: Testing
**Date:** 2026-01-09

Verified all test cases:
- ✅ Tap settings icon → opens settings activity
- ✅ Reset button disabled when widget is "Do"
- ✅ Reset button enabled when widget is "Done"
- ✅ Tap Reset → widget resets, activity closes
- ✅ Back navigation works

---

### Phase 3b: Period Selector ✅

#### Step 3b.1: State Schema Update
**Date:** 2026-01-10

Updated `BigButtonStateDefinition.kt`:
- Added `PERIOD_DAYS` key (Int, default: 1)
- Added constants: `DEFAULT_PERIOD_DAYS`, `MIN_PERIOD_DAYS` (1), `MAX_PERIOD_DAYS` (90)

#### Step 3b.2: Period Selector UI
**Date:** 2026-01-10

Updated `ui/SettingsScreen.kt`:
- Added `PeriodPreset` enum (DAILY, WEEKLY, MONTHLY, CUSTOM)
- Radio button list for preset selection
- Inline OutlinedTextField for custom days input
- Input validation (digits only, 1-90 range)
- Immediate persistence to DataStore on selection/input

#### Step 3b.3: Custom Radio Focus Behavior
**Date:** 2026-01-10

Added FocusRequester to custom input field:
- Clicking Custom radio button focuses the text input
- Clicking anywhere on the Custom row focuses input
- Enables immediate typing without additional tap

#### Step 3b.4: Custom Value Bug Fix
**Date:** 2026-01-10

**Problem:** Typing a value matching a preset (1, 7, 30) caused UI to jump to that preset, resetting the custom input field mid-typing.

**Solution:** Used normalized remember key for `customDaysText`:
```kotlin
val customRememberKey = if (periodDays in listOf(1, 7, 30)) 0 else periodDays
```
This keeps the key constant (0) when value matches any preset, preventing text reset while typing.

#### Step 3b.5: Testing
**Date:** 2026-01-10

Verified all test cases:
- ✅ Default shows "Daily" selected
- ✅ Select Weekly → saves, persists after restart
- ✅ Select Monthly → saves, persists after restart
- ✅ Custom: enter 14 → saves, persists
- ✅ Custom: enter values matching presets (1, 7, 30) → works correctly
- ✅ Custom radio click → focuses input field
- ✅ Input validation prevents values outside 1-90

---

### Phase 3c: Reset Time Selector ✅

#### Step 3c.1: State Schema Update
**Date:** 2026-01-10

Updated `BigButtonStateDefinition.kt`:
- Added `RESET_HOUR` key (Int, default: 4)
- Added `DEFAULT_RESET_HOUR = 4` constant

#### Step 3c.2: Time Picker UI
**Date:** 2026-01-10

Updated `ui/SettingsScreen.kt`:
- Added `formatHour()` helper function for 12h/24h time formatting
- Added "Reset Time" section with OutlinedButton showing current time
- Tapping button opens Android TimePickerDialog
- Selection saves to DataStore immediately
- Respects system 12h/24h preference via `DateFormat.is24HourFormat()`

#### Step 3c.3: Testing
**Date:** 2026-01-10

Verified all test cases:
- ✅ Default shows "4:00 AM"
- ✅ Time picker opens on button tap
- ✅ Time selection saves and persists after restart
- ✅ Midnight displays as "12:00 AM"
- ✅ Noon displays as "12:00 PM"
- ✅ Cancel picker leaves time unchanged

---

### Phase 4: Automatic Reset ✅

#### Step 4.1: Build Configuration Fixes
**Date:** 2026-01-10

Fixed JVM target mismatch error:
- Added `kotlinOptions { jvmTarget = "1.8" }` to `app/build.gradle.kts`
- Generated missing Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`)

#### Step 4.2: Reset Calculator Implementation
**Date:** 2026-01-10

Created `util/ResetCalculator.kt` with reset timing logic.

**Challenge:** Initial implementation calculated reset time incorrectly for edge cases.

**Problem:** If user marked Done at 2:03 PM with reset time 2:05 PM (period=1 day), the original logic calculated:
- `nextReset = date of lastChanged + 1 day at reset time = 2:05 PM TOMORROW`

**Expected:** Reset should happen at 2:05 PM TODAY (2 minutes later).

**Solution:** The reset time defines a "day boundary". If `lastChanged` is before the reset time on its day, the action belongs to the "previous logical day":
```kotlin
if (lastChanged < resetTimeOnSameDay) {
    calendar.add(Calendar.DAY_OF_YEAR, periodDays - 1)  // Same day or less
} else {
    calendar.add(Calendar.DAY_OF_YEAR, periodDays)      // Next period
}
```

#### Step 4.3: AlarmManager Integration
**Date:** 2026-01-10

Created:
- `receiver/ResetAlarmScheduler.kt` - Schedules exact alarms
- `receiver/ResetAlarmReceiver.kt` - Handles alarm broadcast
- `receiver/BootReceiver.kt` - Reschedules after device reboot

**Key Decisions:**
- Used `setExactAndAllowWhileIdle()` for precise timing even in Doze mode
- Added `canScheduleExactAlarms()` check for Android 12+ compatibility
- Added comprehensive logging for debugging

#### Step 4.4: Glance State Management Challenge
**Date:** 2026-01-10

**Major Challenge:** Widget visual not updating after state changes.

**Symptoms:**
1. Tapping button changed state (visible in Settings) but widget stayed on "Do"
2. Settings icon stopped working entirely
3. Automatic reset alarm fired but widget didn't update

**Root Cause Analysis:**

Glance maintains its own state layer on top of DataStore:
- `updateAppWidgetState()` → Updates Glance's state cache
- `context.dataStore.edit()` → Updates DataStore directly (Glance doesn't know)
- `currentState<T>()` → Reads from Glance's cached state
- `context.dataStore.data.first()` → Reads from DataStore directly

**The Problem:** We were mixing these inconsistently:
- Some writes used `dataStore.edit()` (bypassed Glance)
- Some reads used `currentState()` (read from Glance cache)
- Widget updates called but Glance used stale cached state

**Critical Discovery:** Calling `updateAppWidgetState()` inside `provideGlance()` caused a deadlock/corruption. Glance was mid-render when we tried to update state, breaking everything.

**Solution:**
1. **Never call `updateAppWidgetState()` inside `provideGlance()`**
2. All state writes go through `updateAppWidgetState()` (in action callbacks and receivers)
3. Widget reads use `currentState<Preferences>()` inside `provideContent`
4. Reset check in `provideGlance` only reads state and calculates display value (no writes)
5. Actual reset happens on user tap via `MarkDoneAction`

**Architecture Pattern:**
```
Write Path: ActionCallback/Receiver → updateAppWidgetState() → update()
Read Path:  provideGlance() → provideContent { currentState() } → render
```

#### Step 4.5: Android 12+ Alarm Permission
**Date:** 2026-01-10

**Challenge:** Alarms not firing on Android 12+ devices.

**Cause:** `SCHEDULE_EXACT_ALARM` permission requires manual user approval in system settings (not a runtime permission dialog).

**Solution:**
- Added `canScheduleExactAlarms()` check before scheduling
- Added logging to indicate when permission is denied
- Updated documentation with required user action

#### Step 4.6: Final Testing
**Date:** 2026-01-10

Verified all test cases:
- ✅ Button tap: Do → Done (visual updates immediately)
- ✅ Settings sync: State matches between widget and settings
- ✅ Manual reset: Done → Do via settings
- ✅ Automatic reset: Alarm fires at scheduled time, widget updates
- ✅ Reset boundary: Marking done before reset time triggers reset at that time
- ✅ Alarm rescheduling: Next alarm scheduled ~24 hours after reset

**Sample Log Output (successful reset):**
```
ResetAlarmReceiver: onReceive called with action: com.example.bigbutton.ACTION_RESET_CHECK
ResetAlarmReceiver: Processing reset check...
ResetAlarmReceiver: State: isDone=true, lastChanged=1768098954367, period=1, resetTime=18:40
ResetAlarmReceiver: shouldReset=true
ResetAlarmReceiver: Performing reset...
ResetAlarmReceiver: Found 1 widgets to update
ResetAlarmReceiver: Updated widget AppWidgetId(appWidgetId=2)
ResetAlarmScheduler: Scheduling reset alarm for: 1768185600000 (in 86399s)
ResetAlarmReceiver: Scheduled next alarm
```

---

### Phase 4.1: Tab Navigation & Info Screen ✅

#### Step 4.1.1: Tab Structure in MainActivity
**Date:** 2026-01-11

Refactored MainActivity to use tab-based navigation:
- Added `TabRow` with three tabs: Settings, Calendar, Info
- Created `MainContent` composable with tab state management
- Moved "BigButton" title from SettingsScreen to MainActivity (above tabs)
- Default tab is Settings (index 0)

#### Step 4.1.2: InfoScreen Implementation
**Date:** 2026-01-11

Created `ui/InfoScreen.kt`:
- App name and version (from PackageManager)
- Brief app description
- Permissions section (Android 12+ only) with:
  - Explanation of "Alarms & reminders" permission requirement
  - Guidance to scroll down in system settings to find it
  - "Open App Settings" button using `ACTION_APPLICATION_DETAILS_SETTINGS`
- Credits section

#### Step 4.1.3: CalendarScreen Placeholder
**Date:** 2026-01-11

Created `ui/CalendarScreen.kt` placeholder:
- Simple "Coming Soon" message
- Brief description of upcoming feature
- Ready for Phase 5 implementation

#### Step 4.1.4: SettingsScreen Refactor
**Date:** 2026-01-11

Updated `ui/SettingsScreen.kt`:
- Removed "BigButton" title and "Settings" subtitle (moved to MainActivity)
- Adjusted padding for tab content layout
- All existing functionality preserved

#### Step 4.1.5: Portrait Orientation Lock
**Date:** 2026-01-11

Updated `AndroidManifest.xml`:
- Added `android:screenOrientation="portrait"` to MainActivity
- App now locked to portrait mode

#### Step 4.1.6: Widget Refresh on Boot Fix
**Date:** 2026-01-11

**Bug:** Widget stuck on "Loading..." after device restart.

**Cause:** `BootReceiver` rescheduled alarms but didn't refresh widget UI.

**Solution:** Updated `BootReceiver` to also refresh all widget instances:
```kotlin
val manager = GlanceAppWidgetManager(context)
val widgetIds = manager.getGlanceIds(BigButtonWidget::class.java)
widgetIds.forEach { glanceId ->
    BigButtonWidget().update(context, glanceId)
}
```

#### Step 4.1.7: Testing
**Date:** 2026-01-11

Verified all test cases:
- ✅ Default tab is Settings
- ✅ Tab switching works (Settings, Calendar, Info)
- ✅ Info screen shows version, permissions section, credits
- ✅ "Open App Settings" button opens system settings
- ✅ All Settings tab functionality works as before
- ✅ App locked to portrait orientation
- ✅ Widget renders correctly after device restart

**Files Created:**
- `ui/InfoScreen.kt`
- `ui/CalendarScreen.kt`

**Files Modified:**
- `MainActivity.kt` - Tab navigation structure
- `ui/SettingsScreen.kt` - Removed header
- `AndroidManifest.xml` - Portrait lock
- `receiver/BootReceiver.kt` - Widget refresh on boot

---

## Issues & Resolutions

### Issue #1: Missing Launcher Icon Resource
**Date:** 2026-01-08 | **Severity:** High | **Status:** Resolved

**Problem:** Build failed with AAPT error about missing `mipmap/ic_launcher_foreground`.

**Solution:** Created vector drawable in `drawable/` directory and updated adaptive icon XML files to reference it.

**Lesson:** Adaptive icons require both background and foreground resources. Use Android Studio's Image Asset Studio for production icons.

---

### Issue #2: JVM Target Mismatch
**Date:** 2026-01-10 | **Severity:** High | **Status:** Resolved

**Problem:** Build failed with "Inconsistent JVM-target compatibility detected for tasks 'compileDebugJavaWithJavac' (1.8) and 'compileDebugKotlin' (21)."

**Solution:** Added missing `kotlinOptions` block to `app/build.gradle.kts`:
```kotlin
kotlinOptions {
    jvmTarget = "1.8"
}
```

**Lesson:** When using Kotlin with Android, always explicitly set `jvmTarget` in `kotlinOptions` to match the Java `compileOptions`.

---

### Issue #3: Missing Gradle Wrapper
**Date:** 2026-01-10 | **Severity:** Medium | **Status:** Resolved

**Problem:** `./gradlew` command failed - wrapper files were missing from repository.

**Solution:** Downloaded Gradle 8.13 and ran `gradle wrapper` to generate:
- `gradlew` (Unix script)
- `gradlew.bat` (Windows script)
- `gradle/wrapper/gradle-wrapper.jar`

**Lesson:** Always commit Gradle wrapper files to the repository for reproducible builds.

---

### Issue #4: Glance State Deadlock
**Date:** 2026-01-10 | **Severity:** Critical | **Status:** Resolved

**Problem:** Widget stopped responding to all interactions. Button taps did nothing, settings icon stopped working.

**Root Cause:** Called `updateAppWidgetState()` inside `provideGlance()`, causing Glance to try to update state while mid-render, resulting in deadlock/corruption.

**Solution:**
1. Never modify state inside `provideGlance()`
2. Only read state using `currentState<T>()` inside `provideContent`
3. All writes happen in ActionCallbacks or BroadcastReceivers

**Lesson:** Glance's `provideGlance()` should be pure/read-only. State modifications must happen outside the render cycle.

---

### Issue #5: Android 12+ Exact Alarm Permission
**Date:** 2026-01-10 | **Severity:** High | **Status:** Resolved

**Problem:** Automatic reset alarms not firing on Android 12+ devices.

**Root Cause:** `SCHEDULE_EXACT_ALARM` permission on API 31+ requires manual user approval in system settings, not a runtime permission dialog.

**Solution:**
1. Added `canScheduleExactAlarms()` check before scheduling
2. Added logging to help diagnose permission issues
3. Documented required user action in testing procedure

**Lesson:** On Android 12+, apps must guide users to Settings > Apps > [App] > Alarms & reminders to enable exact alarms. Consider adding in-app prompt.

---

### Issue #6: Widget Stuck on "Loading..." After Device Restart
**Date:** 2026-01-11 | **Severity:** High | **Status:** Resolved

**Problem:** After device restart, widget displayed "Loading..." indefinitely instead of rendering the button.

**Root Cause:** `BootReceiver` only rescheduled alarms after `BOOT_COMPLETED`. It did not trigger a widget UI refresh, so `provideGlance()` was never called.

**Solution:** Updated `BootReceiver` to refresh all widget instances after boot:
```kotlin
val manager = GlanceAppWidgetManager(context)
val widgetIds = manager.getGlanceIds(BigButtonWidget::class.java)
widgetIds.forEach { glanceId ->
    BigButtonWidget().update(context, glanceId)
}
```

**Lesson:** When handling `BOOT_COMPLETED`, always refresh Glance widgets explicitly. The system's `APPWIDGET_UPDATE` broadcast may not be sufficient to trigger `provideGlance()` after a cold boot.

---

## Testing Checklist

For each increment:
- [ ] Project builds without errors
- [ ] App runs on emulator/device
- [ ] Visual changes are as expected
- [ ] No regressions in existing functionality
- [ ] Accessibility guidelines met

---

Last Updated: 2026-01-11 (Phase 4.1 complete, ready for Phase 5)
