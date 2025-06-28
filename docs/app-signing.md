# App Signing Configuration

This document explains how the app signing process is configured for the TRMNL Display Mirror app.

## Local Development

For local development and debug builds, the app uses the debug keystore located in `keystore/debug.keystore`. This allows all developers to build and test the app easily without needing access to the production signing keys.

## Production Signing

The production signing uses GitHub Actions and secrets for secure key management:

1. The production keystore file is stored as a base64-encoded secret in the GitHub repository
2. The GitHub Actions workflow decodes the keystore during the build process
3. The app's `build.gradle.kts` file detects whether it's running in a CI environment and uses the appropriate signing configuration

## GitHub Secrets

The following secrets are configured in the GitHub repository:

- `KEYSTORE_BASE64`: The base64-encoded keystore file
- `KEYSTORE_PASSWORD`: The keystore password
- `KEY_ALIAS`: The key alias
- `KEY_PASSWORD`: The key password

## How It Works

### During GitHub Actions Builds

1. The workflow decodes the `KEYSTORE_BASE64` secret and saves it as a file
2. Environment variables are set up with the path to the keystore file and the credentials
3. The app's build configuration detects that it's running in CI and uses these environment variables for signing

### During Local Development

The app's build configuration checks if it's running in a CI environment:
- If not, it falls back to the debug keystore for both debug and release builds
- This allows developers to work without needing the production keystore

## F-Droid Builds

The F-Droid build flavor is not signed, as F-Droid handles the signing process with its own keys. This is configured in the app's `build.gradle.kts` file.

## Verifying the Configuration

You can verify that the signing configuration is working correctly by:

1. Checking the GitHub Actions workflow logs to ensure the keystore is being decoded and used
2. Inspecting the APK artifacts to confirm they are properly signed
3. Installing the APK on a device and verifying that it has the correct signature

## Rotating Keys

If you need to rotate the signing keys:

1. Generate a new keystore file
2. Base64-encode the new keystore file: `base64 new_keystore.jks > new_keystore.base64`
3. Update the GitHub secrets with the new values
4. Update the `KEY_ALIAS` and other secrets as needed

**Important**: Changing the signing key will mean users can't upgrade directly and will need to uninstall and reinstall the app, unless you implement key rotation using [Play App Signing](https://developer.android.com/studio/publish/app-signing#app-signing-google-play).
