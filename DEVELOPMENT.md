# BigButton Development

## Product Vision

BigButton is a habit tracking app that helps users track whether they've performed a specific action within a defined time period. Examples:
- "Watered the plants this week"
- "Took a vitamin today"
- "Called mom this month"

The app focuses on simplicity with a single-action interface presented as a home screen widget.

---

## Roadmap

### Phase 1: MVP Visual Design ✅ Complete
Non-interactive widget showing button and settings icon.

**Deliverables:**
- Widget appears on home screen
- Button displays "Do" state with proper styling
- Settings icon visible in corner
- Visual design matches mockup

### Phase 2: Basic Interactivity ✅ Complete
Tap functionality and state persistence.

**Deliverables:**
- Tap button to mark action as done (Do → Done)
- State persists across device restarts
- Widget updates immediately on tap

**Requirements:**

| ID | Requirement |
|----|-------------|
| P2.1 | Tapping button changes state from Do → Done |
| P2.2 | Tapping button when already Done does nothing (no toggle back) |
| P2.3 | Reset (Done → Do) only available via settings (Phase 3) |
| P2.4 | State persists across widget updates |
| P2.5 | State persists across app/device restarts |
| P2.6 | Widget UI updates immediately on tap |
| P2.7 | Settings icon does not trigger state change |

**Design Rationale:** One-way toggle (Do→Done only) prevents accidental resets. Users must intentionally reset via settings, ensuring completed actions aren't accidentally unmarked.

**Decisions Made:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| State storage | PreferencesGlanceStateDefinition | Built for Glance, simple, sufficient |
| Click target | Button only | Settings icon reserved for Phase 3 |
| State schema | isDone + lastChanged timestamp | Timestamp needed for Phase 4 auto-reset |

**State Schema:**
```
isDone: Boolean (default: false)
lastChanged: Long (epoch milliseconds)
```

**Implementation Plan:**

1. Create `BigButtonStateDefinition` - Glance state definition using Preferences
2. Create `MarkDoneAction` - ActionCallback to set isDone=true
3. Update `BigButtonWidget` - Read state, add clickable to button only
4. Update `BigButtonWidgetReceiver` - Wire up state definition

**Files to Create:**
- `widget/BigButtonStateDefinition.kt`
- `widget/MarkDoneAction.kt`

**Files to Modify:**
- `widget/BigButtonWidget.kt`
- `widget/BigButtonWidgetReceiver.kt`

**Testing Plan:**

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Mark done | Tap button in Do state | Changes to Done (green) |
| No toggle back | Tap button in Done state | Nothing happens, stays Done |
| Persist on resize | Mark done, resize widget | Still Done |
| Persist on reboot | Mark done, reboot device | Still Done |
| Rapid taps | Tap 10x quickly | No crash, shows Done |
| Settings isolation | Tap settings icon | Nothing happens |
| Fresh install | Install app, add widget | Shows Do state |

### Phase 3: Settings & Configuration ✅ Complete
User-configurable reset periods. Broken into sub-phases for incremental delivery.

---

#### Phase 3a: Settings Activity + Manual Reset ✅ Complete

**Deliverables:**
- Settings activity launched from settings icon tap
- Manual reset button to change Done → Do
- Basic settings UI scaffold

**Requirements:**

| ID | Requirement |
|----|-------------|
| P3a.1 | Tapping settings icon opens settings activity |
| P3a.2 | Settings activity displays "Reset" button |
| P3a.3 | Tapping Reset button sets widget to "Do" state |
| P3a.4 | Reset button only enabled when widget is in "Done" state |
| P3a.5 | After reset, user returns to home screen and sees updated widget |
| P3a.6 | Settings activity has proper back navigation |

**Decisions Made:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Activity vs Dialog | Activity | Room for future settings, standard pattern |
| UI Framework | Compose | Consistent with app |
| Navigation after reset | Auto-close | Confirms action, fewer steps |
| Activity approach | Repurpose MainActivity | Already exists, will become main app hub |

**Future App Structure (Tabs):**
```
MainActivity
├── Tab 1: Settings (Phase 3a/b/c)
├── Tab 2: Calendar & History (Phase 5)
└── Tab 3: Info / Manual (Future)
```

**Implementation Plan:**
1. Replace IntroScreen content with Settings UI in MainActivity
2. Create `OpenSettingsAction.kt` ActionCallback to launch MainActivity
3. Create `ResetAction.kt` to set isDone=false
4. Update widget settings icon to be clickable
5. Pass widget ID to MainActivity for state access
6. Add reset button UI (enabled only when isDone=true)

**Files to Create:**
- `widget/OpenSettingsAction.kt`
- `widget/ResetAction.kt`
- `ui/SettingsScreen.kt` (replaces IntroScreen content)

**Files to Modify:**
- `MainActivity.kt` (load SettingsScreen instead of IntroScreen)
- `BigButtonWidget.kt` (settings icon clickable)
- `IntroScreen.kt` (remove or repurpose)

**Testing Plan:**

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Open settings | Tap settings icon | Settings activity opens |
| Reset when Done | Mark done, open settings, tap Reset | Widget shows "Do", activity closes |
| Reset disabled when Do | Fresh widget, open settings | Reset button disabled/hidden |
| Back navigation | Open settings, press back | Returns to home |
| Widget updates | Reset from settings | Widget immediately shows "Do" |

---

#### Phase 3b: Period Selector ✅ Complete

**Deliverables:**
- Period configuration in settings
- Presets: Daily, Weekly, Monthly
- Custom option (1-90 days)
- Period saved to DataStore

**Requirements:**

| ID | Requirement |
|----|-------------|
| P3b.1 | Settings shows period selector below reset section |
| P3b.2 | Preset options: Daily (1 day), Weekly (7 days), Monthly (30 days) |
| P3b.3 | Custom option allows entering number of days (1-90) |
| P3b.4 | Selected period persists across app restarts |
| P3b.5 | Default period is 1 day (Daily) if not configured |
| P3b.6 | Period change takes effect on next reset cycle |

