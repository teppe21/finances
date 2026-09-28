import { describe, it, expect } from 'vitest';
import { parseReceiptText } from '../../src/core/receipts/receiptParser';
import receiptFixtures from '../fixtures/receipts/receipts.json';

describe('Receipt OCR Parser & Hungarian Semantics', () => {
  it('parses supermarket receipt with cash payment and tax', () => {
    const fixture = receiptFixtures[0];
    const parsed = parseReceiptText(fixture.rawOcrText);

    expect(parsed.merchant).toBe(fixture.expected.merchant);
    expect(parsed.totalMinor).toBe(fixture.expected.totalMinor);
    expect(parsed.taxMinor).toBe(fixture.expected.taxMinor);
    expect(parsed.paymentMethod).toBe(fixture.expected.paymentMethod);
    expect(parsed.date).toBe(fixture.expected.date);
    expect(parsed.confidence).toBeGreaterThanOrEqual(0.8);
    expect(parsed.items.length).toBeGreaterThan(0);
  });

  it('parses restaurant receipt with card payment', () => {
    const fixture = receiptFixtures[1];
    const parsed = parseReceiptText(fixture.rawOcrText);

    expect(parsed.merchant).toBe(fixture.expected.merchant);
    expect(parsed.totalMinor).toBe(fixture.expected.totalMinor);
    expect(parsed.paymentMethod).toBe(fixture.expected.paymentMethod);
    expect(parsed.date).toBe(fixture.expected.date);
  });

  it('tolerates OCR noise like FIZETEND0', () => {
    const fixture = receiptFixtures[2];
    const parsed = parseReceiptText(fixture.rawOcrText);

    expect(parsed.merchant).toBe(fixture.expected.merchant);
    expect(parsed.totalMinor).toBe(fixture.expected.totalMinor);
    expect(parsed.paymentMethod).toBe(fixture.expected.paymentMethod);
  });

  it('distinguishes total from change and amount tendered', () => {
    const raw = `
      CBA PRÍMA
      NYUGTA 2026.03.10
      KENYÉR 800 Ft
      FIZETENDŐ: 800 Ft
      KÉSZPÉNZ: 1 000 Ft
      VISSZAJÁRÓ: 200 Ft
    `;
    const parsed = parseReceiptText(raw);

    // Total must be 800 Ft (80,000 minor), NOT 1,000 Ft or 200 Ft!
    expect(parsed.totalMinor).toBe(80000);
    expect(parsed.paymentMethod).toBe('cash');
  });
});
