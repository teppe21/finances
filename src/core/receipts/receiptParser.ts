import { CurrencyCode, ReceiptItem, ReceiptScan } from '../../types';
import { extractAmountAndCurrency, parseNumericAmount } from '../normalization/amount';
import { normalizeDate } from '../normalization/date';
import { cleanMerchantName, removeDiacritics } from '../normalization/text';
import { toMinorUnits } from '../normalization/currency';
import {
  HU_TOTAL_KEYWORDS,
  HU_SUBTOTAL_KEYWORDS,
  HU_TAX_KEYWORDS,
  HU_CASH_KEYWORDS,
  HU_CARD_KEYWORDS,
  KNOWN_MERCHANT_CHAINS,
} from './hungarianSemantics';

export interface ParsedReceiptResult {
  merchant?: string;
  date?: string;
  totalMinor?: number;
  subtotalMinor?: number;
  taxMinor?: number;
  currency: CurrencyCode;
  paymentMethod: 'cash' | 'card' | 'unknown';
  items: ReceiptItem[];
  confidence: number;
  rawText: string;
}

/**
 * Normalizes OCR noise (character confusions such as '0' vs 'O', '1' vs 'l' or 'I').
 */
export function normalizeOcrText(text: string): string {
  if (!text) return '';
  return text
    .replace(/\r\n/g, '\n')
    .replace(/[—–]/g, '-')
    // Replace FIZETEND0 with FIZETENDO
    .replace(/fizetend0/gi, 'fizetendo')
    .replace(/vegosszeg/gi, 'vegosszeg')
    .replace(/osszesen/gi, 'osszesen');
}

/**
 * Extracts merchant name from raw receipt text.
 */
export function extractReceiptMerchant(lines: string[]): string | undefined {
  // 1. Look for known chains anywhere in the first 8 lines
  for (let i = 0; i < Math.min(lines.length, 8); i++) {
    const normLine = removeDiacritics(lines[i]);
    for (const chain of KNOWN_MERCHANT_CHAINS) {
      if (normLine.includes(chain)) {
        // Return capitalized chain name
        return chain.charAt(0).toUpperCase() + chain.slice(1);
      }
    }
  }

  // 2. Fallback: inspect top lines, skipping obvious document header keywords
  const skipKeywords = ['nyugta', 'szamla', 'penztar', 'egyszerusitett', 'adotorzs', 'adoszam', 'blokk', 'receipt'];
  for (let i = 0; i < Math.min(lines.length, 4); i++) {
    const raw = lines[i].trim();
    const norm = removeDiacritics(raw);
    if (raw.length > 2 && !skipKeywords.some((kw) => norm.includes(kw))) {
      return cleanMerchantName(raw);
    }
  }

  return undefined;
}

/**
 * Extracts transaction total from receipt lines based on semantic keywords.
 */
export function extractReceiptTotal(
  lines: string[]
): { totalMinor?: number; currency: CurrencyCode } {
  let currency: CurrencyCode = 'HUF';

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const norm = removeDiacritics(line);

    if (HU_TOTAL_KEYWORDS.some((kw) => norm.includes(kw))) {
      // Look for amount on this line
      let extracted = extractAmountAndCurrency(line, 'HUF');

      // If not on this line, check the next line
      if (!extracted && i + 1 < lines.length) {
        extracted = extractAmountAndCurrency(lines[i + 1], 'HUF');
      }

      if (extracted) {
        currency = extracted.currency;
        return {
          totalMinor: Math.abs(extracted.amountMinor),
          currency,
        };
      }
    }
  }

  // Fallback: If no explicit total keyword found, look for largest reasonable amount
  let maxMinor = 0;
  for (const line of lines) {
    const extracted = extractAmountAndCurrency(line, 'HUF');
    if (extracted && Math.abs(extracted.amountMinor) > maxMinor) {
      maxMinor = Math.abs(extracted.amountMinor);
      currency = extracted.currency;
    }
  }

  return {
    totalMinor: maxMinor > 0 ? maxMinor : undefined,
    currency,
  };
}

/**
 * Extracts payment method from receipt (Cash vs Card vs Unknown).
 */
export function extractPaymentMethod(text: string): 'cash' | 'card' | 'unknown' {
  const norm = removeDiacritics(text);

  const hasCash = HU_CASH_KEYWORDS.some((kw) => norm.includes(kw));
  const hasCard = HU_CARD_KEYWORDS.some((kw) => norm.includes(kw));

  if (hasCash && !hasCard) return 'cash';
  if (hasCard && !hasCash) return 'card';
  return 'unknown';
}

/**
 * Extracts tax / ÁFA amount if present.
 */
export function extractReceiptTax(lines: string[]): number | undefined {
  for (const line of lines) {
    const norm = removeDiacritics(line);
    if (HU_TAX_KEYWORDS.some((kw) => norm.includes(kw))) {
      const extracted = extractAmountAndCurrency(line, 'HUF');
      if (extracted) {
        return Math.abs(extracted.amountMinor);
      }
    }
  }
  return undefined;
}

/**
 * Extracts receipt items when item line patterns match.
 */
export function extractReceiptItems(lines: string[]): ReceiptItem[] {
  const items: ReceiptItem[] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    // Pattern: Text followed by price: e.g. "TEJ 2,8% 1L 399 Ft" or "KENYER 1KG 850"
    const match = trimmed.match(/^([A-Za-zÁÉÍÓÖŐÚÜŰáéíóöőúüű0-9\s/.,%-]{3,40})\s+(\d{1,3}(?:[.,\s]\d{3})*(?:[.,]\d{1,2})?)\s*(?:Ft|HUF)?$/i);
    if (match) {
      const name = match[1].trim();
      const normName = removeDiacritics(name);

      // Skip lines that are totals, subtotals, or dates
      if (
        HU_TOTAL_KEYWORDS.some((kw) => normName.includes(kw)) ||
        HU_TAX_KEYWORDS.some((kw) => normName.includes(kw)) ||
        normName.includes('nyugta')
      ) {
        continue;
      }

      const priceMajor = parseNumericAmount(match[2]);
      if (priceMajor > 0) {
        items.push({
          name,
          totalPriceMinor: toMinorUnits(priceMajor, 'HUF'),
        });
      }
    }
  }

  return items;
}

/**
 * Main parser entry point: transforms raw OCR text into structured receipt data.
 */
export function parseReceiptText(rawText: string): ParsedReceiptResult {
  const normalized = normalizeOcrText(rawText);
  const lines = normalized
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0);

  const merchant = extractReceiptMerchant(lines);
  const { totalMinor, currency } = extractReceiptTotal(lines);
  const taxMinor = extractReceiptTax(lines);
  const paymentMethod = extractPaymentMethod(normalized);
  const items = extractReceiptItems(lines);

  // Date detection: Look for date patterns in lines
  let date: string | undefined;
  for (const line of lines) {
    const dateMatch = line.match(/(20\d{2}[./-]\d{1,2}[./-]\d{1,2})/);
    if (dateMatch) {
      date = normalizeDate(dateMatch[1]);
      break;
    }
  }

  // Calculate overall parsing confidence score
  let score = 0;
  if (totalMinor && totalMinor > 0) score += 0.45;
  if (merchant) score += 0.25;
  if (date) score += 0.15;
  if (paymentMethod !== 'unknown') score += 0.1;
  if (items.length > 0) score += 0.05;

  return {
    merchant,
    date: date || new Date().toISOString().substring(0, 10),
    totalMinor,
    taxMinor,
    currency,
    paymentMethod,
    items,
    confidence: Math.min(1.0, score),
    rawText,
  };
}