**Decisions Made:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI component | Radio list with 4 options | Clear hierarchy, all options visible |
| Custom input | Inline number field (1-90) | Always visible, no hidden state |
| Period storage | Same DataStore | Single source of truth |
| Max custom days | 90 | Practical limit, 365 unnecessary |
| Custom matches preset | Allowed | User can type "1" or "7" in custom; uses normalized remember key to prevent UI jumping |
| Custom radio click | Focus input field | Clicking Custom radio focuses the text input for immediate typing |

**UI Design:**
```
┌─────────────────────────────────┐
│  Period                         │
│  ┌─────────────────────────┐    │
│  │ ○ Daily (1 day)         │    │
│  ├─────────────────────────┤    │
│  │ ○ Weekly (7 days)       │    │
│  ├─────────────────────────┤    │
│  │ ○ Monthly (30 days)     │    │
│  ├─────────────────────────┤    │
│  │ ○ Custom: [__] days     │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘
```

**Implementation Plan:**
1. Add `PERIOD_DAYS` key to `BigButtonStateDefinition`
2. Add period selector UI to `SettingsScreen.kt`
3. Implement radio selection with DataStore persistence
4. Add number input for custom with validation (1-90)

**Files to Modify:**
- `widget/BigButtonStateDefinition.kt` (add PERIOD_DAYS key)
- `ui/SettingsScreen.kt` (add period selector UI)

**State Schema Addition:**
```
periodDays: Int (default: 1)
```

**Testing Plan:**

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Default period | Fresh install, open settings | Shows "Daily" selected |
| Select weekly | Tap "Weekly" | Selection saved, persists after restart |
| Select monthly | Tap "Monthly" | Selection saved, persists after restart |
| Custom period | Select custom, enter 14 | Saves 14 days, persists |
| Custom min | Enter 0 | Validation error or clamped to 1 |
| Custom max | Enter 100 | Validation error or clamped to 90 |
| Persistence | Set period, restart app | Period preserved |

---

#### Phase 3c: Reset Time Selector ✅ Complete

**Deliverables:**
- Reset time configuration (hour of day)
- Time picker UI
- Reset time saved to DataStore

**Requirements:**

| ID | Requirement |
|----|-------------|
| P3c.1 | Settings shows reset time selector |
| P3c.2 | Time picker allows selecting hour (and optionally minute) |
| P3c.3 | Default reset time is 4:00 AM |
| P3c.4 | Reset time persists across app restarts |
| P3c.5 | Display shows selected time in 12h or 24h format (follow system) |

**Decisions Made:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Time granularity | Hour only | Simpler, sufficient for habit reset |
| Time picker UI | Android TimePickerDialog | Familiar UX, less code |
| Display format | System preference | Respects user's 12h/24h setting |

**State Schema Addition:**
```
resetHour: Int (default: 4, range: 0-23)
```

**Testing Plan:**

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Default time | Fresh install, open settings | Shows "4:00 AM" |
| Change time | Tap time, select 6 AM | Shows "6:00 AM", persists |
| Midnight edge case | Select 12:00 AM | Shows "12:00 AM" (midnight) |
| Persistence | Set time, restart app | Time preserved |

---

### Phase 4: Automatic Reset ✅ Complete
Background scheduling for period resets.

**Deliverables:**
- Widget automatically resets to "Do" when period elapses
- AlarmManager integration for exact-time scheduled resets
- Fallback check on widget load/interaction
- Reliable reset even if device was off
- Reset time supports hour + minute granularity

**Dependencies:** Phase 3b (period) and Phase 3c (reset time) - ✅ Complete

**Requirements:**

| ID | Requirement |
|----|-------------|
| P4.1 | Widget resets to "Do" at configured reset time after period elapses |
| P4.2 | Reset occurs even if device was off during scheduled time |
| P4.3 | Widget shows accurate state immediately when viewed/tapped |
| P4.4 | Reset respects local timezone |
| P4.5 | Period/time changes apply correctly to next reset |

**Decisions Made:**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scheduling mechanism | AlarmManager with setExactAndAllowWhileIdle() | Exact timing even in Doze mode; WorkManager timing was inexact |
| Reset logic | Hybrid (scheduled alarm + on-load check) | Best reliability - exact alarm + fallback on widget interaction |
| Device off during reset | Reset immediately on wake | User expects fresh start when period elapsed |
| Timezone handling | Follow local time | More natural for habit tracking |
| Period change mid-cycle | Use lastChanged as anchor | If marked Done on Monday, weekly reset = next Monday |
| Reset time change | Apply immediately if due | If reset already due, apply it; otherwise use for next reset |
| Time granularity | Hour + Minute | More flexible, easier to test |

**Reset Logic Algorithm:**
```
function shouldReset(lastChanged, periodDays, resetHour, resetMinute):
    nextResetDateTime = dateOf(lastChanged) + periodDays days, at resetHour:resetMinute

    if now >= nextResetDateTime:
        return true
    return false
```

**Implementation (Completed):**

1. ✅ **Created ResetCalculator utility** (`util/ResetCalculator.kt`)
   - `shouldReset(lastChanged, periodDays, resetHour, resetMinute): Boolean`
   - `calculateNextResetTime()` - for reset due calculation
   - `calculateNextResetTimeFromNow()` - for alarm scheduling

2. ✅ **Created AlarmManager-based scheduling** (`receiver/ResetAlarmScheduler.kt`)
   - Uses `setExactAndAllowWhileIdle()` for precise timing
   - `scheduleNextReset(context, resetHour, resetMinute)`
   - `scheduleImmediateCheck(context)`
   - `cancelScheduledReset(context)`

3. ✅ **Created ResetAlarmReceiver** (`receiver/ResetAlarmReceiver.kt`)
   - BroadcastReceiver handles alarm trigger
   - Checks if reset is due, performs reset, updates widget
   - Schedules next alarm

4. ✅ **Created BootReceiver** (`receiver/BootReceiver.kt`)
   - Reschedules alarm after device reboot

5. ✅ **Updated BigButtonWidget**
   - On-load check in `provideGlance()` before rendering
   - Uses `updateAppWidgetState` for proper Glance state sync

6. ✅ **Updated scheduling triggers**
   - Widget added → schedules immediate check
   - Mark Done → schedules next reset alarm
   - Period/time change in settings → reschedules alarm

