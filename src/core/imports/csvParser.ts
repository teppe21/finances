import { CurrencyCode, TransactionDirection, TransactionSource } from '../../types';
import { extractAmountAndCurrency, parseNumericAmount } from '../normalization/amount';
import { normalizeDate } from '../normalization/date';
import { cleanMerchantName } from '../normalization/text';
import { toMinorUnits } from '../normalization/currency';
import { generateTransactionFingerprint } from '../deduplication/fingerprint';
import { generateId } from '../../utils/hashing';

export interface CsvColumnMapping {
  dateCol: number;
  descriptionCol: number;
  amountCol?: number;
  debitCol?: number;
  creditCol?: number;
  categoryCol?: number;
  currencyCol?: number;
  sourceCol?: number;
}

export interface ParsedCsvRow {
  date: string;
  description: string;
  merchant?: string;
  amountMinor: number;
  currency: CurrencyCode;
  direction: TransactionDirection;
  categoryName?: string;
  source: TransactionSource;
  fingerprint: string;
  rawLineIndex: number;
}

/**
 * Robust CSV Line Tokenizer handling quotes, escaped quotes (""), and varied delimiters.
 */
export function tokenizeCsvLine(line: string, delimiter: string = ','): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];

    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++; // skip escaped quote
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === delimiter && !inQuotes) {
      result.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }

  result.push(current.trim());
  return result;
}

/**
 * Detects delimiter (comma, semicolon, tab) by analyzing the first few rows.
 */
export function detectCsvDelimiter(text: string): string {
  const sample = text.substring(0, 2048);
  const semicolons = (sample.match(/;/g) || []).length;
  const commas = (sample.match(/,/g) || []).length;
  const tabs = (sample.match(/\t/g) || []).length;

  if (semicolons > commas && semicolons > tabs) return ';';
  if (tabs > commas && tabs > semicolons) return '\t';
  return ',';
}

/**
 * Detects CSV column indices from header row.
 */
export function detectColumnMapping(headers: string[]): CsvColumnMapping {
  const normHeaders = headers.map((h) => h.toLowerCase().trim().replace(/^["']|["']$/g, ''));

  const findIdx = (...keywords: string[]): number => {
    return normHeaders.findIndex((h) => keywords.some((kw) => h.includes(kw)));
  };

  const dateCol = findIdx('dátum', 'datum', 'date', 'started date', 'könyvelés napja', 'értéknap', 'booking date');
  const descCol = findIdx('leírás', 'leiras', 'description', 'partner', 'partner neve', 'megjegyzés', 'közlemény', 'payee', 'merchant');
  const amountCol = findIdx('összeg', 'osszeg', 'amount', 'érték', 'ertek');
  const debitCol = findIdx('terhelés', 'terheles', 'kiadás', 'debit', 'outflow');
  const creditCol = findIdx('jóváírás', 'jovairas', 'bevétel', 'credit', 'inflow');
  const categoryCol = findIdx('kategória', 'kategoria', 'category');
  const currencyCol = findIdx('pénznem', 'penznem', 'currency', 'deviza');
  const sourceCol = findIdx('számla', 'szamla', 'account', 'forrás');

  return {
    dateCol: dateCol !== -1 ? dateCol : 0,
    descriptionCol: descCol !== -1 ? descCol : 1,
    amountCol: amountCol !== -1 ? amountCol : undefined,
    debitCol: debitCol !== -1 ? debitCol : undefined,
    creditCol: creditCol !== -1 ? creditCol : undefined,
    categoryCol: categoryCol !== -1 ? categoryCol : undefined,
    currencyCol: currencyCol !== -1 ? currencyCol : undefined,
    sourceCol: sourceCol !== -1 ? sourceCol : undefined,
  };
}

/**
 * Robust CSV parser that handles BOM, escaped quotes, semicolons, tabs, and European numbers.
 */
export function parseCsvFile(
  content: string,
  options: {
    sourceName?: string;
    defaultCurrency?: CurrencyCode;
    mapping?: CsvColumnMapping;
  } = {}
): ParsedCsvRow[] {
  // Strip UTF-8 BOM
  let cleanContent = content.startsWith('\uFEFF') ? content.substring(1) : content;

  const lines = cleanContent.split(/\r?\n/).filter((l) => l.trim().length > 0);
  if (lines.length < 2) return [];

  const delimiter = detectCsvDelimiter(cleanContent);
  const headerCells = tokenizeCsvLine(lines[0], delimiter);
  const mapping = options.mapping || detectColumnMapping(headerCells);
  const defaultCurrency: CurrencyCode = options.defaultCurrency || 'HUF';

  const rows: ParsedCsvRow[] = [];

  for (let i = 1; i < lines.length; i++) {
    const cells = tokenizeCsvLine(lines[i], delimiter);
    if (cells.length < 2) continue;

    const rawDate = cells[mapping.dateCol] || '';
    const rawDesc = cells[mapping.descriptionCol] || 'Tranzakció';
    const date = normalizeDate(rawDate);

    let amountMajor = 0;
    let currency: CurrencyCode = defaultCurrency;

    if (mapping.currencyCol !== undefined && cells[mapping.currencyCol]) {
      const parsedCurr = cells[mapping.currencyCol].trim().toUpperCase();
      if (['HUF', 'EUR', 'USD'].includes(parsedCurr)) {
        currency = parsedCurr as CurrencyCode;
      }
    }

    if (mapping.debitCol !== undefined && mapping.creditCol !== undefined) {
      const debitRaw = cells[mapping.debitCol] || '';
      const creditRaw = cells[mapping.creditCol] || '';

      if (debitRaw && debitRaw !== '0') {
        amountMajor = -Math.abs(parseNumericAmount(debitRaw));
      } else if (creditRaw && creditRaw !== '0') {
        amountMajor = Math.abs(parseNumericAmount(creditRaw));
      }
    } else if (mapping.amountCol !== undefined && cells[mapping.amountCol]) {
      const extracted = extractAmountAndCurrency(cells[mapping.amountCol], currency);
      if (extracted) {
        amountMajor = extracted.amountMajor;
        currency = extracted.currency;
      } else {
        amountMajor = parseNumericAmount(cells[mapping.amountCol]);
      }
    }

    const amountMinor = toMinorUnits(amountMajor, currency);
    const direction: TransactionDirection =
      amountMajor > 0 ? 'income' : amountMajor < 0 ? 'expense' : 'adjustment';

    const merchant = cleanMerchantName(rawDesc);
    const fingerprint = generateTransactionFingerprint({
      date,
      amountMinor,
      currency,
      description: rawDesc,
      merchant,
    });

    rows.push({
      date,
      description: rawDesc,
      merchant: merchant || undefined,
      amountMinor,
      currency,
      direction,
      categoryName: mapping.categoryCol !== undefined ? cells[mapping.categoryCol] : undefined,
      source: 'csv',
      fingerprint,
      rawLineIndex: i,
    });
  }

  return rows;
}
