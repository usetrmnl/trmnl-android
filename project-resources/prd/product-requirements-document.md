# TRMNL Display App - Product Requirements Document (PRD)

## 1. Introduction

The TRMNL Display app serves as a digital display mirror for TRMNL e-ink devices. It connects to the
TRMNL API, fetches display data, and shows it on mobile devices. This document outlines the
requirements for implementing this app on different platforms.

## 2. User Requirements

### Authentication

| ID      | User Requirement                                                                  | Technical Requirement                                                                                                |
|---------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| AUTH-01 | As a user, I should be able to configure my TRMNL API token.                      | The app must provide a secure token input field with options to show/hide the token.                                 |
| AUTH-02 | As a user, I should be able to validate my token before saving.                   | The app must connect to TRMNL API with the provided token to verify its validity and retrieve any associated images. |
| AUTH-03 | As a user, I should be able to understand how to get a token if I don't have one. | The app must provide help text with links to the TRMNL dashboard and documentation.                                  |
| AUTH-04 | As a user, I should be able to update my token if it changes.                     | The app must allow editing and re-validation of a previously saved token.                                            |

### Display

| ID      | User Requirement                                                                               | Technical Requirement                                                                                  |
|---------|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| DISP-01 | As a user, I should see the TRMNL e-ink display content mirrored on my device.                 | The app must retrieve and display the current image from the TRMNL API and fit it to the screen.       |
| DISP-02 | As a user, I should be able to enable high contrast mode for better viewing on e-ink displays. | The app must provide a toggle for high contrast mode that enhances image visibility on e-ink displays. |
| DISP-03 | As a user, I should be able to manually refresh the current image.                             | The app must provide a refresh button that triggers an immediate API call to fetch the latest image.   |
| DISP-04 | As a user, I should be able to load the next image in my playlist.                             | The app must provide a button to request the next image in the TRMNL playlist from the API.            |
| DISP-05 | As a user, I should understand how to access the control panel.                                | The app must provide a visual indicator for tap interaction when first launched.                       |
| DISP-06 | As a user, I should be able to pin the control panel if needed.                                | The app must provide an option to keep the control panel visible until manually dismissed.             |

### Refresh Schedule

| ID       | User Requirement                                                                                     | Technical Requirement                                                                                                                         |
|----------|------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| SCHED-01 | As a user, I should see when the next automatic refresh will occur.                                  | The app must display a countdown or timestamp for the next scheduled refresh.                                                                 |
| SCHED-02 | As a user, I should understand the refresh schedule visually.                                        | The app must provide a visual timeline representing the time until the next refresh.                                                          |
| SCHED-03 | As a user, I should be able to cancel the scheduled refresh if needed.                               | The app must provide an option to cancel any scheduled refresh jobs.                                                                          |
| SCHED-04 | As a user, I should have my display automatically refreshed based on the server-configured interval. | The app must implement a background job scheduler to periodically refresh the image according to API-provided intervals (minimum 15 minutes). |

### Logging

| ID     | User Requirement                                                        | Technical Requirement                                                                                        |
|--------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| LOG-01 | As a user, I should be able to view a history of refresh attempts.      | The app must maintain and display a log of all refresh attempts, including successful and failed operations. |
| LOG-02 | As a user, I should be able to see statistics about refresh operations. | The app must display counts of successful and failed refresh attempts.                                       |
| LOG-03 | As a user, I should be able to clear logs when needed.                  | The app must provide an option to clear the refresh history logs.                                            |
| LOG-04 | As a user, I should be able to filter and sort logs.                    | The app must provide filtering and sorting options for the log entries.                                      |

### Error Handling

| ID     | User Requirement                                                        | Technical Requirement                                                                                                   |
|--------|-------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| ERR-01 | As a user, I should see clear error messages when something goes wrong. | The app must display descriptive error messages for various failure scenarios (network issues, token validation, etc.). |
| ERR-02 | As a user, I should be able to recover from errors easily.              | The app must provide troubleshooting options and retry buttons for error states.                                        |
| ERR-03 | As a user, I should be informed when there is no token configured.      | The app must automatically prompt for token configuration if none is found.                                             |
| ERR-04 | As a user, I should be informed of the progress during operations.      | The app must show loading indicators with descriptive text for operations like validation and image refresh.            |

## 3. Technical Requirements

### API Integration

| ID     | Requirement                                                                               |
|--------|-------------------------------------------------------------------------------------------|
| API-01 | The app must implement authentication with the TRMNL API using the provided access token. |
| API-02 | The app must fetch the current display image from the TRMNL API.                          |
| API-03 | The app must handle API responses that include refresh interval configurations.           |
| API-04 | The app must support requesting the next image in a playlist through the API.             |
| API-05 | The app must gracefully handle API errors with appropriate user feedback.                 |

### Image Handling

| ID     | Requirement                                                                  |
|--------|------------------------------------------------------------------------------|
| IMG-01 | The app must cache images locally for offline viewing.                       |
| IMG-02 | The app must properly scale and display images to fit the device screen.     |
| IMG-03 | The app must support high contrast mode for e-ink display optimization.      |
| IMG-04 | The app must handle image loading/rendering errors gracefully.               |
| IMG-05 | The app must update the cached image when a new one is fetched from the API. |

