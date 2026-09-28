import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export function parseGenericNotification(
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

  if (
    lower.includes('jóváírás') ||
    lower.includes('érkezett') ||
    lower.includes('received') ||
    lower.includes('salary') ||
    lower.includes('fizetés')
  ) {
    direction = 'income';
  } else if (lower.includes('utalás') || lower.includes('transfer')) {
    direction = 'transfer';
  } else {
    direction = 'expense';
  }

  let merchant = title && !title.toLowerCase().includes('bank') ? cleanMerchantName(title) : 'Bank Partner';

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
    suggestedAccountName: notification.applicationLabel || 'Bank',
    confidence: 0.65, // Generic has moderate confidence
    rawTextExcerpt: fullText,
  };
}
