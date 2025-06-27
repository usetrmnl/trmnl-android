[![Android CI](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml/badge.svg)](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml) [![Android Release Build](https://github.com/usetrmnl/trmnl-android/actions/workflows/android-release.yml/badge.svg)](https://github.com/usetrmnl/trmnl-android/actions/workflows/android-release.yml) [![latest-build](https://badgen.net/github/release/usetrmnl/trmnl-android?label=Latest%20Build)](https://github.com/usetrmnl/trmnl-android/releases/latest)

# Android - TRMNL Display Mirror 🪞
A simple app to mirror existing TRMNL's content to your Android devices like Phone, Tablet, e-Ink Display.

## Application Overview

The TRMNL Display Mirror app serves as a digital display mirror for TRMNL e-ink devices. The app connects to the TRMNL API, fetches display data, and shows it on Android devices.

### Key Features

- [x] Token-based authentication with the TRMNL/BYOS API
- [x] Automatic periodic image refresh from the server
- [x] Adaptive refresh rate based on server response config
- [x] Manual refresh capabilities and option to load next playlist image
- [x] Support for custom server URLs for your BYOS installations
- [x] Refresh history logging for tracking & validation

## 📜 Preconditions
You must have a **valid** `access-token` or `ID` _(MAC Address)_ to access the [screen content](https://docs.usetrmnl.com/go/private-api/fetch-screen-content) using TRMNL server API.

Here are some of the known ways you can get access to the `access-token`.

1. You must own a [TRMNL](https://usetrmnl.com/) device with "developer edition" add-on purchased
2. You have purchased their [BYOD](https://docs.usetrmnl.com/go/diy/byod) product.
3. You have self-serve installation of TRMNL service using [BYOS](https://docs.usetrmnl.com/go/diy/byos)


## How to try
⬇️ Install the APK on your Android device.
<img src="https://github.com/user-attachments/assets/6ec04cf5-b72c-429a-a435-406f0051d221" align="right" width="150">
1. Configure the API `access-token` or `ID` _(MAC Address)_ in the app settings
2. Save the token and keep the app always-on with the TRMNL's display image showing.

### <img src="https://github.com/user-attachments/assets/64b4b132-a885-4783-98e3-c201bae6ccff" width="25"> Download Release
Check installable APK from Assets in [latest release](https://github.com/usetrmnl/trmnl-android/releases).

### 📱 F-Droid
The app will *soon* be available on F-Droid, providing a free and open source Android app repository.
1. Add the F-Droid repository to your F-Droid client
2. Search for "TRMNL Display Mirror"
3. Install the app

<img alt="Demo Video" src="https://github.com/user-attachments/assets/2e3fcdef-2681-4c06-9372-2ad98131fb3c" width="500">  

### Limitations 🚧
1. Right now, screen lock using Google's [recommended](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) **`FLAG_KEEP_SCREEN_ON`** is not working on e-Ink tablet due to strict battery optimization. So, if you plan to keep the screen on indefinitely, you should set that in the device settings.
    * On normal Android tablet or device, screen wake lock should work. However it's not recommended to use it without device being always plugged-in 🔌.
2. Currently the app uses Android WorkManager to schedule refresh job and it has minimum interval of ⏰ `15 min` between jobs. So, if your TRMNL is setup to refresh every `5 min`, you will not see it refresh until `15 min` is elapsed.
    * This can be overcome by using some clever logic or not using `WorkManager`. However, this is a OS optimized and reliable way to refresh image periodically.
    * Imagine a user running the app on an Android phone or tablet. When the app is in the background (e.g., the screen is off), it avoids unnecessary image refresh calls, conserving the user's battery. These optimizations are built into `WorkManager`.



## <img src="project-resources/logo/android-logo-head.svg" width="60" alt="android logo"/>Android Development & Contribution Guide
See [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to get started and contribute to the project.

### Build Variants
The app supports multiple build variants:
- **Standard**: The default variant with all features
- **F-Droid**: A variant optimized for F-Droid distribution without Google dependencies

To build specific variants:
```bash
# Build the standard release variant
./gradlew buildStandard

# Build the F-Droid release variant
./gradlew buildFDroid

# Build all variants
./gradlew buildAllFlavors
```

### Release Process

For instructions on creating new releases and managing versions across the project, see the [Release Checklist](RELEASE_CHECKLIST.md).

---

## Related References 📖
* https://usetrmnl.com/
* https://usetrmnl.com/integrations
* https://github.com/usetrmnl/
