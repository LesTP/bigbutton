# BigButton Android App

A simple Android application built with Kotlin and Jetpack Compose.

## Project Structure

```
bigbutton/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/bigbutton/
│   │       │   ├── MainActivity.kt
│   │       │   ├── IntroScreen.kt
│   │       │   └── ui/theme/
│   │       │       ├── Color.kt
│   │       │       ├── Theme.kt
│   │       │       └── Type.kt
│   │       ├── res/
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Features

- Material 3 Design
- Jetpack Compose UI
- Dark/Light theme support
- Intro screen with welcome message

## Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- Minimum Android API 24 (Android 7.0)
- Kotlin 1.9.20
- Gradle 8.2

## Setup Instructions

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the project directory and open it
4. Wait for Gradle sync to complete
5. Run the app on an emulator or physical device

## Note on Launcher Icons

The project currently uses placeholder launcher icon references. To add proper launcher icons:

1. Use Android Studio's Image Asset Studio (Right-click on `res` → New → Image Asset)
2. Generate launcher icons in all required densities
3. Place the generated images in the appropriate mipmap directories

## Building the Project

To build the project from the command line:

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Running Tests

```bash
./gradlew test
```

## License

This project is open source and available under the MIT License.

---

# Product Specification

## Vision

BigButton is a habit tracking app that helps users track whether they've performed a specific action within a defined time period. Examples:
- "Watered the plants this week"
- "Took a vitamin today"
- "Called mom this month"

The app focuses on simplicity with a single-action interface presented as a home screen widget.

## Final Feature Set

### 1. Home Screen Widget

**Visual Components:**
- Large button displaying current state: "Do" or "Done"
- Small settings icon in the corner

**Behavior:**
- Default state: "Do"
- Tap button: Changes to "Done"
- After configured time period elapses: Automatically resets to "Do"

### 2. Settings Interface

Accessed by tapping the settings icon on the widget.

**Tab 1: Configuration**
- **Manual Reset Button**: Resets the widget from "Done" to "Do" (for accidental taps)
- **Period Selector**: Configure the tracking period
  - Minimum: Any number of days (1-365+)
  - Quick presets: 1 day, 1 week, 1 month
- **Reset Time**: Set the time of day when period resets (e.g., "3:00 AM")

**Tab 2: History Calendar**
- Calendar view showing historical data
- Visual indicators:
  - Green/checkmark: Periods where action was completed ("Done")
  - Red/empty: Periods where action was not completed (stayed "Do")
- Scrollable to view past months/years

### 3. Action Label (Future Enhancement)

- Customizable text label for the action being tracked
- Displayed on widget (e.g., "Water plants", "Take vitamin")

## MVP Scope

The Minimum Viable Product focuses on visual design and basic structure:

### MVP Deliverables

1. **Widget Design (Non-Interactive)**
   - Visual layout of the widget
   - Button in two states: "Do" and "Done"
   - Settings icon placement
   - Color scheme and typography

2. **No Functionality Required**
   - Button does not need to respond to taps
   - No period tracking
   - No settings interface
   - No calendar view

### MVP Success Criteria

- Widget appears on home screen
- Visual design is clear and appealing
- Two button states are visually distinct
- Settings icon is visible and appropriately positioned

## Development Approach

### Incremental Development

All work will proceed in the smallest possible increments, with each increment:
1. **Testable**: Can be built and run without errors
2. **Visually Confirmable**: Changes can be seen in the Android emulator/device
3. **Self-Contained**: Does not break existing functionality

### Example Increments

1. Create basic widget layout XML/Compose structure
2. Add button component with "Do" state
3. Add visual styling to button
4. Create "Done" state appearance
5. Add settings icon to widget
6. Position and style settings icon
7. ... (continue with small steps)

### Testing Each Increment

After each change:
1. Build the project
2. Deploy to emulator or device
3. Verify visual appearance matches expectation
4. Confirm no regressions in existing features

## Technical Considerations

### Widget Implementation

- **Technology**: Android App Widget using RemoteViews or Glance (Jetpack Compose for widgets)
- **Update Mechanism**: AlarmManager or WorkManager for period reset
- **State Persistence**: SharedPreferences or local database

### Future Scalability

The architecture should support:
- Multiple widgets tracking different actions
- Export/import of historical data
- Notifications/reminders
- Widget size variations (1x1, 2x1, 2x2)

## User Stories

### As a user, I want to...

1. See a large button on my home screen so I can quickly track my daily/weekly action
2. Tap the button to mark my action as complete for the current period
3. Have the button automatically reset after the configured time period
4. Configure how often the button resets (daily, weekly, custom)
5. Manually reset the button if I tap it by accident
6. See a calendar showing which periods I completed the action
7. Set what time of day the period resets (e.g., 3 AM instead of midnight)

## Design Mockup Notes

### Widget States

**"Do" State:**
- Large button with text "Do" or custom action label
- Neutral color (gray/blue)
- Small settings gear icon in top-right corner

**"Done" State:**
- Same button with text "Done" or checkmark
- Success color (green)
- Settings icon remains visible

### Accessibility

- Button must have sufficient contrast ratio
- Text size should be readable at standard widget size
- Settings icon should be at least 48x48dp touch target
