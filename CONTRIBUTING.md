# Contributing Guide

Thank you for your interest in contributing to TRMNL Display Mirror App! This document provides guidelines and instructions to help you contribute effectively.

## Application Overview

This guide will help you get started with the TRMNL Display Mirror Android application development.

<details>

<summary>See technical details on the project</summary>

### Project Structure

The app uses a modern Android architecture with the following components:

- **UI**: Jetpack Compose with Circuit UDF architecture
- **Background Processing**: WorkManager for scheduled image (re)loading
- **Networking**: Retrofit and OkHttp for API communication
- **DI**: Dagger with Anvil for dependency injection
- **Data Storage**: DataStore for preferences and token storage

#### Key Features/Screens

- Main TRMNL Mirror Display visualization in `TrmnlMirrorDisplayScreen`
- Settings management via `AppSettingsScreen`
- Image refresh log/history in `DisplayRefreshLogScreen`
- Background refresh scheduling with `TrmnlWorkScheduler` & `TrmnlImageRefreshWorker`

</details>

## Development Setup

1. Clone the repository
2. Open the project in [Android Studio](https://developer.android.com/studio)
3. Sync Gradle files
4. Connect an Android device or start an emulator and select it
5. Click the Run button (green triangle) in the toolbar

## Code Style and Formatting

We use [ktlint](https://pinterest.github.io/ktlint/latest/) for Kotlin code formatting. Before submitting any changes, run:

```bash
./gradlew formatKotlin
```

This will automatically format your Kotlin code according to project standards.

## Pull Request Process

1. Fork the repository and create your branch from `main`
2. Make your changes
3. Run `./gradlew formatKotlin` to ensure code style compliance
4. Run tests with `./gradlew test`
5. Submit a pull request to the `main` branch

## Testing

Before submitting your changes, please run:

```bash
./gradlew lintKotlin testDebugUnitTest
```

## Building

To build a debug APK:

```bash
./gradlew assembleDebug
```

## Issues

- For bug reports, include steps to reproduce, expected behavior, and actual behavior
- For feature requests, describe the feature and why it would be valuable

## Additional Resources

- [Project README](README.md)
- [Android Jetpack Compose](https://developer.android.com/jetpack)
- [Circuit UDF Architecture](https://slackhq.github.io/circuit/)
- [Android Work Manager](https://developer.android.com/topic/libraries/architecture/workmanager)

Thank you for contributing to TRMNL Display Mirror App!
