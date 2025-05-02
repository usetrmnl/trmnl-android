[![Android CI](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml/badge.svg)](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml)

# Android - TRMNL Display Mirror 🪞
A simple app to mirror existing TRMNL's content to your Android device (preferably e-ink display).

## 📜 Preconditions
You must have a **valid** `access-token` to access the [screen content](https://docs.usetrmnl.com/go/private-api/fetch-screen-content) using TRMNL server API.

Here are some of the known ways you can get access to the `access-token`.

1. You must own a [TRMNL](https://usetrmnl.com/) device with "developer edition" add-on purchased
2. You have purchased their [BYOD](https://docs.usetrmnl.com/go/diy/byod) product.
3. You have self-serve installation of TRMNL service using [BYOS](https://docs.usetrmnl.com/go/diy/byos)


## How to try
⬇️ Install the APK on your Android device.
<img src="https://github.com/user-attachments/assets/6ec04cf5-b72c-429a-a435-406f0051d221" align="right" width="150">
1. Configure the API `access-token` in the app settings  
    i. 📝 NOTE: Right now only `https://trmnl.app/api/` service API is supported, custom service URL support will be added later
2. Save the token and keep the app always-on with the TRMNL's display image showing.

### <img src="https://github.com/user-attachments/assets/64b4b132-a885-4783-98e3-c201bae6ccff" width="25"> Download Release
Check installable APK from Assets in [latest release](https://github.com/usetrmnl/trmnl-android/releases).

<img alt="Demo Video" src="https://github.com/user-attachments/assets/2e98a5b4-fcd5-4aa9-bb57-43a8089919d6" width="500">  

▶️ See [full demo](https://youtu.be/SRSwBphZTvs) 

### Limitations 🚧
1. Right now, screen lock using Google's [recommended](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) **`FLAG_KEEP_SCREEN_ON`** is not working on e-Ink tablet due to strict battery optimization. So, if you plan to keep the screen on indefinitely, you should set that in the device settings.
    * On normal Android tablet or device, screen wake lock should work. However it's not recommended to use it without device being always plugged-in 🔌.
2. Currently the app uses Android WorkManager to schedule refresh job and it has minimum interval of ⏰ `15 min` between jobs. So, if your TRMNL is setup to refresh every `5 min`, you will not see it refresh until `15 min` is elapsed.
    * This can be overcome by using some clever logic or not using `WorkManager`. However, this is a OS optimized and reliable way to refresh image periodically.


## Application Overview

The TRMNL Display Mirror app serves as a digital display mirror for TRMNL e-ink devices. The app connects to the TRMNL API, fetches display data, and shows it on Android devices.

### Key Features

- [x] Token-based authentication with the TRMNL API
- [x] Automatic periodic image refresh from the server
- [x] Adaptive refresh rate based on server response config
- [x] Manual refresh capabilities
- [x] Image caching for offline viewing
- [x] Refresh history logging for tracking & validation


## Android Developer Guide

This guide will help you get started with the TRMNL Display Mirror Android application development.

<details>

<summary>See technical details on the project</summary>

## Prerequisites

- Android Studio Meerkat or latest Android Studio (https://developer.android.com/studio)
- JDK 17+
- Git

### Getting Started

#### Clone the Repository

```bash
git clone https://github.com/usetrmnl/trmnl-android.git
cd trmnl-android
```

#### Open Project in Android Studio

1. Start Android Studio
2. Select "Open an existing project"
3. Navigate to and select the cloned repository folder

#### Build the Project

- Wait for the Gradle sync to complete
- Build the project by selecting **Build > Make Project** or pressing **Ctrl+F9** (Windows/Linux) or **Cmd+F9** (macOS)

#### Run the App

- Connect an Android device or start an emulator
- Click the Run button (green triangle) in the toolbar
- Select your target device and click OK

### Project Structure

The app uses a modern Android architecture with the following components:

- **UI**: Jetpack Compose with Circuit UI architecture
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

---

## Related References 📖
* https://usetrmnl.com/
* https://usetrmnl.com/integrations
* https://github.com/usetrmnl/
