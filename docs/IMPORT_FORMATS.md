# Import & Export Formats

## 1. Supported File Import Formats

### CSV & Bank Statements
The application includes an RFC 4180-compliant CSV engine supporting:
- Auto-detection of delimiter: comma (`,`), semicolon (`;`), tab (`\t`)
- Escaped quotes (`"Lidl, Debrecen"`)
- UTF-8 with BOM (`\uFEFF`) and without BOM
- European formatting: `14.500,50` (dot thousands, comma decimal)
- US formatting: `14,500.50` (comma thousands, dot decimal)
- Combined amount column or separate Debit / Credit columns

### Tested Bank Statement Profiles
- **Revolut**: `Started Date, Completed Date, Description, Amount, Fee, Currency, State, Balance`
- **OTP Bank**: `Könyvelés napja, Értéknap, Terhelés / Jóváírás, Partner neve, Közlemény, Összeg, Deviza`
- **Erste Bank (George)**: `Dátum, Partner, Összeg, Pénznem, Tranzakció típusa, Közlemény`
- **MBH Bank**: `Könyvelés dátuma, Megbízó/Kedvezményezett neve, Összeg, Devizanem`
- **Wise**: `TransferWise ID, Date, Amount, Currency, Description, Payment Reference`

---

## 2. Backup & Migration Formats

### Application Backup Format (JSON v1.0.0)
Standardized schema holding accounts, transactions, categories, rules, budgets, and saved periods.

### Legacy Web LocalStorage Migration
Supports importing legacy data from the previous web application:
- `financial_current_transactions`: Array of legacy transaction objects
- `financial_saved_months`: Monthly saved periods dictionary
- `financial_custom_categories`: User category and keyword list
