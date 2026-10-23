# Google Play Console Publishing Design

## Intent

Allow an approved release to be uploaded to Google Play Console without
manually downloading and uploading the AAB. The release must be deliberate:
working on a branch or creating a tag must never publish to Google Play.

## User flow

1. Gloria and Copilot finish a version and agree what should be published.
2. Copilot proposes release notes.
3. Gloria approves the notes.
4. Copilot manually starts the GitHub Actions workflow with the approved
   notes and the target track.
5. The workflow builds the signed release AAB, validates its version metadata,
   uploads it to Google Play, and assigns it to the selected track.

The default track is `alpha`, corresponding to the existing closed test
channel. The workflow may support `internal`, `alpha`, `beta`, and
`production` as explicit choices, but no track is selected implicitly except
the default `alpha`.

## Goals

- Permit a manual, auditable Play Console publication through `workflow_dispatch`.
- Pass approved release notes into the publication request.
- Publish the existing signed release AAB, preserving the repository's
  versionCode and versionName checks.
- Keep Google service-account credentials and signing credentials out of Git.
- Fail before upload when credentials, notes, version metadata, or the bundle
  are invalid.

## Non-goals

- Do not publish automatically on every push, merge, or tag.
- Do not change the app's runtime behavior.
- Do not create or manage Google Play testers, tracks, store listings, or
  production access automatically.
- Do not commit a service-account JSON file or keystore.

## Implementation decisions

- Use a dedicated manually-triggered GitHub Actions workflow rather than
  coupling Play publishing to the existing GitHub APK Release workflow.
- Use the Google Play Developer API through a maintained CLI/action integration
  in CI; the workflow is the operational interface Copilot invokes with `gh`.
- Store the Google service-account JSON as a GitHub Actions secret and write it
  only to the runner's temporary directory.
- Keep the existing four signing secrets for the release keystore.
- Require non-empty release notes and expose the track as a constrained input
  with `alpha` as the default.
- Upload only the AAB produced by `bundleRelease`; do not upload the GitHub
  APK to Play Console.

## Security and failure behavior

- The workflow has only the permissions needed to read the repository contents.
- Secret files are created in runner-temporary storage and are not uploaded as
  artifacts or printed.
- Missing secrets, invalid JSON, unsupported tracks, empty notes, version
  mismatches, and missing AABs fail the workflow before publication.
- Publishing remains a user-approved side effect: no automatic trigger exists.

## Success criteria

- A manual run with approved notes and default `alpha` completes and reports
  the Play Console upload result.
- A manual run with empty notes or missing credentials fails before upload.
- A manual run can target another explicitly selected track.
- The existing APK Release workflow remains tag-triggered and independent.
