# BigButton

A simple habit tracking Android widget. Track whether you've done something today/this week/this month with a single tap.

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
│   │   ├── IntroScreen.kt
│   │   ├── widget/
│   │   │   ├── BigButtonWidget.kt
│   │   │   └── BigButtonWidgetReceiver.kt
│   │   └── ui/theme/
│   └── res/
│       ├── drawable/
│       │   ├── button_do_gradient.xml
│       │   ├── button_done_gradient.xml
│       │   └── ic_settings.xml
│       ├── layout/
│       │   └── widget_loading.xml
│       └── xml/
│           └── big_button_widget_info.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## Documentation

- **[DEVPLAN.md](DEVPLAN.md)** - Product vision, roadmap, requirements, and design specifications
- **[DEVLOG.md](DEVLOG.md)** - Implementation history, issues encountered, and lessons learned

## License

This project is open source and available under the MIT License.
