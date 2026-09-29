# Privacy Policy & Air-Gap Principles

**Last Updated:** September 2026  
**Application:** Finances (`com.teppe21.finances`)

---

## 1. Summary: 100% Privacy by Design

**Finances** is developed with an uncompromising commitment to privacy. The application is built as an **offline-first, zero-telemetry personal finance system**.

- **No User Accounts**: You never create an account, log in, or provide an email address.
- **No Remote Database**: Your financial data lives exclusively in local SQLite storage on your physical device.
- **Zero Third-Party Trackers**: The application contains no analytics SDKs, advertising libraries, crash-tracking aggregators, or telemetry tools.
- **No Credentials Stored**: The application never asks for, stores, or transmits your bank login passwords, account numbers, PINs, or card CVVs.

---

## 2. Permissions & How They Are Used

### 1. Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`)
- **Purpose**: Enables real-time purchase detection from supported banking applications (OTP Bank, Revolut, Erste George, MBH Bank, Wise).
- **On-Device Scope**: Notifications are intercepted and parsed strictly in volatile device memory.
- **Security Codes Discarded**: Any notification containing security verification codes, two-factor authentication tokens (2FA), or personal messages is immediately discarded and never saved.
- **Revocability**: You can grant or revoke this access at any time in Android *Special app access $\rightarrow$ Notification access*.

### 2. Camera (`CAMERA`)
- **Purpose**: Allows you to photograph physical paper receipts for on-device OCR expense logging.
- **On-Device OCR**: Image frames are processed locally by Google ML Kit's neural text recognition model running on your device hardware.
- **Zero Cloud Upload**: Receipt photos and OCR text are never transmitted over the internet.

### 3. Biometric Authentication (`USE_BIOMETRIC`)
- **Purpose**: Secures entry into the application using your phone's fingerprint sensor or facial recognition.
- **Security**: Handled by Android's native `BiometricPrompt` framework; your biometric templates remain locked in your device's hardware Secure Element / Keystore.

---

## 3. Network Communication & Exchange Rates

The application requests standard Android `INTERNET` permission for exactly **one optional feature**: syncing European Central Bank (ECB) daily currency exchange rates via the public, free Frankfurter API (`https://api.frankfurter.app`).

### Privacy Guarantee:
- The network call is an anonymous, parameter-free HTTP GET request for general market rates.
- No personal information, device identifiers, account names, or transaction values are ever transmitted.
- **Complete Air-Gap Toggle**: If you do not want the app to make network calls, toggle **Auto-sync Exchange Rates** OFF in *Settings $\rightarrow$ Default Currency*. The app will operate completely offline using cached or user-entered manual rates.

---

## 4. Android Auto Backup Configuration

To protect your financial data while ensuring seamless device upgrades, the app configures explicit backup rules (`data_extraction_rules.xml` and `backup_rules.xml`):

### Included in Android Backups:
- `database/`: Your local Room SQLite database (`finances.db`), containing accounts, transactions, and categories.
- `sharedpref/`: Your user preferences (theme, selected currency, app lock configuration).

### Strictly Excluded from Cloud Backups:
- `cache/`: All temporary operating system and application cache.
- `code_cache/`: Compiled runtime artifacts.
- `receipt_images/`: Raw camera receipt photos.

---

## 5. Data Retention & Deletion

Because all data resides strictly on your physical phone:
- You retain complete ownership and control over your financial records.
- Deleting a transaction or account permanently erases it from your local SQLite database.
- Uninstalling the application or clearing app data in Android system settings permanently wipes all data from the device.
