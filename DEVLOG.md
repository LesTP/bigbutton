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

### Phase 5a: Room Database Setup ✅

#### Step 5a.1: Build Configuration
**Date:** 2026-01-11

Added Room dependencies and KSP annotation processor:
- KSP plugin 1.9.20-1.0.14 (matches Kotlin version)
- Room 2.6.1 (runtime, ktx, compiler)

**Files Modified:**
- `settings.gradle.kts` - Added KSP plugin declaration
- `app/build.gradle.kts` - Applied KSP plugin, added Room dependencies

#### Step 5a.2: Entity Classes
**Date:** 2026-01-11

Created three Room entities per DEVPLAN specification:

1. **CompletionEvent** - Records "Done" button presses
   - `id: Long` (auto-generated PK)
   - `timestamp: Long` (epoch millis)
   - `periodDays: Int` (period at time of completion)

2. **FinalizedDay** - Locked-in day status
   - `date: String` (PK, ISO format "2026-01-15")
   - `completed: Boolean` (green/red)

3. **TrackingMetadata** - Key-value config store
   - `key: String` (PK)
   - `value: String`
   - Companion object with key constants

#### Step 5a.3: DAO Interface
**Date:** 2026-01-11

Created `BigButtonDao` with operations for all three entities:
- CompletionEvent: insert, getEventsInRange, deleteEventsInRange, deleteAll
- FinalizedDay: insertIgnore, insertFinalizedDays (batch), getDaysInRange, getDay, deleteAll
- TrackingMetadata: upsert, get, deleteAll

#### Step 5a.4: Database Class
**Date:** 2026-01-11

Created `BigButtonDatabase` with:
- Room database annotation (version 1, exportSchema=false)
- Abstract DAO accessor
- Thread-safe singleton pattern using `@Volatile` and `synchronized`

**Files Created:**
- `data/CompletionEvent.kt`
- `data/FinalizedDay.kt`
- `data/TrackingMetadata.kt`
- `data/BigButtonDao.kt`
- `data/BigButtonDatabase.kt`

---

### Phase 5b: Completion Event Recording & Manual Reset ✅

#### Step 5b.1: Period Start Calculator
**Date:** 2026-01-11

