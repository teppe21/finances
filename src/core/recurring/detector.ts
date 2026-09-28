import { Transaction, RecurringTransactionRule } from '../../types';
import { normalizeSearchText } from '../normalization/text';
import { normalizeDate } from '../normalization/date';

export interface RecurringDetectionResult {
  recurringTransactionIds: Set<string>;
  detectedRules: RecurringTransactionRule[];
}

/**
 * Detects recurring transactions (e.g. Netflix, Spotify, Rent, Utilities, Salaries)
 * based on periodic repetition, day-of-month alignment, and amount similarity.
 */
export function detectRecurringTransactions(
  transactions: Transaction[]
): RecurringDetectionResult {
  const recurringIds = new Set<string>();
  const detectedRules: RecurringTransactionRule[] = [];

  if (!transactions || transactions.length < 2) {
    return { recurringTransactionIds: recurringIds, detectedRules };
  }

  // Group by normalized merchant or description
  const groups: Record<string, Transaction[]> = {};

  transactions.forEach((t) => {
    const key = normalizeSearchText(t.merchant || t.description);
    if (!key) return;
    if (!groups[key]) groups[key] = [];
    groups[key].push(t);
  });

  Object.entries(groups).forEach(([patternKey, items]) => {
    if (items.length < 2) return;

    // Sort chronologically
    items.sort((a, b) => new Date(normalizeDate(a.date)).getTime() - new Date(normalizeDate(b.date)).getTime());

    const matchedIds = new Set<string>();
    let totalAmountMinor = 0;
    let currency = items[0].currency;

    for (let i = 0; i < items.length - 1; i++) {
      for (let j = i + 1; j < items.length; j++) {
        const d1 = new Date(normalizeDate(items[i].date));
        const d2 = new Date(normalizeDate(items[j].date));

        const monthDiff =
          (d2.getFullYear() - d1.getFullYear()) * 12 + (d2.getMonth() - d1.getMonth());

        if (monthDiff >= 1 && monthDiff <= 2) {
          const dayDiff = Math.abs(d1.getDate() - d2.getDate());
          const amt1 = Math.abs(items[i].amountMinor);
          const amt2 = Math.abs(items[j].amountMinor);
          const maxAmt = Math.max(amt1, amt2);
          const diffRatio = maxAmt === 0 ? 0 : Math.abs(amt1 - amt2) / maxAmt;

          // Repetition criteria: day of month within 3 days and amount within 10%
          if (dayDiff <= 3 && diffRatio <= 0.1) {
            recurringIds.add(items[i].id);
            recurringIds.add(items[j].id);
            matchedIds.add(items[i].id);
            matchedIds.add(items[j].id);
          }
        }
      }
    }

    if (matchedIds.size >= 2) {
      const matchedList = items.filter((item) => matchedIds.has(item.id));
      const avgMinor = Math.round(
        matchedList.reduce((acc, curr) => acc + Math.abs(curr.amountMinor), 0) / matchedList.length
      );
      const lastTx = matchedList[matchedList.length - 1];

      // Predict next occurrence (~1 month from last date)
      const lastD = new Date(normalizeDate(lastTx.date));
      const nextD = new Date(lastD.getFullYear(), lastD.getMonth() + 1, lastD.getDate());

      detectedRules.push({
        id: `recurring_${patternKey.replace(/\s+/g, '_')}`,
        descriptionPattern: lastTx.description,
        merchant: lastTx.merchant,
        categoryId: lastTx.categoryId,
        frequency: 'monthly',
        estimatedAmountMinor: avgMinor,
        currency,
        lastDate: lastTx.date,
        nextDate: nextD.toISOString().substring(0, 10),
        matchedTransactionIds: Array.from(matchedIds),
        isActive: true,
      });
    }
  });

  return { recurringTransactionIds: recurringIds, detectedRules };
}
