# BigButton Development Plan

This document contains the product vision, roadmap, requirements, and design specifications for BigButton.

For implementation history and issue resolutions, see [DEVLOG.md](DEVLOG.md).

---

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
| Scheduling mechanism | AlarmManager with setExactAndAllowWhileIdle() | Exact timing even in Doze mode |
| Reset logic | Hybrid (scheduled alarm + on-load check) | Best reliability |
| Timezone handling | Follow local time | More natural for habit tracking |
| Time granularity | Hour + Minute | More flexible, easier to test |

**Important Notes:**
- On Android 12+ (API 31+), the `SCHEDULE_EXACT_ALARM` permission requires **manual user approval** in system settings
- Without this permission, exact alarms will not fire and automatic reset will not work

---

### Phase 4.1: Tab Navigation & Info Screen ✅ Complete

Add tab-based navigation to MainActivity with three tabs: Settings, Calendar, and Info.

**Deliverables:**
- Tab navigation in MainActivity (Settings | Calendar | Info)
- Info screen with About section and permissions guidance
- Calendar tab placeholder (content in Phase 5)
- Settings remains the default tab

**Requirements:**

| ID | Requirement |
|----|-------------|
| P4.1.1 | MainActivity displays three tabs: Settings, Calendar, Info |
| P4.1.2 | Settings tab is selected by default on app open |
| P4.1.3 | Tab selection persists during session |
| P4.1.4 | Info tab displays app name, version, and author |
| P4.1.5 | Info tab includes permissions guidance for Android 12+ |
| P4.1.6 | Calendar tab shows placeholder text (implemented in Phase 5) |

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

##### Manual Reset Behavior

**Key principle:** Manual reset during the current period acts as an "undo" for accidental presses.

When user manually resets (via Settings):
1. Widget state set to `isDone = false`
2. **Delete all `CompletionEvent` records from the current period**
3. This allows the period to end as "missed" (red) if user doesn't press Done again

| Action | Effect on Widget | Effect on History |
|--------|------------------|-------------------|
| Press Done | Shows "Done!" | CompletionEvent recorded |
| Manual reset (same period) | Shows "Do" | CompletionEvent deleted |
| Manual reset (after period ended) | Shows "Do" | Past finalization unchanged |

##### Period Change Handling

When user changes period length in settings:
1. **Finalized days are immutable** - past `FinalizedDay` records never change
2. **Current period is abandoned** - unfinalized days remain unfinalized (show as transparent/no-data)
3. **New periods start from today** - fresh start with new period length

**Rationale:** Abandoned periods are not "failed" - the user changed the rules mid-game. Showing them as "no data" is more accurate than marking them red.

##### FinalizedDay Immutability

Use `INSERT ... ON CONFLICT IGNORE` to ensure finalized days can never be overwritten.

##### Edge Cases

| Scenario | Behavior |
|----------|----------|
| App installed, no widget added | No tracking starts |
| Widget added, never pressed | Tracking starts on first press |
| Manual reset during period | Deletes completion events, allows "undo" |
| Manual reset after period ended | Widget resets, history unchanged |
| Period shortened (e.g., 7→1 day) | Abandoned period days show as no-data (transparent) |
| Period lengthened (e.g., 1→7 day) | Past finalized days unchanged, new period starts |

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
| Storage for history | Room database | Structured queries, scales well |
| Day status storage | Per-day records (FinalizedDay) | Fast rendering, handles period changes gracefully |
| Period boundary anchor | Tracking start date | Predictable, user-controlled |
| Current period display | Grey color | Prevents gaming; status locked only when period ends |
| Calendar scroll style | Continuous vertical | Handles periods spanning month boundaries naturally |
| Past data on period change | Immutable | Simplifies logic, preserves historical accuracy |
| Tracking start trigger | First "Done" press | Natural starting point for habit tracking |

---

#### Phase 5 Sub-phases

- **Phase 5a:** Room Database Setup ✅ Complete
- **Phase 5b:** Completion Event Recording & Manual Reset ✅ Complete
- **Phase 5c:** Period Finalization ✅ Complete
- **Phase 5d:** Calendar UI - Basic ✅ Complete
- **Phase 5e:** Permission Warning Banner ✅ Complete
- **Phase 5f.0:** Calendar Initial Scroll Position ✅ Complete
- **Phase 5f:** Calendar UI - Polish ✅ Complete
- **Phase 5g:** Clear History ✅ Complete

