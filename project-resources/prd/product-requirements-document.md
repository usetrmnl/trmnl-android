# TRMNL App - Product Requirements Document (PRD)

## 1. Introduction

The TRMNL app serves as a digital display for TRMNL e-ink devices and BYOS installations. It connects to the
TRMNL or BYOS API, fetches display data, and shows it on Android mobile devices. This app can function both as a mirror
for existing TRMNL devices or as a standalone TRMNL device connected directly to BYOS servers. The app supports multiple
device types including official TRMNL devices, Bring Your Own Device (BYOD), and Bring Your Own Server
(BYOS) configurations. The app provides automatic and manual refresh capabilities to keep the display
synchronized with the TRMNL content.

This document outlines the requirements for the TRMNL Android application.

## 2. User Requirements

### Authentication

| ID      | User Requirement                                                                  | Technical Requirement                                                                                                |
|---------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| AUTH-01 | As a user, I should be able to configure my TRMNL API token.                      | The app must provide a secure token input field with options to show/hide the token.                                 |
| AUTH-02 | As a user, I should be able to validate my token before saving.                   | The app must connect to TRMNL API with the provided token to verify its validity and retrieve any associated images. |
| AUTH-03 | As a user, I should be able to understand how to get a token if I don't have one. | The app must provide help text with links to the TRMNL dashboard and documentation.                                  |
| AUTH-04 | As a user, I should be able to update my token if it changes.                     | The app must allow editing and re-validation of a previously saved token.                                            |
| AUTH-05 | As a user, I should be able to select my device type (TRMNL, BYOD, or BYOS).      | The app must provide a device type selector with appropriate explanatory text for each type.                         |
| AUTH-06 | As a user of BYOS, I should be able to provide a custom server URL.               | The app must provide a server URL input field for BYOS device types.                                                 |
| AUTH-07 | As a user of BYOS, I should be able to provide my device MAC address.             | The app must provide a MAC address input field with format validation and show/hide functionality.                   |
| AUTH-08 | As a user of BYOS, I should be able to set up a new device if required.           | The app must detect when device setup is required and provide a setup flow for new BYOS devices.                     |

### Display

| ID      | User Requirement                                                                               | Technical Requirement                                                                                  |
|---------|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| DISP-01 | As a user, I should see the TRMNL e-ink display content mirrored on my device.                 | The app must retrieve and display the current image from the TRMNL API and fit it to the screen.       |
| DISP-02 | As a user, I should be able to manually refresh the current image.                             | The app must provide a refresh button that triggers an immediate API call to fetch the latest image.   |
| DISP-03 | As a user, I should be able to load the next image in my playlist.                             | The app must provide a button to request the next image in the TRMNL playlist from the API.            |
| DISP-04 | As a user, I should understand how to access the control panel.                                | The app must provide a visual indicator for tap interaction when first launched.                       |
| DISP-05 | As a user, I should be able to pin the control panel if needed.                                | The app must provide an option to keep the control panel visible until manually dismissed.             |
| DISP-06 | As a user, I should be able to see information about the current display configuration.        | The app must show device type and connection information when relevant.                                |
| DISP-07 | As a user, different device types should behave appropriately for playlist navigation.         | The app must automatically use next image endpoint for BYOD/BYOS and current image for TRMNL devices.  |

### Refresh Schedule

| ID       | User Requirement                                                                                     | Technical Requirement                                                                                                                         |
|----------|------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| SCHED-01 | As a user, I should see when the next automatic refresh will occur.                                  | The app must display a countdown or timestamp for the next scheduled refresh.                                                                 |
| SCHED-02 | As a user, I should be able to cancel the scheduled refresh if needed.                               | The app must provide an option to cancel any scheduled refresh jobs.                                                                          |
| SCHED-03 | As a user, I should have my display automatically refreshed based on the server-configured interval. | The app must implement a background job scheduler to periodically refresh the image according to API-provided intervals (minimum 15 minutes). |
| SCHED-04 | As a user, I should understand the limitations of background refresh intervals.                      | The app must inform users of the minimum 15-minute refresh interval limitation imposed by Android WorkManager.                               |
| SCHED-05 | As a user, I should have a consistent refresh experience across device types.                        | The app must adapt refresh logic based on device type (TRMNL, BYOD, BYOS) while maintaining consistent UX.                                   |
| SCHED-06 | As a user, my display refresh interval should adapt based on server configuration.                   | The app must update its refresh schedule when the server provides a new refresh interval.                                                    |

### Logging

