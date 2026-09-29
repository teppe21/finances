# Data Model & Room SQLite Schema

## 1. Overview & Room Architecture

The application persistence layer is implemented via Android Jetpack Room (`AppDatabase`, version 3). The schema is formally exported to JSON schemas at:
`android-native/app/schemas/com.teppe21.finances.data.local.database.AppDatabase/3.json`

### Core Schema Principles:
1. **Integer Minor Units**: All monetary values (`amountMinor`, `openingBalanceMinor`, `currentBalanceMinor`, `monthlyLimitMinor`, `taxMinor`) are stored as 64-bit integers (`Long`), representing the currency's smallest sub-unit (cents, fillérs).
2. **Deterministic Fingerprints**: Transactions have a unique cryptographic fingerprint preventing duplicate entries.
3. **Optimized B-Tree Indices**: Composite and single-column indices support constant-time deduplication and responsive dashboard rendering.

---

## 2. Entity-Relationship Model

```mermaid
erDiagram
    ACCOUNTS ||--o{ TRANSACTIONS : contains
    CATEGORIES ||--o{ TRANSACTIONS : categorizes
    CATEGORIES ||--o{ CATEGORY_RULES : defines
    CATEGORIES ||--o{ BUDGETS : targets
    RECEIPTS ||--o| TRANSACTIONS : generates
    NOTIFICATION_EVENTS ||--o| TRANSACTIONS : generates

    ACCOUNTS {
        string id PK
        string name
        string institution
        string type
        string currency
        int64 openingBalanceMinor
        int64 currentBalanceMinor
        boolean isActive
        int64 createdAt
        int64 updatedAt
    }

    TRANSACTIONS {
        string id PK
        string accountId
        string date
        int64 valueDate
        int64 amountMinor
        string currency
        string direction
        string description
        string merchant
        string categoryId
        string source
        string sourceAppPackage
        string externalId
        string fingerprint UK
        string receiptId
        string notificationEventId
        string recurringRuleId
        string transferId
        string notes
        boolean pending
        float confidence
        int64 importedAt
        int64 createdAt
        int64 updatedAt
    }

    CATEGORIES {
        string id PK
        string name
        string icon
        string color
        boolean isIncome
        boolean isSystem
        int64 createdAt
        int64 updatedAt
    }

    CATEGORY_RULES {
        string id PK
        string categoryId
        string keyword
        string matchType
        int priority
        boolean isActive
        int64 createdAt
    }

    NOTIFICATION_EVENTS {
        string id PK
        string packageName
        string title
        string text
        int64 postedAt
        string sourceBank
        boolean processed
        string parseStatus
        string transactionId
        string fingerprint
    }

    RECEIPTS {
        string id PK
        string photoUri
        int64 scannedAt
        int64 totalMinor
        string currency
        int64 taxMinor
        string merchant
        string paymentMethod
        float confidence
        string rawOcrText
        boolean isProcessed
        string transactionId
    }

    BUDGETS {
        string id PK
        string categoryId
        int64 monthlyLimitMinor
        string currency
        string period
        int64 createdAt
        int64 updatedAt
    }

    EXCHANGE_RATES {
        string baseCurrency PK
        string targetCurrency PK
        real rate
        string rateDate PK
        int64 timestamp
        string provider
    }
```

---

## 3. Database Indices & Performance Optimization

### `transactions` Table
| Index Name | Columns | Type | Purpose |
|---|---|---|---|
| `index_transactions_fingerprint` | `fingerprint` | UNIQUE | Deterministic idempotency check ($O(1)$) |
| `index_transactions_externalId` | `externalId` | Non-unique | Bank transaction reference matching ($O(\log N)$) |
| `index_transactions_amount_curr_date` | `amountMinor, currency, date` | Composite | Fast fuzzy duplicate matching across $\pm 2$ day window |
| `index_transactions_accountId` | `accountId` | Non-unique | Account balance calculation and filtering |
| `index_transactions_date` | `date` | Non-unique | Chronological range queries and analytics |
| `index_transactions_categoryId` | `categoryId` | Non-unique | Category breakdown aggregation |

---

## 4. Room Type Converters

Defined in `Converters.kt`:
- **`LocalDate` $\leftrightarrow$ `String`**: ISO-8601 formatted (`YYYY-MM-DD`).
- **`Instant` $\leftrightarrow$ `Long`**: Epoch milliseconds.
- **Enums $\leftrightarrow$ `String`**:
  - `AccountType`: `BANK`, `SAVINGS`, `CASH`, `CARD`, `INVESTMENT`, `LOAN`
  - `TransactionDirection`: `EXPENSE`, `INCOME`, `TRANSFER`, `REFUND`
  - `TransactionSource`: `MANUAL`, `NOTIFICATION`, `RECEIPT`, `CSV`, `RECURRING`
  - `PaymentMethod`: `CASH`, `CARD`, `TRANSFER`, `UNKNOWN`
  - `NotificationParseStatus`: `PARSED`, `NEEDS_REVIEW`, `IGNORED`, `FAILED`

---

## 5. Schema Evolution & Migrations

- **Migration 1 $\rightarrow$ 2**: Added the `exchange_rates` table supporting offline multi-currency conversions and historical ECB rates.
- **Migration 2 $\rightarrow$ 3**: Added `index_transactions_externalId` and the composite index `index_transactions_amountMinor_currency_date` on the `transactions` table to optimize deduplication queries.
