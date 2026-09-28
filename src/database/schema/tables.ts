export const SQL_CREATE_TABLES = `
-- 1. Accounts
CREATE TABLE IF NOT EXISTS accounts (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  institution TEXT NOT NULL,
  type TEXT NOT NULL,
  currency TEXT NOT NULL DEFAULT 'HUF',
  openingBalanceMinor INTEGER NOT NULL DEFAULT 0,
  isActive INTEGER NOT NULL DEFAULT 1,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL
);

-- 2. Categories
CREATE TABLE IF NOT EXISTS categories (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  icon TEXT,
  color TEXT,
  isIncome INTEGER NOT NULL DEFAULT 0,
  isDefault INTEGER NOT NULL DEFAULT 0,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL
);

-- 3. Category Rules
CREATE TABLE IF NOT EXISTS category_rules (
  id TEXT PRIMARY KEY,
  categoryId TEXT NOT NULL,
  pattern TEXT NOT NULL,
  matchType TEXT NOT NULL DEFAULT 'contains',
  priority INTEGER NOT NULL DEFAULT 10,
  isActive INTEGER NOT NULL DEFAULT 1,
  createdAt TEXT NOT NULL,
  FOREIGN KEY (categoryId) REFERENCES categories (id) ON DELETE CASCADE
);

-- 4. Notification Sources
CREATE TABLE IF NOT EXISTS notification_sources (
  packageName TEXT PRIMARY KEY,
  displayName TEXT NOT NULL,
  enabled INTEGER NOT NULL DEFAULT 1,
  bankProfileId TEXT NOT NULL,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL
);

-- 5. Notification Events
CREATE TABLE IF NOT EXISTS notification_events (
  id TEXT PRIMARY KEY,
  packageName TEXT NOT NULL,
  applicationLabel TEXT,
  title TEXT,
  text TEXT,
  bigText TEXT,
  subText TEXT,
  postedAt TEXT NOT NULL,
  notificationKey TEXT,
  sourceBankProfile TEXT,
  processed INTEGER NOT NULL DEFAULT 0,
  parseStatus TEXT NOT NULL DEFAULT 'parsed',
  transactionId TEXT,
  fingerprint TEXT,
  failureReason TEXT
);

-- 6. Receipt Scans
CREATE TABLE IF NOT EXISTS receipt_scans (
  id TEXT PRIMARY KEY,
  imageUri TEXT,
  scannedAt TEXT NOT NULL,
  merchant TEXT,
  date TEXT,
  totalMinor INTEGER,
  subtotalMinor INTEGER,
  taxMinor INTEGER,
  currency TEXT NOT NULL DEFAULT 'HUF',
  paymentMethod TEXT NOT NULL DEFAULT 'unknown',
  itemsJson TEXT,
  rawOcrAvailable INTEGER NOT NULL DEFAULT 1,
  rawOcrText TEXT,
  confidence REAL NOT NULL DEFAULT 0.0,
  processedLocally INTEGER NOT NULL DEFAULT 1,
  transactionId TEXT
);

-- 7. Transactions
CREATE TABLE IF NOT EXISTS transactions (
  id TEXT PRIMARY KEY,
  accountId TEXT NOT NULL,
  date TEXT NOT NULL,
  valueDate TEXT,
  amountMinor INTEGER NOT NULL,
  currency TEXT NOT NULL DEFAULT 'HUF',
  direction TEXT NOT NULL,
  description TEXT NOT NULL,
  merchant TEXT,
  categoryId TEXT,
  source TEXT NOT NULL,
  sourceAppPackage TEXT,
  externalId TEXT,
  fingerprint TEXT NOT NULL,
  receiptId TEXT,
  notificationEventId TEXT,
  recurringRuleId TEXT,
  transferId TEXT,
  notes TEXT,
  pending INTEGER NOT NULL DEFAULT 0,
  confidence REAL DEFAULT 1.0,
  importedAt TEXT NOT NULL,
  createdAt TEXT NOT NULL,
  updatedAt TEXT NOT NULL,
  FOREIGN KEY (accountId) REFERENCES accounts (id),
  FOREIGN KEY (categoryId) REFERENCES categories (id)
);

-- 8. Budgets
CREATE TABLE IF NOT EXISTS budgets (
  id TEXT PRIMARY KEY,
  categoryId TEXT NOT NULL,
  amountMinor INTEGER NOT NULL,
  period TEXT NOT NULL DEFAULT 'monthly',
  startDate TEXT,
  endDate TEXT,
  notes TEXT,
  FOREIGN KEY (categoryId) REFERENCES categories (id) ON DELETE CASCADE
);

-- 9. Saved Periods
CREATE TABLE IF NOT EXISTS saved_periods (
  id TEXT PRIMARY KEY,
  periodKey TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  savedAt TEXT NOT NULL,
  transactionCount INTEGER NOT NULL DEFAULT 0,
  totalIncomeMinor INTEGER NOT NULL DEFAULT 0,
  totalExpenseMinor INTEGER NOT NULL DEFAULT 0,
  balanceMinor INTEGER NOT NULL DEFAULT 0,
  dataJson TEXT
);

-- 10. Import Sessions
CREATE TABLE IF NOT EXISTS import_sessions (
  id TEXT PRIMARY KEY,
  source TEXT NOT NULL,
  fileName TEXT,
  importedCount INTEGER NOT NULL DEFAULT 0,
  duplicateCount INTEGER NOT NULL DEFAULT 0,
  failedCount INTEGER NOT NULL DEFAULT 0,
  importedAt TEXT NOT NULL
);

-- 11. Settings
CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

-- INDEXES
CREATE INDEX IF NOT EXISTS idx_tx_account_date ON transactions (accountId, date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions (date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions (categoryId);
CREATE INDEX IF NOT EXISTS idx_tx_fingerprint ON transactions (fingerprint);
CREATE INDEX IF NOT EXISTS idx_tx_external_id ON transactions (externalId);
CREATE INDEX IF NOT EXISTS idx_tx_source ON transactions (source);
CREATE INDEX IF NOT EXISTS idx_tx_direction ON transactions (direction);
CREATE INDEX IF NOT EXISTS idx_rules_priority ON category_rules (priority DESC, isActive);
CREATE INDEX IF NOT EXISTS idx_notif_events_date ON notification_events (postedAt DESC);
CREATE INDEX IF NOT EXISTS idx_receipts_scanned_at ON receipt_scans (scannedAt DESC);
`;
