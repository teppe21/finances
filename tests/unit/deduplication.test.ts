import { describe, it, expect } from 'vitest';
import { checkDuplicate } from '../../src/core/deduplication/deduplicator';
import { Transaction } from '../../src/types';

describe('Cross-Source Deduplication Engine', () => {
  const existingTransactions: Transaction[] = [
    {
      id: 'tx_notif_1',
      accountId: 'acc_revolut',
      date: '2026-03-01',
      amountMinor: -1450000,
      currency: 'HUF',
      direction: 'expense',
      description: 'Lidl Debrecen Kishegyesi út',
      merchant: 'Lidl',
      source: 'notification',
      fingerprint: 'fp_notif_1',
      importedAt: '2026-03-01T14:32:00Z',
      createdAt: '2026-03-01T14:32:00Z',
      updatedAt: '2026-03-01T14:32:00Z',
    },
  ];

  it('detects duplicate when CSV import arrives after notification for same purchase', () => {
    // Later user imports bank statement CSV containing the same Lidl purchase
    const csvCandidate = {
      date: '2026-03-01',
      amountMinor: -1450000,
      currency: 'HUF',
      description: 'LIDL MAGYARORSZAG KFT',
      merchant: 'Lidl',
    };

    const result = checkDuplicate(csvCandidate as any, existingTransactions);
    expect(result.status).toBe('exact_duplicate');
    expect(result.matchedTransaction?.id).toBe('tx_notif_1');
  });

  it('detects possible duplicate when date is 1 day off (booking vs settlement date)', () => {
    const nextDayCandidate = {
      date: '2026-03-02', // settled next day
      amountMinor: -1450000,
      currency: 'HUF',
      description: 'LIDL MAGYARORSZAG KFT',
      merchant: 'Lidl',
    };

    const result = checkDuplicate(nextDayCandidate as any, existingTransactions);
    expect(result.status).toBe('possible_duplicate');
    expect(result.matchedTransaction?.id).toBe('tx_notif_1');
  });

  it('does not flag different amounts on same day as duplicate', () => {
    const differentPurchase = {
      date: '2026-03-01',
      amountMinor: -320000,
      currency: 'HUF',
      description: 'Lidl Debrecen',
      merchant: 'Lidl',
    };

    const result = checkDuplicate(differentPurchase as any, existingTransactions);
    expect(result.status).toBe('none');
  });
});
