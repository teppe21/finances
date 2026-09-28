import { RawNotification, ParsedNotificationTransaction, TransactionDirection } from '../../../types';
import { extractAmountAndCurrency } from '../../normalization/amount';
import { cleanMerchantName } from '../../normalization/text';
import { normalizeDate } from '../../normalization/date';

export const OTP_PACKAGES = [
  'hu.otpbank.smartbank',
  'hu.otpbank.mobilbank',
  'hu.otpbank.simple',
];

export function canHandleOtp(notification: RawNotification): boolean {
  if (OTP_PACKAGES.includes(notification.packageName)) return true;
  const label = (notification.applicationLabel || '').toLowerCase();
  return label.includes('otp') || label.includes('simple');
}

export function parseOtpNotification(
  notification: RawNotification
): ParsedNotificationTransaction | null {
  const title = notification.title || '';
  const body = `${notification.text || ''} ${notification.bigText || ''}`.trim();
  const fullText = `${title} ${body}`.trim();

  if (!fullText) return null;

  // Ignore marketing or auth OTP codes
  if (fullText.includes('belépési kód') || fullText.includes('SMS kód') || fullText.includes('jóváhagyás')) {
    return null;
  }

  const extracted = extractAmountAndCurrency(fullText, 'HUF');
  if (!extracted) return null;

  let direction: TransactionDirection = 'expense';
  const lower = fullText.toLowerCase();

  if (lower.includes('jóváírás') || lower.includes('jovairas') || lower.includes('beérkező') || lower.includes('munkabér')) {
    direction = 'income';
  } else if (lower.includes('átutalás') && !lower.includes('sikeres fizetés')) {
    direction = 'transfer';
  } else {
    direction = 'expense';
  }

  // Merchant extraction:
  // "Hely: SPAR BUDAPEST"
  // "-4.500 Ft - McDonald's"
  // "Partner: MVM Next"
  let merchant = '';
  const placeMatch = fullText.match(/(?:Hely|Partner|Elfogadóhely):\s*([^,.\n]+)/i);
  const dashMatch = fullText.match(/[-–]\s*([A-Za-z0-9\s&'-]+?)(?:\s*,|\s*Kártya|\.|$)/i);

  if (placeMatch) {
    merchant = cleanMerchantName(placeMatch[1]);
  } else if (dashMatch && !dashMatch[1].toLowerCase().includes('ft')) {
    merchant = cleanMerchantName(dashMatch[1]);
  } else {
    merchant = 'OTP Tranzakció';
  }

  // Extract card suffix if present: e.g. "Kártya: *1234"
  const cardMatch = fullText.match(/kártya:?\s*\*?(\d{4})/i);
  const cardLast4 = cardMatch ? cardMatch[1] : undefined;

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
    suggestedAccountName: 'OTP Bank',
    cardLast4,
    confidence: merchant && merchant !== 'OTP Tranzakció' ? 0.95 : 0.85,
    rawTextExcerpt: fullText,
  };
}
