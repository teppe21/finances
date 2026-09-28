# Finances 

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.01-4285F4.svg?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-M3-6750A4.svg)](https://m3.material.io)
[![Room SQLite](https://img.shields.io/badge/Room%20DB-2.6.1-4285F4.svg)](https://developer.android.com/training/data-storage/room)
[![ML Kit OCR](https://img.shields.io/badge/Google%20ML%20Kit-On--Device%20OCR-34A853.svg)](https://developers.google.com/ml-kit)
[![CameraX](https://img.shields.io/badge/CameraX-1.3.2-EA4335.svg)](https://developer.android.com/training/camerax)
[![Offline First](https://img.shields.io/badge/Privacy-100%25%20Offline-000000.svg)](https://github.com/teppe21/finances)

A privacy-focused, zero-telemetry, 100% offline-first personal finance application for Android. 

Built with Kotlin and Jetpack Compose, **Finances** combines **real-time on-device bank notification tracking** (Revolut, OTP, Erste, MBH, Wise) and **neural on-device receipt OCR** with an ACID-compliant Room SQLite database. No cloud accounts, no third-party analytics, and no subscription paywalls.

---

## 💡 Origin & Evolution

> **Note on Project History:**
> This repository originally started as an experimental web-based personal finance dashboard (`/src`) built with React and Tailwind CSS to design the data model and refine transaction ingestion algorithms.
>
> While the web prototype was great for desktop bookkeeping, it couldn't solve mobile friction: manual receipt logging and lack of real-time purchase detection. After experimenting with web wrappers and hybrid frameworks (which proved clunky, power-hungry, and unreliable for system services), I rebuilt the entire application from the ground up as a **pure native Android app** (`/android-native`) in Kotlin and Jetpack Compose.
>
> Both implementations remain in this repository:
> - **`android-native/`**: The primary, production-grade native Android app.
> - **`/` & `src/`**: The original web dashboard prototype, kept for reference and desktop analytics.

---

## 🎯 Why I Built This

Most popular budgeting apps require bank credentials, transmit financial records to remote servers, or lock basic CSV exports behind monthly subscriptions.

I wanted something different:
1. **Zero Data Leakage:** Financial data should live on my phone's storage, encrypted, and nowhere else.
2. **Instant Logging:** Transactions from Revolut or local bank apps should be captured in real time via system notifications without opening the app.
3. **Receipt Scanning That Actually Works Offline:** ML Kit running on the device's neural engine, parsing Hungarian and European receipt formats without sending photos to an external API.
4. **Reliable Math:** Exact minor-unit (integer `Long`) calculations with zero floating-point rounding errors.

---

## 🧱 Architecture & Ingestion Flow

The native app follows clean MVVM architecture with unidirectional data flow (StateFlow), repository patterns, and domain use cases.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DATA INGESTION SOURCES                           │
└──────┬───────────────────────┬───────────────────────┬──────────────────────┬┘
       │                       │                       │                      │
       ▼                       ▼                       ▼                      ▼
  Bank Push              CameraX Frame             Bank CSV /             Manual
Notification             On-Device OCR              Statement             Entry
(ListenerService)       (Google ML Kit)            (RFC-4180)            (Dialog)
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
                 │  - Diacritic Normalizer   │
                 │  - Multi-tier Category    │
                 │    Rule Engine (User > DB)│
                 │  - Cross-Source Dedup     │
                 │  - Transfer Filter        │
                 └─────────────┬─────────────┘
                               │
                               ▼
                 ┌───────────────────────────┐
                 │       Room SQLite DB      │
                 │  (ACID Local Persistence) │
                 └─────────────┬─────────────┘
                               │ Flow<List<T>>
                               ▼
                 ┌───────────────────────────┐
                 │  Jetpack Compose M3 UI    │
                 │  - Canvas Donut Chart     │
                 │  - Dynamic Rule Manager   │
                 │  - 4-Tab Navigation       │
                 └───────────────────────────┘
```

---

## ⚡ Core Features

### 1. Real-Time Bank Notification Automation
An Android `NotificationListenerService` captures transaction notifications from supported banking apps the second you tap your card or send a transfer.
- **Supported Banks:** Revolut, OTP Bank (SmartBank & new OTP MobilBank), Erste Bank (George), MBH Bank, Wise, and generic Hungarian card purchase SMS/push formats.
- **Privacy Filter:** Any non-banking notification (messaging apps, emails, OTP security tokens, 2FA codes) is instantly discarded in memory before storage.
- **Transfer Awareness:** Automatically identifies incoming/outgoing transfers (`"átutalás"`, `"pénzt küldtél"`, `"transfer"`) and routes them to a dedicated internal transfer pool without skewing your monthly spending statistics.

### 2. On-Device Receipt OCR (CameraX + ML Kit)
- **Live Viewfinder:** Built with CameraX lifecycle integration, featuring an alignment grid and torch toggle.
- **Hungarian Semantic Parser:** Recognizes merchant tax headers, total amounts (`FIZETENDŐ`, `ÖSSZESEN`, `VÉGÖSSZEG`), VAT percentages, and payment methods (`KÉSZPÉNZ`, `BANKKÁRTYA`).
- **Cash Wallet Automation:** Cash transactions automatically debit your local "Cash Wallet" balance rather than bank accounts.
- **Zero Cloud Upload:** Optical character recognition runs 100% on the Android device hardware.

### 3. Interactive Analytics & Hardware-Accelerated Charts
- **Canvas Donut Chart:** Smooth, hardware-accelerated Compose Canvas chart with deterministic category color palette (`CategoryColorProvider`).
- **Touch-to-Inspect:** Tap any slice on the chart or row in the breakdown to isolate category metrics, totals, percentages, and merchant transaction counts.
- **Period Engine:** Filter by *This Month, Last Month, Last 3 Months, Last 6 Months, This Year, Last Year*, or a *Custom Date Range* (with strict start-before-end validation).
- **Merchant Leaderboard:** Top 5 merchant ranking calculated dynamically for any selected period.

### 4. Dynamic Category & Keyword Management
- **Full Custom Categories:** Create custom categories with custom color chips.
- **Interactive Keywords:** Add, view, or remove rule keywords directly as chips.
- **User Priority Override:** Custom user keywords take precedence over built-in system rules.
- **Safe Category Deletion:** If a category contains assigned transactions, the app prevents orphan records by requiring a destination category reassignment before deletion.

### 5. Algorithmic Recurring Expense Detector
- Analyzes transaction histories across merchants, cadence intervals (28–32 days), and amount variance ($\le 10\%$) to detect subscriptions (Netflix, Spotify, gym memberships, utility bills) automatically.
- No fake demo records: cleanly separates detected subscriptions from a clearly marked sample card when no recurring data exists yet.

---

## 📱 App UI Overview

```text
 ┌───────────────────────────┐      ┌───────────────────────────┐
 │ Finances       2026.09    │      │ Analytics         Period  │
 ├───────────────────────────┤      ├───────────────────────────┤
 │ Total Balance             │      │  [This Month] [3M] [Year] │
 │ 420 500 Ft                │      │                           │
 │                           │      │          ╭─────╮          │
 │ +280 000 Ft   -145 000 Ft │      │        ╭─╯     ╰─╮        │
 │ Income        Expense     │      │        │  145k Ft│        │
 ├───────────────────────────┤      │        ╰─╮     ╭─╯        │
 │ Quick Actions             │      │          ╰─────╯          │
 │ [📷 Scan] [📂 CSV] [🔔 Auto]│     │                           │
 ├───────────────────────────┤      │ Food & Groceries   42.5%  │
 │ Recent Transactions       │      │ Dining Out         18.2%  │
 │ 🛒 SPAR Budapest  -4 500Ft│      │ Transport & Fuel   12.0%  │
 │ 🍔 Wolt Courier   -3 200Ft│      │ Subscriptions       8.4%  │
 ├───────────────────────────┤      ├───────────────────────────┤
 │ [Home] [Tx] [Charts] [More│      │ [Home] [Tx] [Charts] [More│
 └───────────────────────────┘      └───────────────────────────┘
```

---

## 🗂️ Project Structure

```text
finances/
├── finances-native.apk              # Install-ready native Android APK (built in root)
├── NATIVE_APP_GUIDE.md              # Android phone installation & quick-start guide
├── android-native/                  # 100% Native Android Application
│   ├── app/
│   │   ├── src/main/java/com/sajatpenzugyek/app/
│   │   │   ├── core/                # Theme, Currency & Date Formatter, Text Normalizer
│   │   │   ├── data/                # Room SQLite Entities, DAOs, Repositories
│   │   │   ├── domain/              # UseCases (Ingest, Dedup, Recurring, Stats)
│   │   │   ├── feature/             # Jetpack Compose Screens & ViewModels
│   │   │   │   ├── dashboard/       # Main Overview & Financial Metrics
│   │   │   │   ├── transactions/    # Filtered & Sorted Transaction List
│   │   │   │   ├── analytics/       # Interactive Donut Chart & Cash Flow Trends
│   │   │   │   ├── categories/      # Category & Keyword Rules Management
│   │   │   │   ├── receipt/         # CameraX + ML Kit OCR Scanner
│   │   │   │   ├── notifications/   # Bank Notification Automation & Test Lab
│   │   │   │   ├── importcsv/       # Bank Statement CSV Importer
│   │   │   │   └── settings/        # Privacy Manifesto, Theme, PIN Lock
│   │   │   ├── native/              # NotificationListenerService & Bank Parsers
│   │   │   ├── FinancesApp.kt       # Application class (English locale enforcement)
│   │   │   └── MainActivity.kt      # Edge-to-edge Compose host activity
│   │   ├── src/test/java/           # Comprehensive Unit Test Suite
│   │   └── build.gradle.kts         # App dependencies (Compose BOM, Room, ML Kit)
│   ├── gradlew                      # Gradle Wrapper script (Linux/macOS)
│   ├── gradlew.bat                  # Gradle Wrapper script (Windows)
│   └── settings.gradle.kts
│
├── src/                             # Original Web Dashboard (Reference Prototype)
│   ├── components/                  # React UI components
│   ├── core/                        # TypeScript domain logic
│   └── store/                       # Zustand state stores
│
├── docs/                            # Architectural & Integration Documentation
│   ├── ARCHITECTURE.md              # In-depth system design & pipelines
│   ├── BANK_INTEGRATION.md          # Supported bank regex patterns
│   └── SECURITY.md                  # Local storage & threat model
│
├── .gitignore                       # Clean exclusion of build artifacts & secrets
└── README.md
```

---

## 🛠️ Building & Running

### Requirements
- **JDK 17**
- **Android SDK 34** (Platform & Build Tools)
- *(Optional for web prototype)*: Node.js 20+

### 1. Building the Native Android App (Release APK)

Open a terminal in `android-native/`:

```bash
# On Linux / macOS:
./gradlew assembleRelease

# On Windows:
.\gradlew.bat assembleRelease
```

The compiled and signed APK will be output to:
```text
android-native/app/build/outputs/apk/release/app-release.apk
```

### 2. Running Unit Tests

The test suite verifies financial math edge cases, regex parsers, recurring detection algorithms, and deduplication logic:

```bash
# On Linux / macOS:
./gradlew test

# On Windows:
.\gradlew.bat test
```

### 3. Running the Original Web Dashboard (Prototype)

If you want to run the desktop web prototype:

```bash
npm install
npm run dev
```

Visit `http://localhost:5173` in your browser.

---

## 🔒 Security & Privacy Manifesto

- **No Network Permissions for Telemetry:** The app makes zero external network requests for analytics or tracking.
- **On-Device OCR:** Receipt photos are processed locally through Google ML Kit on-device models. Photos can be deleted immediately after OCR extraction while keeping the transaction record.
- **Zero Credential Access:** The app never asks for bank usernames, passwords, API tokens, or card numbers. It observes only standard system notifications created by your official bank apps.
- **App Lock:** Optional PIN code protection with encrypted salt hashing.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — free to use, modify, and build upon.
