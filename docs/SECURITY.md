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

## 3. Network Communication Transparency

The application requires the standard Android `INTERNET` and `ACCESS_NETWORK_STATE` permissions for exactly **one optional feature**: fetching daily foreign exchange (FX) reference rates.

| Endpoint | Protocol | Payload Sent | Data Received |
|---|---|---|---|
| `https://api.frankfurter.app/latest?from=EUR` | HTTPS (TLS 1.3) | None (anonymous HTTP GET) | Public ECB reference exchange rates (EUR, USD, HUF, GBP, CHF) |

### Strict Privacy Guarantee on Network Calls:
- **No Identifiers**: Requests contain no device IDs, no IP tracking headers, no user tokens, and no account details.
- **No Financial Data**: The request queries general market rates against 1 EUR; it never transmits local transaction amounts or user currency selections.
- **Offline & Air-Gap Mode**: If the user disables "Auto-sync Exchange Rates" in Settings, the app operates 100% offline, relying on cached rates in Room SQLite or user-defined manual rates.

---

## 4. Bank Notification Listener Security

The `BankNotificationListenerService` requires the `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` system permission.

### Data Protection Guardrails:
1. **Targeted Package Filtering**: The service immediately ignores notifications from non-banking packages.
2. **Immediate 2FA Discard**: Any notification containing security tokens, verification codes, or two-factor authentication markers (e.g. `belépési kód`, `SMS kód`, `biztonsági kód`, `jóváhagyás`, `security code`) is **immediately discarded** and never stored.
3. **Structured Logcat Hygiene**: Uncaught exceptions or processing errors are captured within safe `try / catch` boundaries. Log statements never print notification text, merchant names, card numbers, or transaction values to `android.util.Log`.
4. **Prominent In-App Disclosure**: Prior to directing the user to system settings, the app displays a prominent modal disclosure explaining that only supported banking apps are monitored and that all parsing occurs in device RAM.

---

## 5. Camera & On-Device OCR Security

Receipt scanning requires the `android.permission.CAMERA` hardware permission.
- **CameraX Lifecycle**: The camera frame analyzer operates in real time using AndroidX CameraX. Frames are analyzed in memory.
- **On-Device Neural Engine**: Text recognition is performed via Google ML Kit On-Device Text Recognition (`com.google.mlkit:text-recognition`). The OCR model runs locally on the phone's CPU/GPU/NPU without uploading image bitmaps to external cloud vision servers.
- **Receipt Images Storage**: Captured receipt images are saved strictly in the app-private files directory (`context.filesDir/receipt_images/`) and excluded from cloud backups.

---

## 6. App Lock & Biometrics

The application provides an optional application lock barrier:
- **Biometrics**: Uses `androidx.biometric:BiometricPrompt`, interfacing directly with the Android BiometricManager and hardware-backed keystore.
- **PIN Lock**: If a numeric PIN is configured, the app stores a SHA-256 hash combined with an application-level salt in Jetpack DataStore; the plaintext PIN is never stored in persistent memory.
