import { Transaction } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';
import { removeDiacritics } from '../../core/normalization/text';

export interface TransactionFilterOptions {
  period?: 'All' | 'this_month' | 'last_month' | 'last_3_months' | 'last_6_months';
  categoryId?: string;
  source?: string;
  searchTerm?: string;
  onlyRecurring?: boolean;
  recurringIds?: Set<string>;
}

export class TransactionRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAll(): Promise<Transaction[]> {
    const rows = await this.db.query<Transaction>('SELECT * FROM transactions ORDER BY date DESC');
    return rows;
  }

  async getById(id: string): Promise<Transaction | null> {
    return this.db.queryOne<Transaction>('SELECT * FROM transactions WHERE id = ?', [id]);
  }

  async getByFingerprint(fingerprint: string): Promise<Transaction | null> {
    const all = await this.getAll();
    return all.find((t) => t.fingerprint === fingerprint) || null;
  }

  async create(tx: Transaction): Promise<void> {
    await this.db.execute(
      `INSERT INTO transactions (
        id, accountId, date, valueDate, amountMinor, currency, direction,
        description, merchant, categoryId, source, sourceAppPackage,
        externalId, fingerprint, receiptId, notificationEventId,
        recurringRuleId, transferId, notes, pending, confidence,
        importedAt, createdAt, updatedAt
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        tx.id,
        tx.accountId,
        tx.date,
        tx.valueDate || '',
        tx.amountMinor,
        tx.currency,
        tx.direction,
        tx.description,
        tx.merchant || '',
        tx.categoryId || '',
        tx.source,
        tx.sourceAppPackage || '',
        tx.externalId || '',
        tx.fingerprint,
        tx.receiptId || '',
        tx.notificationEventId || '',
        tx.recurringRuleId || '',
        tx.transferId || '',
        tx.notes || '',
        tx.pending ? 1 : 0,
        tx.confidence || 1.0,
        tx.importedAt,
        tx.createdAt,
        tx.updatedAt,
      ]
    );
  }

  async update(tx: Transaction): Promise<void> {
    await this.db.execute(
      `UPDATE transactions SET
        accountId = ?, date = ?, valueDate = ?, amountMinor = ?, currency = ?,
        direction = ?, description = ?, merchant = ?, categoryId = ?,
        source = ?, notes = ?, pending = ?, updatedAt = ?
       WHERE id = ?`,
      [
        tx.accountId,
        tx.date,
        tx.valueDate || '',
        tx.amountMinor,
        tx.currency,
        tx.direction,
        tx.description,
        tx.merchant || '',
        tx.categoryId || '',
        tx.source,
        tx.notes || '',
        tx.pending ? 1 : 0,
        tx.updatedAt,
        tx.id,
      ]
    );
  }

  async delete(id: string): Promise<void> {
    await this.db.execute('DELETE FROM transactions WHERE id = ?', [id]);
  }

  async filter(options: TransactionFilterOptions): Promise<Transaction[]> {
    let items = await this.getAll();

    // 1. Period filter
    if (options.period && options.period !== 'All') {
      const now = new Date();
      const currentYear = now.getFullYear();
      const currentMonth = now.getMonth();

      items = items.filter((t) => {
        const d = new Date(t.date);
        if (isNaN(d.getTime())) return true;

        if (options.period === 'this_month') {
          return d.getFullYear() === currentYear && d.getMonth() === currentMonth;
        }
        if (options.period === 'last_month') {
          const lm = new Date(currentYear, currentMonth - 1, 1);
          return d.getFullYear() === lm.getFullYear() && d.getMonth() === lm.getMonth();
        }
        if (options.period === 'last_3_months') {
          const threeAgo = new Date(currentYear, currentMonth - 3, 1);
          return d >= threeAgo;
        }
        if (options.period === 'last_6_months') {
          const sixAgo = new Date(currentYear, currentMonth - 6, 1);
          return d >= sixAgo;
        }
        return true;
      });
    }

    // 2. Category filter
    if (options.categoryId && options.categoryId !== 'All') {
      items = items.filter((t) => t.categoryId === options.categoryId);
    }

    // 3. Source filter
    if (options.source && options.source !== 'All') {
      items = items.filter((t) => t.source === options.source);
    }

    // 4. Recurring filter
    if (options.onlyRecurring && options.recurringIds) {
      items = items.filter((t) => options.recurringIds!.has(t.id));
    }

    // 5. Search term
    if (options.searchTerm && options.searchTerm.trim().length > 0) {
      const termNorm = removeDiacritics(options.searchTerm.trim());
      items = items.filter((t) => {
        const textNorm = removeDiacritics(`${t.merchant || ''} ${t.description || ''} ${t.notes || ''}`);
        return textNorm.includes(termNorm);
      });
    }

    return items;
  }
}
