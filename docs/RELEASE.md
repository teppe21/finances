# Release & Google Play Publication Guide

## 1. Build Types & Artifact Formats

| Build Type | Artifact Type | Output Path | Purpose |
|---|---|---|---|
| **Debug** | APK | `app/build/outputs/apk/debug/app-debug.apk` | Local testing, logging, and inspection |
| **Release** | APK | `app/build/outputs/apk/release/app-release.apk` | Sideload testing on real hardware |
| **Release** | Android App Bundle (AAB) | `app/build/outputs/bundle/release/app-release.aab` | **Google Play Console distribution** |

---

## 2. Release Signing Configuration

The Gradle build script (`app/build.gradle.kts`) dynamically configures release signing. It avoids committing passwords or keystore files into version control by reading credentials from **environment variables** or `local.properties`.

### Environment Variables (Recommended for CI/CD)
```bash
export FINANCES_KEYSTORE_PATH="/path/to/release.keystore"
export FINANCES_KEYSTORE_PASSWORD="your-keystore-password"
export FINANCES_KEY_ALIAS="finances-key-alias"
export FINANCES_KEY_PASSWORD="your-key-password"
```

### Local Properties (Recommended for Local Dev)
In `android-native/local.properties` (which is git-ignored):
```properties
FINANCES_KEYSTORE_PATH=C:\\path\\to\\release.keystore
FINANCES_KEYSTORE_PASSWORD=your-keystore-password
FINANCES_KEY_ALIAS=finances-key-alias
FINANCES_KEY_PASSWORD=your-key-password
```

### Safe Fallback Behavior
If release keystore credentials are not provided or the keystore file does not exist, the release build **automatically falls back to the debug signing configuration**. This ensures that local builds and CI validation (`./gradlew assembleRelease`, `./gradlew bundleRelease`) always compile and succeed without requiring private keys.

### Generating a New Production Keystore
```bash
keytool -genkey -v -keystore finances-release.keystore -alias finances-key -keyalg RSA -keysize 2048 -validity 10000
```

---

## 3. Build Commands

Execute these commands from `android-native/`:

### 1. Run All Unit & Regression Tests
```bash
./gradlew test
```

### 2. Build Debug APK
```bash
./gradlew assembleDebug
```

### 3. Build Release APK (for Direct Sideloading)
```bash
./gradlew assembleRelease
```

### 4. Build Release Android App Bundle (for Google Play Console)
```bash
./gradlew bundleRelease
```

---

## 4. R8 / Proguard Rules

R8 shrinker rules are maintained in `app/proguard-rules.pro`. The rules explicitly keep:
- **Room Database**: Preserves entity classes, DAOs, and the database instance holder.
- **Domain Models**: Preserves data classes and type converters.
- **Google ML Kit**: Keeps on-device OCR model bindings and native code interfaces.
- **AndroidX CameraX**: Retains hardware camera provider references.
- **Kotlin Coroutines**: Retains `MainDispatcherFactory` and exception handlers.
- **Jetpack DataStore**: Retains preferences serialization.

---

## 5. Google Play Publication Checklist

- [x] **Package Name**: Refactored to `com.teppe21.finances` (clean, non-localized, aligned with GitHub).
- [x] **Target SDK**: Target SDK 36 (Android 16 API level 36) - meets and exceeds Google Play target API requirements.
- [x] **Compile SDK**: Compile SDK 35 (Android 15) with `android.suppressUnsupportedCompileSdk=35` configured.
- [x] **Zero Fake Balances**: Clean installations start with `0` balance accounts. Demo data is available only via explicit user action in Settings.
- [x] **Android Auto Backup**: Explicit `data_extraction_rules.xml` and `backup_rules.xml` configured, excluding temporary cache and receipts while preserving database and preferences.
- [x] **Prominent Disclosure**: In-app disclosure dialog displayed prior to requesting notification listener permissions.
- [x] **Data Safety Declaration**:
  - No personal data collected or shared.
  - No financial telemetry uploaded to cloud.
  - Device storage used exclusively for local SQLite.
  - Network access used exclusively for anonymous public ECB FX rates (can be disabled).
- [x] **App Bundle (AAB)**: Verified compilation producing ~29.8 MB optimized `.aab`.
