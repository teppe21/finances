# Google Play Console Compliance & Policy Reference

This document outlines the policy declarations, Data Safety responses, and sensitive permission justifications for publishing **Finances** (`com.teppe21.finances`) on the Google Play Store.

---

## 1. Google Play Data Safety Form Responses

### 1.1 Data Collection & Sharing Overview
- **Does your app collect or share any of the required user data types?**  
  &rarr; **NO.** The app operates entirely on a local-first, on-device architecture. No user data, financial data, or telemetry is collected, transmitted, or shared with third parties.
- **Is all user data collected by your app encrypted in transit?**  
  &rarr; **Yes.** While the app does not transmit user data, any public exchange rate queries use standard HTTPS.
- **Do you provide a way for users to request that their data be deleted?**  
  &rarr; **Yes.** The app provides a direct "Delete All Financial Data" feature in Settings that permanently erases all on-device records. Additionally, uninstalling the app deletes all application storage.

---

## 2. Sensitive Permissions & Special App Access Declarations

### 2.1 `BIND_NOTIFICATION_LISTENER_SERVICE` (Special App Access)
- **Declaration Category:** Financial Management / Expense Automation.
- **Core Purpose Justification:**  
  *Finances requires Notification Access to allow users to automatically record physical card purchases and bank transfers as they occur in real time, eliminating the burden of manual transaction entry.*
- **Scope Limitation & Safety Guarantees:**
  1. The service only parses notifications from explicitly whitelisted financial applications (Revolut, Google Wallet, OTP Bank, Erste George, MBH Bank, Wise).
  2. Notifications from personal messaging apps (WhatsApp, Messenger, SMS), emails, and other apps are ignored and discarded immediately.
  3. **Zero Raw Text Persistence:** Raw notification titles and message text are **never stored** in SQLite. Only structured transaction metadata (amount, currency, merchant name, timestamp) is recorded locally.
  4. Security tokens, one-time passwords (2FA/OTP), and login codes are actively discarded before any processing.
  5. Promotional/referral notifications are filtered out automatically.
  6. **100% On-Device:** All parsing logic runs completely on the device with zero cloud processing or remote logging.

### 2.2 `CAMERA`
- **Core Purpose Justification:**  
  *Used exclusively for the on-device receipt scanning feature (`ScanReceiptScreen`), allowing users to photograph paper purchase receipts.*
- **Safety Guarantees:**
  1. Images are processed locally using Google ML Kit On-Device Text Recognition.
  2. No image bytes or extracted text snippets are ever uploaded to remote servers.
  3. Camera access is requested only at runtime when the user initiates a receipt scan.

### 2.3 `USE_BIOMETRIC`
- **Core Purpose Justification:**  
  *Used to provide optional hardware-level app lock protection (`BiometricPrompt`), allowing users to lock their personal financial records with fingerprint or face recognition.*
- **Safety Guarantees:**
  1. Handled by the official AndroidX Biometric library.
  2. Biometric credentials never leave the device's hardware Secure Enclave.

### 2.4 `INTERNET` & `ACCESS_NETWORK_STATE`
- **Core Purpose Justification:**  
  *Used solely to fetch public European Central Bank reference exchange rates from the open-source frankfurter.app API for multi-currency conversion.*
- **Safety Guarantees:**
  1. Queries consist only of public currency codes (e.g. `GET /v1/latest?base=EUR`).
  2. Zero user identifiers, device identifiers, account details, or transaction data are ever sent in the query payload or headers.
  3. Users can toggle "Automatic FX Rates Sync" to OFF for complete air-gapped offline operation.

---

## 3. Prominent In-App Disclosures

Google Play policies require prominent in-app disclosures for features using sensitive capabilities before the permission request is presented:

1. **Notification Listener In-App Disclosure:**  
   Implemented in `NotificationAutomationScreen.kt` &rarr; Displays an interactive dialog clearly describing that notification access is used solely on-device for whitelisted banking applications and that no data ever leaves the phone before opening system settings.
2. **Camera Permission In-App Disclosure:**  
   Implemented in `ScanReceiptScreen.kt` &rarr; In-context explanation before requesting camera access for on-device OCR.
3. **Privacy Policy Link:**  
   Included directly in the in-app Settings screen linking to `https://teppe21.github.io/finances/privacy-policy.html`.
