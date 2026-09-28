import { describe, it, expect } from 'vitest';
import { detectRecurringTransactions } from '../../src/core/recurring/detector';
import { Transaction } from '../../src/types';

describe('Recurring Transactions Detector', () => {
  const transactions: Transaction[] = [
    {
      id: 'tx_netflix_jan',
      accountId: 'acc1',
      date: '2026-01-03',
      amountMinor: -449000,
      currency: 'HUF',
      direction: 'expense',
      description: 'Netflix előfizetés',
      source: 'notification',
      fingerprint: 'fp1',
      importedAt: '2026-01-03T00:00:00Z',
      createdAt: '2026-01-03T00:00:00Z',
      updatedAt: '2026-01-03T00:00:00Z',
    },
    {
      id: 'tx_netflix_feb',
      accountId: 'acc1',
      date: '2026-02-03',
      amountMinor: -449000,
      currency: 'HUF',
      direction: 'expense',
      description: 'Netflix előfizetés',
      source: 'notification',
      fingerprint: 'fp2',
      importedAt: '2026-02-03T00:00:00Z',
      createdAt: '2026-02-03T00:00:00Z',
      updatedAt: '2026-02-03T00:00:00Z',
    },
    {
      id: 'tx_netflix_mar',
      accountId: 'acc1',
      date: '2026-03-03',
      amountMinor: -449000,
      currency: 'HUF',
      direction: 'expense',
      description: 'Netflix előfizetés',
      source: 'notification',
      fingerprint: 'fp3',
      importedAt: '2026-03-03T00:00:00Z',
      createdAt: '2026-03-03T00:00:00Z',
      updatedAt: '2026-03-03T00:00:00Z',
    },
    {
      id: 'tx_one_off',
      accountId: 'acc1',
      date: '2026-02-15',
      amountMinor: -2500000,
      currency: 'HUF',
      direction: 'expense',
      description: 'IKEA bútor',
      source: 'notification',
      fingerprint: 'fp4',
      importedAt: '2026-02-15T00:00:00Z',
      createdAt: '2026-02-15T00:00:00Z',
      updatedAt: '2026-02-15T00:00:00Z',
    },
  ];

  it('identifies monthly recurring transactions', () => {
    const { recurringTransactionIds, detectedRules } = detectRecurringTransactions(transactions);

    expect(recurringTransactionIds.has('tx_netflix_jan')).toBe(true);
    expect(recurringTransactionIds.has('tx_netflix_feb')).toBe(true);
    expect(recurringTransactionIds.has('tx_netflix_mar')).toBe(true);
    expect(recurringTransactionIds.has('tx_one_off')).toBe(false);

    expect(detectedRules.length).toBeGreaterThan(0);
    const rule = detectedRules[0];
    expect(rule.frequency).toBe('monthly');
    expect(rule.estimatedAmountMinor).toBe(449000);
  });
});