7. ✅ **Added minute support to reset time**
   - `RESET_MINUTE` key in DataStore
   - Time picker saves both hour and minute
   - Display shows "4:30 AM" format

**Files Created:**
- `util/ResetCalculator.kt` - Reset timing calculations
- `receiver/ResetAlarmScheduler.kt` - AlarmManager scheduling
- `receiver/ResetAlarmReceiver.kt` - Alarm broadcast handler
- `receiver/BootReceiver.kt` - Reschedule on device boot

**Files Modified:**
- `widget/BigButtonStateDefinition.kt` - Added RESET_MINUTE key
- `widget/BigButtonWidget.kt` - On-load reset check
- `widget/BigButtonWidgetReceiver.kt` - Schedule on widget add/remove
- `widget/MarkDoneAction.kt` - Schedule after marking Done
- `ui/SettingsScreen.kt` - Minute support, reschedule on changes
- `AndroidManifest.xml` - Permissions and receiver registrations

**State Schema:**
```
isDone: Boolean
lastChanged: Long (epoch ms) - anchor for reset calculation
periodDays: Int
resetHour: Int (0-23)
resetMinute: Int (0-59) - NEW
```

**Permissions Added:**
- `SCHEDULE_EXACT_ALARM` - Required for exact alarms on API 31+
- `RECEIVE_BOOT_COMPLETED` - Reschedule after reboot

**Testing Plan:**

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Basic reset | Set daily, reset time 2-3 min from now, mark Done, wait | Widget auto-resets to "Do" |
| On-load check | Set reset time in past, mark Done, force-stop app, TAP widget | Widget shows "Do" |
| Device restart | Set reset time, mark Done, restart device after time | Widget shows "Do" |
| Future reset | Set daily, mark Done, check before reset time | Widget stays "Done" |
| Period change | Change period, verify alarm rescheduled | No crash, continues working |
| Time change | Change reset time | Alarm rescheduled to new time |
| Cancel on remove | Remove all widgets | Alarm cancelled |

**Current Status:** ✅ Complete and tested.

**Quick Test Procedure:**
1. Build and install app
2. **Android 12+ REQUIRED:** Go to Settings > Apps > BigButton > Alarms & reminders and enable it
3. Add widget to home screen
4. Open settings, set period to 1 day
5. Set reset time to 2-3 minutes from now (e.g., if 1:15 PM, set to 1:17 PM)
6. Tap the widget button to mark "Done"
7. Wait for reset time
8. Widget should automatically change to "Do"

**Important Notes:**
- On Android 12+ (API 31+), the `SCHEDULE_EXACT_ALARM` permission requires **manual user approval** in system settings
- Without this permission, exact alarms will not fire and automatic reset will not work
- The app includes `canScheduleExactAlarms()` check and logging for debugging

---

### Phase 4.1: Tab Navigation & Info Screen

Add tab-based navigation to MainActivity with three tabs: Settings, Calendar, and Info.

**Deliverables:**
- Tab navigation in MainActivity (Settings | Calendar | Info)
- Info screen with About section and permissions guidance
- Calendar tab placeholder (content in Phase 5)
- Settings remains the default tab

**Dependencies:** Phase 4 (Automatic Reset) - ✅ Complete

---

#### Phase 4.1 Requirements

| ID | Requirement |
|----|-------------|
| P4.1.1 | MainActivity displays three tabs: Settings, Calendar, Info |
| P4.1.2 | Settings tab is selected by default on app open |
| P4.1.3 | Tab selection persists during session (survives rotation) |
| P4.1.4 | Info tab displays app name, version, and author |
| P4.1.5 | Info tab includes permissions guidance for Android 12+ |
| P4.1.6 | Calendar tab shows placeholder text (implemented in Phase 5) |

---

#### Phase 4.1 Design

**Tab Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│  BigButton                                                  │
├───────────────┬───────────────┬─────────────────────────────┤
│   Settings    │   Calendar    │    Info                     │
│   (active)    │               │                             │
├───────────────┴───────────────┴─────────────────────────────┤
│                                                             │
│  [Current Settings Screen content]                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Info Screen Content:**
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  BigButton                                                  │
│  Version 1.0                                                │
│                                                             │
│  A simple habit tracking widget.                            │
│                                                             │
│  ─────────────────────────────────────────────              │
│                                                             │
│  ⚠️  Permissions                                            │
│                                                             │
│  For automatic reset to work on Android 12+,                │
│  you need to enable "Alarms & reminders" permission:        │
│                                                             │
│  Settings → Apps → BigButton → Alarms & reminders           │
│                                                             │
│  [Open App Settings]  ← button to open system settings      │
│                                                             │
│  ─────────────────────────────────────────────              │
│                                                             │
│  Made with ♥ by [Your Name]                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Calendar Tab Placeholder:**
```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│              📅                                             │
│                                                             │
│         Calendar coming soon                                │
│                                                             │
│    Track your completion history over time.                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

#### Phase 4.1 Technical TODOs

1. **Update `MainActivity.kt`:**
   - Add `TabRow` or `NavigationBar` with three tabs
   - Track selected tab with `rememberSaveable` (survives rotation)
   - Conditionally render content based on selected tab
   - Default to Settings tab (index 0)

2. **Create `ui/InfoScreen.kt`:**
   - App name and version (read from BuildConfig)
   - Brief description
   - Permissions section with:
     - Explanation of alarm permission requirement
     - "Open App Settings" button using `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)`
   - Made by / credits section

3. **Create `ui/CalendarScreen.kt` (placeholder):**
   - Simple centered text: "Calendar coming soon"
   - Icon or illustration (optional)
   - Brief description of upcoming feature

4. **Refactor `ui/SettingsScreen.kt`:**
   - Ensure it works as a tab content (no Scaffold of its own)
   - Move any top-level app bar to MainActivity if needed

**Files to Create:**
- `ui/InfoScreen.kt`
- `ui/CalendarScreen.kt` (placeholder)

**Files to Modify:**
- `MainActivity.kt` (add tab navigation)
- `ui/SettingsScreen.kt` (ensure works as tab content)

---

#### Phase 4.1 Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Tab style | Top tabs (TabRow) | Standard Material pattern, fits 3 tabs well |
| Default tab | Settings | Most common action, matches current behavior |
| Tab state | rememberSaveable | Survives configuration changes |
| Permission button | Opens system app settings | Direct path to enable alarms |

---

#### Phase 4.1 Testing Plan

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Default tab | Open app | Settings tab selected |
| Tab switching | Tap Calendar tab | Calendar placeholder shown |
| Tab switching | Tap Info tab | Info screen shown |
| Tab persistence | Rotate device | Same tab stays selected |
| Info content | View Info tab | Shows version, permissions, credits |
| Permission button | Tap "Open App Settings" | System settings opens for BigButton |
| Settings functionality | Use Settings tab | All existing features work |

---

### Phase 5: History & Calendar
Track completion history over time with a visual calendar.

**Deliverables:**
- Continuously scrollable calendar view
- Color-coded days: green (completed), red (missed), grey (in progress)
- Room database for historical data storage
- Reset/erase history option in settings

**Dependencies:** Phase 4 (Automatic Reset) - ✅ Complete

See `calendar_mockup.png` for visual reference.

---

#### Phase 5 Design Specification

##### Core Concept

The calendar displays **per-day completion status** based on the period configuration:
- **Daily period:** Each day is independently green/red based on whether user pressed Done that day
- **Multi-day period (e.g., 3 days):** All days in the period share the same color based on whether user pressed Done at any point during that period

**Key principle:** Status is only "locked in" when a period **ends**. The current in-progress period shows grey until it completes.

##### Data Model

**Storage Strategy:**
- **DataStore (existing):** Widget state and configuration (isDone, periodDays, resetHour, etc.)
- **Room Database (new):** Historical completion data

**Room Entities:**

```kotlin
@Entity(tableName = "completion_events")
data class CompletionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,  // epoch millis when user pressed "Done"
    val periodDays: Int   // period setting at time of completion (for audit)
)

