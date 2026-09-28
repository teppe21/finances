import { Transaction, CurrencyCode } from '../../types';
import { normalizeSearchText } from '../normalization/text';
import { normalizeDate } from '../normalization/date';
import { simpleHash } from '../../utils/hashing';

export interface FingerprintComponents {
  date: string;
  amountMinor: number;
  currency: CurrencyCode;
  normalizedText: string;
  externalId?: string;
}

/**
 * Computes a deterministic normalized fingerprint for cross-source transaction matching.
 */
export function generateTransactionFingerprint(params: {
  date: string;
  amountMinor: number;
  currency: CurrencyCode;
  description: string;
  merchant?: string;
  externalId?: string;
}): string {
  const normDate = normalizeDate(params.date);
  const normText = normalizeSearchText(params.merchant || params.description);
  const ext = params.externalId ? params.externalId.trim() : '';

  const raw = `${normDate}|${params.amountMinor}|${params.currency.toUpperCase()}|${normText}|${ext}`;
  return simpleHash(raw);
}

/**
 * Computes a soft fingerprint ignoring merchant noise for fuzzy cross-source matching (e.g. CSV vs Notification).
 */
export function generateFuzzyFingerprint(params: {
  date: string;
  amountMinor: number;
  currency: CurrencyCode;
}): string {
  const normDate = normalizeDate(params.date);
  return simpleHash(`${normDate}|${params.amountMinor}|${params.currency.toUpperCase()}`);
}