---

#### Phase 5a: Room Database Setup ✅ Complete

**Deliverables:**
- Room dependencies added (room-runtime, room-ktx, room-compiler via KSP)
- Three entity classes created per spec
- DAO interface with CRUD operations
- Database singleton class

**Files Created:**
- `data/CompletionEvent.kt` - Entity for "Done" button presses
- `data/FinalizedDay.kt` - Entity for locked-in day status
- `data/TrackingMetadata.kt` - Entity for tracking config
- `data/BigButtonDao.kt` - DAO interface
- `data/BigButtonDatabase.kt` - Room database + singleton

**Files Modified:**
- `settings.gradle.kts` - Added KSP plugin
- `app/build.gradle.kts` - Added KSP plugin + Room dependencies

---

#### Phase 5b: Completion Event Recording & Manual Reset ✅ Complete

**Deliverables:**
- CompletionEvent recorded when user presses "Done"
- tracking_start_date set on first-ever completion
- Manual reset deletes completion events from current period (undo behavior)
- Period start calculation helper for determining current period boundaries

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5b.1 | Pressing "Done" inserts a CompletionEvent with timestamp and periodDays |
| P5b.2 | First "Done" press sets tracking_start_date in TrackingMetadata |
| P5b.3 | Manual reset deletes all CompletionEvents from current period |
| P5b.4 | Manual reset after period ended does not affect past data |

**Files to Modify:**
- `util/ResetCalculator.kt` - Add `calculateCurrentPeriodStart()` helper
- `widget/MarkDoneAction.kt` - Insert CompletionEvent on Done press
- `ui/SettingsScreen.kt` - Delete current period events on manual reset

**Testing Procedure:**
1. Tap "Done" → verify 1 CompletionEvent in database
2. Open settings, tap "Reset" → verify 0 CompletionEvents (deleted)
3. Tap "Done" again → verify 1 CompletionEvent (new entry)

Use Android Studio's Database Inspector (App Inspection tab) to verify.

---

#### Phase 5c: Period Finalization ✅ Complete

**Deliverables:**
- When reset alarm fires and a period ends, FinalizedDay records are written
- All days in the period get the same status (completed or missed)
- Immutability preserved via INSERT IGNORE
- last_finalized_date metadata updated

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5c.1 | Period finalization occurs when reset alarm fires and shouldReset=true |
| P5c.2 | Finalization writes FinalizedDay for each day in the ending period |
| P5c.3 | completed=true if CompletionEvents exist in period, false otherwise |
| P5c.4 | No finalization if tracking hasn't started (no tracking_start_date) |
| P5c.5 | Already-finalized days are not overwritten (INSERT IGNORE) |

**Files to Modify:**
- `receiver/ResetAlarmReceiver.kt` - Add finalizePeriod() function and call it when shouldReset=true

**Testing Procedure:**
1. Set reset time to 2 minutes from now, tap "Done", wait → verify finalized_days has completed=true
2. Reset widget, wait for period end → verify finalized_days has completed=false (missed)
3. Set 3-day period, tap "Done", wait → verify 3 FinalizedDay entries

---

#### Phase 5d: Calendar UI - Basic ✅ Complete

**Scope:** Replace CalendarScreen placeholder with a working continuously-scrollable calendar that displays day status from the database.

**Deliverables:**
- Continuously scrollable calendar using LazyColumn with week rows
- Day cells colored based on status (green/red/grey/transparent)
- Month headers inline when week crosses month boundary
- Day-of-week header row (Sun-Sat)
- Initial scroll position at current week
- Query FinalizedDay records and tracking metadata from Room

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5d.1 | Calendar displays as vertical scroll of week rows |
| P5d.2 | Each week row shows 7 day cells (Sun-Sat) |
| P5d.3 | Day cells show date number |
| P5d.4 | Finalized completed days show green background |
| P5d.5 | Finalized missed days show red background |
| P5d.6 | Current in-progress period days show grey background |
| P5d.7 | Days before tracking started show no background |
| P5d.8 | Future days show no background |
| P5d.9 | Month header appears above first week of each month |
| P5d.10 | Day-of-week labels (S M T W T F S) at top |
| P5d.11 | Calendar scrolls to current week on load |

**Color Scheme (from mockup):**
- Green (completed): #4CAF50 or similar
- Red (missed): #F44336 or similar
- Grey (in-progress): #9E9E9E or similar
- Transparent: no background

