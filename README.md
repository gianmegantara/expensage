# 🌿 ExpenSage Android Companion

ExpenSage Android is a lightweight, ergonomic "Quick Add" utility designed for the fastest possible expense logging. It serves as a native companion to the ExpenSage web platform.

## ✨ Key Features

- ⚡ **Ergonomic Quick Add**: Designed for one-handed use. Essential controls (Save, Settings, Close) are positioned at the bottom of the screen.
- 📦 **Fast-Fail Offline Mode**: If the network is slow or unavailable, expenses are saved locally to the device within 10 seconds.
- 📡 **Automatic Background Sync**: Uses Android `WorkManager` to automatically sync queued expenses when a stable connection returns.
- 🛡️ **Double-Submit Protection**: Implements `X-Request-Id` (Idempotency) to ensure slow connections or retries never create duplicate entries.
- 🎨 **Modern Material 3 UI**: A clean, focused interface utilizing playful emoji iconography and ExpenSage Green branding.
- 🔗 **Deep Link Setup**: Import your configuration instantly via a specialized setup link.

## 📱 User Experience (UX)

- **Keyboard First**: "Next" on the note field moves to the amount; "Done" on the amount field triggers submission.
- **Muscle Memory**: The **❌ Close** button is consistently located in the bottom-left across all screens for a quick exit.
- **Visual Feedback**: Bold, centered inputs for high visibility and clear success/offline status screens.

## ⚙️ Setup & Configuration

The app is configured via a deep link containing your endpoint and credentials:

```text
https://expensage.online/android/setup?endpointUrl=https%3A%2F%2F...&accessKey=etk_xxx
```

1. **Automatic**: Click the link from any app (WhatsApp, Email, Browser) to open ExpenSage Android.
2. **Manual**: Paste the link into the "Import Setup" screen if deep linking is not configured on your domain.

## 🛠️ Technical Architecture

- **Language**: Kotlin + Jetpack Compose
- **State Management**: `MainViewModel` with `StateFlow`
- **Network**: `OkHttp` with a 10-second fast-timeout policy.
- **Persistence**: `SharedPreferences` for config and a local JSON-based queue for offline expenses.
- **Background Tasks**: `WorkManager` for reliable background synchronization.

### 🛡️ Backend Requirements (Idempotency)
To fully support the app's duplicate protection, your backend should:
1. Extract the `X-Request-Id` (UUID) from the request headers.
2. Check if this ID has been processed in the last 24-48 hours.
3. If processed, return the cached success response instead of creating a new entry.

## 🚀 Building & Deployment

### Debug Build
```bash
./gradlew :app:assembleDebug
```

### Optimized Release Build
To build the smallest possible APK (~4MB) with R8 minification:
```bash
./gradlew :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## 🧪 Testing
Unit tests for the offline queuing logic are located in:
`app/src/test/java/online/expensage/android/data/PendingExpenseStoreTest.kt`
