import { ReceiptScan } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';

export class ReceiptRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAll(): Promise<ReceiptScan[]> {
    const rows = await this.db.query<any>('SELECT * FROM receipt_scans ORDER BY scannedAt DESC');
    return rows.map((r) => ({
      ...r,
      items: r.itemsJson ? JSON.parse(r.itemsJson) : [],
      processedLocally: Boolean(r.processedLocally),
      rawOcrAvailable: Boolean(r.rawOcrAvailable),
    }));
  }

  async getById(id: string): Promise<ReceiptScan | null> {
    const row = await this.db.queryOne<any>('SELECT * FROM receipt_scans WHERE id = ?', [id]);
    if (!row) return null;
    return {
      ...row,
      items: row.itemsJson ? JSON.parse(row.itemsJson) : [],
      processedLocally: Boolean(row.processedLocally),
      rawOcrAvailable: Boolean(row.rawOcrAvailable),
    };
  }

  async create(scan: ReceiptScan): Promise<void> {
    await this.db.execute(
      `INSERT INTO receipt_scans (
        id, imageUri, scannedAt, merchant, date, totalMinor, subtotalMinor,
        taxMinor, currency, paymentMethod, itemsJson, rawOcrAvailable,
        rawOcrText, confidence, processedLocally, transactionId
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        scan.id,
        scan.imageUri || '',
        scan.scannedAt,
        scan.merchant || '',
        scan.date || '',
        scan.totalMinor || 0,
        scan.subtotalMinor || 0,
        scan.taxMinor || 0,
        scan.currency,
        scan.paymentMethod,
        JSON.stringify(scan.items || []),
        scan.rawOcrAvailable ? 1 : 0,
        scan.rawOcrText || '',
        scan.confidence,
        scan.processedLocally ? 1 : 0,
        scan.transactionId || '',
      ]
    );
  }

  /**
   * Deletes receipt image URI while keeping the transaction record intact (Privacy feature #45, #77).
   */
  async deleteReceiptImage(id: string): Promise<void> {
    await this.db.execute('UPDATE receipt_scans SET imageUri = "" WHERE id = ?', [id]);
  }
}
