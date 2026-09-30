# Google Play Financial Features Declaration & Regulatory Scope

This document details the financial classification, feature scope, and regulatory positioning of **Finances** (`com.teppe21.finances`) for compliance with Google Play's Financial Services policy.

---

## 1. Application Classification

- **Category:** Personal Finance Management (PFM) / Personal Expense Tracker.
- **Operating Model:** Standalone, on-device utility tool for expense tracking, budgeting, and analytics.

---

## 2. Explicit Non-Financial Scope Declarations

Finances **DOES NOT** provide, facilitate, or broker any of the following regulated financial services:

1. **No Money Transmission:** The app cannot transfer money, process card payments, or execute bank transactions.
2. **No Banking or Deposit Taking:** The app does not hold, custody, or manage user funds.
3. **No Lending or Credit Products:** The app does not offer personal loans, payday loans, peer-to-peer credit, mortgages, or buy-now-pay-later (BNPL) services.
4. **No Investment or Brokerage Services:** The app does not buy, sell, trade, or hold securities, stocks, bonds, or commodities.
5. **No Cryptocurrency or Virtual Asset Services:** The app does not manage crypto wallets, tokens, staking, or exchanges.
6. **No Account Credential Access:** The app never requests, stores, or handles bank login credentials, internet banking passwords, PINs, or card CVVs.

---

## 3. Financial Features Within Scope

The application provides strictly read-only personal finance management capabilities:

- **Manual Transaction Tracking:** Adding and editing personal cash and bank transactions.
- **On-Device Receipt Scanning:** Photographing paper receipts for on-device OCR total extraction via Google ML Kit.
- **Bank Notification Parsing:** Optional on-device listener detecting payment alert notifications from user-installed banking apps (OTP Bank, Revolut, Google Wallet, Erste, MBH, Wise) to automate local expense recording.
- **Multi-Currency Analytics:** Displaying aggregate expense totals converted into the user's preferred display currency using public ECB reference exchange rates.
- **Budgeting & Recurring Rules:** Setting monthly category budgets and auto-detecting recurring subscription patterns based on local transaction history.

---

## 4. Privacy & Regulatory Alignment (GDPR)

Because Finances operates entirely on-device, it inherently complies with major privacy frameworks:

- **Article 17 (Right to Erasure):** A one-click "Delete All Financial Data" button in Settings immediately wipes the SQLite database.
- **Article 20 (Right to Data Portability):** Users can export all recorded transactions to standard CSV format at any time.
- **Zero Third-Party Data Transfer:** No user records, financial balances, or transaction histories are transferred to third parties or remote cloud infrastructure.
