# Finances — Native Android Application

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Target](https://img.shields.io/badge/Target%20SDK-36%20(Android%2016)-34A853.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Compile SDK](https://img.shields.io/badge/Compile%20SDK-36-34A853.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.01-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-M3-6750A4.svg)](https://m3.material.io)
[![Room SQLite](https://img.shields.io/badge/Room%20DB-2.6.1%20(v4)-4285F4.svg)](https://developer.android.com/training/data-storage/room)
[![ML Kit OCR](https://img.shields.io/badge/Google%20ML%20Kit-On--Device%20OCR-34A853.svg)](https://developers.google.com/ml-kit)
[![CameraX](https://img.shields.io/badge/CameraX-1.3.2-EA4335.svg)](https://developer.android.com/training/camerax)
[![CI Build](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF.svg?logo=github-actions&logoColor=white)](https://github.com/teppe21/finances)
[![Privacy](https://img.shields.io/badge/Privacy-Offline%20First%20%7C%20No%20Telemetry-000000.svg)](docs/privacy-policy.html)

A privacy-focused, offline-first personal finance application for Android, built natively in Kotlin and Jetpack Compose.

Most personal finance apps suffer from one of two extremes: either they require tedious manual entry for every receipt, or they demand full access to bank account credentials via cloud aggregators. **Finances** explores a practical middle ground: leveraging native Android platform capabilities (real-time notification listening and on-device neural OCR) to automate logging at the device level, with zero tracking SDKs, no cloud servers, and no subscription paywalls.

---

## 📱 Quick Download & Install

- **Direct Install APK:** [`finances-native.apk`](finances-native.apk) in the repository root (~52.7 MB, self-contained with offline ML Kit model).
- **Google Play Store Bundle:** `android-native/app/build/outputs/bundle/release/app-release.aab` (~29.8 MB).
- **Step-by-step Sideloading Guide:** See [NATIVE_APP_GUIDE.md](NATIVE_APP_GUIDE.md).
- **30-Day Testing Protocol:** See [docs/REAL_DEVICE_TEST_PLAN.md](docs/REAL_DEVICE_TEST_PLAN.md).

---

## 💡 Origin & Evolution

> **Project History Note:**
> This repository originally started as an experimental web-based dashboard (`/src`) using React and Tailwind CSS to model transaction schemas, duplicate detection, and categorization rules.
>
> While the web prototype was useful for testing business logic, desktop bookkeeping cannot solve real-world mobile friction. Hybrid frameworks were initially explored, but proved power-hungry and unsuited for background Android services. The application was therefore rebuilt as a **100% native Android application** (`/android-native`) using Kotlin, Jetpack Compose, Room SQLite, CameraX, and Android system services.
>
> Both codebases remain in this repository:
> - **`android-native/`**: The primary, production-grade native Android app (`com.teppe21.finances`).
> - **`/` & `src/`**: The original web prototype, preserved for reference.

---

## 🛠️ Core Engineering Highlights

### 1. On-Device Automation & Privacy-Hardened Ingestion (Room v4)
- **Push Notification Ingestion:** An Android `NotificationListenerService` captures transaction notifications in volatile memory from supported banking apps (Revolut, Google Wallet, OTP Bank, Erste George, MBH Bank, Wise).
- **Google Wallet & Contactless Ingestion:** Full native parsing of contactless NFC card payments (`com.google.android.apps.walletnfcrel`) with card brand and last-4 identification.
- **Cross-Source Temporal Deduplication:** When both Google Wallet and Revolut fire notifications for the same physical payment within 120 seconds, the engine automatically recognizes the duplicate pair and suppresses double expense logging.
- **Zero Raw Notification Persistence (Room v4):** Raw notification titles, notification text, and summaries are **never stored in SQLite**. Only sanitized structured metadata (amount, currency, merchant name, timestamp, reason code) is persisted.
- **2FA & OTP Security Shield:** Any notification containing one-time passwords, SMS authentication codes, or login verification tokens is filtered and discarded immediately.
- **Marketing & Promo Rejection:** Promotional marketing alerts (such as Revolut referral reward campaigns) are detected and discarded without generating phantom transactions.
- **On-Device Receipt OCR:** Integrates CameraX with Google ML Kit's on-device neural text recognition. Hungarian and European tax receipts (merchants, totals, VAT, payment methods) are parsed locally on the device's CPU/NPU without sending image data to external APIs.

### 2. Reliable Ingestion Pipeline & Safe Account Matching
- **Confidence-Tier Matching:**
  - **HIGH:** User-assigned bank mappings or exact institution matches post directly to the target account.
  - **MEDIUM:** Substring or currency-inferred matches are flagged as `Pending Review`.
  - **LOW / Unmatched:** Transactions from unrecognized banks are held in `NEEDS_REVIEW` with an unassigned account. **They never silently corrupt existing account balances.**
- **$O(1)$ Indexed Deduplication:** Composite Room indices (`amountMinor, currency, date`) and deterministic SHA-256 fingerprint lookups (`findByFingerprint`, `findByExternalId`), providing instant deduplication over a sliding $\pm 2$-day window.

### 3. Financial Precision & Multi-Currency Engine
- **Integer Minor-Unit Arithmetic:** All transaction values are stored as 64-bit integer minor units (e.g. HUF fillér, EUR cents), eliminating IEEE 754 floating-point rounding inaccuracies.
- **True Multi-Currency Support:** Original currency codes and transaction amounts are permanently preserved in the database. When switching display currencies (HUF, EUR, USD, GBP, CHF), values are converted dynamically using European Central Bank (ECB) reference rates.
- **Air-Gapped Offline FX Caching:** Exchange rates are stored in Room (`exchange_rates`) with bundled fallbacks and support for user-defined custom rates. The app functions fully offline with a dedicated toggle in Settings.

### 4. Transparent Security & Privacy Posture
- **Private App Storage:** Financial data resides exclusively in `/data/data/com.teppe21.finances/databases/finances.db`, protected by Android's Linux UID process isolation and hardware-backed File-Based Encryption (FBE).
- **Interactive App Lock Gate:** Full-screen lock gate on launch and backgrounding (`onStop`), supporting AndroidX `BiometricPrompt` and salted **SHA-256 PIN hashing** with a cryptographically random salt per install.
- **Complete Right to Erasure:** A dedicated **"Delete All Financial Data"** control in Settings permanently wipes all local SQLite tables and resets balances to clean 0 state.
- **Minimal, Anonymous Network Footprint:** The app uses network access for exactly one optional purpose: fetching daily public FX reference rates from the Frankfurter API (an anonymous, parameter-free GET request). This can be completely disabled in Settings.
- **Zero Third-Party Telemetry:** No analytics libraries, no crash aggregators, no advertising SDKs, and no tracking identifiers.

---

## 🧱 Ingestion & Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DATA INGESTION CHANNELS                          │
└──────┬───────────────────────┬───────────────────────┬──────────────────────┬┘
       │                       │                       │                      │
       ▼                       ▼                       ▼                      ▼
   Bank Push              CameraX Frame             Bank CSV /             Manual
  Notification             On-Device OCR             Statement             Entry
(ListenerService)         (Google ML Kit)           (RFC-4180)            (Dialog)
       │                       │                       │                      │
       │  Sanitized Metadata   │  OCR Text             │  Raw Rows            │
       ▼                       ▼                       ▼                      │
┌──────────────┐        ┌──────────────┐        ┌──────────────┐              │
│ Revolut / GW │        │ European/HU  │        │ Multi-format │              │
│ Bank Parsers │        │ Receipt      │        │ CSV Parser   │              │
│ (Regex & NLP)│        │ Semantic NLP │        │              │              │
└──────┬───────┘        └──────┬───────┘        └──────┬───────┘              │
       │                       │                       │                      │
       └───────────────────────┼───────────────────────┘                      │
                               │                                              │
                               ▼                                              │
                 ┌───────────────────────────┐                                │
                 │ IngestTransactionUseCase  │◄───────────────────────────────┘
                 └─────────────┬─────────────┘
                               │
                 ┌─────────────┴─────────────┐
                 │ 1. Diacritic Normalizer   │
                 │ 2. Deterministic Hash     │
                 │ 3. Cross-Source Dedup     │
                 │ 4. Keyword Rule Engine    │
                 │ 5. Transfer Auto-Detector │
                 │ 6. Confidence Tier Filter │
                 └─────────────┬─────────────┘
                               │
                               ▼
                 ┌───────────────────────────┐
                 │       Room SQLite DB      │
                 │     (Schema Version 4)    │
                 └─────────────┬─────────────┘
                               │ Flow<List<T>>
                               ▼
                 ┌───────────────────────────┐
                 │  Jetpack Compose M3 UI    │
                 │  - Donut Spending Chart   │
                 │  - Dynamic Rule Manager   │
                 │  - Multi-Currency Display │
                 │  - Biometric Lock Gate    │
                 └───────────────────────────┘
```

---

## ⚡ Key Features

- **Clean Installation State:** Fresh installs initialize with verified 0 balances across default accounts (`Cash Wallet`, `OTP Current Account`, `Revolut`, `Savings`). No fake balances or generated dummy data are seeded automatically.
- **Isolated Demo & Portfolio Mode:** Users and technical interviewers can populate representative sample balances on demand via **Settings $\rightarrow$ Data Management $\rightarrow$ Load Demo**, and reset back to clean 0 balances via **Clear Demo**.
- **Interactive Analytics:** Hardware-accelerated Compose Canvas donut chart with touch-to-inspect category breakdowns, spending trends, and top merchant leaderboards over customizable time windows.
- **Smart Categorization & User Rules:** Automated keyword categorization with custom user rules taking priority over built-in heuristics. Categories can be dynamically added or safely reassigned.
- **CSV Statement Importer:** Supports standard exports from OTP Bank, Revolut, Erste, MBH, and Wise with automatic character set detection (UTF-8, UTF-8 BOM, ISO-8859-2) and European comma formatting.
- **Strict Android Auto Backup Rules:** Configured via `data_extraction_rules.xml` and `backup_rules.xml` to include user databases and settings while excluding temporary cache and receipt images.

---

## 🗂️ Project Structure

```text
finances/
├── finances-native.apk              # Install-ready signed release APK
├── NATIVE_APP_GUIDE.md              # Android phone installation & quick-start guide
├── android-native/                  # 100% Native Android Application
│   ├── app/
│   │   ├── src/main/java/com/teppe21/finances/
│   │   │   ├── core/                # Theme, Currency & Date Formatter, Text Normalizer
│   │   │   ├── data/                # Room SQLite Entities, DAOs, Repositories, DataStore
│   │   │   ├── domain/              # Use Cases (Ingest, Dedup, Recurring, Stats)
│   │   │   ├── feature/             # Jetpack Compose Screens & ViewModels
│   │   │   │   ├── dashboard/       # Main Overview & Financial Metrics
│   │   │   │   ├── transactions/    # Filtered & Sorted Transaction List
│   │   │   │   ├── accounts/        # Account & Wallet Balances
│   │   │   │   ├── analytics/       # Donut Chart & Cash Flow Trends
│   │   │   │   ├── categories/      # Category & Keyword Rules Management
│   │   │   │   ├── receipt/         # CameraX + ML Kit OCR Scanner
│   │   │   │   ├── notifications/   # Bank Notification Automation & Test Lab
│   │   │   │   ├── importcsv/       # Bank Statement CSV Importer
│   │   │   │   └── settings/        # Privacy Manifesto, FX Rates, PIN/Biometric Lock
│   │   │   ├── native/              # BankNotificationListenerService & Parsers
│   │   │   ├── FinancesApp.kt       # Application class (Locale configuration)
│   │   │   └── MainActivity.kt      # FragmentActivity with LockGateScreen & Compose host
│   │   ├── src/test/java/           # Comprehensive Unit & Regression Test Suite (39 tests)
│   │   ├── schemas/                 # Tracked Room JSON Schema Exports (v1, v2, v3, v4)
│   │   └── build.gradle.kts         # Target SDK 36, Compile SDK 36, Strict Release Signing
│   ├── gradlew                      # Gradle Wrapper script (Linux/macOS)
│   ├── gradlew.bat                  # Gradle Wrapper script (Windows)
│   └── settings.gradle.kts
│
├── .github/workflows/
│   ├── android.yml                  # PR & Main Code Verification CI (test, lint, assembleDebug)
│   └── release.yml                  # Protected Release Signing Workflow (bundleRelease)
│
├── docs/                            # In-Depth Documentation Suite
│   ├── privacy-policy.html          # Public standalone HTML privacy policy
│   ├── REAL_DEVICE_TEST_PLAN.md     # 30-day physical device testing protocol
│   ├── PLAY_CONSOLE_COMPLIANCE.md   # Google Play Data Safety & permissions guide
│   ├── PLAY_FINANCIAL_FEATURES_DECLARATION.md # Financial services regulatory scope
│   ├── ARCHITECTURE.md              # Clean architecture & ingestion pipeline
│   ├── SECURITY.md                  # Storage sandbox, threat model & network hygiene
│   ├── BANK_INTEGRATION.md          # Bank packages, regex patterns & OEM guidance
│   ├── DATA_MODEL.md                # Room v4 schema, indices, converters & ER diagram
│   ├── RELEASE.md                   # Keystore configuration, Proguard & Play Store
│   ├── TESTING.md                   # Test suite structure & CI execution
│   └── PRIVACY.md                   # Air-gap principles & Android Auto Backup rules
│
├── src/                             # Original Web Dashboard (Reference Prototype)
├── .gitignore                       # Clean exclusion of build artifacts & secrets
└── README.md
```

---

## 🛠️ Building & Testing

### Requirements
- **JDK 17** (e.g. Eclipse Temurin 17)
- **Android SDK 36** (Installed platform: `android-36`; Build Tools: `36.0.0`)
- **Gradle 8.9** & **Android Gradle Plugin 8.7.2**

### 1. Run Unit Tests (39 tests, JVM execution)
```bash
cd android-native
./gradlew testDebugUnitTest
```

### 2. Build Debug APK (No keystore required)
```bash
cd android-native
./gradlew assembleDebug
# Output: android-native/app/build/outputs/apk/debug/app-debug.apk
```

### 3. Build Signed Release APK & App Bundle (AAB)
Release tasks strictly require signing credentials via environment variables or `local.properties` (never falling back to debug keys):
```bash
cd android-native
./gradlew assembleRelease bundleRelease
```
Outputs:
- **APK**: `android-native/app/build/outputs/apk/release/app-release.apk`
- **AAB**: `android-native/app/build/outputs/bundle/release/app-release.aab`

For step-by-step keystore generation, verification with `apksigner`, and Google Play Console release instructions, refer to [docs/RELEASE.md](docs/RELEASE.md).

---

## 📖 In-Depth Documentation

- [Privacy Policy (Static HTML)](docs/privacy-policy.html)
- [30-Day Real Device Test Plan](docs/REAL_DEVICE_TEST_PLAN.md)
- [Google Play Console Compliance](docs/PLAY_CONSOLE_COMPLIANCE.md)
- [Google Play Financial Features Declaration](docs/PLAY_FINANCIAL_FEATURES_DECLARATION.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Security & Threat Model](docs/SECURITY.md)
- [Bank Notification Integration](docs/BANK_INTEGRATION.md)
- [Room Database Schema & Migrations](docs/DATA_MODEL.md)
- [Release & Signing Guide](docs/RELEASE.md)
- [Testing Strategy & CI](docs/TESTING.md)
- [Privacy Policy & Backup Rules](docs/PRIVACY.md)
- [Device Sideloading Guide](NATIVE_APP_GUIDE.md)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — free to inspect, use, and build upon.
