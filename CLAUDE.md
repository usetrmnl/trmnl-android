# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TRMNL Android is a native Android app that displays TRMNL e-ink device content on Android phones, tablets, and e-ink displays. It connects to TRMNL or BYOS (Bring Your Own Server) APIs, fetches display images periodically, and renders them. The app can function as a mirror for existing TRMNL devices or as a standalone device.

**Tech Stack:**
- Language: Kotlin (100%)
- Build: Gradle 8.13 with AGP 8.9.2
- Min SDK: 28 (Android 9.0 Pie), Target SDK: 36 (Android 16.0)
- UI: Jetpack Compose with Circuit UDF architecture (Slack's unidirectional data flow)
- DI: Metro 0.12.1 (dev.zacsweers.metro) with MetroX Android for compile-time DI
- Background Work: WorkManager 2.10.1 (15-minute minimum interval limitation)
- Networking: Retrofit 2.11.0 + OkHttp 4.12.0 + Moshi 1.15.2
- Image Loading: Coil 3.2.0 with OkHttp integration
- API Result Modeling: EitherNet 2.0.0 from Slack
- Data Storage: DataStore 1.1.7 for preferences/tokens
- Logging: Timber 5.0.1

## Essential Build Commands

### Fresh Checkout (First Time Setup)
**CRITICAL: On a fresh clone, you MUST build first to generate code:**

```bash
# 1. Build debug APK (generates Metro/Circuit code via compiler plugin + KSP)
./gradlew assembleDebug --parallel --daemon

# 2. Now you can run tests
./gradlew testDebugUnitTest --parallel --daemon
```

**Why?** Running tests before building will fail with Metro errors about missing generated modules. The build step generates necessary code via Metro compiler plugin (DI) and KSP (Circuit).

### Regular Development (After Initial Build)

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
- JDK: Minimum JDK 21 (required by Metro gradle plugin)
- ALWAYS use `./gradlew` wrapper, NEVER system gradle
- First build: ~1-2 minutes (optimized with G1GC, parallel execution, configuration cache)
- Subsequent builds: ~2-8 seconds with Gradle cache and configuration cache
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
├── di/                          # Metro DI modules
│   ├── AppGraph.kt                       # Main app dependency graph
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
- All screens use `@CircuitInject` annotation for DI with Metro
- Presenter pattern: State + Event → Presenter → Updated State
- Example: `TrmnlMirrorDisplayScreen` with presenter handling events

**Dependency Injection:**
- Metro for compile-time code generation via Kotlin compiler plugin (no KAPT)
- `@ContributesTo(AppScope::class)` for module contribution
- `@SingleIn(AppScope::class)` for app-scoped singletons
- `@DependencyGraph(AppScope::class)` on `AppGraph` as the root component
- Circuit integration: `arg("circuit.codegen.mode", "metro")`

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
- `gradle.properties` - **Optimized for performance** (see below)

### Gradle Performance Optimizations

The project uses optimized Gradle settings based on best practices from the [Now in Android](https://github.com/android/nowinandroid) reference project:

**JVM & Kotlin Daemon Settings:**
- G1 Garbage Collector (`-XX:+UseG1GC`) for better memory management
- Increased heap size to 4GB (`-Xmx4g`) for faster builds
- Optimized code cache size (256m for Gradle, 320m for Kotlin daemon)
- Soft reference tuning for better GC performance
- Heap dump on OutOfMemoryError for debugging

**Build Performance Features:**
- **Parallel execution** (`org.gradle.parallel=true`) - Runs independent tasks in parallel
- **Configuration cache** (`org.gradle.configuration-cache=true`) - Caches build configuration
- **Build cache** (`org.gradle.caching=true`) - Reuses outputs from previous builds
- **Disabled unused features** - resvalues and shaders build features disabled

**Performance Impact:**
- Clean build: ~1m 22s (56% faster than baseline)
- Cached builds: ~2-8 seconds
- Configuration cache significantly speeds up subsequent builds

**Note:** These settings require at least **8GB+ of available RAM** (4GB for Gradle daemon + 4GB for Kotlin daemon). On memory-constrained systems, reduce the `-Xmx` values in `gradle.properties` (e.g., to 2GB each for systems with 4-6GB RAM).

## Build Types (Not Flavors)

**Note:** This project uses only build TYPES (debug/release), NOT product flavors.

**Debug:**
- Uses debug keystore with password "android" (included in repo)
- HttpLoggingInterceptor enabled with BODY level

**Release:**
- Requires production keystore from CI secrets or `secret.properties`
- Keystore env vars: `KEYSTORE_PASSWORD`, `KEY_ALIAS`
- Code shrinking: `isMinifyEnabled = true`, `isShrinkResources = true`
- ProGuard: `proguard-android-optimize.txt` + `proguard-rules.pro`
- Privacy: `dependenciesInfo.includeInApk = false` (disables dependency metadata)

## Version Management

**DO NOT manually edit version numbers.** Use automated workflow:
- Check version: `./scripts/show_version_info.sh`
- Update workflow: `.github/workflows/version-management.yml`
- Updates: `app/build.gradle.kts`, changelogs

## CI/CD Workflows

1. **android.yml** - PR & main branch: lint + test + build (~3 min)
2. **android-lint.yml** - Post-merge: Android lint validation (~50s)
3. **android-release.yml** - Release builds: signed APK + AAB
4. **version-management.yml** - Manual version bumping

## Common Issues

- **Tests fail on fresh checkout**: Must run `./gradlew assembleDebug` first to generate Metro/Circuit code
- **"secret.properties not found"**: Informational only, debug builds work without it
- **First build slow**: Expected for clean builds, ~1-2 minutes with optimized Gradle settings
- **"Sharing is only supported for boot loader classes"**: Harmless Robolectric warning, tests still pass
- **"INVISIBLE_REFERENCE" in NetworkingTools.kt:56**: Expected, uses internal Kotlin API (documented warning)
- **WorkManager 15-min minimum**: Android OS limitation, not a bug
- **Metro codegen errors about missing modules**: Run `assembleDebug` first to generate code

## Code Style

- Linter: ktlint via kotlinter-gradle 5.0.2
- Format: `./gradlew formatKotlin` (auto-fixes)
- Check: `./gradlew lintKotlin` (no modifications)
- Only customization: Allow PascalCase for `@Composable` functions

## Icons & Vector Drawables

**Custom Icons Class:** `ink.trmnl.android.ui.icons.Icons` replaces deprecated `androidx.compose.material.icons`

- **Location:** `app/src/main/java/ink/trmnl/android/ui/icons/Icons.kt`
- **Structure:** Nested objects matching Material Icons API
  - `Icons.Default.*` - Filled icons (CheckCircle, Clear, DateRange, PlayArrow, Refresh, Settings, Share, Warning)
  - `Icons.Outlined.*` - Outlined variants (Info, Warning)
  - `Icons.AutoMirrored.Filled.*` - Auto-mirrored for RTL (ArrowBack, ArrowForward, List)

**Adding New Icons:**
1. Download from [Google Fonts Material Symbols](https://fonts.google.com/icons)
2. Convert to Android Vector Drawable (use Vector Asset Studio in Android Studio)
3. Save to `app/src/main/res/drawable/ic_[name]_[size]dp.xml`
4. Expose in `Icons.kt` with `@Composable get() = ImageVector.vectorResource(id = R.drawable.ic_name)`
5. Place in appropriate category (Default/Outlined/AutoMirrored)

**Never use:** `androidx.compose.material.icons.Icons` (deprecated, issue #226)

## Testing

- Unit tests: `app/src/test/` (Robolectric 4.14.1, MockK 1.14.2, Truth 1.4.4)
- Run: `./gradlew testDebugUnitTest`
- Current status: 89 tests, 3 skipped

## Important Notes

- **Authentication**: Token-based auth required (TRMNL access token or device ID/MAC address)
- **BYOS Support**: Supports custom server URLs for BYOS installations
- **Screen Wake Lock**: `FLAG_KEEP_SCREEN_ON` doesn't work reliably on e-Ink tablets due to aggressive battery optimization
- **Kotlin Version**: 2.3.20+ (Metro removed the Dagger/KAPT compatibility constraint)
- **No Product Flavors**: Only debug/release build types exist

## Copilot Instructions Context

From `.github/copilot-instructions.md`:
- Always run format + lint + test before committing
- Use Circuit's `@CircuitInject` for UI components
- WorkManager periodic work has 15-min minimum interval
- Image loading handles HTTP 403 auto-refresh for expired URLs
- Metro uses a Kotlin compiler plugin — no KAPT, no KSP for DI itself
- Trust the documented build commands - they're CI-validated
