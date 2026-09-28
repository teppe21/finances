import { describe, it, expect } from 'vitest';
import { processNotification, DEFAULT_NOTIFICATION_SOURCES } from '../../src/core/notifications/notificationEngine';
import revolutFixtures from '../fixtures/notifications/revolut.json';
import otpFixtures from '../fixtures/notifications/otp.json';
import ersteFixtures from '../fixtures/notifications/erste.json';

describe('Bank Notification Processing Engine', () => {
  it('parses Revolut notification fixtures correctly', () => {
    for (const fixture of revolutFixtures) {
      const res = processNotification(fixture.raw as any, DEFAULT_NOTIFICATION_SOURCES);
      expect(res.status).toBe('parsed');
      expect(res.bankProfileId).toBe('revolut');
      expect(res.transaction).toBeDefined();

      if (fixture.expected.merchant) {
        expect(res.transaction?.merchant).toContain(fixture.expected.merchant);
      }
      expect(res.transaction?.amountMinor).toBe(fixture.expected.amountMinor);
      expect(res.transaction?.currency).toBe(fixture.expected.currency);
      expect(res.transaction?.direction).toBe(fixture.expected.direction);
    }
  });

  it('parses OTP notification fixtures correctly', () => {
    for (const fixture of otpFixtures) {
      const res = processNotification(fixture.raw as any, DEFAULT_NOTIFICATION_SOURCES);
      expect(res.status).toBe('parsed');
      expect(res.bankProfileId).toBe('otp');
      expect(res.transaction).toBeDefined();

      if (fixture.expected.merchant) {
        expect(res.transaction?.merchant?.toLowerCase()).toContain(
          fixture.expected.merchant.toLowerCase()
        );
      }
      expect(res.transaction?.amountMinor).toBe(fixture.expected.amountMinor);
      expect(res.transaction?.currency).toBe(fixture.expected.currency);
      expect(res.transaction?.direction).toBe(fixture.expected.direction);
    }
  });

  it('parses Erste notification fixtures correctly', () => {
    for (const fixture of ersteFixtures) {
      const res = processNotification(fixture.raw as any, DEFAULT_NOTIFICATION_SOURCES);
      expect(res.status).toBe('parsed');
      expect(res.bankProfileId).toBe('erste');
      expect(res.transaction).toBeDefined();
      expect(res.transaction?.amountMinor).toBe(fixture.expected.amountMinor);
      expect(res.transaction?.direction).toBe(fixture.expected.direction);
    }
  });

  it('ignores unauthorized apps (WhatsApp, Messenger, YouTube) for privacy', () => {
    const chatNotification = {
      packageName: 'com.whatsapp',
      title: 'Mom',
      text: 'Can you transfer 5000 Ft please?',
      postedAt: '2026-03-01T12:00:00Z',
    };

    const res = processNotification(chatNotification, DEFAULT_NOTIFICATION_SOURCES);
    expect(res.status).toBe('unsupported_source');
  });

  it('ignores non-financial notifications from banking apps (e.g. security codes)', () => {
    const secNotification = {
      packageName: 'com.revolut.revolut',
      title: 'Revolut biztonsági kód',
      text: 'A belépési kódod: 123456. Soha ne add ki másnak!',
      postedAt: '2026-03-01T12:00:00Z',
    };

    const res = processNotification(secNotification, DEFAULT_NOTIFICATION_SOURCES);
    expect(res.status).toBe('ignored');
  });
});
