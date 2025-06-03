# Debug Keystore

The debug keystore file is added to the repository to make it easier for developers to build and run
the app without having to generate a new keystore file each time. The debug keystore is used for
signing the app during development and [CI builds](https://github.com/usetrmnl/trmnl-android/actions/workflows/android-release.yml), 
which allows early adopters test drive the app. 

This solution is **not intended for production use**, and can't be used to publish the app to the
Google Play Store. If that time ever comes, aside from creating release keystore, there needs to be
a clear strategy for release and maintenance of the keystore file with passcodes.

> [!NOTE]  
> The debug keystore is generated automatically by Android Studio
> and copied from the `$HOME/.android/debug.keystore` location.

## Related Resources
- https://developer.android.com/studio/publish/app-signing
- https://source.android.com/docs/security/features/apksigning