**Day Status Logic:**
```
For each day:
1. If day > today → transparent (future)
2. If day < tracking_start_date → transparent (before tracking)
3. If FinalizedDay exists for date:
   - completed=true → green
   - completed=false → red
4. If day is in current period (not yet finalized) → grey
5. Otherwise → transparent (gap/abandoned period)
```

**Current Period Calculation:**
- Read periodDays, resetHour, resetMinute from DataStore
- Calculate period boundaries based on reset time
- Days from current period start to today (inclusive) are "in progress"

**Data Flow:**
```
CalendarScreen
  ├── reads: tracking_start_date from TrackingMetadata
  ├── reads: FinalizedDay records for visible date range
  ├── reads: periodDays, resetHour, resetMinute from DataStore
  └── calculates: current period boundaries
```

**Files to Create/Modify:**
- `ui/CalendarScreen.kt` - Replace placeholder with full implementation
- `ui/components/WeekRow.kt` - Week row composable (optional, can inline)
- `ui/components/DayCell.kt` - Day cell composable (optional, can inline)

**Testing Checklist:**
- [x] Calendar displays with week rows
- [x] Day-of-week header shows S M T W T F S
- [x] Month headers appear (e.g., "January 2026")
- [x] Green days appear for finalized completed
- [x] Red days appear for finalized missed
- [x] Grey days appear for current in-progress period
- [x] Days before tracking start have no color
- [x] Future days have no color
- [x] Calendar scrolls to current week on open
- [x] Scrolling up/down works smoothly

**Testing Limitation:** Time-travel testing (manually advancing device clock) does not trigger period finalization. This is because AlarmManager doesn't retroactively fire alarms when the clock jumps forward. The widget resets via tap-check but `finalizePeriod()` only runs from `ResetAlarmReceiver`. Real-world usage is unaffected since alarms fire naturally.

---

#### Phase 5e: Permission Warning Banner ✅ Complete

**Scope:** Add a persistent warning banner to the Settings tab that appears when the exact alarm permission is not granted on Android 12+ devices.

**Deliverables:**
- Warning banner at top of Settings tab
- Permission check using `AlarmManager.canScheduleExactAlarms()`
- Button to open system's "Alarms & reminders" settings page

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5e.1 | Banner displays at top of Settings tab when `canScheduleExactAlarms()` returns false |
| P5e.2 | Banner only appears on Android 12+ (API 31+) devices |
| P5e.3 | Banner displays warning icon, explanatory text, and "Open Settings" button |
| P5e.4 | Button opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (direct to Alarms & reminders page) |
| P5e.5 | Banner automatically hides when permission is granted (reactive) |
| P5e.6 | Banner does not appear when permission is already granted |
| P5e.7 | Banner uses warning color scheme (amber/orange) to draw attention |

**Design:**
- Text: "BigButton widget requires 'Alarms & reminders' permission to reset automatically. Enable it in system settings."
- Button: "Open Settings" → opens `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`
- Color: Warning/amber background for visibility

**Files to Modify:**
- `ui/SettingsScreen.kt` - Add permission check and warning banner composable

**Testing Checklist:**
- [ ] Banner appears on Android 12+ emulator/device without permission
- [ ] Banner does NOT appear on Android 11 or lower
- [ ] Banner does NOT appear when permission is granted
- [ ] "Open Settings" button opens system Alarms & reminders page
- [ ] Banner disappears after granting permission and returning to app
- [ ] Banner styling is visible and readable (warning colors)
- [ ] Settings content remains scrollable with banner present

---

#### Phase 5f.0: Calendar Initial Scroll Position ✅ Complete

**Scope:** Fix calendar scroll position so the current week appears in the lower portion of the screen, with past weeks visible above and future weeks below.

**Problem:** Currently the calendar scrolls to put the current week at the top of the viewport. On smaller screens, this pushes the current and future weeks below the visible area, showing only old past weeks.

**Deliverables:**
- Calendar opens with current week visible in lower portion of screen
- Past weeks visible above current week
- Future weeks (at least 1-2) visible below current week

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5f0.1 | On calendar open, (current week + 1) aligns to bottom of viewport |
| P5f0.2 | Current week is visible just above the bottom |
| P5f0.3 | Past weeks fill the area above current week |
| P5f0.4 | At least 1 future week visible at bottom |

