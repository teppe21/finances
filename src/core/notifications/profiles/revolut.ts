import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export const REVOLUT_PACKAGES = ['com.revolut.revolut'];

export function canHandleRevolut(notification: RawNotification): boolean {
  if (REVOLUT_PACKAGES.includes(notification.packageName)) return true;
  const label = (notification.applicationLabel || '').toLowerCase();
  return label.includes('revolut');
}

export function parseRevolutNotification(
  notification: RawNotification
): ParsedNotificationTransaction | null {
  const title = notification.title || '';
  const body = `${notification.text || ''} ${notification.bigText || ''}`.trim();
  const fullText = `${title} ${body}`.trim();

  if (!fullText) return null;

  // Non-financial notifications filter
  if (
    fullText.includes('biztonsági kód') ||
    fullText.includes('security code') ||
    fullText.includes('új funkció') ||
    fullText.includes('new feature')
  ) {
    return null;
  }

  const extracted = extractAmountAndCurrency(fullText, 'HUF');
  if (!extracted) return null;

  let direction: TransactionDirection = 'expense';
  let merchant = '';

  const lower = fullText.toLowerCase();

  if (lower.includes('refund') || lower.includes('visszatérítés')) {
    direction = 'refund';
  } else if (
    lower.includes('received') ||
    lower.includes('kapott') ||
    lower.includes('érkezett') ||
    lower.includes('jóváírás')
  ) {
    direction = 'income';
  } else {
    direction = 'expense';
  }

  // Merchant extraction patterns:
  // "Fizetés a következőnek: LIDL..."
  // "Paid 14,500 HUF to LIDL"
  // "elköltöttél ... itt: SPAR"
  // "You spent ... at Tesco"
  const huPaidMatch = fullText.match(/(?:következőnek|itt:|elfogadóhely:?)\s*([^,.\n]+)/i);
  const enPaidMatch = fullText.match(/(?:to|at)\s+([A-Za-z0-9\s&'-]+?)(?:\s+with|\s+from|\s+for|\.|$)/i);

  if (huPaidMatch) {
    merchant = cleanMerchantName(huPaidMatch[1]);
  } else if (enPaidMatch) {
    merchant = cleanMerchantName(enPaidMatch[1]);
  } else if (title && !title.toLowerCase().includes('revolut') && !title.toLowerCase().includes('fizetés')) {
    merchant = cleanMerchantName(title);
  } else {
    merchant = 'Revolut Partner';
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
    suggestedAccountName: 'Revolut',
    confidence: merchant && merchant !== 'Revolut Partner' ? 0.95 : 0.85,
    rawTextExcerpt: fullText,
  };
}
