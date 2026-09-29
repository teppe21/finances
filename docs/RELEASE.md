# Release & Google Play Publication Guide

## 1. Build Types & Artifact Formats

| Build Type | Artifact Type | Output Path | Purpose |
|---|---|---|---|
| **Debug** | APK | `app/build/outputs/apk/debug/app-debug.apk` | Local testing, logging, and inspection |
| **Release** | APK | `app/build/outputs/apk/release/app-release.apk` | Sideload testing on real hardware |
| **Release** | Android App Bundle (AAB) | `app/build/outputs/bundle/release/app-release.aab` | **Google Play Console distribution** |

---

## 2. Release Signing Configuration (Mandatory for Release Builds)

The Gradle build script (`app/build.gradle.kts`) requires an official signing key for all release tasks (`assembleRelease`, `bundleRelease`). 

**Safety Guarantee**: Release builds **NEVER** silently fall back to debug signing. If signing credentials are missing, the build fails immediately with a clear error:
`"Release signing is not configured. Set FINANCES_KEYSTORE_PATH, FINANCES_KEYSTORE_PASSWORD, FINANCES_KEY_ALIAS and FINANCES_KEY_PASSWORD."`

Debug builds (`assembleDebug`, `test`, `lint`) continue to work out-of-the-box for local development without configuring any signing keys.

### Step 1: Generate an Upload Keystore
Run the following command using Java `keytool`:
```bash
keytool -genkeypair -v \
  -keystore finances-upload-key.jks \
  -alias finances-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Finances Upload, OU=Mobile, O=Finances, L=Budapest, C=HU"
```
> [!IMPORTANT]
> Store `finances-upload-key.jks` in a secure location outside the git repository. Never commit keystores, passwords, or key aliases to version control.

### Step 2: Configure Credentials

#### Option A: Environment Variables (Recommended for CI/CD)
```bash
export FINANCES_KEYSTORE_PATH="/path/to/finances-upload-key.jks"
export FINANCES_KEYSTORE_PASSWORD="your-keystore-password"
export FINANCES_KEY_ALIAS="finances-upload"
export FINANCES_KEY_PASSWORD="your-key-password"
```

#### Option B: Local Properties (Recommended for Local Machine)
Add to `android-native/local.properties` (which is excluded in `.gitignore`):
```properties
FINANCES_KEYSTORE_PATH=C:/path/to/finances-upload-key.jks
FINANCES_KEYSTORE_PASSWORD=your-keystore-password
FINANCES_KEY_ALIAS=finances-upload
FINANCES_KEY_PASSWORD=your-key-password
```

---

## 3. Build Commands

Execute these commands from `android-native/`:

### 1. Run All Unit & Regression Tests
```bash
./gradlew testDebugUnitTest
```

### 2. Build Debug APK (No keystore needed)
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 3. Build Release APK (Signed with upload key)
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### 4. Build Release Android App Bundle (for Google Play Console)
```bash
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

---

## 4. Signing Verification

Verify the generated artifact signatures using Android SDK's `apksigner`:

### Check Release APK
```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```
Expected output:
```text
Verifies: true
Verified using v2 scheme (APK Signature Scheme v2): true
Signer #1 certificate DN: CN=Finances Upload, ...
```

### Check Debug APK
```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
```
Expected output:
```text
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
```

---

## 5. Google Play Publication Preparation Checklist

### Repository-Side Readiness (Completed)
- [x] **Package Name**: `com.teppe21.finances` (clean, non-localized, aligned with GitHub).
- [x] **Target SDK**: Target SDK 36 (Android 16 API level 36) - meets Google Play requirements.
- [x] **Compile SDK**: Compile SDK 36.
- [x] **Build Toolchain**: Android Gradle Plugin 8.7.2 + Gradle 8.9 + Kotlin 1.9.22 + Java 17.
- [x] **R8 & Resource Shrinking**: Fully enabled (`isMinifyEnabled = true`, `isShrinkResources = true`) with verified Proguard rules.
- [x] **Zero Fake Balances**: Clean installations start with `0` balance accounts.
- [x] **Demo Data Isolation**: Dedicated "Load Demo" and "Clear Demo" controls in Settings.
- [x] **Android Auto Backup**: Strict `data_extraction_rules.xml` and `backup_rules.xml` configured.
- [x] **Prominent Disclosure**: In-app modal explaining notification listener privacy.
- [x] **Separate CI Workflows**: Code verification workflow (`android.yml`) and isolated release signing workflow (`release.yml`).

### Remaining Manual Google Play Console Steps
1. **Google Play Developer Account**: Pay the $25 one-time registration fee and complete identity verification.
2. **Google Play App Signing**: Opt-in to Google Play App Signing (Play Console manages the master distribution key; you upload using the upload key).
3. **Public Privacy Policy URL**: Host `docs/PRIVACY.md` publicly (e.g. via GitHub Pages at `https://teppe21.github.io/finances/` or on a personal domain) and enter the URL in the App Content section.
4. **Data Safety Form**:
   - Financial Data: Collected locally, stored on-device, not shared with third parties.
   - Photos / Videos: Temporary CameraX receipt processing on-device, not uploaded.
   - Device Identifiers: None collected.
5. **Financial Features Declaration**: Declare the app as a **Personal Financial Management (PFM)** tool for tracking personal expenses and budgets. (Confirm the app does not provide banking, lending, investment brokerage, or payment transfers).
6. **Store Listing Assets**:
   - App Icon: 512x512 PNG, 32-bit color, max 1MB.
   - Feature Graphic: 1024x500 PNG or JPEG, max 15MB.
   - Phone Screenshots: Minimum 2 screenshots (16:9 or 9:16 aspect ratio, min 1080px).