**Design:**
- Use `LazyListState.layoutInfo` to calculate viewport height
- Scroll to position where (currentWeek + 1) item aligns to viewport bottom edge
- Two-step scroll: first scroll to target item, then adjust offset based on layout measurements

**Files to Modify:**
- `ui/CalendarScreen.kt` - Modify `LaunchedEffect` scroll logic

**Testing Checklist:**
- [ ] Current week visible on calendar open (not scrolled off bottom)
- [ ] At least 1 future week visible below current week
- [ ] Past weeks visible above current week
- [ ] Works on small screens (Pixel 2 size)
- [ ] Works on larger screens

---

#### Phase 5f: Calendar UI - Polish ✅ Complete

**Scope:** Add visual enhancements to distinguish today in the calendar.

**Deliverables:**
- Border ring around today's date cell

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5f.1 | Today's cell displays a colored border ring |
| P5f.2 | Border visible regardless of day status (green/red/grey/transparent) |

**Design:**
- Today border: 2dp stroke, primary/accent color

**Files Modified:**
- `ui/CalendarScreen.kt` - `DayCell` composable (implemented in Phase 5d)

**Testing Checklist:**
- [x] Today shows border ring
- [x] Border visible on all background colors (green/red/grey/transparent)

**Note:** Shadow/glow effect for in-progress days was removed from requirements - effect was too subtle to be reliably visible across devices.

---

#### Phase 5g: Clear History ✅ Complete

**Scope:** Add option to clear all tracking history data.

**Deliverables:**
- "Clear History" button in Calendar tab (fixed footer)
- Confirmation dialog before clearing
- Clears all Room database tables (completion_events, finalized_days, tracking_metadata)

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5g.1 | Calendar tab displays "Clear History" button in fixed footer |
| P5g.2 | Tapping button shows confirmation dialog |
| P5g.3 | Confirming clears all tracking data from database |
| P5g.4 | Calendar shows empty state after clearing |
| P5g.5 | Widget state (isDone) is NOT affected by clear |
| P5g.6 | Settings (period, reset time) preserved after clear |

**Design:**
- Button placement: Fixed footer below the calendar LazyColumn
- Button style: Destructive (red/error color)
- Dialog title: "Clear History?"
- Dialog message: "This will permanently delete all tracking data. The calendar will be empty. This cannot be undone."
- Dialog buttons: "Cancel" / "Clear"

**What gets cleared:**
- `completion_events` table (all records)
- `finalized_days` table (all records)
- `tracking_metadata` table (tracking_start_date, last_finalized_date)

**What stays:**
- Widget state (isDone) - stored in DataStore
- Settings (period, reset time) - stored in DataStore

**Files to Modify:**
- `ui/CalendarScreen.kt` - Add fixed footer with clear history button and confirmation dialog
- `data/BigButtonDao.kt` - Add clearAllHistory() function

**Testing Checklist:**
- [ ] Clear History button appears in Calendar tab footer
- [ ] Confirmation dialog appears on tap
- [ ] Canceling dialog does not clear data
- [ ] Confirming clears all history
- [ ] Calendar shows no colored days after clear
- [ ] Widget continues to function normally
- [ ] Settings (period, reset time) preserved after clear

---

### Phase 6: User Documentation

#### Phase 6 Sub-phases

- **Phase 6a:** Info Tab Instructions ✅ Complete

---

#### Phase 5h: Settings UI - Small Screen Fix ✅ Complete

**Scope:** Fix the Custom period selector row in Settings to fit on smaller screens without wrapping to two lines.

**Problem:** On smaller hardware screens, the Custom row (RadioButton + "Custom: " + TextField + "days") wraps to two lines even though there is available horizontal space after "days". The TextField uses a fixed `width(70.dp)` instead of adapting to available space.

**Deliverables:**
- Custom period row displays on a single line on all screen sizes
- TextField constrained to reasonable size range

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5h.1 | Custom row displays on a single line on small screens |
| P5h.2 | TextField adapts to available width (not fixed 70.dp) |
| P5h.3 | Existing functionality unchanged |

**Design:**
- Replace `Modifier.width(70.dp)` with `Modifier.widthIn(min = 56.dp, max = 88.dp)` on the OutlinedTextField
- This constrains the TextField to a reasonable size range - wide enough for placeholder "1-90" to display on one line, but not excessively wide

