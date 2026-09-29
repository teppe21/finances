# Bank Notification Integration Guide

## 1. Architectural Strategy: Push Notifications vs. Open Banking (PSD2)

| Dimension | Android Notification Listener (`BankNotificationListenerService`) | Open Banking (PSD2 / AISP) |
|---|---|---|
| **Mechanism** | Android `NotificationListenerService` API | OAuth 2.0 redirected authentication to bank API |
| **Latency** | **Instantaneous** (captured within milliseconds of the purchase) | Periodic sync (typically 4x daily, or manual pull) |
| **Server Footprint** | **Zero backend server required** (100% on-device) | Requires licensed cloud server, QWAC/QSEAL eIDAS certificates |
| **Privacy Guarantee** | Air-gapped on device; zero intermediary visibility | Aggregator servers inspect and store statement payloads |
| **User Barrier** | Enable system permission once in Android settings | Re-authenticate every 90–180 days via bank login portal |
| **Availability** | Works immediately with any bank push alert | Restricted to participating Open Banking institutions |

---

## 2. Supported Banking Institutions & Packages

The native notification engine uses specialized parsers implementing `BankNotificationParser`:

| Bank | Package IDs | Typical Notification Patterns | Extracted Fields |
|---|---|---|---|
| **OTP Bank** | `hu.otpbank.smartbank`<br/>`hu.otpbank.mobilbank`<br/>`hu.otpbank.simple` | `Sikeres kártyás vásárlás: -4.500 Ft. Hely: McDonald's. Kártya: *1234` | Amount, Currency, Merchant, Card Last 4, Direction |
| **Revolut** | `com.revolut.revolut` | `Fizetés a következőnek: LIDL. Elköltöttél 14 500 Ft-ot itt: LIDL.`<br/>`You spent 45.50 EUR at GitHub.` | Amount, Currency, Merchant, Direction |
| **Erste Bank** | `hu.erstebank.george.hungary`<br/>`hu.erstebank.mobilebank` | `Sikeres kártyás fizetés George: 8.990 Ft értékben.` | Amount, Currency, Direction, Timestamp |
| **MBH Bank** | `hu.takarek.mobilbank`<br/>`hu.mbhbank.app` | `Kártyás tranzakció: -12.450 Ft` | Amount, Currency, Direction, Timestamp |
| **Wise** | `com.transferwise.android` | `You paid 45.50 EUR to GitHub` | Amount, Currency, Merchant, Direction |
| **Generic Bank** | Any Hungarian / European banking app | Fallback parser detecting Hungarian transactional verbs (`vásárlás`, `terhelés`, `fizetés`) | Amount, Currency, Direction |

---

## 3. Account Matching & Confidence Tiers

To prevent misassigning transactions across different bank accounts (e.g., assigning a Revolut purchase to an OTP account), the service evaluates match confidence across 3 distinct tiers:

```mermaid
flowchart TD
    Raw[Raw Bank Notification] --> Parser[Bank Notification Parser]
    Parser --> Matcher{Account Matching Engine}

    Matcher -->|Explicit User Mapping in Settings| Tier1[Tier 1: HIGH Confidence (0.95)<br/>Assigns to chosen account]
    Matcher -->|Exact Institution / IBAN Match| Tier1
    Matcher -->|Substring / Fuzzy Name Match| Tier2[Tier 2: MEDIUM Confidence (0.80)<br/>Assigns to fuzzy account]
    Matcher -->|No Match Found| Tier3[Tier 3: LOW Confidence (0.40)<br/>Flags for User Review]

    Tier1 --> Ingest[Ingestion Pipeline]
    Tier2 --> Ingest
    Tier3 --> IngestReview[Ingest as Pending: Needs Account Review]
```

### User Bank-to-Account Mapping
In the **Notification Automation Screen**, users can explicitly map any supported bank (e.g. `OTP Bank`, `Revolut`, `Erste Bank`, `MBH Bank`, `Wise`) to their created account. User mappings are persisted in Jetpack DataStore and always take precedence over automated name guessing.

---

## 4. Security & Safety Filters

### Automatic 2FA / Security Code Filtering
The parser pipeline immediately discards any incoming alert that contains security tokens, one-time passwords (OTP), or two-factor authentication strings:
- Hungarian triggers: `belépési kód`, `SMS kód`, `biztonsági kód`, `jóváhagyás`
- English triggers: `security code`, `verification code`, `one-time password`, `login attempt`
These notifications are dropped in memory and are never inserted into the database.

---

## 5. Platform Limitations & OEM Configuration

### Android System Permissions
The `NotificationListenerService` requires the user to grant special notification access via Android System Settings. The app provides a direct shortcut and a prominent privacy disclosure dialog.

### Xiaomi / Redmi / POCO (MIUI / HyperOS) Special Settings
Aggressive vendor battery savers may kill background services if not configured:
1. **Autostart**: Navigate to *Settings $\rightarrow$ Apps $\rightarrow$ Manage Apps $\rightarrow$ Finances* and toggle **Autostart** ON.
2. **Battery Saver**: Set Battery Saver to **No restrictions** (*Nincs korlátozás*).
3. **Notification Access**: In *Settings $\rightarrow$ Privacy $\rightarrow$ Special app access $\rightarrow$ Notification access*, ensure **Finances** is enabled.

### Samsung One UI / Android 14+ Battery Optimization
1. Ensure **Finances** is added to **Never sleeping apps** (*Soha nem alvó alkalmazások*) in *Device Care $\rightarrow$ Battery*.
