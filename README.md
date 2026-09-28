# Sensor Toolbox

Sensor Toolbox is an Android app built with Kotlin and Jetpack Compose that displays live readings from the device's built-in sensors. It provides a simple dashboard for common mobile sensors and a references screen for project-related resources.

## Features

- Live sensor monitoring for:
  - Accelerometer
  - Gyroscope
  - Magnetic field
  - Light sensor
  - Relative humidity
  - Ambient temperature
  - Pressure sensor
- Automatic detection of available sensors on the device
- Clean Material 3 UI
- Navigation between the home screen and reference screen
- Developer banner and reference links

## Tech Stack

- Kotlin
- Jetpack Compose
- AndroidX Navigation
- Material 3
- Android Sensor API

## Project Structure

- `app/` — Android application module
- `app/src/main/java/com/example/sensortoolbox/` — application code
- `gradlew` — Gradle wrapper for building the project

## Prerequisites

- Android Studio
- JDK 11 or later
- Android SDK with appropriate platform tools installed

## Running the App

1. Open the project in Android Studio.
2. Let Gradle sync the project.
3. Connect a physical device or start an emulator.
4. Select the app run configuration and click Run.

## Build from Command Line

From the project root:

```bash
./gradlew assembleDebug
```

To install directly to a connected device:

```bash
./gradlew installDebug
```

## Notes

- The app checks which sensors are available on the current device and displays only supported readings.
- Some sensors may not be available on all phones or emulators.
- The app is configured as a Compose-based Android application with Material 3 styling.

## License

This project is provided as-is for educational and demonstration purposes.
