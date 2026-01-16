# BigButton

A simple habit tracking Android widget. Track whether you've done something today/this week/this month with a single tap.

## Cold Start Summary

- **What:** Personal Android widget for habit tracking with configurable periods (daily/weekly/monthly/custom 1-90 days)
- **Architecture:** Jetpack Compose + Glance for widget, DataStore for widget state, Room for history
- **Key constraints:** Android 12+ requires manual user approval for exact alarms (Settings > Apps > Alarms & reminders). Without this, auto-reset won't fire.
- **Gotchas:** Never call `updateAppWidgetState()` inside `provideGlance()` - causes deadlock. See DEVLOG Issue #4.

## Current Status

- **Phase:** 5d - Calendar UI (basic)
- **Focus:** Implement scrollable calendar view showing green (completed) / red (missed) / grey (in-progress) days
- **What works:** Widget interaction, state persistence, manual reset, period/time config, auto-reset via AlarmManager, Room database with completion events and period finalization
- **Blocked/Broken:** None

## Requirements

- Android Studio Hedgehog or later
- Android SDK 34
- Minimum Android API 26 (Android 8.0)
- Kotlin 1.9.20
- Gradle 8.2

## Setup

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the project directory and open it
4. Wait for Gradle sync to complete

## Building

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

## Adding the Widget

1. Build and install the app on your device/emulator
2. Long-press on your home screen
3. Select "Widgets"
4. Find "BigButton" and drag it to your home screen

## Project Structure

```
bigbutton/
├── app/src/main/
│   ├── java/com/example/bigbutton/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── BigButtonDatabase.kt
│   │   │   ├── BigButtonDao.kt
│   │   │   ├── CompletionEvent.kt
│   │   │   ├── FinalizedDay.kt
│   │   │   └── TrackingMetadata.kt
│   │   ├── receiver/
│   │   ├── ui/
│   │   ├── util/
│   │   └── widget/
│   └── res/
├── build.gradle.kts
└── settings.gradle.kts
```

## Documentation

- **[DEVPLAN.md](DEVPLAN.md)** - Product vision, roadmap, requirements, and design specifications
- **[DEVLOG.md](DEVLOG.md)** - Implementation history, issues encountered, and lessons learned

## License

This project is open source and available under the MIT License.
