# Finances

A native Android personal finance app built with Kotlin and Jetpack Compose.

Finances is a personal project focused on making everyday expense tracking less manual. Instead of requiring every transaction to be entered by hand, the app can use Android notification access, receipt scanning, CSV imports, and automated categorization to reduce the friction of personal bookkeeping.

The project is built on a local-first architecture. Transaction records, account balances, categorization rules, and scanned receipts remain stored on the physical device rather than being sent to a remote personal finance service.

---

## Overview

Most mobile expense trackers fall into two categories: simple spreadsheet-like tools that require typing in every coffee and grocery receipt, or cloud aggregators that require sharing bank login credentials with third-party servers.

Finances looks for a practical middle ground on Android. By taking advantage of platform APIs such as `NotificationListenerService` and on-device text recognition, the app can capture expenses as they occur:

- Transactions can be created from supported bank and payment push notifications.
- Physical paper receipts can be scanned with the camera using on-device optical character recognition (OCR).
- Historical bank statements can be imported using standard CSV files.
- Manual entry is available as a straightforward fallback.

All inputs feed into a single transaction pipeline that cleans merchant names, checks for duplicates, applies category rules, and writes to an on-device Room SQLite database.

---

## Main Features

### Automatic transaction recognition

When enabled, the app listens for push notifications from supported banking and wallet applications:
- Revolut
- Google Wallet / Google Pay
- OTP Bank (SmartBank, MobilBank, Simple)
- Erste Bank (George)
- MBH Bank
- Wise

Rather than treating any notification that happens to contain numbers as a financial transaction, the parser validates notifications against transaction-specific semantics (such as card payments, purchases, transfers, and ATM withdrawals).

Promotional alerts (such as referral rewards or marketing campaigns) and security messages (such as two-factor authentication codes and one-time passwords) are detected and discarded. When a physical purchase triggers alerts from both a digital wallet (like Google Wallet) and the underlying card issuer (like Revolut), the engine correlates the two events within a short time window to avoid recording the expense twice.

### Receipt scanning

For cash purchases or paper receipts, the app includes a camera-based scanner built with CameraX and Google ML Kit's on-device text recognition.

The OCR engine processes frames locally to identify merchant names, dates, amounts, and VAT details. It is tuned for common European and Hungarian receipt layouts, helping convert printed slips into transactions without manual typing. Like any vision-based OCR system, recognition quality depends on lighting and print clarity, but it serves as a practical shortcut for logging physical expenses.

### Automatic categorization

The categorization engine applies a combination of built-in keyword patterns and custom user rules.

Incoming transactions are evaluated against merchant names and descriptions:
- Common merchants and services are automatically mapped to default categories (groceries, dining, transport, utilities, subscriptions).
- Internal transfers and peer-to-peer payments (such as Revolut, Wise, or Hungarian bank transfers) are flagged as transfers rather than regular expenses.
- Custom rules created in the app take priority over built-in heuristics, allowing users to override default behavior for specific stores or recurring payments.

### Multi-currency support

Financial values are stored in the database as 64-bit integers representing minor units (cents or fillér) alongside their original currency code. Storing whole integers avoids the rounding drift common with floating-point calculations.

Users can choose a preferred display currency (such as HUF, EUR, USD, GBP, or CHF). When viewing balances and analytics, amounts in other currencies are converted using cached exchange rates rather than simply replacing currency symbols.

Exchange rates are cached locally in Room. By default, the app can refresh European Central Bank reference rates over an anonymous public API, or it can be switched to an entirely offline mode where only cached or manually entered rates are used.

### Analytics

The analytics section provides visual summaries of spending patterns over customizable time periods (current month, previous month, or custom ranges).

It includes a breakdown of expenses by category, monthly cash flow trends, and a merchant leaderboard showing where the largest portions of the budget are spent.

### CSV import

The app includes a dedicated CSV parser for importing past account statements from supported banks (such as OTP Bank, Revolut, Erste, MBH, and Wise).

The parser detects common European CSV variants, handles character encodings (including UTF-8 with BOM and ISO-8859-2), normalizes European number formats with comma decimals, and passes rows through the duplicate detection pipeline to avoid re-importing existing entries.

### Duplicate detection

Duplicate prevention runs at the ingestion boundary:
- Deterministic fingerprinting: Generates a hash based on transaction date, amount, currency, and cleaned merchant information.
- Short-window cross-source matching: Detects identical purchases reported simultaneously by payment wallets and card issuers within a 120-second window.
- Sliding date window: Checks for possible duplicates within a two-day window to catch delayed postings, while preserving distinct transactions that share identical amounts at different merchants.

