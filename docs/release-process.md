# Release process

This project has two independent release paths.

## GitHub APK download

Update `versionName` and `versionCode`, commit the changes, and push a matching
tag such as `v0.1.4`. The `Release APK` workflow builds the signed APK and
attaches it to a GitHub Release. This path is for direct APK downloads.

## Google Play Console

Google Play publication is always manual and approved:

1. Finish and test the version.
2. Propose release notes in Spanish to Gloria.
3. Wait for Gloria to approve the exact notes.
4. Confirm the track. The default is the existing closed test `alpha`.
5. From `main`, open **Actions → Publish to Google Play → Run workflow**.
6. Select `alpha` unless Gloria explicitly chooses `internal`, `beta`, or
   `production`, then paste the approved notes.
7. Check the workflow result in GitHub Actions and Play Console.

The workflow builds and uploads only the signed Android App Bundle. It does
not create testers, create channels, change store listings, or publish
automatically. Routine closed-test updates use the existing Alpha channel.

Before publishing, increment `versionCode`; keep `versionName` aligned with
the user-facing release. The workflow rejects unsupported tracks, empty notes,
missing artifacts, missing credentials, and version conflicts.

## Required GitHub secrets

Configure these in **Settings → Secrets and variables → Actions**:

- `ANDROID_PUBLISHER_CREDENTIALS`: the Google service-account JSON contents;
- `ANDROID_KEYSTORE_BASE64`: base64-encoded release keystore;
- `ANDROID_KEYSTORE_PASSWORD`: keystore password;
- `ANDROID_KEY_ALIAS`: upload-key alias;
- `ANDROID_KEY_PASSWORD`: upload-key password.

Never commit the JSON key, keystore, passwords, or `keystore.properties`.
GitHub APK Releases and Google Play use separate workflows; do not upload the
GitHub APK to Play Console.
