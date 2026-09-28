import { CurrencyCode } from '../../types';

export interface CurrencyMeta {
  code: CurrencyCode;
  symbol: string;
  decimals: number; // e.g. 2 for HUF (100 minor units = 1 HUF), 2 for EUR, 2 for USD
  name: string;
}

export const SUPPORTED_CURRENCIES: Record<string, CurrencyMeta> = {
  HUF: { code: 'HUF', symbol: 'Ft', decimals: 2, name: 'Magyar Forint' },
  EUR: { code: 'EUR', symbol: '€', decimals: 2, name: 'Euro' },
  USD: { code: 'USD', symbol: '$', decimals: 2, name: 'US Dollar' },
};

/**
 * Converts major units to minor units (e.g. 14500 HUF -> 1450000 minor units, 12.50 EUR -> 1250 minor units).
 * Uses Math.round to avoid floating point precision issues.
 */
export function toMinorUnits(amountMajor: number, currency: CurrencyCode = 'HUF'): number {
  const meta = SUPPORTED_CURRENCIES[currency] || { decimals: 2 };
  const factor = Math.pow(10, meta.decimals);
  return Math.round(amountMajor * factor);
}

/**
 * Converts minor units to major units (e.g. 1450000 minor units -> 14500 HUF, 1250 minor units -> 12.5 EUR).
 */
export function fromMinorUnits(amountMinor: number, currency: CurrencyCode = 'HUF'): number {
  const meta = SUPPORTED_CURRENCIES[currency] || { decimals: 2 };
  const factor = Math.pow(10, meta.decimals);
  return amountMinor / factor;
}

/**
 * Format minor units into human readable localized currency string.
 */
export function formatCurrency(
  amountMinor: number,
  currency: CurrencyCode = 'HUF',
  locale: string = 'hu-HU'
): string {
  const major = fromMinorUnits(amountMinor, currency);
  const meta = SUPPORTED_CURRENCIES[currency] || { decimals: 2 };

  // For HUF in Hungarian locale, integer display is customary unless fractional fillér exists
  const maxFraction = currency === 'HUF' && Math.round(major) === major ? 0 : meta.decimals;

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: maxFraction,
    maximumFractionDigits: maxFraction,
  }).format(major);
}
