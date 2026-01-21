# BigButton

A simple habit tracking Android widget. Track whether you've done something today/this week/this month with a single tap.

## Cold Start Summary

- **What:** Personal Android widget for habit tracking with configurable periods (daily/weekly/monthly/custom 1-90 days)
- **Architecture:** Jetpack Compose + Glance for widget, DataStore for widget state, Room for history
- **Key constraints:** Android 12+ requires manual user approval for exact alarms (Settings > Apps > Alarms & reminders). Without this, auto-reset won't fire.
- **Gotchas:** Never call `updateAppWidgetState()` inside `provideGlance()` - causes deadlock. See DEVLOG Issue #4.

## Current Status

- **Phase:** Release Ready (v1.0)
- **Package:** `com.movingfingerstudios.bigbutton`
- **Focus:** All core features complete, ready for Google Play
- **What works:** Full widget functionality, auto-reset, Room database with history tracking, calendar view with day coloring, user documentation, responsive UI for small screens
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

# Release build (APK)
./gradlew assembleRelease

# Release bundle (for Google Play)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

### Release Signing Setup

1. Generate a keystore (if you don't have one):
   ```bash
   keytool -genkey -v -keystore bigbutton-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bigbutton
   ```

2. Copy `keystore.properties.template` to `keystore.properties` and fill in your credentials

3. Build the release bundle with `./gradlew bundleRelease`

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
│   ├── java/com/movingfingerstudios/bigbutton/
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
├── keystore.properties.template
└── settings.gradle.kts
```

## Documentation

- **[DEVPLAN.md](DEVPLAN.md)** - Product vision, roadmap, requirements, and design specifications
- **[DEVLOG.md](DEVLOG.md)** - Implementation history, issues encountered, and lessons learned

## License

This project is open source and available under the MIT License.
