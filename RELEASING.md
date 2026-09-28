# Releasing

## One-time: create a signing keystore

```bash
keytool -genkey -v -keystore release.keystore -alias expensage \
  -keyalg RSA -keysize 2048 -validity 10000
```

**Keep `release.keystore` private and backed up.** If you lose it you can never update
installs of the app. Never commit it (`*.keystore`/`*.jks`/`keystore.properties` are gitignored).

## Local signed build

Create `keystore.properties` at the repo root:

```properties
storeFile=/absolute/path/release.keystore
storePassword=…
keyAlias=expensage
keyPassword=…
```

Then:

```bash
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

## Release via GitHub Actions

`.github/workflows/release.yml` builds the signed APK and attaches it to a GitHub
Release whenever you push a `v*` tag. Add these repository secrets
(Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `expensage` |
| `ANDROID_KEY_PASSWORD` | key password |

Generate the base64 value with:

```bash
base64 -w0 release.keystore
```

## Cut a release

1. Bump `versionCode` (integer, must increase) and `versionName` in `app/build.gradle.kts`.
2. Commit, then tag and push:

```bash
git tag v1.0.3
git push origin v1.0.3
```

The workflow builds `app-release.apk` and creates the release. Users can install from
the release page, or set up auto-updates with [Obtainium](https://github.com/ImranR98/Obtainium)
by pointing it at this repo's releases.

## F-Droid (separate channel)

F-Droid builds from source and signs with **its own** key — so the F-Droid APK has a
different signature than your GitHub releases. Users can't switch channels without
uninstalling. See `FDROID.md`.

## Website link (optional)

Point the web app's `ANDROID_APK_URL` (and `ANDROID_APK_VERSION` / `ANDROID_APK_SIZE`)
at the release asset URL so the `/android` page serves the latest build.
