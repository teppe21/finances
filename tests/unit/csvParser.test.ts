import { describe, it, expect } from 'vitest';
import { parseCsvFile, detectCsvDelimiter, tokenizeCsvLine } from '../../src/core/imports/csvParser';

describe('CSV & Bank Statement Parser', () => {
  it('correctly tokenizes CSV lines with quotes and commas', () => {
    const line = '"2026-03-01","Lidl, Debrecen Kishegyesi út","-14500","Élelmiszer"';
    const tokens = tokenizeCsvLine(line, ',');
    expect(tokens.length).toBe(4);
    expect(tokens[1]).toBe('Lidl, Debrecen Kishegyesi út');
  });

  it('detects semicolon delimiter', () => {
    const csv = 'Dátum;Leírás;Összeg\n2026.03.01;SPAR;-4500\n';
    expect(detectCsvDelimiter(csv)).toBe(';');
  });

  it('parses European semicolon CSV with Hungarian numbers and UTF-8 BOM', () => {
    const csvWithBom =
      '\uFEFFDátum;Leírás;Összeg;Pénznem\r\n' +
      '2026.03.01;Lidl bevásárlás;-14.500;HUF\r\n' +
      '2026.03.02;Fizetés jóváírás;650.000;HUF\r\n';

    const rows = parseCsvFile(csvWithBom);
    expect(rows.length).toBe(2);
    expect(rows[0].date).toBe('2026-03-01');
    expect(rows[0].amountMinor).toBe(-1450000);
    expect(rows[0].direction).toBe('expense');

    expect(rows[1].date).toBe('2026-03-02');
    expect(rows[1].amountMinor).toBe(65000000);
    expect(rows[1].direction).toBe('income');
  });

  it('parses bank CSV with separate Debit and Credit columns', () => {
    const csv =
      'Date,Description,Debit,Credit,Currency\n' +
      '2026-03-01,Netflix subscription,4490,,HUF\n' +
      '2026-03-02,Client invoice payment,,120000,HUF\n';

    const rows = parseCsvFile(csv);
    expect(rows.length).toBe(2);
    expect(rows[0].amountMinor).toBe(-449000);
    expect(rows[0].direction).toBe('expense');

    expect(rows[1].amountMinor).toBe(12000000);
    expect(rows[1].direction).toBe('income');
  });
});
