# Data Model & SQLite Schema

This document details the database schema and entity models powering the local-first storage architecture.

---

## 1. Relational Schema Diagram

```mermaid
erDiagram
    ACCOUNTS ||--o{ TRANSACTIONS : contains
    CATEGORIES ||--o{ TRANSACTIONS : categorizes
    CATEGORIES ||--o{ CATEGORY_RULES : defines
    CATEGORIES ||--o{ BUDGETS : targets
    RECEIPT_SCANS ||--o| TRANSACTIONS : generates
    NOTIFICATION_EVENTS ||--o| TRANSACTIONS : generates
    NOTIFICATION_SOURCES ||--o{ NOTIFICATION_EVENTS : filters

    ACCOUNTS {
        string id PK
        string name
        string institution
        string type
        string currency
        int openingBalanceMinor
        int isActive
        string createdAt
        string updatedAt
    }

    TRANSACTIONS {
        string id PK
        string accountId FK
        string date
        string valueDate
        int amountMinor
        string currency
        string direction
        string description
        string merchant
        string categoryId FK
        string source
        string sourceAppPackage
        string externalId
        string fingerprint
        string receiptId FK
        string notificationEventId FK
        string recurringRuleId
        string transferId
        string notes
        int pending
        real confidence
        string importedAt
        string createdAt
        string updatedAt
    }

    CATEGORIES {
        string id PK
        string name
        string icon
        string color
        int isIncome
        int isDefault
        string createdAt
        string updatedAt
    }

    CATEGORY_RULES {
        string id PK
        string categoryId FK
        string pattern
        string matchType
        int priority
        int isActive
        string createdAt
    }

    RECEIPT_SCANS {
        string id PK
        string imageUri
        string scannedAt
        string merchant
        string date
        int totalMinor
        int subtotalMinor
        int taxMinor
        string currency
        string paymentMethod
        string itemsJson
        int rawOcrAvailable
        string rawOcrText
        real confidence
        int processedLocally
        string transactionId
    }

    NOTIFICATION_SOURCES {
        string packageName PK
        string displayName
        int enabled
        string bankProfileId
        string createdAt
        string updatedAt
    }

    NOTIFICATION_EVENTS {
        string id PK
        string packageName
        string applicationLabel
        string title
        string text
        string bigText
        string subText
        string postedAt
        string notificationKey
        string sourceBankProfile
        int processed
        string parseStatus
        string transactionId
        string fingerprint
        string failureReason
    }

    BUDGETS {
        string id PK
        string categoryId FK
        int amountMinor
        string period
        string startDate
        string endDate
        string notes
    }

    SAVED_PERIODS {
        string id PK
        string periodKey
        string name
        string savedAt
        int transactionCount
        int totalIncomeMinor
        int totalExpenseMinor
        int balanceMinor
        string dataJson
    }
```

---

## 2. Integer Minor Currency Units
All monetary amounts are represented as 64-bit integer minor currency units (`amountMinor`):
- **HUF**: 1 HUF = 100 minor units (fillér). E.g. `14 500 HUF` is stored as `1450000`.
- **EUR**: 1 EUR = 100 minor units (cents). E.g. `12.50 EUR` is stored as `1250`.
- **USD**: 1 USD = 100 minor units (cents). E.g. `99.00 USD` is stored as `9900`.

This integer arithmetic model prevents floating-point cumulative rounding errors common in JavaScript and financial spreadsheets.

---

## 3. Account Entity & The Cash Model
Cash is a **first-class account** (`type: 'cash'`) rather than an afterthought:
- Receipt scans with detected cash payment (`KÉSZPÉNZ`) automatically map to the Cash account.
- ATM cash withdrawals from bank accounts create a transfer (`acc_otp` -> `acc_cash`) preserving net worth without double-counting expenses.

---

## 4. Stable Category Identifiers
Category identifiers are canonical and language-independent:
- `food`: Groceries / Élelmiszer
- `dining`: Restaurants & Fast Food / Étkezés & Vendéglátás
- `transport`: Fuel & Public Transit / Tankolás & Közlekedés
- `subscriptions`: Subscriptions & Gaming / Előfizetések & Játék
- `housing`: Utilities & Rent / Rezsi & Szolgáltatás
- `entertainment`: Leisure & Culture / Szórakozás & Szabadidő
- `savings`: Transfers & Savings / Utalás & Megtakarítás
- `income`: Salaries & Income / Fizetés & Bevétel
- `health`: Healthcare & Pharmacy / Egészség & Gyógyszertár
- `shopping`: Clothing & Retail / Bevásárlás & Ruházat
- `other`: Uncategorized / Egyéb

---

## 5. Provenance & Auditability
Every transaction explicitly records its origin:
- `source`: `'notification' | 'receipt' | 'csv' | 'manual' | 'open_banking' | 'backup'`
- `sourceAppPackage`: Package ID of the bank application (e.g., `com.revolut.revolut`).
- `notificationEventId`: Foreign key to `notification_events` record.
- `receiptId`: Foreign key to `receipt_scans` record.
- `fingerprint`: Deterministic hash for duplicate detection.
