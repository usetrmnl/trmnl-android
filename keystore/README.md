# Debug Keystore

The debug keystore file is added to the repository to make it easier for developers to build and run
the app without having to generate a new keystore file each time. The debug keystore is used for
signing the app during development and CI builds, and it is **not intended for production use**.

> [!NOTE]  
> The debug keystore is generated automatically by Android Studio
> and copied from the `$HOME/.android/debug.keystore` location.

## Related Resources
- https://developer.android.com/studio/publish/app-signing
- https://source.android.com/docs/security/features/apksigning