@Entity(tableName = "finalized_days")
data class FinalizedDay(
    @PrimaryKey val date: String,  // "2026-01-15" (LocalDate ISO format)
    val completed: Boolean         // true = done (green), false = missed (red)
)

@Entity(tableName = "tracking_metadata")
data class TrackingMetadata(
    @PrimaryKey val key: String,
    val value: String
)
// Keys: "tracking_start_date", "last_finalized_date"
```

**Why this structure:**
- `CompletionEvent`: Audit trail of every "Done" press, enables future analytics
- `FinalizedDay`: Pre-computed day status for fast calendar rendering
- `TrackingMetadata`: Configuration that affects history interpretation

##### Period Lifecycle

```
┌─────────────────────────────────────────────────────────────────┐
│                        PERIOD LIFECYCLE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User presses "Done"                                            │
│        │                                                        │
│        ▼                                                        │
│  ┌──────────────────┐                                           │
│  │ Record timestamp │ → CompletionEvent saved to Room           │
│  │ in completion_   │                                           │
│  │ events table     │                                           │
│  └──────────────────┘                                           │
│        │                                                        │
│        ▼                                                        │
│  Widget shows "Done!" (existing behavior)                       │
│                                                                 │
│  ════════════════════════════════════════════════════════════   │
│                                                                 │
│  Reset alarm fires (period ends)                                │
│        │                                                        │
│        ▼                                                        │
│  ┌──────────────────┐                                           │
│  │ Finalize period  │ → For each day in the ended period:       │
│  │                  │   - Check if any CompletionEvent exists   │
│  │                  │   - Write FinalizedDay (true/false)       │
│  └──────────────────┘                                           │
│        │                                                        │
│        ▼                                                        │
│  Widget resets to "Do" (existing behavior)                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

##### Tracking Start Date

- **Set automatically** when user first presses "Done" (if not already set)
- **Alternative:** Set when period configuration is first saved in settings
- **Stored in:** `TrackingMetadata` table with key `"tracking_start_date"`

##### Calendar UI Specification

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│                     [Calendar Tab]                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  January 2026                                       │    │
│  │  Sun   Mon   Tue   Wed   Thu   Fri   Sat            │    │
│  │                     1     2     3                    │    │
│  │   4     5    [6]   [7]   (8)   (9)   [10]           │    │
│  │  [11]  [12]  [13]  [14]  [15]  {16}  {17}           │    │
│  │   18    19    20    21    22    23    24            │    │
│  │   25    26    27    28    29    30    31            │    │
│  ├─────────────────────────────────────────────────────┤    │
│  │  February 2026                                      │    │
│  │  Sun   Mon   Tue   Wed   Thu   Fri   Sat            │    │
│  │   1     2     3     4     5     6     7             │    │
│  │   ...                                               │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│                    ▼ (scroll continues)                     │
└─────────────────────────────────────────────────────────────┘

Legend:
  [N]  = Green (completed)
  (N)  = Red (missed)
  {N}  = Grey (current period, in progress)
   N   = No color (future / before tracking)
```

**Visual Properties:**

| Element | Specification |
|---------|---------------|
| Day cell size | ~40dp square |
| Completed (green) | Background: #81C784 (same as Done button) |
| Missed (red) | Background: #E57373 (same as Do button) |
| In-progress (grey) | Background: #BDBDBD (Material grey 400) |
| Day number text | 14sp, center-aligned |
| Month header | 18sp bold, sticky or inline |
| Week header | Sun-Sat labels, 12sp, muted color |

**Scrolling Behavior:**
- Continuous vertical scroll (not paginated by month)
- Month headers appear inline when month changes
- Smooth scrolling with momentum
- Initial position: scroll to show current week/month

**Scroll Bounds:**
- **Top:** Beginning of month containing tracking start date
- **Bottom:** End of current month (or current week + buffer)

##### Period Boundary Calculation

```kotlin
/**
 * Calculate which period a given date belongs to.
 * Periods are sequential starting from trackingStartDate.
 *
 * @param date The date to check
 * @param trackingStartDate When tracking began
 * @param periodDays Length of each period
 * @param resetHour Hour when periods reset (for boundary calculation)
 * @param resetMinute Minute when periods reset
 * @return Period index (0-based) and whether this period has ended
 */
