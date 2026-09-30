# Security & Threat Model

## 1. Core Security Principles

Financial data represents highly sensitive personal information. **Finances** (`com.teppe21.finances`) is architected with a strict privacy-first, zero-telemetry threat model:

1. **Air-Gap Architecture for Core Financials**:
   - All transactions, accounts, account balances, categories, keyword rules, and receipt records are stored **strictly on-device**.
   - No financial data, transaction payloads, merchant names, or bank account identifiers are ever transmitted to any external backend, cloud database, or third-party server.
2. **Zero Credentials Requested or Stored**:
   - The app never prompts for, stores, or transmits bank account numbers, passwords, online banking credentials, or card CVVs.
3. **No Invasive Accessibility Scraping**:
   - The app does **not** employ Android Accessibility Services or webview injection to scrape banking apps. Ingestion occurs strictly through official, user-authorized Android APIs.
4. **Zero Third-Party Telemetry**:
   - The application contains **zero** analytics SDKs (no Firebase Analytics, no Google Analytics, no Facebook SDK, no Mixpanel, no Sentry).

---

## 2. On-Device Storage & Encryption Transparency

### Local SQLite Sandbox
The app stores its structured data in a SQLite database via Android Room (`finances.db`).
- **Storage Location**: The database file resides exclusively within Android's private internal storage:
  `/data/user/0/com.teppe21.finances/databases/finances.db`
- **Sandbox Security**: On Android, each application is assigned a unique Linux UID (User ID). Android's Linux kernel enforces process-level isolation: no other non-root app on the device can read or access files belonging to `com.teppe21.finances`.
- **Operating System Encryption**: Data at rest is encrypted by Android's native File-Based Encryption (FBE) utilizing the device's hardware-backed cryptographic keystore (AES-256-XTS). When the device is locked (Credential Encrypted storage), files cannot be decrypted without the user unlocking the screen.
- **Honest Security Posture**: The SQLite database file itself is not encrypted with a custom SQLCipher layer; it relies on the operating system's hardware-backed disk encryption and application sandbox isolation.

---

## 3. Bank Notification Listener Security & Storage Sanitization (Room v4)

The `BankNotificationListenerService` requires the `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` system permission.

### Data Protection Guardrails:
1. **Targeted Package Whitelist**: The service immediately ignores notifications from non-whitelisted packages. Unknown third-party apps (e.g. messaging, email, browsers) are rejected at the service entry point.
2. **Zero Raw Text Persistence (Room v4)**: Raw notification `title`, `text`, `bigText`, and `subText` are **never saved to disk or SQLite**. Only sanitized structured metadata (amount, currency, merchant name, timestamp, reason code) is stored.
3. **Immediate 2FA Discard**: Any notification containing security tokens, verification codes, or two-factor authentication markers (e.g. `belépési kód`, `SMS kód`, `biztonsági kód`, `jóváhagyás`, `security code`, `verification code`) is **immediately discarded** before processing.
4. **Promotional Filtering**: Marketing campaigns, referral promotions, and cashback alerts from banking apps (e.g. Revolut referral bonuses) are dropped immediately.
5. **Cross-Source Deduplication**: When both Google Wallet and Revolut trigger notifications for the same contactless physical payment within 120 seconds, the engine detects the pair and ignores the redundant alert, preventing double expense counting.
6. **Structured Logcat Hygiene**: Uncaught exceptions or processing errors are captured within safe `try / catch` boundaries. Log statements never print notification text, merchant names, card numbers, or transaction values to `android.util.Log`.

---

## 4. App Lock & Biometrics Architecture

The application provides an interactive application lock barrier:

### 4.1 Salted Cryptographic PIN Storage
- When a 4-digit PIN is configured, the app generates a cryptographically random salt (`UUID.randomUUID().toString().take(16)`).
- The PIN is hashed using **SHA-256** combined with the salt: `SHA-256(salt + ":" + pin)`.
- Only the 64-character hexadecimal digest and salt are stored in Android Jetpack DataStore; the plaintext PIN is never stored in persistent memory.

### 4.2 Biometric Authentication
- Biometric authentication uses official AndroidX `BiometricPrompt` interfacing with Android's `BiometricManager` (`BIOMETRIC_STRONG | BIOMETRIC_WEAK`).
- Biometric templates remain locked in the device's hardware Secure Enclave.
- A PIN code is required as a fallback before biometrics can be activated.

### 4.3 Lock Gate Lifecycle
- On app launch or when the app is resumed after backgrounding (`onStop`), the `MainActivity` displays a full-screen `LockGateScreen` blocking access to navigation, dashboards, and accounts until authentication succeeds.
- Disabling the app lock requires entering the active PIN code.

---

## 5. Camera & On-Device OCR Security

Receipt scanning requires the `android.permission.CAMERA` hardware permission.
- **CameraX Lifecycle**: The camera frame analyzer operates in real time using AndroidX CameraX. Frames are analyzed in memory.
- **On-Device Neural Engine**: Text recognition is performed via Google ML Kit On-Device Text Recognition (`com.google.mlkit:text-recognition`). The OCR model runs locally on the phone's CPU/GPU/NPU without uploading image bitmaps to external cloud vision servers.
- **Receipt Images Storage**: Captured receipt images are saved strictly in the app-private files directory (`context.filesDir/receipt_images/`) and excluded from cloud backups.

---

## 6. Complete Data Deletion (Right to Erasure)

- A dedicated **"Delete All Financial Data"** control in Settings permanently wipes all SQLite tables (`transactions`, `accounts`, `categories`, `category_rules`, `receipts`, `notification_events`, `budgets`, `recurring_rules`) and resets preferences.
- Default clean accounts are re-initialized with 0 balance, leaving zero orphaned personal records on the device.
