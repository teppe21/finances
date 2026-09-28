import {
  RawNotification,
  ParsedNotificationTransaction,
  NotificationSourceConfig,
} from '../../types';
import { canHandleRevolut, parseRevolutNotification } from './profiles/revolut';
import { canHandleOtp, parseOtpNotification } from './profiles/otp';
import { canHandleErste, parseErsteNotification } from './profiles/erste';
import { canHandleMbh, parseMbhNotification } from './profiles/mbh';
import { canHandleWise, parseWiseNotification } from './profiles/wise';
import { parseGenericNotification } from './profiles/generic';

export interface ParseNotificationResult {
  status: 'parsed' | 'ignored' | 'needs_review' | 'unsupported_source';
  bankProfileId?: string;
  transaction?: ParsedNotificationTransaction;
  reason?: string;
}

export const DEFAULT_NOTIFICATION_SOURCES: NotificationSourceConfig[] = [
  {
    packageName: 'com.revolut.revolut',
    displayName: 'Revolut',
    enabled: true,
    bankProfileId: 'revolut',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    packageName: 'hu.otpbank.smartbank',
    displayName: 'OTP SmartBank / MobilBank',
    enabled: true,
    bankProfileId: 'otp',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    packageName: 'hu.otpbank.simple',
    displayName: 'OTP Simple',
    enabled: true,
    bankProfileId: 'otp',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    packageName: 'hu.erstebank.george.app',
    displayName: 'Erste George',
    enabled: true,
    bankProfileId: 'erste',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    packageName: 'hu.mbhbank.app',
    displayName: 'MBH Bank',
    enabled: true,
    bankProfileId: 'mbh',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    packageName: 'com.transferwise.android',
    displayName: 'Wise',
    enabled: true,
    bankProfileId: 'wise',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

/**
 * Main bank notification ingestion engine.
 * Filters by enabled packages, selects profile parser, extracts financial data, and scores confidence.
 */
export function processNotification(
  notification: RawNotification,
  enabledSources: NotificationSourceConfig[] = DEFAULT_NOTIFICATION_SOURCES
): ParseNotificationResult {
  const source = enabledSources.find(
    (s) => s.enabled && s.packageName.toLowerCase() === notification.packageName.toLowerCase()
  );

  // If source is not in user's enabled sources list, ignore for privacy and performance
  if (!source) {
    return {
      status: 'unsupported_source',
      reason: `Package ${notification.packageName} is not an enabled bank source`,
    };
  }

  let profileId = source.bankProfileId;
  let parsed: ParsedNotificationTransaction | null = null;

  if (canHandleRevolut(notification)) {
    profileId = 'revolut';
    parsed = parseRevolutNotification(notification);
  } else if (canHandleOtp(notification)) {
    profileId = 'otp';
    parsed = parseOtpNotification(notification);
  } else if (canHandleErste(notification)) {
    profileId = 'erste';
    parsed = parseErsteNotification(notification);
  } else if (canHandleMbh(notification)) {
    profileId = 'mbh';
    parsed = parseMbhNotification(notification);
  } else if (canHandleWise(notification)) {
    profileId = 'wise';
    parsed = parseWiseNotification(notification);
  } else {
    // Generic fallback for configured source
    profileId = 'generic';
    parsed = parseGenericNotification(notification);
  }

  if (!parsed) {
    return {
      status: 'ignored',
      bankProfileId: profileId,
      reason: 'Notification does not contain financial payment data',
    };
  }

  // Evaluate confidence
  if (parsed.confidence >= 0.85) {
    return {
      status: 'parsed',
      bankProfileId: profileId,
      transaction: parsed,
    };
  } else {
    return {
      status: 'needs_review',
      bankProfileId: profileId,
      transaction: parsed,
      reason: 'Low or moderate confidence parsing result',
    };
  }
}