| ID     | User Requirement                                                        | Technical Requirement                                                                                                     |
|--------|-------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| LOG-01 | As a user, I should be able to view a history of refresh attempts.      | The app must maintain and display a log of all refresh attempts, including successful and failed operations.              |
| LOG-02 | As a user, I should be able to see statistics about refresh operations. | The app must display counts of successful and failed refresh attempts.                                                    |
| LOG-03 | As a user, I should be able to clear logs when needed.                  | The app must provide an option to clear the refresh history logs.                                                         |
| LOG-04 | As a user, I should be able to see detailed information about each log. | The app must record HTTP response metadata, image URLs, timestamps, and device types for each refresh operation.          |
| LOG-05 | As a user, I should be able to view logs with appropriate formatting.   | The app must present logs in a readable format with timestamps, success/failure indicators, and relevant error messages.  |

### Error Handling

| ID     | User Requirement                                                        | Technical Requirement                                                                                                   |
|--------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| ERR-01 | As a user, I should see clear error messages when something goes wrong. | The app must display descriptive error messages for various failure scenarios (network issues, token validation, etc.). |
| ERR-02 | As a user, I should be able to recover from errors easily.              | The app must provide troubleshooting options and retry buttons for error states.                                        |
| ERR-03 | As a user, I should be informed when there is no token configured.      | The app must automatically prompt for token configuration if none is found.                                             |
| ERR-04 | As a user, I should be informed of the progress during operations.      | The app must show loading indicators with descriptive text for operations like validation and image refresh.            |
| ERR-05 | As a user, I should be alerted to device setup requirements.            | The app must detect and handle device setup requirements for BYOS configurations.                                       |
| ERR-06 | As a user, I should be informed when server URL or MAC formats are invalid. | The app must validate server URLs and MAC address formats with clear error messages.                                |

### App Information

| ID      | User Requirement                                                                      | Technical Requirement                                                                                 |
|---------|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------- |
| INFO-01 | As a user, I should be able to access information about the app.                     | The app must provide an app info screen with version details and developer information.               |
| INFO-02 | As a user, I should be able to understand when fake API data is being used.          | The app must display a prominent banner when running in development mode with fake API data.          |
| INFO-03 | As a user, I should have access to relevant documentation and resources.             | The app must provide links to TRMNL documentation, websites, and appropriate developer resources.     |
| INFO-04 | As a user, I should be able to navigate between the app settings and display screen. | The app must provide intuitive navigation between configuration and display screens.                  |

## 3. Technical Requirements

### API Integration

| ID     | Requirement                                                                                  |
|--------|----------------------------------------------------------------------------------------------|
| API-01 | The app must implement authentication with the TRMNL API using the provided access token.    |
| API-02 | The app must fetch the current display image from the TRMNL API for TRMNL device types.      |
| API-03 | The app must fetch the next display image from the TRMNL API for BYOD/BYOS device types.     |
| API-04 | The app must handle API responses that include refresh interval configurations.              |
| API-05 | The app must support requesting the next image in a playlist through the API.                |
| API-06 | The app must gracefully handle API errors with appropriate user feedback.                    |
| API-07 | The app must support device setup API calls for BYOS configurations.                         |
| API-08 | The app must support custom server URLs for BYOS device types.                               |
| API-09 | The app must properly handle device MAC ID authentication for applicable device types.       |
| API-10 | The app must support fake/mock API data when running in development mode.                    |

### Image Handling

| ID     | Requirement                                                                  |
|--------|------------------------------------------------------------------------------|
| IMG-01 | The app must cache images locally for offline viewing.                       |
| IMG-02 | The app must properly scale and display images to fit the device screen.     |
| IMG-03 | The app must handle image loading/rendering errors gracefully.               |
| IMG-04 | The app must update the cached image when a new one is fetched from the API. |
| IMG-05 | The app must support proper image transitions when switching between images. |
| IMG-06 | The app must display image metadata including filename when available.       |

### Background Processing

| ID    | Requirement                                                                              |
|-------|------------------------------------------------------------------------------------------|
| BG-01 | The app must implement a background job scheduler for periodic image refreshes.          |
| BG-02 | The app must respect the minimum refresh interval of 15 minutes for scheduled refreshes. |
| BG-03 | The app must adapt refresh schedules based on server-provided configuration.             |
| BG-04 | The app must allow one-time immediate refreshes outside the schedule.                    |
| BG-05 | The app must log all refresh attempts (scheduled and manual) with timestamps.            |
| BG-06 | The app must optimize background work to respect battery optimization settings.          |
| BG-07 | The app must handle background work cancellation and rescheduling.                       |

