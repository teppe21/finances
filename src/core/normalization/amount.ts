import { CurrencyCode } from '../../types';
import { toMinorUnits } from './currency';

export interface ExtractedAmount {
  amountMajor: number;
  amountMinor: number;
  currency: CurrencyCode;
  isNegative: boolean;
  rawMatched: string;
}

/**
 * Normalizes all whitespace characters including Unicode non-breaking & thin spaces.
 */
export function normalizeWhitespace(text: string): string {
  return text.replace(/[\u00A0\u1680\u180E\u2000-\u200B\u202F\u205F\u3000\uFEFF]/g, ' ').trim();
}

/**
 * Parses numeric amount strings with robust handling of European and US formatting.
 * Examples:
 *  "12 450,50" -> 12450.5
 *  "12.450,50" -> 12450.5
 *  "12,450.50" -> 12450.5
 *  "12 450" -> 12450
 *  "-6 490" -> -6490
 */
export function parseNumericAmount(str: string): number {
  if (!str) return 0;

  // Clean unicode spaces and dashes
  let s = normalizeWhitespace(str)
    .replace(/[\u2010\u2011\u2012\u2013\u2014\u2015\u2212]/g, '-')
    .replace(/[^\d.,\-+]/g, '');

  const isNeg = s.includes('-');
  s = s.replace(/[-+]/g, '').trim();

  if (!s) return 0;

  // Detect decimal separator vs thousand separators
  const lastDot = s.lastIndexOf('.');
  const lastComma = s.lastIndexOf(',');

  let cleanStr = s;

  if (lastDot !== -1 && lastComma !== -1) {
    if (lastComma > lastDot) {
      // Format: 1.234,56 -> dot is thousand, comma is decimal
      cleanStr = s.replace(/\./g, '').replace(',', '.');
    } else {
      // Format: 1,234.56 -> comma is thousand, dot is decimal
      cleanStr = s.replace(/,/g, '');
    }
  } else if (lastComma !== -1) {
    // Only comma present
    const parts = s.split(',');
    if (parts.length === 2 && parts[1].length <= 2) {
      // Likely decimal comma e.g. "12450,50" or "450,5"
      cleanStr = s.replace(',', '.');
    } else {
      // Likely thousand separator e.g. "12,450" or multiple commas "1,000,000"
      cleanStr = s.replace(/,/g, '');
    }
  } else if (lastDot !== -1) {
    // Only dot present
    const parts = s.split('.');
    if (parts.length === 2 && parts[1].length <= 2) {
      // Likely decimal dot e.g. "12450.50"
      cleanStr = s;
    } else {
      // Likely thousand separator in European Hungarian e.g. "14.500"
      cleanStr = s.replace(/\./g, '');
    }
  }

  let val = parseFloat(cleanStr);
  if (isNaN(val)) val = 0;
  return isNeg ? -val : val;
}

/**
 * Extracts amount and currency from arbitrary text (such as bank notification or receipt text).
 */
export function extractAmountAndCurrency(
  text: string,
  defaultCurrency: CurrencyCode = 'HUF'
): ExtractedAmount | null {
  if (!text) return null;

  const normalized = normalizeWhitespace(text);

  // Pattern matching:
  // e.g. "-12 450 Ft", "- 6,490 HUF", "12 450,50 EUR", "HUF 12,450", "4 500 Ft", "€ 15.20", "$99.00"
  const regex =
    /(?:(HUF|EUR|USD|Ft|€|\$)\s*)?([+\-–—−]?\s*\d{1,3}(?:[.,\s]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(HUF|EUR|USD|Ft|€|\$)?/i;

  const match = normalized.match(regex);
  if (!match) return null;

  const currencyToken = (match[1] || match[3] || '').trim().toUpperCase();
  const rawNumberStr = match[2];

  let currency: CurrencyCode = defaultCurrency;
  if (currencyToken === 'FT' || currencyToken === 'HUF') {
    currency = 'HUF';
  } else if (currencyToken === 'EUR' || currencyToken === '€') {
    currency = 'EUR';
  } else if (currencyToken === 'USD' || currencyToken === '$') {
    currency = 'USD';
  }

  const amountMajor = parseNumericAmount(rawNumberStr);
  const isNegative = amountMajor < 0 || normalized.includes(`-${rawNumberStr}`) || normalized.includes(`– ${rawNumberStr}`);
  const finalMajor = isNegative && amountMajor > 0 ? -amountMajor : amountMajor;

  return {
    amountMajor: finalMajor,
    amountMinor: toMinorUnits(finalMajor, currency),
    currency,
    isNegative: finalMajor < 0,
    rawMatched: match[0],
  };
}
