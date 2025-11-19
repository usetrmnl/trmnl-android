# App Signing Keystores

This directory contains the keystores used for signing the Android app.

## Debug Keystore

The debug keystore file (`debug.keystore`) is added to the repository to make it easier for developers to build and run
the app without having to generate a new keystore file each time. The debug keystore is used for
signing the app during development.

> [!NOTE]  
> The debug keystore is generated automatically by Android Studio
> and copied from the `$HOME/.android/debug.keystore` location.

## Production Keystore

The production keystore (`trmnl-app-release.keystore`) is used for all release builds and is stored as a base64-encoded secret in GitHub Actions. The keystore is decoded during CI/CD builds.

**Note:** This project uses only build types (debug/release), not product flavors. There is no separate F-Droid flavor. The release build is F-Droid compatible by default.

### Release Build Distribution

- **General Distribution**: Release APK signed with production keystore
- **F-Droid Distribution**: Same release APK (F-Droid verifies the signature during their reproducible build process)

### Important Notes About the Production Keystore

The production keystore has a specific configuration quirk that's important to understand:

- **Store Password**: Used to access the keystore file
- **Key Password**: The keystore was created with a key password, but due to PKCS12 format behavior, both `storePassword` and `keyPassword` are set to the same value in the build configuration
- **Solution**: The Android Gradle Plugin requires both passwords to be explicitly set, so we use the store password for both purposes

This is a known characteristic of certain PKCS12 keystores where explicit key passwords can cause "key associated with alias not a private key" errors, even when the keystore is completely valid.

### Secrets Configuration

The following GitHub Actions secrets are required:
- `KEYSTORE_BASE64`: Base64-encoded production keystore file
- `KEYSTORE_PASSWORD`: Password for accessing the keystore (used for both store and key access)
- `KEY_ALIAS`: Alias of the signing key within the keystore

### CI/CD Workflows Using Production Keystore

- **`android-release.yml`**: Builds and signs release APKs using the production keystore in CI, with debug keystore fallback for local builds

The workflow decodes the keystore from the base64 secret and provides the necessary environment variables for signing.

## Related Resources
- https://developer.android.com/studio/publish/app-signing
- https://source.android.com/docs/security/features/apksigning