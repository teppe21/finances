export type CurrencyCode = 'HUF' | 'EUR' | 'USD' | (string & {});

export type TransactionDirection =
  | 'income'
  | 'expense'
  | 'transfer'
  | 'refund'
  | 'adjustment'
  | 'unknown';

export type TransactionSource =
  | 'manual'
  | 'csv'
  | 'notification'
  | 'receipt'
  | 'open_banking'
  | 'backup';

export type AccountType =
  | 'bank'
  | 'savings'
  | 'credit_card'
  | 'cash'
  | 'investment'
  | 'other';

export interface Account {
  id: string;
  name: string;
  institution: string;
  type: AccountType;
  currency: CurrencyCode;
  openingBalanceMinor: number;
  currentBalanceMinor?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Transaction {
  id: string;
  accountId: string;
  date: string; // YYYY-MM-DD
  valueDate?: string; // YYYY-MM-DDTHH:mm:ssZ
  amountMinor: number; // Integer minor currency units (e.g. 100 HUF = 10000 minor if 2 decimals, or 100)
  currency: CurrencyCode;
  direction: TransactionDirection;
  description: string;
  merchant?: string;
  categoryId?: string;
  source: TransactionSource;
  sourceAppPackage?: string;
  externalId?: string;
  fingerprint: string;
  receiptId?: string;
  notificationEventId?: string;
  recurringRuleId?: string;
  transferId?: string; // Links paired transfer transactions
  notes?: string;
  pending?: boolean;
  confidence?: number; // 0.0 - 1.0
  importedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  icon?: string;
  color?: string;
  isIncome?: boolean;
  isDefault?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRule {
  id: string;
  categoryId: string;
  pattern: string; // Keyword or regex pattern
  matchType: 'contains' | 'exact' | 'regex';
  priority: number; // higher = evaluated first
  isActive: boolean;
  createdAt: string;
}

export interface NotificationSourceConfig {
  packageName: string;
  displayName: string;
  enabled: boolean;
  bankProfileId: string;
  createdAt: string;
  updatedAt: string;
}

export interface RawNotification {
  packageName: string;
  applicationLabel?: string;
  title?: string;
  text?: string;
  bigText?: string;
  subText?: string;
  postedAt: string; // ISO 8601
  notificationKey?: string;
}

export interface NotificationEvent extends RawNotification {
  id: string;
  sourceBankProfile?: string;
  processed: boolean;
  parseStatus: 'parsed' | 'ignored' | 'needs_review' | 'failed';
  transactionId?: string;
  fingerprint?: string;
  failureReason?: string;
}

export interface ParsedNotificationTransaction {
  amountMinor: number;
  currency: CurrencyCode;
  direction: TransactionDirection;
  merchant?: string;
  description: string;
  date: string; // YYYY-MM-DD
  valueDate: string; // ISO timestamp
  suggestedAccountType?: AccountType;
  suggestedAccountName?: string;
  externalId?: string;
  cardLast4?: string;
  confidence: number;
  rawTextExcerpt?: string;
}

export interface ReceiptItem {
  id?: string;
  name: string;
  quantity?: number;
  unitPriceMinor?: number;
  totalPriceMinor?: number;
  categorySuggestion?: string;
}

export interface ReceiptScan {
  id: string;
  imageUri?: string;
  scannedAt: string;
  merchant?: string;
  date?: string; // YYYY-MM-DD
  totalMinor?: number;
  subtotalMinor?: number;
  taxMinor?: number; // ÁFA
  currency: CurrencyCode;
  paymentMethod: 'cash' | 'card' | 'unknown';
  items: ReceiptItem[];
  rawOcrAvailable: boolean;
  rawOcrText?: string;
  confidence: number;
  processedLocally: boolean;
  transactionId?: string;
}

export interface Budget {
  id: string;
  categoryId: string;
  amountMinor: number;
  period: 'monthly' | 'yearly';
  startDate?: string;
  endDate?: string;
  spentMinor?: number;
  notes?: string;
}

export interface RecurringTransactionRule {
  id: string;
  descriptionPattern: string;
  merchant?: string;
  categoryId?: string;
  frequency: 'weekly' | 'monthly' | 'yearly';
  estimatedAmountMinor: number;
  currency: CurrencyCode;
  lastDate?: string;
  nextDate?: string;
  matchedTransactionIds: string[];
  isActive: boolean;
}

export interface SavedPeriod {
  id: string;
  periodKey: string; // e.g. "2026-03"
  name: string;
  savedAt: string;
  transactionCount: number;
  totalIncomeMinor: number;
  totalExpenseMinor: number;
  balanceMinor: number;
  dataJson?: string;
}

export interface ImportSession {
  id: string;
  source: TransactionSource;
  fileName?: string;
  importedCount: number;
  duplicateCount: number;
  failedCount: number;
  importedAt: string;
}

export interface IngestionResult {
  status:
    | 'created'
    | 'duplicate'
    | 'possible_duplicate'
    | 'needs_review'
    | 'rejected';
  transaction?: Transaction;
  transactionId?: string;
  duplicateOf?: string;
  warnings: string[];
  confidence?: number;
}

export interface FinancialStats {
  incomeMinor: number;
  expenseMinor: number;
  balanceMinor: number;
  transfersMinor: number;
  savingsRate: number; // 0.0 - 1.0 (e.g. 0.25 = 25%)
  transactionCount: number;
  maxExpenseItem: {
    description: string;
    amountMinor: number;
  };
  avgExpenseMinor: number;
}

export interface MonthlyTrendData {
  month: string; // YYYY-MM
  incomeMinor: number;
  expenseMinor: number;
  balanceMinor: number;
}

export interface CategorySummary {
  categoryId: string;
  categoryName: string;
  color?: string;
  icon?: string;
  totalMinor: number;
  percentage: number;
  transactionCount: number;
}
