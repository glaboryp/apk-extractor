# APK Extractor — Design

## Intent and why

Gloria wants her own Android app, free of ads, to extract the APK of any app installed on her phone so she can move it to another phone, send it to her PC, or share it. She tried "APK Extractor" from the Play Store (it worked, but is full of ads) while trying to extract the APK of a game.

This is also a learning project: Gloria is an experienced web/backend developer (Laravel, Node.js, Python) with no native mobile experience. She wants to learn native Android development. The builder writes the code and explains each part as it goes (she does not want to write it herself).

## Goals

1. List installed apps (name, icon, searchable).
2. Extract the APK of a chosen app.
   - Single-APK apps: copy the file as-is.
   - Apps with split APKs (base + config/resource splits, typical of games and modern apps): bundle all splits into one archive (`.apks` or `.zip`).
3. Share the result via the Android share sheet (WhatsApp, email, Bluetooth, Drive...) or save it to device storage (e.g. Downloads) so it can be moved to a PC.
4. Install APKs on the device, including split-APK bundles, via Android's `PackageInstaller` session API (single APKs can be installed by any phone anyway; the value is the multi-split case).
5. No ads, no tracking, no accounts.
6. Aim to publish on Google Play (free or cheap) as a final attempt.

## Non-goals

- Backing up app data (saves, settings).
- Root-only features.
- Monetisation.

## Anti-goals

- Adding ads or analytics.
- Wrapping everything in a cross-platform framework when the app is almost entirely Android system APIs.

## Decided by Gloria

- Native Android app. Developer environment: Android Studio already installed.
- Phased delivery, with the installer last: list apps → extract a simple APK → handle splits → share/save → install apps (including splits).
- Play Store publication is wanted, but research on Google Play policy (restricted permissions `QUERY_ALL_PACKAGES` and `REQUEST_INSTALL_PACKAGES`, new personal-account testing requirement, one-time developer fee) is deferred until that phase. Do not research it earlier.

## Answered by Gloria (builder check)

- Target phone runs Android 17. Target that API level; scoped storage (`MediaStore` for saving to Downloads), `FileProvider` for sharing, and package-visibility rules all apply. Verify current Android 17 restrictions on non-store installs and on `PackageInstaller` when planning.
- The installer must also accept packages from other sources (`.apk`, `.apks`, `.xapk`, `.zip` from tools like SAI or bundletool, or downloaded), not only this app's own output. Entry via the system file picker; "open with" intent filters for these types are a reasonable addition.

## Builder's calls (not yet confirmed by Gloria)

- Language and UI toolkit: Kotlin + Jetpack Compose.
- UI language: Spanish (assumed, mirroring Gloria's working language).
- Distribution until Play Store: install the debug/release APK directly on her own phone.
- Scope of the UI: minimum. App list with search, a per-app action (extract, then share/save), and an install entry point. No history, no batch selection, no settings screen.
- System apps are hidden by default, with a toggle to show them. Apps that expose no readable path or whose splits cannot be fully collected show a clear warning instead of producing a silent partial archive.
- Large files (games can be multi-GB): extraction runs in the background with a progress indicator and cancellation.
- Minimum Android version and exact architecture (single module, ViewModel + simple state): left to the builder, favouring the simplest approach that is idiomatic for Compose.

## Constraints and known risks

- Reading other apps' APK paths needs the package-visibility permission; Play Store may reject it. Design so the permission handling can be swapped later.
- Installing requires the user to grant "install unknown apps" to this app.
- Apps installed as split APKs cannot be reinstalled from the base APK alone; this is why bundling and the installer both exist.
- Extracting paid apps/games raises piracy concerns; this is a personal-use tool. Keep this in mind for Play Store policy.

## Left to the builder

- Project structure, package name, app name and icon.
- Library choices (prefer the Android/Jetpack standard libraries).
- Archive format details for the extractor's output (`.apks` vs `.zip`), as long as the installer can read it. The installer itself must also read `.xapk`/`.apks`/`.zip` from other tools.
- Testing approach appropriate to a learning project.
