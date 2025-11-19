# TRMNL Android - Copilot Agent Instructions

This document provides essential information for GitHub Copilot agents working on the TRMNL Android codebase.

## Repository Overview

**TRMNL Android** is a native Android application that displays TRMNL e-ink device content on Android phones, tablets, and e-ink displays. The app connects to TRMNL or BYOS (Bring Your Own Server) APIs, fetches display images periodically, and renders them on the device screen.

**Key Statistics:**
- Language: Kotlin (100%)
- Build System: Gradle 8.13
- Min SDK: 28 (Android 9.0 Pie)
- Target SDK: 36 (Android 16.0)
- Architecture: Modern Android with Jetpack Compose, Circuit UDF, Dagger DI

## Critical Build & Test Commands

**ALWAYS run these commands in this specific order to validate changes:**

### 1. Format Code (Required Before Commit)
```bash
./gradlew formatKotlin
```
- Formats all Kotlin code using ktlint
- Must run BEFORE committing code changes
- Takes ~5 seconds
- Zero failures expected

### 2. Lint & Test (Required Before Commit)
```bash
./gradlew lintKotlin testDebugUnitTest --parallel --daemon
```
- Runs Kotlin linting and unit tests
- Takes ~2-3 minutes on first run, ~1 minute with cache
- This is the EXACT command used in CI (`.github/workflows/android.yml`)
- All tests must pass before submitting changes

### 3. Build Debug APK
```bash
./gradlew assembleDebug --parallel --daemon
```
- Builds the debug APK
- Takes ~1-2 minutes with cache
- Output: `app/build/outputs/apk/debug/app-debug.apk`
- Uses debug keystore from `keystore/debug.keystore`

### 4. Android Lint (Post-Merge Validation)
```bash
./gradlew lintDebug
```
- Runs Android lint checks
- Takes ~50 seconds
- Generates HTML report: `app/build/reports/lint-results-debug.html`
- Used in post-merge CI workflow

## Environment Requirements

- **JDK Version:** 17 (OpenJDK 17 - Temurin distribution recommended)
- **Gradle:** 8.13 (via wrapper, do NOT install manually)
- **Android SDK:** Compile SDK 36
- **Build Tools:** Managed by Gradle plugin (AGP 8.9.2)

**IMPORTANT:** Always use `./gradlew` (Gradle wrapper) - NEVER use a system-installed gradle.

## Project Architecture

### Main Source Structure
```
app/src/main/java/ink/trmnl/android/
├── MainActivity.kt               # App entry point (138 lines)
├── TrmnlDisplayMirrorApp.kt     # Application class (51 lines)
├── data/                         # Repositories, DataStore implementations
├── di/                          # Dagger dependency injection modules
├── model/                       # Data models (TrmnlDeviceConfig, etc.)
├── network/                     # Retrofit API service, response models
├── ui/                          # Jetpack Compose screens (Circuit UDF)
├── util/                        # Utilities (InputValidator, etc.)
└── work/                        # WorkManager background refresh logic
```

### Key Configuration Files
- `app/build.gradle.kts` - Main build configuration, versioning (versionCode, versionName)
- `build.gradle.kts` - Root project plugins
- `settings.gradle.kts` - Project settings
- `gradle.properties` - Gradle JVM settings (2GB heap)
- `gradle/libs.versions.toml` - Centralized dependency versions
- `app/proguard-rules.pro` - ProGuard rules (mostly default)
- `app/src/main/AndroidManifest.xml` - App manifest, permissions

