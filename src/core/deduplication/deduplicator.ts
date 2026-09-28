import { Transaction, CurrencyCode } from '../../types';
import { normalizeSearchText } from '../normalization/text';
import { normalizeDate } from '../normalization/date';
import { generateTransactionFingerprint } from './fingerprint';

export interface DuplicateCheckResult {
  status: 'exact_duplicate' | 'possible_duplicate' | 'none';
  matchedTransaction?: Transaction;
  reason?: string;
  confidence: number;
}

/**
 * Calculates Jaccard / token overlap similarity between two normalized strings (0.0 to 1.0).
 */
export function calculateTextSimilarity(text1: string, text2: string): number {
  const norm1 = normalizeSearchText(text1);
  const norm2 = normalizeSearchText(text2);

  if (norm1 === norm2) return 1.0;
  if (!norm1 || !norm2) return 0.0;
  if (norm1.includes(norm2) || norm2.includes(norm1)) return 0.85;

  const tokens1 = new Set(norm1.split(' ').filter(Boolean));
  const tokens2 = new Set(norm2.split(' ').filter(Boolean));

  let intersection = 0;
  tokens1.forEach((t) => {
    if (tokens2.has(t)) intersection++;
  });

  const union = new Set([...tokens1, ...tokens2]).size;
  return union > 0 ? intersection / union : 0;
}

/**
 * Computes day difference between two YYYY-MM-DD date strings.
 */
export function dateDiffDays(d1: string, d2: string): number {
  const t1 = new Date(normalizeDate(d1)).getTime();
  const t2 = new Date(normalizeDate(d2)).getTime();
  return Math.abs(Math.round((t1 - t2) / (1000 * 60 * 60 * 24)));
}

/**
 * Checks an incoming candidate against existing transactions for exact or possible duplicates.
 */
export function checkDuplicate(
  candidate: {
    date: string;
    amountMinor: number;
    currency: CurrencyCode;
    description: string;
    merchant?: string;
    externalId?: string;
    fingerprint?: string;
  },
  existingTransactions: Transaction[]
): DuplicateCheckResult {
  const candidateFp =
    candidate.fingerprint ||
    generateTransactionFingerprint({
      date: candidate.date,
      amountMinor: candidate.amountMinor,
      currency: candidate.currency,
      description: candidate.description,
      merchant: candidate.merchant,
      externalId: candidate.externalId,
    });

  for (const existing of existingTransactions) {
    // 1. External ID exact match
    if (
      candidate.externalId &&
      existing.externalId &&
      candidate.externalId.trim() === existing.externalId.trim()
    ) {
      return {
        status: 'exact_duplicate',
        matchedTransaction: existing,
        reason: `External ID match: ${candidate.externalId}`,
        confidence: 1.0,
      };
    }

    // 2. Exact Fingerprint match
    if (existing.fingerprint && existing.fingerprint === candidateFp) {
      return {
        status: 'exact_duplicate',
        matchedTransaction: existing,
        reason: 'Identical transaction fingerprint',
        confidence: 1.0,
      };
    }

    // 3. Cross-Source fuzzy check (e.g. Bank Notification vs CSV statement vs Receipt)
    if (
      candidate.currency.toUpperCase() === existing.currency.toUpperCase() &&
      candidate.amountMinor === existing.amountMinor
    ) {
      const daysDiff = dateDiffDays(candidate.date, existing.date);

      // Within 2 days
      if (daysDiff <= 2) {
        // Direct merchant match check
        const merchantSimilarity =
          candidate.merchant && existing.merchant
            ? calculateTextSimilarity(candidate.merchant, existing.merchant)
            : 0;

        const text1 = `${candidate.merchant || ''} ${candidate.description || ''}`;
        const text2 = `${existing.merchant || ''} ${existing.description || ''}`;
        const similarity = Math.max(merchantSimilarity, calculateTextSimilarity(text1, text2));

        if (similarity >= 0.6) {
          return {
            status: daysDiff === 0 ? 'exact_duplicate' : 'possible_duplicate',
            matchedTransaction: existing,
            reason: `Cross-source match (${existing.source} vs candidate): same amount and merchant similarity ${(similarity * 100).toFixed(0)}% within ${daysDiff} day(s)`,
            confidence: similarity,
          };
        } else if (daysDiff <= 1) {
          // Same amount, same/next day, but text differed slightly
          return {
            status: 'possible_duplicate',
            matchedTransaction: existing,
            reason: `Possible duplicate: identical amount ${candidate.amountMinor} on ${candidate.date} / ${existing.date}`,
            confidence: 0.5,
          };
        }
      }
    }
  }

  return {
    status: 'none',
    confidence: 0.0,
  };
}
