# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TRMNL Android is a native Android app that displays TRMNL e-ink device content on Android phones, tablets, and e-ink displays. It connects to TRMNL or BYOS (Bring Your Own Server) APIs, fetches display images periodically, and renders them. The app can function as a mirror for existing TRMNL devices or as a standalone device.

**Tech Stack:**
- Language: Kotlin (100%)
- Build: Gradle 8.13 with AGP 8.9.2
- Min SDK: 28 (Android 9.0 Pie), Target SDK: 36 (Android 16.0)
- UI: Jetpack Compose with Circuit UDF architecture (Slack's unidirectional data flow)
- DI: Dagger 2.56.2 with Anvil 0.4.1 for code generation
- Background Work: WorkManager 2.10.1 (15-minute minimum interval limitation)
- Networking: Retrofit 2.11.0 + OkHttp 4.12.0 + Moshi 1.15.2
- Image Loading: Coil 3.2.0 with OkHttp integration
- API Result Modeling: EitherNet 2.0.0 from Slack
- Data Storage: DataStore 1.1.7 for preferences/tokens
- Logging: Timber 5.0.1

## Essential Build Commands

**ALWAYS run these commands in this order before committing:**

```bash
# 1. Format code (REQUIRED before commit)
./gradlew formatKotlin

# 2. Lint and test (REQUIRED - matches CI exactly)
./gradlew lintKotlin testDebugUnitTest --parallel --daemon

# 3. Build debug APK (verify compilation)
./gradlew assembleDebug --parallel --daemon

# 4. Optional: Android lint (post-merge validation)
./gradlew lintDebug
```

**Notes:**
- Use JDK 17 (OpenJDK 17 - Temurin distribution)
- ALWAYS use `./gradlew` wrapper, NEVER system gradle
- First build takes 2-3 minutes (downloads deps + KSP/KAPT), subsequent ~1 minute
- CI workflow: `.github/workflows/android.yml` runs commands 1-3
- Tests use MockK with JVM arg `-XX:+EnableDynamicAgentLoading`

## Architecture Overview

### Main Source Structure
```
app/src/main/java/ink/trmnl/android/
├── MainActivity.kt               # Entry point, WorkInfo observer
├── TrmnlDisplayMirrorApp.kt     # Application class
├── data/                         # Repos, DataStore, ImageMetadata
│   ├── TrmnlDisplayRepository.kt         # Main API data repository
│   ├── TrmnlDeviceConfigDataStore.kt     # Device config (token, server)
│   ├── ImageMetadataStore.kt             # Image state management
│   ├── log/                              # Refresh log management
├── di/                          # Dagger modules
│   ├── AppComponent.kt                   # Main app component
│   ├── NetworkModule.kt                  # Retrofit, OkHttp, Moshi
│   ├── CircuitModule.kt                  # Circuit UI setup
├── model/                       # Data models
│   ├── TrmnlDeviceConfig.kt             # Device configuration
│   ├── TrmnlDeviceType.kt               # Device type enum
├── network/                     # API layer
│   ├── TrmnlApiService.kt               # Retrofit service
│   ├── model/                           # API response models
├── ui/                          # Jetpack Compose screens
│   ├── display/TrmnlMirrorDisplayScreen.kt    # Main display
│   ├── settings/AppSettingsScreen.kt           # Settings UI
│   ├── refreshlog/DisplayRefreshLogScreen.kt  # Refresh history
├── util/                        # Utilities
│   ├── InputValidator.kt                # Token/URL validation
│   ├── ImageSaver.kt                    # Save images to gallery
├── work/                        # Background work
│   ├── TrmnlImageRefreshWorker.kt       # Worker implementation
│   ├── TrmnlWorkScheduler.kt            # WorkManager scheduling
│   ├── TrmnlImageUpdateManager.kt       # Image update flow
```

### Key Architectural Patterns

**Circuit UDF (Unidirectional Data Flow):**
- All screens use `@CircuitInject` annotation for DI with Anvil
- Presenter pattern: State + Event → Presenter → Updated State
- Example: `TrmnlMirrorDisplayScreen` with presenter handling events

**Dependency Injection:**
- Dagger with Anvil for compile-time code generation
- `@ContributesTo(AppScope::class)` for module contribution
- `@SingleIn(AppScope::class)` for app-scoped singletons
- Circuit integration: `arg("anvil-ksp-extraContributingAnnotations", "com.slack.circuit.codegen.annotations.CircuitInject")`

**Background Work:**
- WorkManager with 15-minute minimum interval (Android OS limitation)
- Periodic work: `IMAGE_REFRESH_PERIODIC_WORK_NAME` for scheduled refreshes
- One-time work: `IMAGE_REFRESH_ONETIME_WORK_NAME` for manual refreshes
- `TrmnlImageRefreshWorker` fetches image, logs results, returns URL
- MainActivity observes WorkInfo and updates `TrmnlImageUpdateManager`
- Workaround: Periodic work directly calls `TrmnlImageUpdateManager.updateImage()` due to observer limitations

**Networking:**
- Base URL is dummy (`https://dummy-base-url.com/`) - actual URLs use `@Url` parameter
- Dynamic URL construction supports TRMNL, BYOD, BYOS servers
- Custom User-Agent: `{appName}/{versionName} (build:{versionCode}; Android {OS_VERSION})`
- OkHttp cache: 10MB in `context.cacheDir/http_cache`
- Timeouts: 30s connect/read/write
- EitherNet wraps responses: `ApiResult<Success, Failure>`

**Image Loading Flow:**
1. WorkManager triggers `TrmnlImageRefreshWorker`
2. Worker fetches from `TrmnlDisplayRepository`
3. Worker updates `TrmnlImageUpdateManager` (periodic) or returns result (one-time)
4. MainActivity observes WorkInfo, calls `TrmnlImageUpdateManager.updateImage()` (one-time only)
5. `TrmnlMirrorDisplayScreen` collects from `imageUpdateFlow`
6. Coil's `AsyncImage` loads image
   - HTTP 403 (expired URL): Auto-triggers refresh
   - Other errors: Shows error UI

## Configuration Files

- `app/build.gradle.kts` - Version: versionCode=28, versionName="2.4.2"
- `gradle/libs.versions.toml` - Centralized dependency versions
- `.editorconfig` - ktlint rule: `ktlint_function_naming_ignore_when_annotated_with = Composable`
- `secret.properties` (optional) - Local keystore secrets (not in repo)
- `keystore/debug.keystore` - Debug signing (included in repo)

## Build Variants

**Debug:**
- `buildConfigField("Boolean", "USE_FAKE_API", "true")`
- Uses debug keystore with password "android"
- Fake API can be overridden in `RepositoryConfigProvider`

**Release:**
- `buildConfigField("Boolean", "USE_FAKE_API", "false")`
- Requires production keystore from CI secrets or `secret.properties`
- Keystore env vars: `KEYSTORE_PASSWORD`, `KEY_ALIAS`
- Code shrinking: `isMinifyEnabled = true`, `isShrinkResources = true`
- ProGuard: `proguard-android-optimize.txt` + `proguard-rules.pro`

## Version Management

**DO NOT manually edit version numbers.** Use automated workflow:
- Check version: `./scripts/show_version_info.sh`
- Update workflow: `.github/workflows/version-management.yml`
- Updates: `app/build.gradle.kts`, `metadata/ink.trmnl.android.yml`, changelogs

## CI/CD Workflows

1. **android.yml** - PR & main branch: lint + test + build (~3 min)
2. **android-lint.yml** - Post-merge: Android lint validation (~50s)
3. **android-release.yml** - Release builds: signed APK + AAB
4. **version-management.yml** - Manual version bumping

## Common Issues

- **"secret.properties not found"**: Informational only, debug builds work without it
- **First build slow**: Expected, dependencies + annotation processors (KSP, KAPT)
- **"Sharing is only supported for boot loader classes"**: Harmless Robolectric warning
- **"INVISIBLE_REFERENCE" in NetworkingTools.kt:56**: Expected, uses internal Kotlin API
- **WorkManager 15-min minimum**: Android OS limitation, not a bug

## Code Style

- Linter: ktlint via kotlinter-gradle 5.0.2
- Format: `./gradlew formatKotlin` (auto-fixes)
- Check: `./gradlew lintKotlin` (no modifications)
- Only customization: Allow PascalCase for `@Composable` functions

## Testing

- Unit tests: `app/src/test/` (Robolectric 4.14.1, MockK 1.14.2, Truth 1.4.4)
- Run: `./gradlew testDebugUnitTest`
- Current status: 89 tests, 3 skipped

## Important Notes

- Token-based auth required: TRMNL access token or device ID (MAC address)
- Supports custom server URLs for BYOS installations
- Screen wake lock: `FLAG_KEEP_SCREEN_ON` doesn't work on e-Ink tablets (battery optimization)
- F-Droid compatible: `dependenciesInfo.includeInApk = false`
- Kotlin version capped at 2.1.10 due to Dagger compatibility

## Copilot Instructions Context

From `.github/copilot-instructions.md`:
- Always run format + lint + test before committing
- Use Circuit's `@CircuitInject` for UI components
- WorkManager periodic work has 15-min minimum interval
- Image loading handles HTTP 403 auto-refresh for expired URLs
- Dagger KSP support is in Alpha - using KAPT for now
- Trust the documented build commands - they're CI-validated
