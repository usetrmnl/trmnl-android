# App Signing

## Debug Keystore

The debug keystore file is added to the repository to make it easier for developers to build and run
the app without having to generate a new keystore file each time. The debug keystore is used for
signing the app during local development, allowing all contributors to build and test the app.

## Production Signing

For production builds through CI, the app now uses a proper release signing key stored securely as GitHub secrets.
The production keystore file is not stored in the repository - instead, it's base64-encoded and stored
as a GitHub secret that is only used during the CI build process.

For more detailed information about the app signing configuration, see the [App Signing Documentation](../docs/app-signing.md).

> [!NOTE]  
> The debug keystore is generated automatically by Android Studio
> and copied from the `$HOME/.android/debug.keystore` location.

## Related Resources
- https://developer.android.com/studio/publish/app-signing
- https://source.android.com/docs/security/features/apksigning