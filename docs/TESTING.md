# Testing Strategy & Verification Suite

## 1. Overview & Test Philosophy

Testing in **Finances** emphasizes absolute reliability in financial math, strict offline privacy boundaries, robust text parsing, and data integrity across database migrations.

The native Android test suite is located in `android-native/app/src/test/java/com/teppe21/finances/` and executes directly on the JVM without requiring an Android emulator or physical device.

---

## 2. Test Suite Structure

| Test Class | Focus Area | Key Scenarios Tested |
|---|---|---|
| `FinancialMathTest` | Financial Arithmetic & Minor Units | - Exact integer minor-unit math (no floating-point errors)<br/>- Monthly income/expense/net total calculations<br/>- Savings rate percentage calculations<br/>- Handling zero income/expense without divide-by-zero crashes |
| `CurrencyConversionTest` | Multi-Currency & FX Engine | - Conversion across HUF, EUR, USD, GBP, CHF<br/>- Original amount and currency preservation<br/>- Minor unit conversions with accurate decimal rounding<br/>- Fallback rate recovery when offline |
| `CategorizationAndDedupTest` | Ingestion Logic & Rules | - Keyword rule matching against normalized merchant names<br/>- Automatic transfer and refund direction detection<br/>- Uncategorized fallback handling |
| `ParsersTest` | On-Device Ingestion Parsers | - Hungarian supermarket OCR receipt parsing (Lidl, Spar)<br/>- Revolut Hungarian and English purchase notifications<br/>- OTP Bank card purchase alerts and card last-4 extraction<br/>- Erste Bank George transaction notifications<br/>- MBH Bank transaction notifications<br/>- Wise multi-currency notifications (`45.50 EUR`)<br/>- Immediate discard of 2FA and SMS security codes<br/>- Multi-column Hungarian and European CSV bank statements |
| `DatabaseAndMatchingTest` | Deduplication & Room Database | - Exact SHA-256 fingerprint collision prevention<br/>- 2-day fuzzy duplicate detection window<br/>- Multi-tier account matching confidence (HIGH, MEDIUM, LOW)<br/>- Verification of Room migrations `1 -> 2` and `2 -> 3` |

---

## 3. Running Tests Locally

From `android-native/`:

```bash
# Run all unit tests for debug and release variants
./gradlew test

# Run a specific test class
./gradlew test --tests "com.teppe21.finances.ParsersTest"

# Run tests and open HTML report
./gradlew test
# Report located at: app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 4. Continuous Integration (CI/CD)

Automated testing is configured in `.github/workflows/android.yml`:
- **Triggers**: Every pull request and push to the `main` branch.
- **Environment**: Ubuntu Latest with Eclipse Temurin JDK 17.
- **Pipeline Stages**:
  1. Checkout repository
  2. Setup JDK 17 with Gradle dependency caching
  3. Execute `./gradlew test --stacktrace`
  4. Compile `./gradlew assembleDebug`
  5. Compile `./gradlew bundleRelease assembleRelease`
  6. Upload test reports and release artifacts
