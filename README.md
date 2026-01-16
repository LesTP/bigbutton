# BigButton

A simple habit tracking Android widget. Track whether you've done something today/this week/this month with a single tap.

## Cold Start Summary

- **What:** Personal Android widget for habit tracking with configurable periods (daily/weekly/monthly/custom 1-90 days)
- **Current state:** Phases 1-4 complete and working. Widget displays, tap toggles Do→Done, auto-resets at configured time, settings UI with tabs (Settings | Calendar | Info)
- **What works:** Widget interaction, state persistence, manual reset, period/time configuration, automatic reset via AlarmManager, Room database with completion events and period finalization
- **What's in progress:** Phase 5d - Calendar UI (basic). Database tables ready, finalization logic complete, need to build the visual calendar
- **What's broken:** Multi-day period testing incomplete (see DEVLOG TODO)
- **Current focus:** Implement scrollable calendar view showing green (completed) / red (missed) / grey (in-progress) days
- **Key constraints:** Android 12+ requires manual user approval for exact alarms (Settings > Apps > Alarms & reminders). Without this, auto-reset won't fire.
- **Gotchas:** Never call `updateAppWidgetState()` inside `provideGlance()` - causes deadlock. See DEVLOG Issue #4.

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