**Files Modified:**
- `ui/SettingsScreen.kt` - Changed OutlinedTextField modifier in Custom row

**Testing Checklist:**
- [x] Custom row displays on single line on small screen device
- [x] TextField input still works correctly
- [x] Number validation (1-90) still works
- [x] Visual appearance acceptable on larger screens

---

#### Phase 5i: Widget Text Scaling for Small Screens ✅ Complete

**Scope:** Fix the widget button text ("Do"/"Done!") being clipped/squished on smaller screens or higher density displays.

**Problem:** The widget uses fixed sizes (button: 52.dp, border: 60.dp, font: 18.sp). On smaller screens or higher density displays, the text doesn't fit properly inside the button.

**Deliverables:**
- Widget text scales proportionally with available widget size
- Button and border sizes adapt to widget dimensions
- Text remains readable on all screen sizes

**Requirements:**

| ID | Requirement |
|----|-------------|
| P5i.1 | Widget text scales proportionally with widget size |
| P5i.2 | Button and border scale proportionally |
| P5i.3 | Widget remains visually correct on larger screens |
| P5i.4 | Settings icon positioning adapts appropriately |

**Design:**
- Enable `SizeMode.Exact` to allow `LocalSize` to return actual widget dimensions
- Use `LocalSize.current` to get the widget's actual dimensions
- Calculate scale factor based on the smaller dimension (min of width/height)
- Base size assumption: 70.dp minimum dimension for scale factor 1.0
- Scale factor clamped between 0.6 and 1.5 to prevent extreme scaling
- Apply scale to: button size (52.dp), border size (60.dp), font size (15.sp base), settings icon (16.dp), icon padding (8.dp)
- Reduced base font from 18.sp to 15.sp to better fit within button on high-density small screens

**Files Modified:**
- `widget/BigButtonWidget.kt` - Added SizeMode.Exact, LocalSize-based responsive sizing, reduced base font

**Testing Checklist:**
- [x] Text fits inside button on small screen device
- [x] Widget looks correct on larger screens
- [x] "Do" and "Done!" both display properly
- [x] Settings icon remains visible and tappable

---

#### Phase 6a: Info Tab Instructions ✅ Complete

**Scope:** Add explanatory instructions to the Info tab to help users understand how to use the widget.

**Deliverables:**
- Clear instructions on how to use the widget
- Explanation of calendar colors (green = completed, red = missed, grey = in progress)
- Explanation of period settings
- Multi-day period behavior explanation

**Requirements:**

| ID | Requirement |
|----|-------------|
| P6a.1 | Info tab displays clear usage instructions |
| P6a.2 | Calendar color meanings are explained |
| P6a.3 | Period/reset settings are explained |
| P6a.4 | Content is scrollable (already implemented) |
| P6a.5 | Multi-day period behavior explained |
| P6a.6 | Period finalization explained |

**Files Modified:**
- `ui/InfoScreen.kt` - Added instructional content with sections for How to Use, Calendar Colors, Multi-Day Periods, and When Periods Finalize

**Testing Checklist:**
- [x] Instructions are clear and readable
- [x] All color meanings explained
- [x] Settings behavior explained
- [x] Content scrolls properly on small screens
- [x] Multi-day period behavior explained with example
- [x] Period finalization and Clear History mentioned

---

### Future Enhancements
- Multiple widget instances for different habits
- Action labels on widgets ("Water plants")
- Notifications/reminders
- Data export/import
- Widget size variations (2x1, 2x2)
- **TODO:** Fix settings gear overlapping with button on squeezed/non-square widget cells (some launchers/screen resolutions cause the button to take up more visual space, leaving no room for the gear icon)

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

### State Persistence
**Date:** 2026-01-09

**Decision:** Use PreferencesGlanceStateDefinition (Glance's built-in Preferences-backed state).

**Rationale:**
- Designed specifically for Glance widgets
- Simple API, minimal boilerplate
- Sufficient for current needs (isDone + timestamp)

**Future Migration:** Will migrate to Room database when implementing Phase 5 (History & Calendar).

### Room Database for History
**Date:** 2026-01-11

**Decision:** Use Room with KSP for history tracking.

**Rationale:**
- KSP is faster than KAPT for annotation processing
- Room provides type-safe queries and compile-time verification
- Sufficient for structured historical data (events, finalized days, metadata)

**Trade-off:** Adds ~1MB to APK size. Acceptable for the functionality gained.
