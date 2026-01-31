[![Android CI](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml/badge.svg)](https://github.com/usetrmnl/trmnl-android/actions/workflows/android.yml) [![Android Release Build](https://github.com/usetrmnl/trmnl-android/actions/workflows/android-release.yml/badge.svg)](https://github.com/usetrmnl/trmnl-android/actions/workflows/android-release.yml) [![latest-build](https://badgen.net/github/release/usetrmnl/trmnl-android?label=Latest%20Build)](https://github.com/usetrmnl/trmnl-android/releases/latest) [![](https://playbadges.pavi2410.com/badge/downloads?id=ink.trmnl.android&pretty)](https://play.google.com/store/apps/details?id=ink.trmnl.android&pcampaignid=web_share)

# Android - TRMNL
A simple app to show TRMNL's content to your Android devices like Phone, Tablet, e-Ink Display.

## Application Overview

<img src="https://github.com/user-attachments/assets/f708c88d-025b-4fe7-9641-edd1aa4aa789" align="right" width="150">

The TRMNL app serves as a digital display for TRMNL e-ink devices and BYOS installations. The app connects to the TRMNL or BYOS API server, fetches display data, and shows it on Android devices. It can function both as a mirror for existing TRMNL devices or as a standalone TRMNL device connected directly to BYOS or TRMNL (with BYOD add-on) servers.

### Key Features

- [x] Token-based authentication with the TRMNL/BYOS API
- [x] Automatic periodic image refresh from the server
- [x] Adaptive refresh rate based on server response config
- [x] Manual refresh capabilities and option to load next playlist image
- [x] Support for custom server URLs for your BYOS installations
- [x] Refresh history logging for tracking & validation

## 📜 Preconditions
You must have a **valid** `access-token` or `ID` _(MAC Address)_ to access the [screen content](https://docs.trmnl.com/go/private-api/fetch-screen-content) using TRMNL server API.

Here are some of the known ways you can get access to the `access-token`.

1. You must own a [TRMNL](https://trmnl.com/) device with "developer edition" add-on purchased
2. You have purchased their [BYOD](https://docs.trmnl.com/go/diy/byod) product.
3. You have self-serve installation of TRMNL service using [BYOS](https://docs.trmnl.com/go/diy/byos)


### 📱 Google Play Store
Download the app from Google Play Store:

<a href="https://play.google.com/store/apps/details?id=ink.trmnl.android" target="_blank"><img src="https://liquidlabs.ca/img/google-play.svg" height="45"></a>


### Setup
1. Configure the API `access-token` or `ID` _(MAC Address)_ in the app settings
2. Save the token and keep the app always-on with the TRMNL's display image showing.

<img alt="Demo Video" src="https://github.com/user-attachments/assets/2e3fcdef-2681-4c06-9372-2ad98131fb3c" width="500">  

### Limitations 🚧
1. Right now, screen lock using Google's [recommended](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) **`FLAG_KEEP_SCREEN_ON`** is not working on e-Ink tablet due to strict battery optimization. So, if you plan to keep the screen on indefinitely, you should set that in the device settings.
    * On normal Android tablet or device, screen wake lock should work. However it's not recommended to use it without device being always plugged-in 🔌.
2. Currently the app uses Android WorkManager to schedule refresh job and it has minimum interval of ⏰ `15 min` between jobs. So, if your TRMNL is setup to refresh every `5 min`, you will not see it refresh until `15 min` is elapsed.
    * This can be overcome by using some clever logic or not using `WorkManager`. However, this is a OS optimized and reliable way to refresh image periodically.
    * Imagine a user running the app on an Android phone or tablet. When the app is in the background (e.g., the screen is off), it avoids unnecessary image refresh calls, conserving the user's battery. These optimizations are built into `WorkManager`.



## <img src="project-resources/logo/android-logo-head.svg" width="60" alt="android logo"/>Android Development & Contribution Guide
See [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to get started and contribute to the project.

---

## Related References 📖
* https://trmnl.com/
* https://trmnl.com/integrations
* https://github.com/usetrmnl/


### Related App - TRMNL Buddy
<img width="96" height="96" alt="App Icon" src="https://github.com/user-attachments/assets/f5871ce0-786d-4f2f-aa51-1c6b72413bf7" align="right" />

Your companion app to monitor and manage your TRMNL e-ink displays on the go.

**TRMNL Android Buddy** is the essential companion app for managing your [TRMNL](https://trmnl.com) e-ink display devices. Monitor device health, track battery life over time, and stay on top of your displays' status—all from your Android phone.

<a href="https://play.google.com/store/apps/details?id=ink.trmnl.android.buddy&pcampaignid=web_share" target="_blank"><img src="https://liquidlabs.ca/img/google-play.svg" height="45"></a>
