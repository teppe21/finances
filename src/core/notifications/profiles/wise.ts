import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export const WISE_PACKAGES = ['com.transferwise.android'];

export function canHandleWise(notification: RawNotification): boolean {
  if (WISE_PACKAGES.includes(notification.packageName)) return true;
  const label = (notification.applicationLabel || '').toLowerCase();
  return label.includes('wise') || label.includes('transferwise');
}

export function parseWiseNotification(
  notification: RawNotification
): ParsedNotificationTransaction | null {
  const title = notification.title || '';
  const body = `${notification.text || ''} ${notification.bigText || ''}`.trim();
  const fullText = `${title} ${body}`.trim();

  if (!fullText) return null;

  const extracted = extractAmountAndCurrency(fullText, 'EUR');
  if (!extracted) return null;

  let direction: TransactionDirection = 'expense';
  const lower = fullText.toLowerCase();

  if (lower.includes('received') || lower.includes('kapott') || lower.includes('érkezett')) {
    direction = 'income';
  } else {
    direction = 'expense';
  }

  let merchant = '';
  const matchSpent = fullText.match(/(?:at|itt:?)\s+([^,.\n]+)/i);
  const matchReceived = fullText.match(/(?:from|küldte:?)\s+([^,.\n]+)/i);

  if (direction === 'expense' && matchSpent) {
    merchant = cleanMerchantName(matchSpent[1]);
  } else if (direction === 'income' && matchReceived) {
    merchant = cleanMerchantName(matchReceived[1]);
  } else {
    merchant = 'Wise Partner';
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
    suggestedAccountName: 'Wise',
    confidence: merchant && merchant !== 'Wise Partner' ? 0.95 : 0.85,
    rawTextExcerpt: fullText,
  };
}