### Demo data

Clean installations initialize with zero balances across default accounts. For evaluation and demonstration purposes, a dedicated toggle in Settings can populate representative sample accounts, transactions, and categories, or wipe them back to zero.

---

## Privacy

Privacy is a core architectural priority of the project.

- Local-first persistence: Accounts, transactions, notes, and receipts are stored in an on-device SQLite database. The project does not use a cloud database or personal finance server.
- Sanitized notification metadata: Raw notification titles, notification text, and subtext are not stored in the database. The system records only structured metadata (timestamp, package name, amount, currency, merchant, and diagnostic reason codes) needed for ledger management.
- Local OCR: Camera frames and receipt images are processed directly on the phone using ML Kit's on-device models. Photos and text snippets are not uploaded to cloud vision APIs.
- Anonymous exchange rates: Network requests for exchange rates are simple HTTP GET queries for public currency pairs. They contain no user identifiers, transaction data, account names, or device tokens.
- Offline mode: If the user disables exchange rate updates in Settings, the application makes no network calls of any kind.
- No analytics SDKs: The application contains no advertising libraries, no tracking identifiers, and no behavioral analytics tools.
- Data deletion: A one-click data wipe option in Settings allows users to permanently delete all financial records and reset the database to an empty state.

For complete details on permissions and data handling, see [docs/privacy-policy.html](docs/privacy-policy.html) and [docs/PRIVACY.md](docs/PRIVACY.md).

---

## Security

Security measures in the project focus on local access control and safe data handling:

- App lock: An optional security gate can be configured to protect the application on launch and when returning from the background.
- Biometric authentication: Supports fingerprint and face unlock through AndroidX `BiometricPrompt`, relying on the device's hardware-backed keystore.
- Salted PIN verification: When a 4-digit PIN is configured, it is stored as a SHA-256 hash combined with a randomly generated 16-character salt. Plaintext PINs are never stored.
- Process isolation: Application data resides in Android's private internal storage (`/data/user/0/com.teppe21.finances`), isolated by Android OS Linux UID permissions and standard device file-based encryption (FBE).
- Release signing enforcement: The Gradle release configuration requires explicit signing credentials and deliberately refuses to fall back to debug keys for release builds.

Technical details and threat model considerations are documented in [docs/SECURITY.md](docs/SECURITY.md).

---

## Technology

The application is written in Kotlin for the Android platform:

- Language: Kotlin 1.9.22
- UI Toolkit: Jetpack Compose with Material 3
- Architecture: MVVM with StateFlow and Kotlin Coroutines
- Local Database: Room 2.6.1 (SQLite) with tracked schema exports (Version 4)
- Camera & Vision: CameraX 1.3.2 with Google ML Kit On-Device Text Recognition
- Preferences: Jetpack DataStore (Preferences)
- System Integration: Android `NotificationListenerService` and `BiometricPrompt`
- Build System: Gradle 8.9 with Android Gradle Plugin 8.7.2
- Target Platform: Minimum SDK 26 (Android 8.0), Target SDK 36 (Android 16), Compile SDK 36

---

## Architecture

The codebase is organized into layered packages within `android-native/`:

- `core/`: Application theme, formatters (currency, dates), and text normalization utilities.
- `data/`: Room SQLite database entities, DAOs, repositories, and preferences DataStore.
- `domain/`: Business models and use cases (transaction ingestion, deduplication, categorization, recurring pattern detection, financial statistics).
- `feature/`: Jetpack Compose screens and ViewModels (dashboard, transactions, accounts, analytics, categories, receipt scanner, notifications, settings).
- `native/`: Android platform services, including the notification listener, bank parser implementations, and receipt OCR analyzers.

Data flows through unidirectional streams: UI components collect state from ViewModels using `StateFlow`, while repository updates emit reactive updates from Room queries.

---

## Notification Reliability

Handling real-world Android notifications reliably requires balancing capture accuracy with noise filtering.

During development, real device testing revealed edge cases where standard pattern matching fails:
- Bank notifications that combine transaction amounts and account balances in the same message (e.g. `You spent HUF804` followed by `HUF balance: HUF42,285.97`). The parser isolates the transaction line so the account balance is not ingested as an expense.
- Marketing alerts that mention monetary values (e.g. referral campaigns offering cash rewards). The parser uses negative pattern matching and requires positive transaction semantics before accepting a candidate.
- Payment apps and banking apps alerting on the same purchase. When a card is charged via Google Wallet, both Google Wallet and the card issuer often fire alerts within seconds. Cross-source temporal deduplication identifies these pairs and prevents recording double expenses.
- Security and authentication messages. Login codes, OTPs, and confirmation requests are filtered out before reaching the parser.

