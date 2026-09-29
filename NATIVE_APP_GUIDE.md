# Finances - Native Android Application User & Deployment Guide

## 📱 Release APK File (Ready to Install)

The fully native (Kotlin + Jetpack Compose + Room + CameraX + ML Kit) Android application is compiled and available in the project root:

- **File Path:** `finances-native.apk` (Root directory)
- **Size:** ~52.7 MB (includes offline ML Kit on-device neural OCR, CameraX, and Room SQLite database)
- **Release Bundle (AAB):** `android-native/app/build/outputs/bundle/release/app-release.aab` (~29.8 MB)

---

## 🚀 Installation & Sideloading Instructions

1. **Transfer to Your Phone:**
   - Connect your phone to your PC via USB cable and copy `finances-native.apk` into your phone's *Downloads* folder.
   - Alternatively, transfer via Google Drive, Telegram, WhatsApp, or local network transfer.
2. **Install:**
   - Open *Files* or *File Manager* on your phone and tap `finances-native.apk`.
   - If prompted by Android, allow *Install unknown apps* for your file manager.
3. **Launch:**
   - Tap **Install / Update**, then launch **Finances** from your app launcher.

---

## 🏦 Features & Operational Guide

### 1. Bank Push Notification Automation
- **Supported Banks:** Revolut, OTP Bank (SmartBank & MobilBank), Erste George, MBH Bank, Wise, and generic banking templates.
- **How to Activate:**
  1. In the app, navigate to **More $\rightarrow$ Bank Notification Automation**.
  2. Tap **Enable Notification Access**.
  3. Review the **Prominent Privacy Disclosure** and tap **Continue to Settings**.
  4. In Android's *Notification access* list, toggle **Finances** to **Allow**.
  5. **Bank Account Assignment:** Under *Monitored Banking Apps*, select which local account (e.g. OTP Current Account, Revolut Pocket) incoming alerts from each bank should automatically post to.
  6. **OEM Device Specifics (Xiaomi / Redmi / POCO - HyperOS / MIUI):**
     - Navigate to *Settings $\rightarrow$ Apps $\rightarrow$ Manage Apps $\rightarrow$ Finances*.
     - Toggle **Autostart** ON.
     - Set *Battery Saver* to **No restrictions** so the background service is not killed by the OS.
  7. **Testing:** Use the in-app simulation buttons (*OTP Test*, *Revolut Test*, *Revolut Transfer*) to verify parsing without spending real money!

### 2. Receipt & Invoice OCR Scanning (CameraX + Google ML Kit)
- **How It Works:**
  - Tap **Scan Receipt** from the bottom bar or quick actions on the Dashboard.
  - Grant camera access when prompted.
  - Frame the physical receipt within the viewfinder guide. Toggle the flashlight (torch) if lighting is low.
  - Capture the photo or pick an existing receipt from your photo gallery.
  - The on-device ML Kit OCR engine parses the merchant name, total amount, VAT, date, and payment method (Cash vs. Card).
  - Review the detected fields in the instant confirmation card and tap **Save Transaction**.

### 3. Transactions, Search & Filters
- **Diacritics-Insensitive Search:** Searching for `etterem` immediately finds `Étterem` transactions.
- **Period Filter:** Quick toggle between *This Month*, *Last Month*, *3 Months*, *6 Months*, and *All*.
- **Category Filter:** Filter by specific system or user-created categories.
- **Recurring Filter:** View only recurring subscriptions and bills.
- **Manual Logging:** Tap the **+** button to log Expenses, Incomes, or Internal Transfers manually.

### 4. CSV Statement Import
- Import standard bank statement exports (OTP, Revolut, Erste, MBH, Wise).
- Robust character encoding detection (UTF-8, UTF-8 BOM, ISO-8859-2).
- Automatic European decimal comma handling (`1.450,50 Ft`).
- Pre-import preview table with duplicate detection statistics.

### 5. Multi-Currency Support & Real FX Conversion
- Select your primary display currency (HUF, EUR, USD, GBP, CHF) in *Settings*.
- Balances and statistics convert accurately using real exchange rates from the European Central Bank.
- **Offline / Air-Gap Resilience:** Exchange rates are cached in local SQLite. You can disable automatic online sync and set custom manual exchange rates at any time.

---

## 🛠 Technology Stack Summary

| Component | Implementation |
|---|---|
| **User Interface** | 100% Jetpack Compose, Material 3, Navigation Compose |
| **Persistence** | SQLite / Room 2.6.1 ORM (Schema v3), Jetpack DataStore |
| **Camera & Vision** | AndroidX CameraX 1.3.2 + Google ML Kit Text Recognition 16.0.0 |
| **Background Automation** | `BankNotificationListenerService` (Coroutine IO Dispatcher) |
| **Financial Math** | Strict 64-bit integer minor unit arithmetic (zero rounding error) |
| **Target Platform** | Android 16 (API 36), Compile SDK 35, Min SDK 26 |
| **Automated Testing** | Comprehensive JUnit test suite (`ParsersTest`, `DatabaseAndMatchingTest`, etc.) |
| **Build System** | Gradle 8.5, Android Gradle Plugin 8.2.2, JDK 17 |
