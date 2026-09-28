import { describe, it, expect } from 'vitest';
import {
  calculateIncome,
  calculateExpenses,
  calculateTransfers,
  calculateBalance,
  calculateSavingsRate,
  calculateFinancialStats,
  calculateMonthlyTrends,
} from '../../src/core/calculations/financialMath';
import { Transaction } from '../../src/types';

describe('Financial Calculations Engine', () => {
  const sampleTransactions: Transaction[] = [
    {
      id: 'tx1',
      accountId: 'acc1',
      date: '2026-03-01',
      amountMinor: -1450000, // -14,500 HUF
      currency: 'HUF',
      direction: 'expense',
      description: 'Lidl bevásárlás',
      source: 'notification',
      fingerprint: 'fp1',
      importedAt: '2026-03-01T10:00:00Z',
      createdAt: '2026-03-01T10:00:00Z',
      updatedAt: '2026-03-01T10:00:00Z',
    },
    {
      id: 'tx2',
      accountId: 'acc1',
      date: '2026-03-01',
      amountMinor: 65000000, // +650,000 HUF
      currency: 'HUF',
      direction: 'income',
      description: 'Fizetés',
      source: 'csv',
      fingerprint: 'fp2',
      importedAt: '2026-03-01T10:00:00Z',
      createdAt: '2026-03-01T10:00:00Z',
      updatedAt: '2026-03-01T10:00:00Z',
    },
    {
      id: 'tx3',
      accountId: 'acc1',
      date: '2026-03-02',
      amountMinor: -420000, // -4,200 HUF
      currency: 'HUF',
      direction: 'expense',
      description: 'Wolt ebéd',
      source: 'notification',
      fingerprint: 'fp3',
      importedAt: '2026-03-02T10:00:00Z',
      createdAt: '2026-03-02T10:00:00Z',
      updatedAt: '2026-03-02T10:00:00Z',
    },
    {
      id: 'tx4',
      accountId: 'acc1',
      date: '2026-03-03',
      amountMinor: -10000000, // -100,000 HUF transfer
      currency: 'HUF',
      direction: 'transfer',
      description: 'Utalás megtakarítási számlára',
      source: 'manual',
      fingerprint: 'fp4',
      importedAt: '2026-03-03T10:00:00Z',
      createdAt: '2026-03-03T10:00:00Z',
      updatedAt: '2026-03-03T10:00:00Z',
    },
  ];

  it('calculates total income excluding transfers', () => {
    const income = calculateIncome(sampleTransactions);
    expect(income).toBe(65000000);
  });

  it('calculates total expense excluding transfers', () => {
    const expense = calculateExpenses(sampleTransactions);
    expect(expense).toBe(1870000); // 14,500 + 4,200 = 18,700 HUF = 1,870,000 minor
  });

  it('tracks transfers separately from income and expense', () => {
    const transfers = calculateTransfers(sampleTransactions);
    expect(transfers).toBe(10000000);
  });

  it('calculates net balance correctly', () => {
    const balance = calculateBalance(sampleTransactions);
    expect(balance).toBe(65000000 - 1870000);
  });

  it('calculates savings rate accurately', () => {
    const rate = calculateSavingsRate(65000000, 1870000);
    expect(rate).toBeCloseTo((65000000 - 1870000) / 65000000, 4);
  });

  it('computes headline stats correctly', () => {
    const stats = calculateFinancialStats(sampleTransactions);
    expect(stats.transactionCount).toBe(4);
    expect(stats.incomeMinor).toBe(65000000);
    expect(stats.expenseMinor).toBe(1870000);
    expect(stats.maxExpenseItem.amountMinor).toBe(1450000);
    expect(stats.maxExpenseItem.description).toBe('Lidl bevásárlás');
    expect(stats.avgExpenseMinor).toBe(935000); // 1870000 / 2
  });

  it('calculates monthly comparison trends', () => {
    const refDate = new Date('2026-03-15T00:00:00Z');
    const trends = calculateMonthlyTrends(sampleTransactions, 6, refDate);
    expect(trends.length).toBe(6);

    const march = trends.find((t) => t.month === '2026-03');
    expect(march).toBeDefined();
    expect(march?.incomeMinor).toBe(65000000);
    expect(march?.expenseMinor).toBe(1870000);
  });
});
