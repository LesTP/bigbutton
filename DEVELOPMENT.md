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

### Phase 3: Settings & Configuration (Next)
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

#### Phase 3c: Reset Time Selector

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

**Decisions Needed:**

| Decision | Options | Recommendation |
|----------|---------|----------------|
| Time granularity | Hour only / Hour + minute | Hour only - simpler, sufficient |
| Time picker UI | Android TimePicker / Custom dropdown | Android TimePicker - familiar UX |
| Display format | 12h / 24h / System | System preference |

**Implementation Plan:**
1. Add reset time key to `BigButtonStateDefinition`
2. Create time picker trigger button showing current time
3. Implement time picker dialog
4. Save selected time to DataStore
5. Display formatted time in settings

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

### Phase 4: Automatic Reset
Background scheduling for period resets.

**Deliverables:**
- Widget automatically resets to "Do" when period elapses
- WorkManager/AlarmManager integration
- Reliable reset even if device was off

**Dependencies:** Requires Phase 3b (period) and Phase 3c (reset time) to be complete.

### Phase 5: History & Calendar
Track completion history over time.

**Deliverables:**
- Calendar view showing historical data
- Green indicators for completed periods
- Red/empty for missed periods
- Scrollable history

### Future Enhancements
- Info/Manual tab in app (usage instructions, about)
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

## Issues & Resolutions

### Issue #1: Missing Launcher Icon Resource
**Date:** 2026-01-08 | **Severity:** High | **Status:** Resolved

**Problem:** Build failed with AAPT error about missing `mipmap/ic_launcher_foreground`.

**Solution:** Created vector drawable in `drawable/` directory and updated adaptive icon XML files to reference it.

**Lesson:** Adaptive icons require both background and foreground resources. Use Android Studio's Image Asset Studio for production icons.

---

## Testing Checklist

For each increment:
- [ ] Project builds without errors
- [ ] App runs on emulator/device
- [ ] Visual changes are as expected
- [ ] No regressions in existing functionality
- [ ] Accessibility guidelines met

---

Last Updated: 2026-01-10 (Phase 3b complete)