### Key Architectural Patterns
- **UI Framework:** Jetpack Compose with Circuit (Slack's UDF architecture)
- **DI:** Dagger with Anvil for code generation
- **Background Work:** WorkManager (15 min minimum interval)
- **Networking:** Retrofit + OkHttp + Moshi for JSON parsing
- **Data Storage:** DataStore (preferences) for settings/tokens
- **Image Loading:** Coil 3.x with OkHttp integration
- **State Management:** Circuit's presenter pattern with `@CircuitInject`

### Main Features & Screens
- `TrmnlMirrorDisplayScreen` - Main display showing TRMNL image
- `AppSettingsScreen` - Configuration (API token, server URL)
- `DisplayRefreshLogScreen` - Refresh history/logs
- `TrmnlImageRefreshWorker` - Background image refresh job
- `TrmnlWorkScheduler` - Manages WorkManager scheduling

## CI/CD Workflows

All workflows are in `.github/workflows/`:

1. **android.yml** (PR & Main Branch)
   - Triggers: PRs to main, pushes to main
   - Commands: `./gradlew lintKotlin testDebugUnitTest --parallel --daemon` then `./gradlew assembleDebug --parallel --daemon`
   - Takes ~3 minutes total

2. **android-lint.yml** (Post-Merge)
   - Triggers: Pushes to main
   - Commands: `./gradlew lintDebug` then `./gradlew assembleDebug`
   - Additional validation after merge

3. **android-release.yml** (Release Builds)
   - Triggers: Pushes to main, manual, GitHub releases
   - Builds signed release APK and AAB
   - Requires: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` secrets
   - Attaches APK to GitHub releases automatically

4. **version-management.yml** (Version Bumping)
   - Manual workflow for updating app version
   - Updates: `app/build.gradle.kts`, `metadata/ink.trmnl.android.yml`, changelogs
   - Run `./scripts/show_version_info.sh` locally to get current version

## Common Issues & Workarounds

### Issue: Build Fails with "secret.properties not found"
**Solution:** This is informational only. The file is optional for local dev (used for release keystores). Debug builds work without it.

### Issue: First Build Takes 2-3 Minutes
**Solution:** Expected behavior. Gradle downloads dependencies and builds annotation processors (KSP, KAPT). Subsequent builds are much faster (~1 minute) due to caching.

### Issue: Test Warning "Sharing is only supported for boot loader classes"
**Solution:** Harmless warning from Robolectric tests. Can be ignored. Tests still pass.

### Issue: Kotlin Compiler Warning "INVISIBLE_REFERENCE"
**Solution:** Expected warning in `NetworkingTools.kt:56`. File uses internal Kotlin API. Warning is documented and safe to ignore.

### Issue: Release Build Requires Keystore
**Solution:** 
- Debug builds: Use `keystore/debug.keystore` (included in repo)
- Release builds: Require production keystore in CI/CD secrets
- Local release builds will fail without `secret.properties` file

### Issue: WorkManager Minimum 15 Minute Interval
**Solution:** Android OS limitation, not a bug. WorkManager enforces 15-min minimum between periodic jobs for battery optimization.

## Version Management

**Do NOT manually edit version numbers.** Use the automated workflow:

1. Check current version: `./scripts/show_version_info.sh`
2. Use GitHub Actions workflow: `.github/workflows/version-management.yml`
3. Workflow updates:
   - `app/build.gradle.kts` (versionCode, versionName)
   - `metadata/ink.trmnl.android.yml` (F-Droid metadata)
   - `fastlane/metadata/android/en-US/changelogs/{versionCode}.txt`

See `RELEASE_CHECKLIST.md` for complete release process.

## Code Style & Formatting

- **Linter:** ktlint via kotlinter-gradle plugin
- **Configuration:** `.editorconfig` - Only setting: `ktlint_function_naming_ignore_when_annotated_with = Composable`
- **Always run:** `./gradlew formatKotlin` before committing
- **Check only:** `./gradlew lintKotlin` (doesn't modify files)

## Testing

- **Unit Tests:** `app/src/test/` (Robolectric, MockK, Truth assertions)
- **Run Tests:** `./gradlew testDebugUnitTest`
- **Test Configuration:** Tests use JVM args `-XX:+EnableDynamicAgentLoading` (required for MockK)
- **Current Status:** 89 tests, 3 skipped (as of last run)

## Making Code Changes

1. **Always format first:** `./gradlew formatKotlin`
2. **Run full validation:** `./gradlew lintKotlin testDebugUnitTest --parallel --daemon`
3. **Build to verify:** `./gradlew assembleDebug --parallel --daemon`
4. **Optional Android lint:** `./gradlew lintDebug`
5. **Commit changes** only if all commands succeed

## Trust These Instructions

These instructions have been validated by running all commands in a clean repository clone. If you encounter issues not documented here:
1. First verify you're using JDK 17 and the Gradle wrapper (`./gradlew`)
2. Try `./gradlew clean` then retry the command
3. Check if similar issues exist in closed GitHub issues
4. Only search the codebase if these instructions prove incomplete or incorrect

## Quick Reference

| Task | Command | Time | Notes |
|------|---------|------|-------|
| Format | `./gradlew formatKotlin` | 5s | Required before commit |
| Lint + Test | `./gradlew lintKotlin testDebugUnitTest --parallel --daemon` | 2-3 min | CI validation |
| Build Debug | `./gradlew assembleDebug --parallel --daemon` | 1-2 min | Creates APK |
| Android Lint | `./gradlew lintDebug` | 50s | Post-merge check |
| Clean | `./gradlew clean` | 10s | Clears build/ dirs |
| Version Info | `./scripts/show_version_info.sh` | 1s | Shows versions |

## Files at Repository Root

```
.editorconfig           # Editor config (ktlint Composable rule)
.gitignore             # Git ignore patterns
CONTRIBUTING.md        # Contribution guidelines (detailed build steps)
LICENSE                # License file
README.md              # Project overview and features
RELEASE_CHECKLIST.md   # Release process documentation
build.gradle.kts       # Root build configuration
settings.gradle.kts    # Project settings
gradle.properties      # Gradle configuration (JVM heap: 2GB)
gradlew / gradlew.bat  # Gradle wrapper scripts
app/                   # Main application module
gradle/                # Gradle wrapper JARs and version catalogs
.github/               # GitHub Actions workflows
.githooks/             # Git hooks
.idea/                 # Android Studio settings
keystore/              # Debug and release keystores
metadata/              # F-Droid metadata
fastlane/              # Fastlane configuration for app store
project-resources/     # Design assets, PRD documents
scripts/               # Utility scripts (version info)
```