---

## Testing

The project contains 39 automated unit and regression tests covering core business logic and data integrity:

- Parser validation: Verifies parsing of genuine transactions, rejection of promotional messages, and suppression of security codes across supported bank formats.
- Deduplication logic: Tests exact fingerprint matches, fuzzy date window matching, and cross-source payment deduplication.
- Database migrations: Validates sequential schema migrations (`MIGRATION_1_2`, `MIGRATION_2_3`, and `MIGRATION_3_4`) and schema exports.
- Mathematical accuracy: Tests integer minor-unit conversions, balance calculations, and multi-currency exchange rate conversions.
- Security verification: Tests salted PIN hashing, salt uniqueness, and verification checks.

Automated tests can be executed with Gradle:

```bash
cd android-native
./gradlew testDebugUnitTest
```

While unit tests verify parser regexes and algorithmic logic, they cannot fully simulate OEM background limits, battery management, or notification listener lifecycle quirks. For that reason, automated testing is complemented by hands-on physical device testing.

---

## Google Play Preparation

The application is being prepared for a future Google Play release. Hardening steps completed so far include:

- Clean package namespace: `com.teppe21.finances`.
- Modern platform targeting: Target SDK 36 (Android 16) and Compile SDK 36.
- Strict release signing: Build tasks fail with clear error messages if signing keystores are missing, preventing unverified release artifacts.
- ProGuard / R8 rules: Code shrinking, resource shrinking, and obfuscation configured for release builds.
- Android Auto Backup: XML configuration rules to include database and settings while excluding temporary cache and camera images.
- Store documentation: Prepared Data Safety responses, sensitive permission justifications (`BIND_NOTIFICATION_LISTENER_SERVICE`, `CAMERA`), and financial services declarations.

See [docs/PLAY_CONSOLE_COMPLIANCE.md](docs/PLAY_CONSOLE_COMPLIANCE.md) and [docs/PLAY_FINANCIAL_FEATURES_DECLARATION.md](docs/PLAY_FINANCIAL_FEATURES_DECLARATION.md) for details.

---

## Project Structure

```text
finances/
├── finances-native.apk              # Installable debug APK for local hardware testing
├── NATIVE_APP_GUIDE.md              # Sideloading and device installation guide
├── android-native/                  # Primary native Android application
│   ├── app/
│   │   ├── src/main/java/com/teppe21/finances/
│   │   │   ├── core/                # Theme, formatters, text normalization
│   │   │   ├── data/                # Room entities, DAOs, repositories, DataStore
│   │   │   ├── domain/              # Use cases (ingest, dedup, recurring, stats)
│   │   │   ├── feature/             # Jetpack Compose screens and ViewModels
│   │   │   ├── native/              # NotificationListenerService, bank parsers, OCR
│   │   │   ├── FinancesApp.kt       # Application class
│   │   │   └── MainActivity.kt      # FragmentActivity with LockGateScreen and navigation
│   │   ├── src/test/java/           # Unit and regression test suite (39 tests)
│   │   ├── schemas/                 # Exported Room database schemas (v1, v2, v3, v4)
│   │   └── build.gradle.kts         # Target SDK 36, signing configuration, dependencies
│   ├── gradlew                      # Gradle wrapper script
│   └── settings.gradle.kts
│
├── docs/                            # Architecture, security, and compliance documentation
│   ├── privacy-policy.html          # Public standalone HTML privacy policy
│   ├── REAL_DEVICE_TEST_PLAN.md     # Real-device testing protocol
│   ├── PLAY_CONSOLE_COMPLIANCE.md   # Google Play permissions and Data Safety answers
│   ├── PLAY_FINANCIAL_FEATURES_DECLARATION.md # Financial category declaration
│   ├── ARCHITECTURE.md              # System architecture and pipeline breakdown
│   ├── SECURITY.md                  # Storage sandbox and threat model
│   ├── BANK_INTEGRATION.md          # Bank packages, regex patterns, and OEM notes
│   ├── DATA_MODEL.md                # Room schema, converters, and entity relationships
│   ├── RELEASE.md                   # Keystore configuration and release procedures
│   ├── TESTING.md                   # Testing approach and test structure
│   └── PRIVACY.md                   # Data protection and air-gap principles
│
├── src/                             # Original React prototype (preserved for reference)
├── .gitignore
└── README.md
```

---

## Development

### Prerequisites

- Java Development Kit: JDK 17
- Android SDK: Platform API 36 with Build Tools 36.0.0
- Gradle: 8.9 (handled by included wrapper)

