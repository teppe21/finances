# Privacy Policy & Air-Gap Principles

**Last Updated:** September 30, 2026  
**Application:** Finances (`com.teppe21.finances`)  
**Public Policy URL:** [https://teppe21.github.io/finances/privacy-policy.html](https://teppe21.github.io/finances/privacy-policy.html)

---

## 1. Summary: 100% Privacy by Design

**Finances** is developed with an uncompromising commitment to privacy. The application is built as an **offline-first, zero-telemetry personal finance system**.

- **No User Accounts:** You never create an account, log in, or provide an email address.
- **No Remote Database:** Your financial data lives exclusively in local SQLite storage on your physical device.
- **Zero Third-Party Trackers:** The application contains no analytics SDKs, advertising libraries, crash-tracking aggregators, or telemetry tools.
- **No Credentials Stored:** The application never asks for, stores, or transmits your bank login passwords, account numbers, PINs, or card CVVs.

---

## 2. Notification Privacy & Storage Hardening (Room v4)

### 2.1 Explicit Whitelist Scope
The `NotificationListenerService` processes notifications exclusively from recognized banking and payment applications:
- **Revolut** (`com.revolut.revolut`)
- **Google Wallet / Google Pay** (`com.google.android.apps.walletnfcrel`, `com.google.android.apps.nbu.paisa.user`)
- **OTP Bank** (`hu.otpbank.smartbank`, `hu.otpbank.mobilbank`, `hu.otpbank.simple`)
- **Erste George** (`hu.erstebank.george.hungary`, `hu.erstebank.mobilebank`)
- **MBH Bank** (`hu.takarek.mobilbank`, `hu.mbhbank.app`)
- **Wise** (`com.transferwise.android`)

Any notification from an unlisted application (e.g. WhatsApp, Messenger, SMS, games) is ignored and discarded immediately.

### 2.2 Zero Raw Notification Persistence
Starting with database migration **v3 &rarr; v4**, the application enforces a strict zero-text policy in SQLite:
- Raw notification `title`, `text`, `bigText`, and `subText` are **never stored** on disk or in the database.
- The `notification_events` table stores only sanitized structured metadata (`id`, `packageName`, `postedAt`, `sourceBank`, `processed`, `parseStatus`, `transactionId`, `fingerprint`, `reasonCode`).
- Diagnostic status codes (e.g., `PARSED_SUCCESS`, `IGNORED_PROMOTION_OR_NON_TRANSACTION`, `DUPLICATE_CROSS_SOURCE_PAYMENT`) provide complete transparency without exposing personal notification contents.

### 2.3 Automated 2FA/OTP Discard & Promo Filtering
- **Two-Factor Authentication (2FA/OTP):** Notifications containing security verification codes, one-time passwords, or login approvals are discarded immediately before parsing.
- **Promotional & Referral Alerts:** Marketing campaigns, referral invitations (e.g. *"Get HUF22,500 for each eligible friend you refer"*), cashback bonuses, and feature announcements are automatically detected and dropped.

---

## 3. Permissions & How They Are Used

### 1. Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose:** Enables real-time purchase detection from supported banking and wallet applications.
- **Revocability:** Can be granted or revoked at any time in Android *Special app access &rarr; Notification access*.

### 2. Camera (`CAMERA`)
- **Purpose:** Allows photographing physical paper receipts for on-device OCR expense logging.
- **On-Device OCR:** Image frames are processed locally by Google ML Kit's neural text recognition model running directly on device hardware. Zero cloud upload.

### 3. Biometric Authentication (`USE_BIOMETRIC`)
- **Purpose:** Secures entry into the application using your phone's fingerprint sensor or facial recognition.
- **Security:** Handled by Android's native `BiometricPrompt` framework; biometric templates remain locked in your device's hardware Secure Enclave.

---

## 4. Network Communication & Exchange Rates

The application requests standard Android `INTERNET` permission for exactly **one optional feature**: syncing European Central Bank (ECB) daily currency exchange rates via the public, free Frankfurter API (`https://api.frankfurter.app`).

### Privacy Guarantee:
- The network call is an anonymous, parameter-free HTTP GET request for general market rates.
- No personal information, device identifiers, account names, or transaction values are ever transmitted.
- **Complete Air-Gap Toggle:** If you do not want the app to make network calls, toggle **Auto-sync Exchange Rates** OFF in *Settings &rarr; Currency*. The app will operate completely offline using cached or user-entered manual rates.

---

## 5. Android Auto Backup Configuration

To protect your financial data while ensuring seamless device upgrades, the app configures explicit backup rules (`data_extraction_rules.xml` and `backup_rules.xml`):

### Included in Android Backups:
- `database/`: Your local Room SQLite database (`finances.db`), containing accounts, transactions, and categories.
- `sharedpref/`: Your user preferences (theme, selected currency, app lock configuration).

### Strictly Excluded from Cloud Backups:
- `cache/`: All temporary operating system and application cache.
- `code_cache/`: Compiled runtime artifacts.
- `receipt_images/`: Raw camera receipt photos.

---

## 6. Complete Data Deletion (Right to Erasure)

Because all data resides strictly on your physical phone:
- You retain complete ownership and control over your financial records.
- **Delete All Financial Data:** Under Settings &rarr; Data Management, a dedicated one-click action permanently wipes all transactions, accounts, rules, receipts, and notification history from SQLite and resets accounts to a clean 0 balance.
- **App Uninstallation:** Uninstalling the application or clearing app data in Android system settings permanently wipes all data from the device.