### Data Storage

| ID     | Requirement                                                                             |
|--------|-----------------------------------------------------------------------------------------|
| DAT-01 | The app must securely store the access token using Android DataStore.                   |
| DAT-02 | The app must store device type configuration (TRMNL, BYOD, BYOS).                       |
| DAT-03 | The app must store server URLs for BYOS configurations.                                 |
| DAT-04 | The app must store device MAC IDs for relevant device types.                            |
| DAT-05 | The app must store image metadata including URLs and refresh intervals.                 |
| DAT-06 | The app must maintain a persistent log of refresh operations.                           |
| DAT-07 | The app must allow clearing stored logs without affecting other stored data.            |
| DAT-08 | The app must handle data migrations for configuration changes.                          |

### UI/UX

| ID    | Requirement                                                                                              |
|-------|----------------------------------------------------------------------------------------------------------|
| UX-01 | The app must implement a full-screen display mode for image viewing.                                     |
| UX-02 | The app must provide a tap-to-reveal control panel with auto-hide functionality.                         |
| UX-03 | The app must include a pin option to keep the control panel visible.                                     |
| UX-04 | The app must display loading states with descriptive text.                                               |
| UX-05 | The app must provide enhanced error states with troubleshooting options.                                 |
| UX-06 | The app must logically group controls by function (configuration, image control, display options, logs). |
| UX-07 | The app must optimize UI for both phone and tablet form factors.                                         |
| UX-08 | The app must support "keep screen on" functionality to prevent device sleep.                             |
| UX-09 | The app must present device type selection with appropriate icons and descriptions.                      |
| UX-10 | The app must implement Material Design 3 components and theming.                                         |
| UX-11 | The app must handle device orientation changes appropriately.                                            |

## 4. Non-Functional Requirements

### Performance

| ID      | Requirement                                                                                    |
|---------|------------------------------------------------------------------------------------------------|
| PERF-01 | The app must load cached images within 1 second of launch when available.                      |
| PERF-02 | The app must minimize battery consumption during background refresh operations.                |
| PERF-03 | The app must optimize image loading with appropriate caching policies.                         |
| PERF-04 | The app must provide responsive UI even during network operations.                             |
| PERF-05 | The app must use coroutines for asynchronous operations.                                       |
| PERF-06 | The app must utilize WorkManager for scheduled background tasks.                               |

### Security

| ID     | Requirement                                                                                  |
|--------|----------------------------------------------------------------------------------------------|
| SEC-01 | The app must securely store access tokens using Android DataStore.                           |
| SEC-02 | The app must mask access tokens and device MAC IDs by default when displayed.                |
| SEC-03 | The app must use secure HTTPS connections for all API communications.                        |
| SEC-04 | The app must validate server URLs to ensure they use HTTPS for BYOS configurations.          |
| SEC-05 | The app must avoid exposing sensitive information in logs.                                   |

### Compatibility

| ID      | Requirement                                                                              |
|---------|------------------------------------------------------------------------------------------|
| COMP-01 | The app must function on Android tablets with appropriate optimizations.                  |
| COMP-02 | The app must support e-ink tablets with appropriate image handling.                       |
| COMP-03 | The app must support different screen sizes and orientations.                             |
| COMP-04 | The app must support Android API level 28 (Android 9.0 Pie) and above.                    |
| COMP-05 | The app must adapt UI components based on device window size class.                       |

## 5. Implementation Architecture

### Android Implementation

| ID     | Component                                                             | Implementation                                                    |
|--------|-----------------------------------------------------------------------|-------------------------------------------------------------------|
| ARCH-01 | UI Framework                                                          | Jetpack Compose with Circuit UDF architecture                     |
| ARCH-02 | Background Processing                                                 | WorkManager for scheduled image refreshes                         |
| ARCH-03 | Networking                                                            | Retrofit and OkHttp with EitherNet for API communication          |
| ARCH-04 | Dependency Injection                                                  | Dagger with Anvil                                                 |
| ARCH-05 | Data Storage                                                          | DataStore for preferences and token storage                       |
| ARCH-06 | Image Loading                                                         | Coil for image loading and caching                                |
| ARCH-07 | Logging                                                               | Timber for debug logging                                          |
| ARCH-08 | JSON Parsing                                                          | Moshi for JSON serialization/deserialization                      |
| ARCH-09 | Threading/Concurrency                                                 | Kotlin Coroutines for async operations                            |

## 6. Implementation Considerations for Cross-Platform Development

