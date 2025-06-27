# Building for F-Droid

This document outlines the steps required to build the TRMNL Display Mirror app for F-Droid submission.

## F-Droid Specific Build Configuration

The app includes specific configurations for F-Droid compatibility:

1. A dedicated `fdroid` product flavor that excludes Google Fonts
2. A specific `fdroidRelease` build type 
3. System fonts are used instead of Google Fonts for the F-Droid version
4. The F-Droid build is **not signed** (as per [PR #106](https://github.com/usetrmnl/trmnl-android/pull/106)) - F-Droid handles the signing process

## Building the F-Droid Version

To build the F-Droid version locally:

```bash
./gradlew assembleFdroidRelease
```

This will generate an unsigned APK in `app/build/outputs/apk/fdroid/release/` that is suitable for F-Droid submission. Unlike the standard release build, the F-Droid build variant does not have a signing configuration, as F-Droid's build system will handle the signing process.

Alternatively, you can use the convenience task:

```bash
./gradlew buildFDroid
```

## F-Droid Metadata

The F-Droid specific metadata is located in:
- `fastlane/metadata/android/en-US/` - Contains descriptions, changelogs and screenshots
- `metadata/` - Contains F-Droid specific YAML configuration

## Dependencies

The F-Droid build variant avoids the following proprietary dependencies:
- Google Fonts (replaced with system fonts)

## Testing the F-Droid Build

After building the F-Droid variant, verify that:
1. The app installs and runs correctly
2. All functionality works without Google dependencies
3. The UI appears correctly with system fonts

## Submitting to F-Droid

The app can be submitted to F-Droid through:

1. The [F-Droid submission form](https://gitlab.com/fdroid/fdroiddata/-/issues/new?issuable_template=App_Submission)
2. A merge request to the F-Droid data repository

Ensure the following are available:
- Source code is publicly accessible on GitHub: https://github.com/usetrmnl/trmnl-android
- The app is licensed under the MIT license
- F-Droid metadata is complete
- The app builds successfully with the F-Droid build system

Note that F-Droid will build and sign the app themselves using their own signing key. Our build process intentionally does not include a signing configuration for the F-Droid variant (as implemented in [PR #106](https://github.com/usetrmnl/trmnl-android/pull/106)).