fun calculatePeriodInfo(
    date: LocalDate,
    trackingStartDate: LocalDate,
    periodDays: Int,
    resetHour: Int,
    resetMinute: Int
): PeriodInfo {
    // Days since tracking started
    val daysSinceStart = ChronoUnit.DAYS.between(trackingStartDate, date)

    // Period index (0 = first period)
    val periodIndex = daysSinceStart / periodDays

    // Period start and end dates
    val periodStartDate = trackingStartDate.plusDays(periodIndex * periodDays)
    val periodEndDate = periodStartDate.plusDays(periodDays - 1)

    // Check if period has ended (reset time has passed on end date)
    val periodEndDateTime = periodEndDate.atTime(resetHour, resetMinute)
    val hasEnded = LocalDateTime.now() >= periodEndDateTime

    return PeriodInfo(periodIndex, periodStartDate, periodEndDate, hasEnded)
}
```

##### Finalization Logic

When reset alarm fires (in `ResetAlarmReceiver`):

```kotlin
suspend fun finalizeEndedPeriod(context: Context, db: AppDatabase) {
    val trackingStart = db.metadataDao().getTrackingStartDate() ?: return
    val lastFinalized = db.metadataDao().getLastFinalizedDate()

    val periodDays = // read from DataStore
    val resetHour = // read from DataStore
    val resetMinute = // read from DataStore

    // Calculate the period that just ended
    val endedPeriod = calculateEndedPeriod(trackingStart, periodDays, resetHour, resetMinute)

    // Check if any completion event exists in this period
    val completions = db.completionEventDao().getEventsBetween(
        endedPeriod.startDate.toEpochMillis(),
        endedPeriod.endDate.toEpochMillis()
    )
    val wasCompleted = completions.isNotEmpty()

    // Write finalized status for each day in the period
    for (day in endedPeriod.startDate..endedPeriod.endDate) {
        db.finalizedDayDao().insert(
            FinalizedDay(date = day.toString(), completed = wasCompleted)
        )
    }

    // Update last finalized date
    db.metadataDao().setLastFinalizedDate(endedPeriod.endDate)
}
```

##### Calendar Rendering Logic

```kotlin
@Composable
fun CalendarDay(
    date: LocalDate,
    finalizedDays: Map<String, Boolean>,
    currentPeriodDates: Set<LocalDate>,
    trackingStartDate: LocalDate?
) {
    val dateStr = date.toString()

    val backgroundColor = when {
        // Future date
        date > LocalDate.now() -> Color.Transparent

        // Before tracking started
        trackingStartDate == null || date < trackingStartDate -> Color.Transparent

        // Current period (in progress)
        date in currentPeriodDates -> Grey400

        // Finalized - completed
        finalizedDays[dateStr] == true -> Green400

        // Finalized - missed
        finalizedDays[dateStr] == false -> Red400

        // Not yet finalized (edge case: old period not finalized)
        else -> Color.Transparent
    }

    // Render day cell with backgroundColor
}
```

##### Manual Reset Behavior

**Key principle:** Manual reset during the current period acts as an "undo" for accidental presses.

When user manually resets (via Settings):
1. Widget state set to `isDone = false`
2. **Delete all `CompletionEvent` records from the current period**
3. This allows the period to end as "missed" (red) if user doesn't press Done again

```kotlin
suspend fun manualReset(context: Context, db: AppDatabase) {
    // 1. Reset widget state
    updateWidgetState(isDone = false)

    // 2. Delete completion events from current period
    val currentPeriod = calculateCurrentPeriod(...)
    db.completionEventDao().deleteEventsBetween(
        currentPeriod.startMillis,
        currentPeriod.endMillis
    )
}
```

**Behavior summary:**

| Action | Effect on Widget | Effect on History |
|--------|------------------|-------------------|
| Press Done | Shows "Done!" | CompletionEvent recorded |
| Manual reset (same period) | Shows "Do" | CompletionEvent deleted |
| Manual reset (after period ended) | Shows "Do" | Past finalization unchanged |

This allows users to fix mistakes during the current period while keeping historical integrity.

##### Period Change Handling

When user changes period length in settings:
1. **Finalized days are immutable** - past `FinalizedDay` records never change
2. **Current period is abandoned** - unfinalized days remain unfinalized (show as transparent/no-data)
3. **New periods start from today** - fresh start with new period length

**Implementation:**
```kotlin
suspend fun onPeriodChanged(newPeriodDays: Int) {
    // 1. Save new period to DataStore
    dataStore.edit { it[PERIOD_DAYS] = newPeriodDays }

    // 2. Do NOT finalize abandoned period - just leave those days unfinalized
    // They will show as transparent (no data) in calendar

    // 3. Reschedule alarm for new period
    ResetAlarmScheduler.scheduleNextReset(context, resetHour, resetMinute)
}
```

**Example: Weekly (7-day) → Daily (1-day)**

User is on day 4 of a 7-day period, pressed Done on day 2:

| Day | Had Completion Event? | After Change |
|-----|----------------------|--------------|
| Day 1 (Jan 6) | No | Transparent (no data) - abandoned period |
| Day 2 (Jan 7) | Yes | Transparent (no data) - abandoned period |
| Day 3 (Jan 8) | No | Transparent (no data) - abandoned period |
| Day 4 (Jan 9) | — | Grey (current 1-day period) |
| Day 5+ | — | New daily periods |

**Example: Daily (1-day) → Weekly (7-day)**

Past daily periods already finalized:

| Day | Finalized Status | After Change |
|-----|-----------------|--------------|
| Jan 6 | Green (done) | Green (unchanged) |
| Jan 7 | Red (missed) | Red (unchanged) |
| Jan 8 | Green (done) | Green (unchanged) |
| Jan 9-15 | — | Grey (current 7-day period) |

**Rationale:** Abandoned periods are not "failed" - the user changed the rules mid-game. Showing them as "no data" is more accurate than marking them red.

##### FinalizedDay Immutability

Use `INSERT ... ON CONFLICT IGNORE` to ensure finalized days can never be overwritten:

```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insert(day: FinalizedDay)
```

This prevents edge cases where re-running finalization could change historical data.

##### Edge Cases

| Scenario | Behavior |
|----------|----------|
| App installed, no widget added | No tracking starts |
| Widget added, never pressed | Tracking starts on first press |
| Manual reset during period | Deletes completion events, allows "undo" |
| Manual reset after period ended | Widget resets, history unchanged |
| Period shortened (e.g., 7→1 day) | Abandoned period days show as no-data (transparent) |
| Period lengthened (e.g., 1→7 day) | Past finalized days unchanged, new period starts |
| Reset time changed mid-period | Current period uses new time, past unchanged |
| Device time changed backwards | Finalized days immutable; may show gaps |
| Device time changed forwards | Missed periods finalized on next alarm/launch |
| App force-stopped for weeks | On next launch, finalize all missed periods |
| Multiple "Done" presses in same period | All recorded; any one is sufficient for completion |
| Widget removed and re-added | Tracking continues from existing data |

##### Settings Integration

Add to Settings screen:
- **"Clear History" button** - Erases all Room data (CompletionEvent, FinalizedDay, TrackingMetadata)
- Confirmation dialog: "This will permanently delete all tracking history. This cannot be undone."
- After clear: tracking restarts on next "Done" press

##### Navigation

**Note:** Tab structure is established in Phase 4.1. Phase 5 replaces the Calendar placeholder with actual content.

```
┌─────────────────────────────────────────────────────────────┐
│  BigButton                                                  │
├───────────────┬───────────────┬─────────────────────────────┤
│   Settings    │   Calendar    │    Info                     │
│               │   (Phase 5)   │   (Phase 4.1)               │
└───────────────┴───────────────┴─────────────────────────────┘
```

**Tab Structure (from Phase 4.1):**
- Tab 1: Settings (existing content)
- Tab 2: Calendar (placeholder → real content in Phase 5)
- Tab 3: Info (about, permissions)

---

#### Phase 5 Requirements

| ID | Requirement |
|----|-------------|
| P5.1 | Calendar view displays scrollable history of tracked days |
| P5.2 | Completed periods show as green |
| P5.3 | Missed periods show as red |
| P5.4 | Current in-progress period shows as grey |
| P5.5 | Days before tracking started show no color |
| P5.6 | Future days show no color |
| P5.7 | Multi-day periods color all days in the period identically |
| P5.8 | Period changes do not affect past finalized data |
| P5.9 | "Clear History" option in settings erases all tracking data |
| P5.10 | Calendar scrolls continuously (not paginated by month) |
| P5.11 | Month headers appear inline during scroll |
| P5.12 | Historical data persists across app restarts |
| P5.13 | Completion events are recorded when user presses "Done" |
| P5.14 | Period finalization occurs when reset alarm fires |

---

#### Phase 5 Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage for history | Room database | Structured queries, scales well, standard Android practice |
| Day status storage | Per-day records (FinalizedDay) | Fast rendering, handles period changes gracefully |
| Period boundary anchor | Tracking start date | Predictable, user-controlled |
| Current period display | Grey color | Prevents gaming; status locked only when period ends |
| Calendar scroll style | Continuous vertical | Handles periods spanning month boundaries naturally |
| Past data on period change | Immutable | Simplifies logic, preserves historical accuracy |
| Tracking start trigger | First "Done" press | Natural starting point for habit tracking |

---

#### Phase 5a: Room Database Setup

**Deliverables:**
- Room database with entities, DAOs, and database class
- Database initialization in Application class

**Technical TODOs:**

1. **Add Room dependencies to `build.gradle.kts`**
   ```kotlin
   implementation("androidx.room:room-runtime:2.6.1")
   implementation("androidx.room:room-ktx:2.6.1")
   kapt("androidx.room:room-compiler:2.6.1")
   ```
   Also add kapt plugin: `id("kotlin-kapt")`

2. **Create `data/db/entities/` package with entity classes:**
   - `CompletionEvent.kt` - Records each "Done" press
   - `FinalizedDay.kt` - Stores locked day status
   - `TrackingMetadata.kt` - Key-value config storage

3. **Create `data/db/dao/` package with DAO interfaces:**
   - `CompletionEventDao.kt`
     - `insert(event: CompletionEvent)`
     - `getEventsBetween(startMillis: Long, endMillis: Long): List<CompletionEvent>`
     - `deleteEventsBetween(startMillis: Long, endMillis: Long)` - for manual reset undo
     - `deleteAll()`
   - `FinalizedDayDao.kt`
     - `insert(day: FinalizedDay)` (with **IGNORE** strategy - immutable once written)
     - `getAll(): List<FinalizedDay>`
     - `getDaysBetween(startDate: String, endDate: String): List<FinalizedDay>`
     - `deleteAll()`
   - `TrackingMetadataDao.kt`
     - `get(key: String): TrackingMetadata?`
     - `set(metadata: TrackingMetadata)` (with REPLACE strategy)
     - `deleteAll()`

4. **Create `data/db/AppDatabase.kt`:**
   - Abstract class extending RoomDatabase
   - Expose DAOs
   - Singleton pattern with `getInstance(context)`

5. **Create `BigButtonApplication.kt`:**
   - Application class for database initialization
   - Register in AndroidManifest.xml

**Files to Create:**
- `data/db/entities/CompletionEvent.kt`
- `data/db/entities/FinalizedDay.kt`
- `data/db/entities/TrackingMetadata.kt`
- `data/db/dao/CompletionEventDao.kt`
- `data/db/dao/FinalizedDayDao.kt`
- `data/db/dao/TrackingMetadataDao.kt`
- `data/db/AppDatabase.kt`
- `BigButtonApplication.kt`

**Files to Modify:**
- `build.gradle.kts` (add Room dependencies)
- `AndroidManifest.xml` (register Application class)

**Testing:**
- App builds without errors
- Database can be instantiated
- Basic insert/query works (via debugger or test)

---

#### Phase 5b: Completion Event Recording & Manual Reset

**Deliverables:**
- "Done" presses recorded to Room database
- Tracking start date set on first completion
- Manual reset deletes current period's completion events (undo functionality)

**Technical TODOs:**

1. **Update `MarkDoneAction.kt`:**
   - Get database instance
   - On successful mark done:
     - Insert `CompletionEvent(timestamp = now, periodDays = currentPeriod)`
     - If tracking not started, set `tracking_start_date` in metadata

2. **Create helper extension in `TrackingMetadataDao.kt`:**
   ```kotlin
   suspend fun getTrackingStartDate(): LocalDate?
   suspend fun setTrackingStartDate(date: LocalDate)
   ```

3. **Add database access to widget actions:**
   - Context → Application → Database pattern
   - Or inject via Hilt (optional, adds complexity)

4. **Update `ui/SettingsScreen.kt` manual reset:**
   - After resetting widget state, delete completion events from current period
   - Calculate current period boundaries using `PeriodCalculator`
   - Call `completionEventDao.deleteEventsBetween(periodStart, periodEnd)`
   - This enables "undo" for accidental Done presses

**Files to Modify:**
- `widget/MarkDoneAction.kt`
- `ui/SettingsScreen.kt` (manual reset deletes events)
- `data/db/dao/TrackingMetadataDao.kt` (add helper functions)

**Testing:**
- Press "Done" on widget
- Verify `completion_events` table has new record (via App Inspection in Android Studio)
- Verify `tracking_metadata` has `tracking_start_date` entry
- Multiple presses create multiple events
- **Manual reset test:** Press Done, then reset via settings → completion event deleted
- **Undo test:** Press Done, reset, let period end → day shows red (not green)

---

#### Phase 5c: Period Finalization

**Deliverables:**
- Periods finalized when reset alarm fires
- Missed periods catch-up on app launch

**Technical TODOs:**

1. **Create `util/PeriodCalculator.kt`:**
   - `calculateCurrentPeriod(trackingStart, periodDays, resetHour, resetMinute): PeriodInfo`
   - `calculatePeriodForDate(date, trackingStart, periodDays): PeriodInfo`
   - `getPeriodsToFinalize(trackingStart, lastFinalized, periodDays, resetHour, resetMinute): List<PeriodInfo>`
   - Data class `PeriodInfo(startDate: LocalDate, endDate: LocalDate, hasEnded: Boolean)`

2. **Update `ResetAlarmReceiver.kt`:**
   - After reset logic, call `finalizeEndedPeriods(context)`
   - Finalization logic:
     - Get tracking start date from metadata
     - Get last finalized date from metadata
     - Calculate periods that need finalization
     - For each period:
       - Query completion events in date range
       - Write FinalizedDay for each day (completed = events.isNotEmpty())
     - Update last finalized date

3. **Handle catch-up on app launch:**
   - In `BigButtonWidgetReceiver.onEnabled()` or widget update
   - Check if any periods need finalization (app was closed during reset times)
   - Finalize missed periods

4. **Create `util/FinalizationHelper.kt`:**
   - Centralize finalization logic used by both alarm receiver and catch-up

**Files to Create:**
- `util/PeriodCalculator.kt`
- `util/FinalizationHelper.kt`

**Files to Modify:**
- `receiver/ResetAlarmReceiver.kt`
- `widget/BigButtonWidgetReceiver.kt` (catch-up check)

**Testing:**
- Set period to 1 day, reset time 2 min from now
- Press "Done", wait for reset
- Check `finalized_days` table has entry for today with `completed=true`
- Repeat without pressing Done → entry with `completed=false`
- Force stop app, wait past reset time, reopen → catch-up finalization occurs

---

#### Phase 5d: Calendar UI - Basic

**Deliverables:**
- Replace Calendar tab placeholder with actual calendar grid
- Basic calendar grid showing colored days

**Dependencies:** Phase 4.1 (Tab Navigation) - tab structure already in place

**Technical TODOs:**

1. **Update `ui/CalendarScreen.kt`:**
   - Replace placeholder with actual calendar implementation
   - Load finalized days from Room (as StateFlow or collectAsState)
   - Load tracking start date from Room
   - Calculate current period for grey highlighting
   - Basic month grid:
     - LazyColumn with month sections
     - Each month: header + 7-column grid of days
     - Day cells with colored backgrounds

2. **Create `ui/components/CalendarDay.kt`:**
   - Composable for single day cell
   - Parameters: date, backgroundColor, isToday
   - 40dp square, centered text, rounded corners

3. **Create `ui/components/CalendarMonth.kt`:**
   - Composable for one month section
   - Month header text
   - Week day labels (Sun-Sat)
   - Grid of CalendarDay cells

4. **Create `data/repository/HistoryRepository.kt`:**
   - Abstracts database access for UI
   - `getFinalizedDays(): Flow<Map<String, Boolean>>`
   - `getTrackingStartDate(): Flow<LocalDate?>`
   - `getCurrentPeriodDates(): Flow<Set<LocalDate>>`

**Files to Create:**
- `ui/components/CalendarDay.kt`
- `ui/components/CalendarMonth.kt`
- `data/repository/HistoryRepository.kt`

**Files to Modify:**
- `ui/CalendarScreen.kt` (replace placeholder with calendar grid)

**Testing:**
- Tap Calendar tab → see month grid (not placeholder)
- Days show correct colors based on finalized data
- Current period shows grey
- Empty state shows correctly when no tracking data

---

#### Phase 5e: Calendar UI - Polish

**Deliverables:**
- Continuous smooth scrolling
- Proper month boundaries and headers
- Scroll to current week on open
- Performance optimization

**Technical TODOs:**

1. **Implement continuous scrolling:**
   - Calculate total weeks from tracking start to current month end
   - LazyColumn with weeks as items (not months)
   - Insert month header rows when month changes

2. **Scroll to current position:**
   - On first composition, scroll to week containing today
   - `LaunchedEffect` with `listState.scrollToItem()`

3. **Visual polish:**
   - Today indicator (border or highlight)
   - Sticky week day headers (optional, may be complex)
   - Smooth color transitions
   - Loading state while data loads

4. **Performance:**
   - Only load visible date range from Room
   - Paging if history is very long (optional)
   - Remember scroll position on tab switch

5. **Edge case handling:**
   - Empty state (no tracking yet)
   - Very short tracking history (< 1 month)
   - Future months (show but no color)

**Files to Modify:**
- `ui/CalendarScreen.kt`
- `ui/components/CalendarMonth.kt`
- `data/repository/HistoryRepository.kt`

**Testing:**
- Scroll smoothly through multiple months
- Month headers appear at correct positions
- On open, scrolls to show current week
- No jank with large history

---

#### Phase 5f: Clear History

**Deliverables:**
- "Clear History" button in Settings
- Confirmation dialog
- Complete data erasure

**Technical TODOs:**

1. **Update `ui/SettingsScreen.kt`:**
   - Add "Clear History" section at bottom
   - Button with destructive styling (red or outlined)
   - Click shows confirmation dialog

2. **Create confirmation dialog:**
   - AlertDialog with warning text
   - "Cancel" and "Clear" buttons
   - Clear button calls repository to delete all data

3. **Update `data/repository/HistoryRepository.kt`:**
   - `clearAllHistory()` function
   - Deletes from all three tables
   - Runs in IO dispatcher

4. **Post-clear behavior:**
   - Show toast/snackbar "History cleared"
   - Calendar shows empty state
   - Next "Done" press starts fresh tracking

**Files to Modify:**
- `ui/SettingsScreen.kt`
- `data/repository/HistoryRepository.kt`

**Testing:**
- Have some history, press Clear History
- Confirm dialog appears
- After confirm, calendar is empty
- Database tables are empty
- Press Done → new tracking starts

---

#### Phase 5 Testing Plan

| Test | Steps | Expected Result |
|------|-------|-----------------|
| Event recording | Press Done | completion_events has new row |
| Tracking start | First Done press | tracking_metadata has start date |
| Period finalization | Wait for reset | finalized_days populated |
| Green day | Done pressed, period ended | Day shows green |
| Red day | Done not pressed, period ended | Day shows red |
| Grey day | Current period | Day shows grey |
| Multi-day period | 3-day period, press Done once | All 3 days green after period ends |
| Missed multi-day | 3-day period, no press | All 3 days red after period ends |
| Calendar scroll | Scroll up/down | Smooth, shows correct months |
| Current week focus | Open calendar | Scrolled to current week |
| Clear history | Settings → Clear | All data deleted, calendar empty |
| Period change (lengthen) | Change from daily to weekly | Past finalized unchanged, new 7-day period starts |
| Period change (shorten) | Mid-week, change to daily | Abandoned days show transparent, new daily starts |
| Catch-up | Force stop, wait, reopen | Missed periods finalized |
| Manual reset undo | Press Done, reset, let period end | Day shows red (event was deleted) |
| Manual reset after period | Period ends green, then reset | Widget resets, history stays green |
| Reset time change | Change reset time mid-period | Current period uses new time |
| Finalized immutability | Try to re-finalize same day | Original status preserved (IGNORE) |

---

### Future Enhancements
- Multiple widget instances for different habits
- Action labels on widgets ("Water plants")
- Notifications/reminders
- Data export/import
- Widget size variations (2x1, 2x2)

---

## User Stories

As a user, I want to:

1. See a large button on my home screen so I can quickly track my daily/weekly action
2. Tap the button to mark my action as complete for the current period
3. Have the button automatically reset after the configured time period
4. Configure how often the button resets (daily, weekly, custom)
5. Manually reset the button if I tap it by accident
6. See a calendar showing which periods I completed the action
7. Set what time of day the period resets (e.g., 3 AM instead of midnight)

---

## Design Specification

See `bigbutton_mockup.png` for visual reference.

### Widget Layout
- **Size:** 1x1 (57x57dp minimum)
- **Background:** Warm beige (#D4C5A9)
- **Style:** Skeuomorphic with 3D appearance

### Button
- **Size:** 52dp circular, centered
- **Border:** 60dp white ring (4dp border width)
- **Gradient:** Radial, lighter center (centerY: 0.4) to darker edges

| State | Center Color | Edge Color | Text |
|-------|--------------|------------|------|
| Do    | #EF8A8A      | #D45A5A    | "Do" |
| Done  | #9CCC9C      | #5DA55D    | "Done!" |

### Typography
- **Font:** Roboto Bold
- **Size:** 18sp
- **Color:** White (#FFFFFF)

### Settings Icon
- **Position:** Bottom-right, 8dp padding
- **Size:** 16dp
- **Color:** #8B7355 at 70% opacity

### Accessibility
- Contrast ratio exceeds 4.5:1 (white on red/green)
- Text changes with state (not color-only indication)
- Content descriptions for screen readers

---

## Architecture Decisions

### Jetpack Compose for UI
**Date:** 2026-01-07

**Decision:** Use Jetpack Compose instead of XML layouts.

**Rationale:**
- Modern, declarative UI toolkit
- Easier to maintain and iterate
- Better preview support

### Jetpack Glance for Widget
**Date:** 2026-01-09

**Decision:** Use Glance instead of traditional RemoteViews.

**Rationale:**
- Aligns with Compose architecture
- Cleaner, more maintainable code
- Modern API with active development

**Trade-off:** Required minSdk 26 (was 24), drops ~2-3% of oldest devices.

### State Persistence (Phase 2)
**Date:** 2026-01-09

**Decision:** Use PreferencesGlanceStateDefinition (Glance's built-in Preferences-backed state).

**Rationale:**
- Designed specifically for Glance widgets
- Simple API, minimal boilerplate
- Sufficient for current needs (isDone + timestamp)

**Schema:**
- `isDone: Boolean` - Whether action is marked complete
- `lastChanged: Long` - Timestamp for future auto-reset feature

**Future Migration:** Will migrate to Room database when implementing Phase 5 (History & Calendar) to support historical data queries.

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

## Testing Checklist

For each increment:
- [ ] Project builds without errors
- [ ] App runs on emulator/device
- [ ] Visual changes are as expected
- [ ] No regressions in existing functionality
- [ ] Accessibility guidelines met

---

Last Updated: 2026-01-10 (Phase 4 implemented, ready for testing)
