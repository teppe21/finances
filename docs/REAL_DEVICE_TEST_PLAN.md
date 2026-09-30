# Finances Native Android — Real Device 30-Day Testing Plan

This document serves as the operational testing protocol for actively daily-driving the production-hardened **Finances** native Android application (`com.teppe21.finances`) on a physical device for approximately one month prior to Google Play publication.

---

## 1. Initial Deployment & Environment Setup

### 1.1 Device Prerequisites
- **OS Version:** Android 11 through Android 15 / 16 (API level 30 to 36).
- **Installed Banking Apps:**
  - Revolut (`com.revolut.revolut`)
  - Google Wallet (`com.google.android.apps.walletnfcrel`)
  - Hungarian Bank App (OTP, Erste George, MBH, or Wise)
- **Biometrics:** At least one enrolled fingerprint or face unlock credential.

### 1.2 Installation Steps
1. Transfer `finances-native.apk` from the repository root to your phone via USB (`adb install -r finances-native.apk`) or local file manager.
2. Launch the app for the first time.
3. **Verify Clean Install Baseline:**
   - Accounts tab should display default accounts (Cash Wallet, OTP Current Account, Revolut, Savings) with exactly **0 HUF balance**.
   - No mock or sample transactions should appear.
   - If desired, test "Load Demo" in Settings, then "Clear Demo" to verify state reset.

---

## 2. Notification Listener & Bank Parsing Protocol

### 2.1 Service Activation
1. Navigate to **More &rarr; Notification Automation**.
2. Click **"Enable in Settings"** &rarr; Verify the system Prominent Disclosure modal appears explaining that data remains 100% on-device.
3. Grant notification listener permission in Android Settings.
4. Return to Finances &rarr; Verify the permission status card turns green with **"Active"**.

### 2.2 Live Banking Transaction Scenarios

#### Scenario A: Genuine In-Store Physical Purchase (Revolut)
- **Action:** Pay at a local grocery store (e.g. Coop, Lidl, Spar) with your physical Revolut card.
- **Expected Notification:** `You spent HUF804` (with optional balance line).
- **Verification:**
  - Opens Finances &rarr; Transaction recorded as **-804 HUF**.
  - Merchant extracted as grocery store name (e.g., "Coop").
  - Direction: `EXPENSE`.
  - Notification event recorded in SQLite with `reasonCode = "PARSED_SUCCESS"`.
  - **Crucial:** Raw notification title and text are **NOT** stored in Room SQLite (`notification_events` table contains only sanitized metadata).

#### Scenario B: Contactless Google Wallet Payment with Revolut Card
- **Action:** Pay at a physical NFC terminal using Google Wallet with a linked Revolut card.
- **Trigger:** Phone receives **two** notifications within seconds:
  1. Google Wallet: `"75. SZ. ABC ÁRUHÁZ" / "HUF804.00 with Revolut Mastercard ••1413"`
  2. Revolut: `"Coop" / "You spent HUF804..."`
- **Verification:**
  - Finances records **only one single transaction** (-804 HUF).
  - The second notification is automatically flagged as `DUPLICATE_CROSS_SOURCE_PAYMENT`.
  - No duplicate expenses are created, preserving balance accuracy.

#### Scenario C: Distinct Purchases at Different Merchants with Same Amount
- **Action:** Purchase an item at Store A for 804 HUF, and then purchase an item at Store B for 804 HUF 1 minute later.
- **Verification:**
  - Because merchant names differ and it is not a Wallet+Bank pair for the same transaction, **both transactions are recorded as separate valid purchases**.

#### Scenario D: Marketing / Referral Notifications (Revolut)
- **Action:** Wait for or simulate a promotional notification (e.g., *"7 days left to earn / Get HUF22,500 for each eligible friend you refer"*).
- **Verification:**
  - Notification is automatically dropped by `RevolutParser`.
  - **No fake income or expense is created**.
  - Recorded in history with `reasonCode = "IGNORED_PROMOTION_OR_NON_TRANSACTION"`.

#### Scenario E: Two-Factor Authentication / OTP Discard
- **Action:** Request a login code or approval SMS from OTP or Revolut.
- **Verification:**
  - Notification containing security tokens (e.g. *"Az Ön belépési kódja: 981245"*) is dropped immediately at the listener boundary.
  - Zero sensitive security codes are ever ingested.

---

## 3. Real FX Conversion & Multi-Currency Testing

1. **Display Currency Switching:**
   - In Settings, switch display currency from `HUF` to `EUR`.
   - **Verification:** Original transaction numeric value (e.g., 10,000 HUF) is **NOT** blindly swapped to 10,000 EUR. The app calculates real converted display amount: `~25 EUR`.
   - Underlying database amount remains `1000000L` (10,000 HUF minor units).
2. **Air-Gapped Offline Mode:**
   - Toggle **"Automatic FX Rates Sync"** to `OFF`.
   - Verify app makes zero outbound network requests.
   - Click **"Edit Rates"** and enter a custom rate (e.g. 400 HUF / EUR).
   - Verify dashboard balances recalculate immediately using the local custom rate.

---

## 4. CameraX & ML Kit Receipt Scanning

1. Open **Scan Receipt**.
2. Capture paper receipts under:
   - High daylight
   - Low evening artificial lighting
   - Crumpled / folded receipts
3. **Verification:**
   - Camera preview renders smoothly with continuous autofocus.
   - Total amount, merchant, and tax are extracted on-device with confidence scoring.
   - Confirming the receipt generates a valid transaction linked to `receiptId`.

---

## 5. Security Gate & App Lock Protocol

1. **PIN Setup:**
   - Go to Settings &rarr; Security &rarr; Select **PIN**.
   - Enter `4826`, confirm `4826`.
   - Verify salted SHA-256 hash is created with random salt in DataStore.
2. **Backgrounding & Lockout Verification:**
   - Minimize the app or lock phone screen.
   - Reopen Finances &rarr; Verify the full-screen **Lock Gate** appears blocking access.
   - Enter wrong PIN &rarr; Shows "Incorrect PIN".
   - Enter `4826` &rarr; Unlocks immediately to previous screen.
3. **Biometric Unlock:**
   - Select **Biometric** in Settings.
   - Reopen app &rarr; System BiometricPrompt appears over the lock screen.
   - Authenticate with fingerprint &rarr; Unlocks instantly.
4. **Disabling Lock:**
   - Select **Off** &rarr; Prompt demands current PIN before lock is disabled.

---

## 6. Complete Data Deletion & Privacy Verification

1. Go to Settings &rarr; Data Management &rarr; Click **"Delete All Financial Data"**.
2. Confirm the destructive dialog.
3. **Verification:**
   - All transactions, receipts, custom categories, category rules, and notification events are wiped from SQLite.
   - Account balances reset to 0 HUF.
   - Preferences reset to defaults.
   - No leftover orphaned rows remain in the database.

---

## 7. Weekly Audit Checklist

| Week | Target Feature | Status | Notes |
|:---|:---|:---:|:---|
| **Week 1** | Notification Ingestion & Wallet Dedup | [ ] | Monitor daily Revolut & Google Wallet alerts |
| **Week 2** | Multi-Currency & Offline FX Caching | [ ] | Verify daily exchange rate stability & offline mode |
| **Week 3** | Receipt OCR Scanning & Categorization | [ ] | Scan grocery receipts and verify auto-categorization |
| **Week 4** | App Lock, Backup, and Store Readiness | [ ] | Stress test biometric gate and verify zero crash logs |
