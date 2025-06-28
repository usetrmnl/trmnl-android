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

The production keystore (`trmnl-app-release.keystore`) is used for release builds and is stored as a base64-encoded secret in GitHub Actions. The keystore is decoded during CI/CD builds.

### Important Notes About the Production Keystore

The production keystore has a specific configuration quirk that's important to understand:

- **Store Password**: Used to access the keystore file
- **Key Password**: The keystore was created with a key password, but due to PKCS12 format behavior, the private key is only accessible when jarsigner uses the store password for both store and key access
- **Solution**: The `keyPassword` parameter is intentionally omitted from the Android build configuration, allowing the Android build system to use the store password for both purposes

This is a known characteristic of certain PKCS12 keystores where explicit key passwords can cause "key associated with alias not a private key" errors, even when the keystore is completely valid.

### Secrets Configuration

The following GitHub Actions secrets are required:
- `KEYSTORE_BASE64`: Base64-encoded production keystore file
- `KEYSTORE_PASSWORD`: Password for accessing the keystore
- `KEY_ALIAS`: Alias of the signing key within the keystore

Note that `KEY_PASSWORD` is not used in the build configuration due to the keystore behavior described above.

## Related Resources
- https://developer.android.com/studio/publish/app-signing
- https://source.android.com/docs/security/features/apksigning