# Agent release instructions

## Google Play releases

Never publish to Google Play automatically after a commit, merge, or tag.
When a version is ready:

1. Check the current `versionName` and `versionCode`.
2. Propose Spanish release notes to Gloria.
3. Wait for Gloria's explicit approval of those notes.
4. Confirm the target track; use closed testing `alpha` by default.
5. Dispatch `.github/workflows/publish-play.yml` manually from `main`.

Use `internal`, `alpha`, `beta`, or `production` only when Gloria explicitly
chooses the track. Routine updates go into the existing Alpha channel; do not
create a new testing channel. Increase `versionCode` for every Play upload.

The workflow accepts `release_notes` as plain Spanish text, limited to 500
characters, and publishes the signed `bundleRelease` AAB. It is separate from
the tag-triggered GitHub APK Release workflow.

## Credentials

Never ask Gloria to paste credentials into chat and never commit them. GitHub
Actions stores these repository secrets:

- `ANDROID_PUBLISHER_CREDENTIALS`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

If the Play workflow fails for authentication, report the failure and ask
Gloria to check the Google Play Developer API, service-account access to
`dev.gloria.apkextractor`, and the repository secrets.
