import { Transaction } from '../../types';
import { fromMinorUnits } from '../../core/normalization/currency';

/**
 * Generates RFC 4180 compliant CSV content with UTF-8 BOM for Excel compatibility.
 */
export function generateCsvContent(transactions: Transaction[]): string {
  const headers = ['Dátum', 'Leírás', 'Partner', 'Összeg', 'Pénznem', 'Kategória', 'Számla / Forrás', 'Típus'];

  const rows = transactions.map((t) => {
    const majorAmount = fromMinorUnits(t.amountMinor, t.currency);
    return [
      `"${t.date}"`,
      `"${(t.description || '').replace(/"/g, '""')}"`,
      `"${(t.merchant || '').replace(/"/g, '""')}"`,
      majorAmount,
      `"${t.currency}"`,
      `"${t.categoryId || 'other'}"`,
      `"${t.source || 'manual'}"`,
      `"${t.direction}"`,
    ];
  });

  const csvBody = [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n');
  return '\uFEFF' + csvBody;
}
