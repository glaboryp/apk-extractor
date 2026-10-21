# apk-extractor

Android app (Kotlin + Jetpack Compose) with no ads that:

- lists installed apps and extracts their APK (single APK, or a `.apks` zip when the app has splits) to Downloads, with progress and cancel;
- shares the result through the Android share sheet;
- installs `.apk`, `.apks`, `.xapk` and `.zip` packages, including split bundles, through `PackageInstaller`.

## Build

Requires Android Studio (or the Android SDK with platform 37) and JDK 17+.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # needs a device or emulator
```

## Release signing

Create a keystore outside the repo and a `keystore.properties` file in the project root (git-ignored):

```
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run `./gradlew assembleRelease`. Without that file the release APK is built unsigned.