### Background Processing

| ID    | Requirement                                                                              |
|-------|------------------------------------------------------------------------------------------|
| BG-01 | The app must implement a background job scheduler for periodic image refreshes.          |
| BG-02 | The app must respect the minimum refresh interval of 15 minutes for scheduled refreshes. |
| BG-03 | The app must adapt refresh schedules based on server-provided configuration.             |
| BG-04 | The app must allow one-time immediate refreshes outside the schedule.                    |
| BG-05 | The app must log all refresh attempts (scheduled and manual) with timestamps.            |

### Data Storage

| ID     | Requirement                                                                             |
|--------|-----------------------------------------------------------------------------------------|
| DAT-01 | The app must securely store the access token using platform-appropriate secure storage. |
| DAT-02 | The app must store image metadata including URLs and refresh intervals.                 |
| DAT-03 | The app must maintain a persistent log of refresh operations.                           |
| DAT-04 | The app must allow clearing stored logs without affecting other stored data.            |

### UI/UX

| ID    | Requirement                                                                                              |
|-------|----------------------------------------------------------------------------------------------------------|
| UX-01 | The app must implement a full-screen display mode for image viewing.                                     |
| UX-02 | The app must provide a tap-to-reveal control panel with auto-hide functionality.                         |
| UX-03 | The app must include a pin option to keep the control panel visible.                                     |
| UX-04 | The app must display loading states with descriptive text.                                               |
| UX-05 | The app must provide enhanced error states with troubleshooting options.                                 |
| UX-06 | The app must include a visual timeline for refresh schedules.                                            |
| UX-07 | The app must logically group controls by function (configuration, image control, display options, logs). |
| UX-08 | The app must optimize UI for both phone and tablet form factors.                                         |
| UX-09 | The app must support "keep screen on" functionality to prevent device sleep.                             |

## 4. Non-Functional Requirements

### Performance

| ID      | Requirement                                                                                    |
|---------|------------------------------------------------------------------------------------------------|
| PERF-01 | The app must load cached images within 1 second of launch when available.                      |
| PERF-02 | The app must minimize battery consumption during background refresh operations.                |
| PERF-03 | The app must optimize image display for e-ink screens, considering refresh rates and contrast. |

### Security

| ID     | Requirement                                                                                  |
|--------|----------------------------------------------------------------------------------------------|
| SEC-01 | The app must securely store access tokens using platform-specific secure storage mechanisms. |
| SEC-02 | The app must mask access tokens by default when displayed.                                   |
| SEC-03 | The app must use secure HTTPS connections for all API communications.                        |

### Compatibility

| ID      | Requirement                                                                              |
|---------|------------------------------------------------------------------------------------------|
| COMP-01 | The app must function on e-ink tablets with appropriate optimizations.                   |
| COMP-02 | The app must support different screen sizes and orientations.                            |
| COMP-03 | The app must adhere to platform-specific UI guidelines while maintaining feature parity. |

## 5. Implementation Considerations for Cross-Platform Development

### iOS Specific

| ID     | Consideration                                                            |
|--------|--------------------------------------------------------------------------|
| IOS-01 | Use KeychainAccess for secure token storage.                             |
| IOS-02 | Implement BGAppRefreshTask for background refresh operations.            |
| IOS-03 | Consider UIKit or SwiftUI based on development team expertise.           |
| IOS-04 | Use UserDefaults for non-sensitive configuration storage.                |
| IOS-05 | Implement UIApplication.shared.isIdleTimerDisabled for screen wake lock. |

### Flutter Specific

| ID     | Consideration                                                                |
|--------|------------------------------------------------------------------------------|
| FLT-01 | Use flutter_secure_storage for token storage.                                |
| FLT-02 | Implement workmanager or flutter_background_fetch for background operations. |
| FLT-03 | Use provider or bloc pattern for state management.                           |
| FLT-04 | Consider using shared_preferences for non-sensitive configuration storage.   |
| FLT-05 | Implement wakelock plugin for keeping the screen on.                         |
| FLT-06 | Use cached_network_image for image caching and loading.                      |

## 6. Limitations and Constraints

| ID     | Limitation/Constraint                                                                                        |
|--------|--------------------------------------------------------------------------------------------------------------|
| LIM-01 | Background refresh intervals are limited to a minimum of 15 minutes due to platform constraints.             |
| LIM-02 | E-ink display wake lock may not work consistently across all devices due to aggressive battery optimization. |
| LIM-03 | API token must be manually obtained from the TRMNL dashboard; no in-app registration is supported.           |
| LIM-04 | App does not directly control the content shown; it only mirrors what's configured on the TRMNL platform.    |

This PRD provides a comprehensive guide for implementing the TRMNL Display app on different
platforms while maintaining feature parity with the Android version. Development teams should use
this document to ensure consistent functionality across platforms while adhering to
platform-specific best practices.