This section provides guidance for teams implementing the TRMNL app on other platforms, ensuring feature parity while leveraging platform-specific capabilities.

### iOS Specific

| ID     | Consideration                                                             |
|--------|---------------------------------------------------------------------------|
| IOS-01 | Use KeychainAccess or Keychain Services for secure token storage.         |
| IOS-02 | Implement BGAppRefreshTask for background refresh operations.             |
| IOS-03 | Consider SwiftUI for modern UI implementation or UIKit for broader compatibility. |
| IOS-04 | Use UserDefaults for non-sensitive configuration storage.                 |
| IOS-05 | Use URLSession for API communication with proper authentication handling. |
| IOS-06 | Implement UIApplication.shared.isIdleTimerDisabled for keeping screen on. |
| IOS-07 | Use Kingfisher or SDWebImage for efficient image loading and caching.     |
| IOS-08 | Implement WidgetKit for optional home screen widget display.              |
| IOS-09 | Use Combine or async/await for asynchronous operations.                   |
| IOS-10 | Consider implementing handoff for continuity between iOS and macOS.       |

### Flutter Specific

| ID     | Consideration                                                                |
|--------|------------------------------------------------------------------------------|
| FLT-01 | Use flutter_secure_storage for token storage.                                |
| FLT-02 | Implement workmanager or flutter_background_fetch for background operations. |
| FLT-03 | Use Riverpod, Provider, or Bloc pattern for state management.               |
| FLT-04 | Consider using shared_preferences for non-sensitive configuration storage.   |
| FLT-05 | Implement wakelock plugin for keeping the screen on.                         |
| FLT-06 | Use cached_network_image or extended_image for image caching and loading.    |
| FLT-07 | Utilize dio or http package with interceptors for API communication.         |
| FLT-08 | Implement adaptive_theme for supporting light/dark modes.                    |
| FLT-09 | Use responsive_framework for handling different screen sizes.                |
| FLT-10 | Consider hive or objectbox for efficient local storage/caching.              |

### React Native Specific

| ID     | Consideration                                                                |
|--------|------------------------------------------------------------------------------|
| RN-01  | Use react-native-keychain for secure token storage.                          |
| RN-02  | Implement react-native-background-tasks for background operations.           |
| RN-03  | Use Redux Toolkit or Context API with hooks for state management.            |
| RN-04  | Consider using @react-native-async-storage/async-storage for configuration.  |
| RN-05  | Implement react-native-keep-awake for keeping the screen on.                 |
| RN-06  | Use react-native-fast-image for efficient image caching and loading.         |
| RN-07  | Utilize axios with interceptors for API communication.                       |
| RN-08  | Implement react-native-paper or Native Base for consistent UI components.    |
| RN-09  | Use react-native-device-info for platform-specific adaptations.              |
| RN-10  | Consider react-native-reanimated for smooth animations and transitions.      |

## 7. Limitations and Constraints

| ID     | Limitation/Constraint                                                                                        |
|--------|--------------------------------------------------------------------------------------------------------------|
| LIM-01 | Background refresh intervals are limited to a minimum of 15 minutes due to Android WorkManager constraints.   |
| LIM-02 | E-ink display wake lock may not work consistently across all devices due to aggressive battery optimization. |
| LIM-03 | API token must be manually obtained from the TRMNL dashboard; no in-app registration is supported.           |
| LIM-04 | App does not directly control the content shown; it only mirrors what's configured on the TRMNL platform.    |
| LIM-05 | Custom server URLs for BYOS must use HTTPS for security reasons.                                             |
| LIM-06 | Device MAC address format must follow standard formats (XX:XX:XX:XX:XX:XX, XX-XX-XX-XX-XX-XX, or XXXXXXXXXXXX). |
| LIM-07 | Support is limited to Android API level 28 (Android 9.0 Pie) and above.                                      |
| LIM-08 | Background refresh capabilities vary across platforms (iOS: ~15 minutes, Flutter/React Native: dependent on platform implementation). |
| LIM-09 | Cross-platform implementations must maintain visual and functional consistency despite platform differences. |
| LIM-10 | iOS will require additional privacy descriptions in info.plist for network and background usage.             |
| LIM-11 | Flutter has more limited e-ink display optimization capabilities compared to native implementations.         |
| LIM-12 | React Native may have performance limitations when handling large image rendering on low-end devices.        |

This PRD provides a comprehensive guide for implementing and maintaining the TRMNL app. Development teams should use this document to ensure the application meets all required functionality while following platform-specific best practices.