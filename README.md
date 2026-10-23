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

## GitHub Releases

Push a version tag such as `v0.1.4` to start the **Release APK** workflow. It
builds the signed release APK and attaches it to a GitHub Release as
`APK Extractor-v0.1.4.apk`.

Before tagging, update both `versionName` and `versionCode` in
`app/build.gradle.kts`. The workflow requires the tag version to match
`versionName` and rejects a `versionCode` that is not greater than the
previous tagged release.

The workflow requires these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: the base64-encoded release keystore;
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password;
- `ANDROID_KEY_ALIAS`: the upload-key alias;
- `ANDROID_KEY_PASSWORD`: the upload-key password.

The keystore and passwords must be configured in GitHub repository settings,
never committed to the repository. An APK downloaded from GitHub Releases may
not update an installation obtained from Google Play, or vice versa, when the
signing keys differ.