Added `calculateCurrentPeriodStart()` to `util/ResetCalculator.kt`:
- Calculates the start timestamp of the current period
- Used to determine which CompletionEvents to delete on manual reset
- Accounts for reset time boundary (before/after today's reset time)

#### Step 5b.2: Completion Event Recording
**Date:** 2026-01-11

Updated `widget/MarkDoneAction.kt`:
- When user presses "Done", inserts CompletionEvent with timestamp and periodDays
- On first-ever completion, sets `tracking_start_date` in TrackingMetadata
- Database writes happen in the same coroutine as alarm scheduling

#### Step 5b.3: Manual Reset Event Deletion
**Date:** 2026-01-11

Updated `ui/SettingsScreen.kt`:
- When user manually resets, deletes all CompletionEvents from current period
- Acts as "undo" for accidental Done presses
- Uses `calculateCurrentPeriodStart()` to determine deletion range

#### Step 5b.4: Testing
**Date:** 2026-01-11

Verified via Database Inspector:
- ✅ Tap "Done" → 1 row in completion_events
- ✅ Manual reset → 0 rows (deleted)
- ✅ Tap "Done" again → 1 row (new timestamp)
- ✅ tracking_metadata contains tracking_start_date after first Done

**Files Modified:**
- `util/ResetCalculator.kt` - Added `calculateCurrentPeriodStart()`
- `widget/MarkDoneAction.kt` - Added DB insert + tracking start date
- `ui/SettingsScreen.kt` - Added DB delete on reset

**Note:** `finalized_days` table remains empty - this is expected. Period finalization (locking in green/red status) is implemented in Phase 5c.

---

### Phase 5c: Period Finalization ✅

#### Step 5c.1: Finalization Logic
**Date:** 2026-01-11

Added `finalizePeriod()` function to `receiver/ResetAlarmReceiver.kt`:
- Called when `shouldReset=true` (period boundary crossed)
- Checks for CompletionEvents in the ending period
- Writes FinalizedDay records for each day in the period
- Updates `last_finalized_date` metadata

#### Step 5c.2: Period Boundary Calculation
**Date:** 2026-01-11

**Challenge:** Calculating the correct time range for the ending period.

**Initial bug:** Using `now` as periodEnd caused issues because the alarm fires slightly after the exact reset time. Events recorded just before reset time were missed.

**Solution:** Calculate period boundaries explicitly:
```kotlin
val calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, resetHour)
    set(Calendar.MINUTE, resetMinute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
val periodEnd = calendar.timeInMillis  // Today's reset time
calendar.add(Calendar.DAY_OF_YEAR, -periodDays)
val periodStart = calendar.timeInMillis  // Yesterday's reset time (for daily)
```

#### Step 5c.3: Day Count Fix
**Date:** 2026-01-11

**Bug:** Daily period was finalizing 2 calendar days instead of 1.

**Cause:** Period spans midnight (e.g., 8 PM to 8 PM crosses two calendar days).

**Solution:** Calculate dates based on `periodDays`, not timestamp conversion:
```kotlin
val endDate = Instant.ofEpochMilli(periodEnd - 1).atZone(zone).toLocalDate()
val startDate = endDate.minusDays(periodDays.toLong() - 1)
```

#### Step 5c.4: Testing
**Date:** 2026-01-11

Verified via Database Inspector:
- ✅ Tap "Done", wait for reset → finalized_days has 1 row with completed=1
- ✅ INSERT IGNORE preserves immutability of already-finalized days
- ✅ Multi-day period (3 days) → finalized_days has 3 rows with same completed status

**Files Modified:**
- `receiver/ResetAlarmReceiver.kt` - Added `finalizePeriod()` function

#### Step 5c.5: Logging Cleanup
**Date:** 2026-01-16

Removed verbose debug logging from receiver classes:
- `ResetAlarmReceiver.kt` - Removed event dumps and per-widget logging, kept error handling
- `BootReceiver.kt` - Removed verbose logging, kept error handling
- `ResetAlarmScheduler.kt` - Kept permission warning and scheduling log (useful for debugging)

---

### Phase 5d: Calendar UI - Basic ✅

#### Step 5d.1: CalendarScreen Implementation
**Date:** 2026-01-16

Replaced placeholder CalendarScreen with full implementation:
- Continuously scrollable LazyColumn with week rows
- Day cells with colored backgrounds based on status
- Inline month headers when week contains 1st of month
- Sticky day-of-week header (S M T W T F S)
- Auto-scroll to current week on load
- Data loaded from Room (FinalizedDay) and DataStore (period settings)

**Day Status Logic:**
1. Future days → transparent
2. Before tracking started → transparent
3. Finalized completed → green (#4CAF50)
4. Finalized missed → red (#F44336)
5. Current period (not finalized) → grey (#9E9E9E)
6. Gap/abandoned period → transparent

**Files Modified:**
- `ui/CalendarScreen.kt` - Complete rewrite with calendar implementation

#### Step 5d.2: Testing
**Date:** 2026-01-16

Verified all test cases:
- ✅ Calendar displays with week rows
- ✅ Day-of-week header shows S M T W T F S
- ✅ Month headers appear (e.g., "January 2026")
- ✅ Green days appear for finalized completed
- ✅ Red days appear for finalized missed
- ✅ Grey days appear for current in-progress period
- ✅ Days before tracking start have no color
- ✅ Future days have no color
- ✅ Calendar scrolls to current week on open
- ✅ Scrolling works smoothly

**Known Limitation:** Time-travel testing (manually advancing device clock) does not trigger period finalization because AlarmManager doesn't retroactively fire alarms. Real-world usage is unaffected.

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

### Issue #7: Widget "Can't load" on Android 12 Physical Device
**Date:** 2026-01-17 | **Severity:** Critical | **Status:** Resolved

**Problem:** Widget displayed "Can't load widget" error on Android 12 physical device. Widget worked in emulator but failed on real hardware.

**Root Cause:** `ResetAlarmScheduler.scheduleImmediateCheck()` was missing the Android 12+ exact alarm permission check. When widget was first added, `BigButtonWidgetReceiver.onEnabled()` called this method, which attempted to schedule an exact alarm without permission. This threw a `SecurityException`, crashing widget initialization before `provideGlance()` could render the UI.

**Solution:**
1. Added `canScheduleExactAlarms()` permission check to `scheduleImmediateCheck()` (matching the existing check in `scheduleNextReset()`)
2. Added try-catch wrapper in `BigButtonWidgetReceiver.onEnabled()` and `onDisabled()`
3. Added try-catch around state reading in `BigButtonWidget.provideGlance()` for additional resilience

**Files Modified:**
- `receiver/ResetAlarmScheduler.kt`
- `widget/BigButtonWidgetReceiver.kt`
- `widget/BigButtonWidget.kt`

**Lesson:** All AlarmManager exact alarm methods (`setExact`, `setExactAndAllowWhileIdle`, etc.) require the `SCHEDULE_EXACT_ALARM` permission check on Android 12+. This applies to *all* scheduling calls, not just the primary ones. Widget initialization code must be wrapped in try-catch to prevent complete widget failure.

---

### Issue #8: Settings Tab Content Cut Off on Small Screens
**Date:** 2026-01-17 | **Severity:** Medium | **Status:** Resolved

**Problem:** On physical device with smaller screen, the Settings tab content was cut off below the "Reset Time" label. Users could not access the time picker button.

**Root Cause:** `SettingsScreen` used a `Column` with `fillMaxSize()` but no scroll modifier. Content exceeding screen height was simply clipped.

**Solution:** Added `verticalScroll(rememberScrollState())` modifier to the Column in `SettingsScreen.kt`.

**Files Modified:**
- `ui/SettingsScreen.kt`

**Lesson:** Always make settings/form screens scrollable, even if content fits on test devices. Physical devices have varying screen sizes, and system UI elements (navigation bar, status bar) reduce available space.

---

## Phase Completion Log

### Phase 5e: Permission Warning Banner ✅
**Date:** 2026-01-17

**Implemented:**
- Added `shouldShowPermissionWarning()` function to check Android 12+ exact alarm permission
- Created `PermissionWarningBanner` composable with amber/warning styling
- Banner displays at top of Settings tab when permission not granted
- "Open Settings" button opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` directly to Alarms & reminders page
- Banner auto-hides when permission is granted (reactive on recomposition)
- Removed redundant Permissions section from Info tab
- Made Info tab scrollable for future content additions

**Files Modified:**
- `ui/SettingsScreen.kt` - Added permission check and warning banner
- `ui/InfoScreen.kt` - Removed Permissions section, added verticalScroll

---

### Phase 5f.0: Calendar Initial Scroll Position ✅
**Date:** 2026-01-17

**Problem:** Calendar opened with current week at the top of viewport. On smaller screens, this pushed present and future weeks below the visible area, showing only old past weeks.

**Root Cause:** The scroll index calculation used the week index from the `weeks` list directly, but the LazyColumn also contains month header items interspersed between weeks. With ~12 month headers over a year, the actual item index was off by 12.

**Solution:**
- Count month headers before target week (weeks containing the 1st of a month)
- Calculate actual LazyColumn item index = weekIndex + monthHeaders
- Scroll to position that aligns (current week + 1) near bottom of viewport
- Uses viewport height and average item height to calculate optimal scroll position

**Files Modified:**
- `ui/CalendarScreen.kt` - Fixed scroll calculation in LaunchedEffect

**Lesson:** When using LazyColumn with heterogeneous items (weeks + month headers), item indices in the data list don't match LazyColumn item indices. Must account for all item types when calculating scroll positions.

---

### Phase 5f: Calendar UI - Polish ✅
**Date:** 2026-01-17

**Implemented:**
- Today's date cell displays a colored border ring (2dp, primary color)
- Border visible regardless of day status (green/red/grey/transparent)
- Today's date number displayed in bold

**Removed from requirements:**
- Shadow/glow effect for in-progress days - effect was too subtle to be reliably visible across devices

**Files Modified:**
- `ui/CalendarScreen.kt` - Removed shadow code, kept today border

**Note:** Today border was already implemented during Phase 5d. This phase simplified the requirements by removing the shadow feature.

---

### Phase 5g: Clear History ✅
**Date:** 2026-01-17

**Implemented:**
- "Clear History" button in Calendar tab fixed footer (red/error color)
- Confirmation dialog with warning message before clearing
- `clearAllHistory()` function in BigButtonDao using @Transaction
- Data reload trigger after clearing to refresh calendar view

**What gets cleared:**
- `completion_events` table (all records)
- `finalized_days` table (all records)
- `tracking_metadata` table (tracking_start_date, last_finalized_date)

**What stays:**
- Widget state (isDone) - stored in DataStore
- Settings (period, reset time) - stored in DataStore

**Files Modified:**
- `data/BigButtonDao.kt` - Added clearAllHistory() transaction function
- `ui/CalendarScreen.kt` - Added fixed footer with button, confirmation dialog, reload trigger

**Design Decision:** Placed Clear History button in Calendar tab (not Settings) because it directly affects the calendar data - more contextually relevant for users viewing their history.

---

### Phase 6a: Info Tab Instructions ✅
**Date:** 2026-01-18

**Implemented:**
- Added comprehensive user instructions to the Info tab
- Four instructional sections: How to Use, Calendar Colors, Multi-Day Periods, When Periods Finalize
- Helper composables for consistent formatting (SectionHeader, InstructionItem, ColorItem, BulletPoint)
- Updated footer to "Made by The Moving Finger Studios"

**Content Added:**
1. **How to Use** - Configure, The Widget, Automatic Reset, Undo Accidental Presses
2. **Calendar Colors** - Explains green (completed), red (missed), grey (in progress), no color (gaps/future)
3. **Multi-Day Periods** - Explains how weekly/custom periods work with all days showing same color
4. **When Periods Finalize** - Explains when status is locked in and how to clear history

**Files Modified:**
- `ui/InfoScreen.kt` - Added instructional sections and helper composables

---

### Phase 5h: Settings UI - Small Screen Fix ✅
**Date:** 2026-01-19

**Problem:** On smaller hardware screens, the Custom period selector row (RadioButton + "Custom:" + TextField + "days") wrapped to two lines, even though there was available horizontal space after "days".

**Root Cause:** The OutlinedTextField used a fixed `width(70.dp)` which didn't adapt to available space. The Row didn't properly utilize the full width.

**Solution:**
- Changed `Modifier.width(70.dp)` to `Modifier.widthIn(min = 56.dp, max = 72.dp)`
- This constrains the TextField to a reasonable size range while allowing it to fit on smaller screens

**Files Modified:**
- `ui/SettingsScreen.kt` - Changed OutlinedTextField modifier

**Lesson:** Avoid fixed widths for form elements that need to work across screen sizes. Use `widthIn()` to constrain to a reasonable range.

---

### Phase 5i: Widget Text Scaling for Small Screens ✅
**Date:** 2026-01-19

**Problem:** The widget button text ("Done!") was clipped/squished on smaller screens or higher density displays. The widget used fixed sizes (button: 52.dp, border: 60.dp, font: 18.sp) that didn't adapt to widget dimensions.

**Investigation:**
1. Initial attempt with `LocalSize.current` didn't work - returned default values
2. Discovered that `SizeMode.Exact` must be enabled for `LocalSize` to return actual widget dimensions
3. After enabling SizeMode.Exact, scaling worked but text still didn't fit
4. Root cause: `sp` and `dp` scale differently based on screen density; 18sp was too large relative to button size on high-density small screens

**Solution:**
1. Added `override val sizeMode = SizeMode.Exact` to enable accurate size reporting
2. Use `LocalSize.current` to get actual widget dimensions
3. Calculate scale factor from smaller dimension: `(minDimension / 70f).coerceIn(0.6f, 1.5f)`
4. Apply proportional scaling to all elements: border, button, font, icon, padding
5. Reduced base font from 18.sp to 15.sp to fit within button on high-density screens

**Files Modified:**
- `widget/BigButtonWidget.kt` - Added SizeMode import, sizeMode override, LocalSize-based scaling

**Key Code:**
```kotlin
override val sizeMode = SizeMode.Exact

// In BigButtonContent:
val size = LocalSize.current
val minDimension = min(size.width.value, size.height.value)
val scale = (minDimension / 70f).coerceIn(0.6f, 1.5f)

val borderSize = (60 * scale).dp
val buttonSize = (52 * scale).dp
val fontSize = (15 * scale).sp  // Reduced from 18sp
val iconSize = (16 * scale).dp
val iconPadding = (8 * scale).dp
```

**Lesson:**
1. Glance widgets need `SizeMode.Exact` for `LocalSize` to return actual dimensions
2. `sp` (scaled pixels for text) and `dp` (density-independent pixels) don't maintain the same ratio across different screen densities
3. When text must fit within a fixed UI element, the font size may need to be smaller than visually ideal to account for high-density displays

---

### Phase 5h (continued): Custom Field Width Adjustment
**Date:** 2026-01-20

**Problem:** On additional testing, the custom period TextField placeholder "1-90" was still wrapping inside the field on some devices.

**Solution:** Increased max width from 72.dp to 88.dp to accommodate the placeholder text on all screen densities.

**Files Modified:**
- `ui/SettingsScreen.kt` - Changed `widthIn(min = 56.dp, max = 88.dp)`

---

### Phase 5i (continued): Widget Font Size Fine-tuning
**Date:** 2026-01-20

**Problem:** After initial fix with 15sp base font, "Done!" text still didn't fit on high-density phone screens.

**Solution:** Further reduced base font from 15sp to 14sp after testing showed 12sp was too small but 15sp was still too large on some devices.

**Files Modified:**
- `widget/BigButtonWidget.kt` - Changed base font to 14sp

---

### Widget Preview Image Fix
**Date:** 2026-01-20

**Problem:** Widget picker showed "Loading..." text instead of actual widget preview when browsing widgets to add.

**Root Cause:** `android:previewLayout` in widget info XML was pointing to `widget_loading.xml` which displays loading text.

**Solution:** Created a static preview layout that shows the widget in "Do" state:
- `res/layout/widget_preview.xml` - FrameLayout with button appearance
- `res/drawable/preview_button_border.xml` - White oval for border ring
- `res/drawable/preview_button_do.xml` - Red gradient oval for button
- Updated `big_button_widget_info.xml` to use new preview layout

**Files Created:**
- `res/layout/widget_preview.xml`
- `res/drawable/preview_button_border.xml`
- `res/drawable/preview_button_do.xml`

**Files Modified:**
- `res/xml/big_button_widget_info.xml` - Changed previewLayout reference

---

### Package Name Change for Google Play
**Date:** 2026-01-20

**Reason:** Google Play rejects apps with `com.example.*` package names. Changed to production package name.

**Change:** `com.example.bigbutton` → `com.movingfingerstudios.bigbutton`

**Files Modified:**
- `app/build.gradle.kts` - Updated namespace and applicationId
- `AndroidManifest.xml` - Updated action name for ResetAlarmReceiver
- All 22 Kotlin files - Updated package declarations and imports
- Directory structure - Moved from `com/example/bigbutton` to `com/movingfingerstudios/bigbutton`

**Important:** After package name change, the old app must be uninstalled before installing the new version (Android treats it as a different app).

---

### Release Signing Configuration
**Date:** 2026-01-20

**Purpose:** Configure app signing for Google Play release.

**Implementation:**
- Created `keystore.properties.template` with placeholder values
- Added signing config to `app/build.gradle.kts` that reads from `keystore.properties`
- Updated `.gitignore` to exclude `keystore.properties` (contains passwords)

**Files Created:**
- `keystore.properties.template` - Template for credentials
- `bigbutton-upload.jks` - Upload keystore (not in git)
- `keystore.properties` - Actual credentials (not in git)

**Files Modified:**
- `app/build.gradle.kts` - Added signingConfigs and release signing
- `.gitignore` - Added keystore.properties

**Key Code (build.gradle.kts):**
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

signingConfigs {
    create("release") {
        if (keystorePropertiesFile.exists()) {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
}
```

**Lesson:** Keep signing credentials in a separate properties file that's gitignored. Use a template file to document required properties.

---

### Code Cleanup: Unused Variables
**Date:** 2026-01-20

**Problem:** Build warnings about unused variables.

**Warnings Fixed:**
1. `ResetAlarmReceiver.kt:110` - Removed unused `trackingStartStr` variable (value only used for null check)
2. `CalendarScreen.kt:236` - Removed unused `showMonthHeader` variable (dead code from earlier refactor)
3. `BigButtonWidget.kt:87` - Removed unused `buttonColor` variable (replaced by drawable resources)

**Files Modified:**
- `receiver/ResetAlarmReceiver.kt`
- `ui/CalendarScreen.kt`
- `widget/BigButtonWidget.kt`

---

## Release Preparation Complete
**Date:** 2026-01-20

**Release Bundle Location:** `app/build/outputs/bundle/release/app-release.aab`

**Ready for Google Play submission with:**
- Production package name: `com.movingfingerstudios.bigbutton`
- Version: 1.0 (versionCode: 1)
- Signed release bundle
- All warnings resolved
- Responsive UI tested on multiple screen sizes

---

## Testing Checklist

For each increment:
- [ ] Project builds without errors
- [ ] App runs on emulator/device
- [ ] Visual changes are as expected
- [ ] No regressions in existing functionality
- [ ] Accessibility guidelines met

---

Last Updated: 2026-01-20 (Release preparation complete - Package rename, signing, Play Store ready)
