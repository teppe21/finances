import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export const MBH_PACKAGES = ['hu.mbhbank.app', 'hu.takarek.mobilbank', 'hu.mkb.mobilbank'];

export function canHandleMbh(notification: RawNotification): boolean {
  if (MBH_PACKAGES.includes(notification.packageName)) return true;
  const label = (notification.applicationLabel || '').toLowerCase();
  return label.includes('mbh') || label.includes('takarek') || label.includes('mkb');
}

export function parseMbhNotification(
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

  if (lower.includes('jóváírás') || lower.includes('érkezett') || extracted.amountMajor > 0) {
    direction = 'income';
  } else {
    direction = 'expense';
  }

  // Merchant pattern: e.g. "Kártyás tranzakció -8 900 Ft, Yettel Magyarország, Kártya:"
  let merchant = '';
  const match = fullText.match(/[-–]?\s*\d[\d\s.,]*\s*(?:Ft|HUF)\s*,\s*([^,.\n]+)/i);
  if (match) {
    merchant = cleanMerchantName(match[1]);
  } else {
    merchant = 'MBH Tranzakció';
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
    suggestedAccountName: 'MBH Bank',
    confidence: merchant && merchant !== 'MBH Tranzakció' ? 0.95 : 0.85,
    rawTextExcerpt: fullText,
  };
}
