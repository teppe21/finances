# Finances — Native Android Application

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Target](https://img.shields.io/badge/Target%20SDK-36%20(Android%2016)-34A853.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.01-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-M3-6750A4.svg)](https://m3.material.io)
[![Room SQLite](https://img.shields.io/badge/Room%20DB-2.6.1%20(v3)-4285F4.svg)](https://developer.android.com/training/data-storage/room)
[![ML Kit OCR](https://img.shields.io/badge/Google%20ML%20Kit-On--Device%20OCR-34A853.svg)](https://developers.google.com/ml-kit)
[![CameraX](https://img.shields.io/badge/CameraX-1.3.2-EA4335.svg)](https://developer.android.com/training/camerax)
[![CI Build](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF.svg?logo=github-actions&logoColor=white)](https://github.com/teppe21/finances)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20First-000000.svg)](docs/PRIVACY.md)

A privacy-focused, zero-telemetry, 100% offline-first personal finance application for Android.

Built with native Kotlin and Jetpack Compose, **Finances** combines **real-time on-device bank notification tracking** (Revolut, OTP, Erste George, MBH, Wise) and **neural on-device receipt OCR** with an ACID-compliant Room SQLite database. No cloud logins, no financial tracking telemetry, and no subscription paywalls.

---

## 📱 Sideload APK & Play Store Bundle

- **Direct Install APK:** [`finances-native.apk`](finances-native.apk) in the repository root (~52.7 MB, includes offline ML Kit model).
- **Google Play Store Bundle:** `android-native/app/build/outputs/bundle/release/app-release.aab` (~29.8 MB).
- **Step-by-step Sideloading Guide:** See [NATIVE_APP_GUIDE.md](NATIVE_APP_GUIDE.md).

---

## 💡 Origin & Evolution

> **Project History Note:**
> This repository originally started as an experimental web-based personal finance dashboard (`/src`) built with React and Tailwind CSS to design the domain model and evaluate transaction ingestion flows.
>
> While the web prototype was great for desktop bookkeeping, it could not solve mobile friction: manual receipt logging and lack of real-time purchase detection. After experimenting with hybrid frameworks (which proved battery-intensive and unreliable for background services), the entire application was rebuilt from scratch as a **pure native Android app** (`/android-native`) in Kotlin and Jetpack Compose.
>
> Both codebases remain in this repository:
> - **`android-native/`**: The primary, production-grade native Android app (`com.teppe21.finances`).
> - **`/` & `src/`**: The original web dashboard prototype, preserved as a historical reference.

---

## 🎯 Architectural Highlights

1. **Air-Gapped Core Financials:** All accounts, transactions, balances, and receipts live strictly in on-device SQLite. Zero financial data is ever transmitted to remote servers.
2. **Zero Fake Money on Clean Installs:** New installs start with exact 0 balances across accounts. Representative demo data is isolated to an explicit, opt-in "Load Sample Data" action in Settings.
3. **Multi-Currency & Real FX Conversions:** Original transaction amounts and currencies are preserved permanently. Display currencies convert dynamically using real European Central Bank (ECB) rates with offline SQLite caching and customizable manual rates.
4. **Real-Time Bank Ingestion:** An Android `NotificationListenerService` captures transaction notifications from supported banking apps (Revolut, OTP, Erste, MBH, Wise) in memory.
5. **Multi-Tier Account Matching:** Distinguishes match confidence across **HIGH** (explicit user mapping or exact institution match), **MEDIUM** (fuzzy substring match), and **LOW** (held as pending for user review, never silently misassigned).
6. **$O(1)$ Indexed Deduplication:** Constant-time duplicate checking via indexed Room lookups (`findByFingerprint`, `findByExternalId`, `findPossibleDuplicates`) over a $\pm 2$-day window.
7. **On-Device Neural Receipt OCR:** CameraX live viewfinder coupled with Google ML Kit On-Device Text Recognition; Hungarian and European tax receipts are parsed locally without uploading images.
8. **Android 16 (API 36) Compliance:** Built with `targetSdk = 36`, `compileSdk = 35`, and structured Android Auto Backup rules (`data_extraction_rules.xml`).

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
       │  Raw Notification     │  OCR Text             │  Raw Rows            │
       ▼                       ▼                       ▼                      │
┌──────────────┐        ┌──────────────┐        ┌──────────────┐              │
│ Revolut/OTP  │        │ Hungarian    │        │ Multi-format │              │
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
                 │ 2. SHA-256 Fingerprint    │
                 │ 3. Indexed Dedup (Room)   │
                 │ 4. Keyword Rule Engine    │
                 │ 5. Transfer Auto-Detector │
                 └─────────────┬─────────────┘
                               │
                               ▼
                 ┌───────────────────────────┐
                 │       Room SQLite DB      │
                 │     (Schema Version 3)    │
                 └─────────────┬─────────────┘
                               │ Flow<List<T>>
                               ▼
                 ┌───────────────────────────┐
                 │  Jetpack Compose M3 UI    │
                 │  - Donut Spending Chart   │
                 │  - Dynamic Rule Manager   │
                 │  - Multi-Currency Display │
                 │  - 5-Tab Navigation       │
                 └───────────────────────────┘
```

---

## ⚡ Feature Deep-Dive

### 1. Bank Push Notification Automation
- **Supported Banks:** Revolut, OTP Bank (SmartBank & MobilBank), Erste Bank (George), MBH Bank, Wise, and generic Hungarian/European transaction formats.
- **Privacy & 2FA Discard:** Security verification codes, OTP tokens, SMS logins, and personal chat messages are immediately filtered and dropped in memory.
- **Custom Bank Mapping:** Users can map specific banks to their accounts in *Settings $\rightarrow$ Bank Notification Automation*.
- **Confidence Tiers:** High confidence matches automatically associate with the account; low/unknown matches are flagged as `Pending: Needs Account Review` to prevent balance contamination.

### 2. On-Device Receipt OCR (CameraX + ML Kit)
- **Live Viewfinder:** Integrated camera guide with flash toggle and gallery picker.
- **Hungarian Semantic Parser:** Automatically detects merchant tax headers, total amounts (`FIZETENDŐ`, `ÖSSZESEN`, `VÉGÖSSZEG`), VAT percentages, and payment methods (`KÉSZPÉNZ`, `BANKKÁRTYA`).
- **Cash Wallet Routing:** Cash payments automatically debit the local Cash Wallet balance.

### 3. Multi-Currency Support & Real FX Engine
- Seamlessly toggle your primary display currency across **HUF, EUR, USD, GBP, CHF**.
- Real-time exchange rate conversions powered by European Central Bank reference rates.
- Full offline caching in SQLite table `exchange_rates`, with fallback rates and user-customizable manual rate overrides.

### 4. Interactive Analytics & Hardware-Accelerated Charts
- **Compose Canvas Donut Chart:** Smooth hardware-accelerated donut visualization with touch-to-inspect category breakdowns.
- **Dynamic Period Engine:** Filter by *This Month, Last Month, 3 Months, 6 Months, This Year, Last Year*, or a *Custom Date Range*.
- **Merchant Leaderboard:** Automatic calculation of top spending destinations.

### 5. Categorization & Rule Manager
- User-defined and built-in rules for categories (`Food & Groceries`, `Dining`, `Transport`, `Subscriptions`, `Housing`, etc.).
- Keyword chips management with user priority overriding default system rules.
- Safe deletion barrier: Prevents orphan records by requiring reassignment before removing an in-use category.

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
│   │   │   │   └── settings/        # Privacy Manifesto, FX Rates, PIN Lock
│   │   │   ├── native/              # BankNotificationListenerService & Parsers
│   │   │   ├── FinancesApp.kt       # Application class (Locale configuration)
│   │   │   └── MainActivity.kt      # Edge-to-edge Compose host activity
│   │   ├── src/test/java/           # Comprehensive Unit & Regression Test Suite
│   │   ├── schemas/                 # Room JSON Schema Exports (v3)
│   │   └── build.gradle.kts         # Target SDK 36, Compile SDK 35, Release Signing
│   ├── gradlew                      # Gradle Wrapper script (Linux/macOS)
│   ├── gradlew.bat                  # Gradle Wrapper script (Windows)
│   └── settings.gradle.kts
│
├── .github/workflows/
│   └── android.yml                  # Automated CI testing & artifact packaging
│
├── docs/                            # In-Depth Documentation Suite
│   ├── ARCHITECTURE.md              # Clean architecture & ingestion pipeline
│   ├── SECURITY.md                  # Storage sandbox, threat model & network hygiene
│   ├── BANK_INTEGRATION.md          # Bank packages, regex patterns & OEM guidance
│   ├── DATA_MODEL.md                # Room v3 schema, indices, converters & ER diagram
│   ├── RELEASE.md                   # Keystore configuration, Proguard & Play Store
│   ├── TESTING.md                   # Test suite structure & CI execution
│   └── PRIVACY.md                   # Air-gap principles & Android Auto Backup rules
│
├── src/                             # Original Web Dashboard (Reference Prototype)
├── .gitignore                       # Clean exclusion of build artifacts & secrets
└── README.md
```

---

## 🛠️ Building & Running

### Requirements
- **JDK 17** (e.g. Eclipse Temurin 17)
- **Android SDK 35+** (Installed platforms: `android-35`, `android-36`; Build Tools: `35.0.1`, `36.0.0`)

### 1. Run All Unit Tests
```bash
cd android-native
./gradlew test
```

### 2. Build Release APK & App Bundle (AAB)
```bash
cd android-native
./gradlew assembleRelease bundleRelease
```
Outputs:
- **APK**: `android-native/app/build/outputs/apk/release/app-release.apk`
- **AAB**: `android-native/app/build/outputs/bundle/release/app-release.aab`

---

## 📖 In-Depth Documentation

- [System Architecture](docs/ARCHITECTURE.md)
- [Security & Threat Model](docs/SECURITY.md)
- [Bank Notification Integration](docs/BANK_INTEGRATION.md)
- [Room Database Schema](docs/DATA_MODEL.md)
- [Release & Signing Guide](docs/RELEASE.md)
- [Testing Strategy & CI](docs/TESTING.md)
- [Privacy Policy & Backup Rules](docs/PRIVACY.md)
- [Device Sideloading Guide](NATIVE_APP_GUIDE.md)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — free to use, inspect, and build upon.
