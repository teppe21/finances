import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export const ERSTE_PACKAGES = ['hu.erstebank.george.app'];

export function canHandleErste(notification: RawNotification): boolean {
  if (ERSTE_PACKAGES.includes(notification.packageName)) return true;
  const label = (notification.applicationLabel || '').toLowerCase();
  return label.includes('erste') || label.includes('george');
}

export function parseErsteNotification(
  notification: RawNotification
): ParsedNotificationTransaction | null {
  const title = notification.title || '';
  const body = `${notification.text || ''} ${notification.bigText || ''}`.trim();
  const fullText = `${title} ${body}`.trim();

  if (!fullText) return null;

  const extracted = extractAmountAndCurrency(fullText, 'HUF');
  if (!extracted) return null;

  let direction: TransactionDirection = 'expense';
  const lower = fullText.toLowerCase();

  if (lower.includes('bejövő') || lower.includes('érkezett') || lower.includes('jóváírás')) {
    direction = 'income';
  } else if (lower.includes('átutalás') && !lower.includes('kártyás')) {
    direction = 'transfer';
  } else {
    direction = 'expense';
  }

  // Merchant pattern: e.g. "a MOL Nyrt. elfogadóhelynél"
  let merchant = '';
  const matchAt = fullText.match(/(?:a|az)\s+([^,.\n]+?)\s+(?:elfogadóhely|üzletben)/i);
  if (matchAt) {
    merchant = cleanMerchantName(matchAt[1]);
  } else {
    merchant = 'Erste Partner';
  }

  const date = normalizeDate(notification.postedAt);
  const absMinor = Math.abs(extracted.amountMinor);

  return {
    amountMinor: direction === 'expense' ? -absMinor : absMinor,
    currency: extracted.currency,
    direction,
    merchant,
    description: fullText.substring(0, 120),
    date,
    valueDate: notification.postedAt,
    suggestedAccountType: 'bank',
    suggestedAccountName: 'Erste Bank',
    confidence: merchant && merchant !== 'Erste Partner' ? 0.95 : 0.85,
    rawTextExcerpt: fullText,
  };
}