### Build Commands

From the `android-native/` directory:

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

Build debug APK:
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Build release APK and Android App Bundle (requires signing environment variables or `local.properties`):
```bash
./gradlew assembleRelease bundleRelease
# Outputs:
# app/build/outputs/apk/release/app-release.apk
# app/build/outputs/bundle/release/app-release.aab
```

Detailed signing setup instructions can be found in [docs/RELEASE.md](docs/RELEASE.md).

---

## Current Status

The core features of the native Android application are fully functional: transaction tracking, bank notification listening, receipt scanning, CSV imports, multi-currency conversion, and app locking are all working in code.

The project is currently in an extended testing phase. Rather than publishing immediately, the application is being used on a daily basis on a physical Android device to verify reliability in real-world conditions.

---

## Real Device Testing

Real-device testing is planned for approximately one month before submitting the application to Google Play.

This phase focuses on scenarios that cannot be adequately evaluated in emulators or unit tests:
- Behavior across varied retail environments (grocery stores, restaurants, transit, online purchases).
- Consistency of notification interception when the phone is locked, in battery-saver mode, or after device restarts.
- Stability of cross-source deduplication when paying with Google Wallet.
- Receipt OCR readability on crumpled, faded, or poorly lit physical receipts.
- Lock gate behavior across long backgrounding intervals.
- Battery impact of background notification listening over multiple weeks.

The testing protocol and progress checklist are tracked in [docs/REAL_DEVICE_TEST_PLAN.md](docs/REAL_DEVICE_TEST_PLAN.md).

---

## Project Background

The project began as an experiment in building a personal finance dashboard with web technologies. The repository's root and `src/` directories still contain an earlier React and Tailwind prototype that was used to model data schemas, CSV parsing logic, and categorization rules.

However, tracking personal expenses on a desktop computer has obvious limitations: receipts pile up, cash transactions are forgotten, and manual entry becomes a chore. Attempting to bring the web app to mobile using hybrid wrappers highlighted typical cross-platform trade-offs: high memory usage, sluggish UI rendering, and poor integration with Android system services.

To solve the mobile logging problem properly, the project was rewritten as a native Android application using Kotlin and Jetpack Compose. This made it possible to integrate directly with CameraX, Google ML Kit, Android's `NotificationListenerService`, and biometric hardware.

---

## What I Wanted to Build

When starting this project, I had a clear set of goals:

1. Reduce manual entry: Most people abandon expense trackers because typing in every transaction is tedious. I wanted an app that could capture purchases automatically from notifications and paper receipts.
2. Keep data private: Financial data is personal. I wanted a system that works locally without requiring account creation, cloud logins, or third-party bank aggregators.
3. Accurate multi-currency handling: Living and traveling in Europe often involves multiple currencies (HUF, EUR, USD, etc.). I wanted proper exchange-rate conversions rather than simply changing currency symbols on screen.
4. Clean native engineering: I wanted to build a modern Android application using current recommended architecture, idiomatic Kotlin, Jetpack Compose, Room SQLite, and proper testing practices.

---

## Documentation

Comprehensive technical documentation is maintained in the `docs/` directory:

- [Privacy Policy (HTML)](docs/privacy-policy.html): Standalone HTML document for users and store compliance.
- [30-Day Real Device Test Plan](docs/REAL_DEVICE_TEST_PLAN.md): Protocol for physical hardware testing.
- [Google Play Console Compliance](docs/PLAY_CONSOLE_COMPLIANCE.md): Data Safety form answers and permission declarations.
- [Financial Features Declaration](docs/PLAY_FINANCIAL_FEATURES_DECLARATION.md): Regulatory positioning and feature scope.
- [Architecture](docs/ARCHITECTURE.md): Pipeline design, ingestion flow, and component breakdown.
- [Security](docs/SECURITY.md): Storage sandbox, threat model, and cryptographic practices.
- [Bank Integration](docs/BANK_INTEGRATION.md): Supported bank packages, parsing patterns, and OEM notes.
- [Data Model](docs/DATA_MODEL.md): Room schema, converters, and entity relationships.
- [Release Guide](docs/RELEASE.md): Keystore generation, signing verification, and build commands.
- [Testing](docs/TESTING.md): Unit testing philosophy and test suite organization.
- [Privacy](docs/PRIVACY.md): Air-gap principles, backup rules, and data handling details.
- [Device Sideloading Guide](NATIVE_APP_GUIDE.md): Practical steps for installing and running the APK on your phone.

---

## License

This project is licensed under the [MIT License](LICENSE) — feel free to inspect the code, adapt it, or build upon it.
