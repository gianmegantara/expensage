# F-Droid

Notes and requirements for publishing ExpenSage on F-Droid.

## Requirements checklist

- [x] **Public source** (this repository, GPL-3.0).
- [x] **Free license** — `LICENSE` (GPL-3.0). Also applies to bundled artwork.
- [x] **No proprietary dependencies** — no Google Play Services / Firebase / proprietary
      crash or analytics SDKs. Libraries used: Jetpack Compose, Material 3, Room, WorkManager,
      OkHttp, kotlinx.serialization (all FOSS).
- [x] **Buildable with the F-Droid build server** — Gradle + Android SDK, standard
      `./gradlew assembleRelease`.
- [x] **No unused/sensitive permissions** — `SYSTEM_ALERT_WINDOW` removed; only
      `INTERNET` and `POST_NOTIFICATIONS` are declared by the app.
- [x] **No cleartext traffic in release** — `usesCleartextTraffic` is debug-only
      (`app/src/debug/AndroidManifest.xml`); release requires HTTPS.
- [x] **Backups disabled** — `allowBackup="false"` (the access key is stored in
      EncryptedSharedPreferences, which can't be restored across devices anyway).
- [x] **Release is unsigned** by default, so a builder (F-Droid) can sign it. Local
      release signing is opt-in via `keystore.properties` (not committed).
- [ ] **Tagged release** matching `versionCode`/`versionName` (see below).
- [ ] **Metadata** in `fastlane/metadata/android/en-US/` (title, descriptions, icon,
      changelogs).
- [ ] **Build recipe** submitted to [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata)
      (draft in `fdroid/online.expensage.android.yml`).

## Versioning / tags

- One Git tag per release: `v<versionName>` (e.g. `v1.0.2`).
- Always bump `versionCode` (integer, must increase) in `app/build.gradle.kts`.
- F-Droid's `UpdateCheckMode: Tags` picks up new tags automatically.

## Submitting to F-Droid

1. Push a tagged release (e.g. `git tag v1.0.2 && git push origin v1.0.2`).
2. Fork [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata), copy
   `fdroid/online.expensage.android.yml` to `metadata/online.expensage.android.yml`.
3. Open a merge request. The F-Droid team builds from source and reviews it.
4. F-Droid signs the APK with **its own key** by default. To keep your own signature,
   enable [reproducible builds](https://f-droid.org/docs/Reproducible_Builds/).

## Anti-features to expect

- **Network** — the app is a client for your ExpenSage server.
- (`NonFreeNet` / `Tracking` do **not** apply: the backend is yours and there are no trackers.)

## Signing

Release builds are **unsigned** unless `android/keystore.properties` exists:

```properties
storeFile=/absolute/path/release.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

`keystore.properties`, `*.jks`, and `*.keystore` are gitignored. F-Droid ignores local
signing and signs with its own key.

## Alternative: self-hosted F-Droid repo

If you don't want to (or can't yet) meet the catalog requirements, you can run your own
repo with [`fdroidserver`](https://f-droid.org/docs/Setup_an_F-Droid_App_Repo/) and
publish your signed APK — users add your repo URL. Or ship the APK as a GitHub/Gitea
release and install via [Obtainium](https://github.com/ImranR98/Obtainium).
