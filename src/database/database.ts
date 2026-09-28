import { SQL_CREATE_TABLES } from './schema/tables';
import { DEFAULT_CATEGORIES, DEFAULT_CATEGORY_RULES } from '../core/categorization/rules';
import { DEFAULT_NOTIFICATION_SOURCES } from '../core/notifications/notificationEngine';

export interface DatabaseDriver {
  execute(sql: string, params?: any[]): Promise<void>;
  query<T = any>(sql: string, params?: any[]): Promise<T[]>;
  queryOne<T = any>(sql: string, params?: any[]): Promise<T | null>;
  close(): Promise<void>;
}

/**
 * High-performance In-Memory SQL & Entity Driver for unit testing and offline fallback.
 */
export class MemoryDatabaseDriver implements DatabaseDriver {
  private tables: Map<string, Map<string, any>> = new Map();

  constructor() {
    this.initTables();
  }

  private initTables() {
    const tableNames = [
      'accounts',
      'categories',
      'category_rules',
      'notification_sources',
      'notification_events',
      'receipt_scans',
      'transactions',
      'budgets',
      'saved_periods',
      'import_sessions',
      'settings',
    ];
    tableNames.forEach((t) => this.tables.set(t, new Map()));
  }

  async execute(sql: string, params: any[] = []): Promise<void> {
    const trimmed = sql.trim();
    if (trimmed.startsWith('CREATE TABLE') || trimmed.startsWith('CREATE INDEX')) {
      return;
    }

    const insertMatch = trimmed.match(/^INSERT (?:OR REPLACE )?INTO (\w+)\s*\(([^)]+)\)\s*VALUES\s*\(([^)]+)\)/i);
    if (insertMatch) {
      const tableName = insertMatch[1].toLowerCase();
      const cols = insertMatch[2].split(',').map((c) => c.trim());
      const record: any = {};
      cols.forEach((col, idx) => {
        record[col] = params[idx];
      });

      const table = this.tables.get(tableName) || new Map();
      const idKey = record.id || record.packageName || record.key || `id_${Date.now()}_${Math.random()}`;
      table.set(String(idKey), record);
      this.tables.set(tableName, table);
      return;
    }

    const updateMatch = trimmed.match(/^UPDATE (\w+)\s+SET\s+(.+?)\s+WHERE\s+(.+)/i);
    if (updateMatch) {
      const tableName = updateMatch[1].toLowerCase();
      const table = this.tables.get(tableName);
      if (!table) return;

      const whereClause = updateMatch[3];
      // Basic id match e.g. "id = ?"
      if (whereClause.includes('id = ?')) {
        const idVal = String(params[params.length - 1]);
        const record = table.get(idVal);
        if (record) {
          const setPairs = updateMatch[2].split(',').map((p) => p.trim().split('=')[0].trim());
          setPairs.forEach((col, idx) => {
            record[col] = params[idx];
          });
          table.set(idVal, record);
        }
      }
      return;
    }

    const deleteMatch = trimmed.match(/^DELETE FROM (\w+)\s*(?:WHERE (.+))?/i);
    if (deleteMatch) {
      const tableName = deleteMatch[1].toLowerCase();
      const table = this.tables.get(tableName);
      if (!table) return;

      if (!deleteMatch[2]) {
        table.clear();
      } else if (deleteMatch[2].includes('id = ?')) {
        const idVal = String(params[0]);
        table.delete(idVal);
      }
      return;
    }
  }

  async query<T = any>(sql: string, params: any[] = []): Promise<T[]> {
    const trimmed = sql.trim();
    const selectMatch = trimmed.match(/^SELECT (.+?) FROM (\w+)(?:\s+WHERE (.+?))?(?:\s+ORDER BY (.+?))?(?:\s+LIMIT (\d+))?$/i);
    if (selectMatch) {
      const tableName = selectMatch[2].toLowerCase();
      const table = this.tables.get(tableName);
      if (!table) return [];

      let list = Array.from(table.values());

      const where = selectMatch[3];
      if (where) {
        if (where.includes('accountId = ?')) {
          const accId = params[0];
          list = list.filter((r) => r.accountId === accId);
        } else if (where.includes('id = ?')) {
          const id = params[0];
          list = list.filter((r) => r.id === id);
        } else if (where.includes('key = ?')) {
          const k = params[0];
          list = list.filter((r) => r.key === k);
        }
      }

      const orderBy = selectMatch[4];
      if (orderBy && orderBy.includes('date DESC')) {
        list.sort((a, b) => (b.date || '').localeCompare(a.date || ''));
      }

      const limit = selectMatch[5] ? parseInt(selectMatch[5], 10) : undefined;
      if (limit) {
        list = list.slice(0, limit);
      }

      return list as T[];
    }
    return [];
  }

  async queryOne<T = any>(sql: string, params: any[] = []): Promise<T | null> {
    const rows = await this.query<T>(sql, params);
    return rows.length > 0 ? rows[0] : null;
  }

  async close(): Promise<void> {
    this.tables.clear();
  }
}

let activeDriver: DatabaseDriver | null = null;

export function getDatabase(): DatabaseDriver {
  if (!activeDriver) {
    activeDriver = new MemoryDatabaseDriver();
  }
  return activeDriver;
}

export function setDatabaseDriver(driver: DatabaseDriver): void {
  activeDriver = driver;
}

/**
 * Initializes database tables and default accounts, categories, rules, and sources.
 */
export async function initializeDatabase(db: DatabaseDriver = getDatabase()): Promise<void> {
  // 1. Create tables
  const statements = SQL_CREATE_TABLES.split(';')
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

  for (const stmt of statements) {
    await db.execute(stmt);
  }

  // 2. Seed Default Cash Account if not present
  const existingAccounts = await db.query('SELECT * FROM accounts');
  if (existingAccounts.length === 0) {
    const now = new Date().toISOString();
    await db.execute(
      `INSERT INTO accounts (id, name, institution, type, currency, openingBalanceMinor, isActive, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      ['acc_cash', 'Készpénz', 'Cash', 'cash', 'HUF', 0, 1, now, now]
    );
    await db.execute(
      `INSERT INTO accounts (id, name, institution, type, currency, openingBalanceMinor, isActive, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      ['acc_revolut', 'Revolut Főszámla', 'Revolut', 'bank', 'HUF', 0, 1, now, now]
    );
    await db.execute(
      `INSERT INTO accounts (id, name, institution, type, currency, openingBalanceMinor, isActive, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      ['acc_otp', 'OTP Lakossági', 'OTP Bank', 'bank', 'HUF', 0, 1, now, now]
    );
  }

  // 3. Seed Default Categories
  const existingCategories = await db.query('SELECT * FROM categories');
  if (existingCategories.length === 0) {
    for (const cat of DEFAULT_CATEGORIES) {
      await db.execute(
        `INSERT INTO categories (id, name, icon, color, isIncome, isDefault, createdAt, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        [cat.id, cat.name, cat.icon || '', cat.color || '', cat.isIncome ? 1 : 0, 1, cat.createdAt, cat.updatedAt]
      );
    }
  }

  // 4. Seed Category Rules
  const existingRules = await db.query('SELECT * FROM category_rules');
  if (existingRules.length === 0) {
    for (const rule of DEFAULT_CATEGORY_RULES) {
      await db.execute(
        `INSERT INTO category_rules (id, categoryId, pattern, matchType, priority, isActive, createdAt)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [rule.id, rule.categoryId, rule.pattern, rule.matchType, rule.priority, rule.isActive ? 1 : 0, rule.createdAt]
      );
    }
  }

  // 5. Seed Default Notification Sources
  const existingSources = await db.query('SELECT * FROM notification_sources');
  if (existingSources.length === 0) {
    for (const src of DEFAULT_NOTIFICATION_SOURCES) {
      await db.execute(
        `INSERT INTO notification_sources (packageName, displayName, enabled, bankProfileId, createdAt, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?)`,
        [src.packageName, src.displayName, src.enabled ? 1 : 0, src.bankProfileId, src.createdAt, src.updatedAt]
      );
    }
  }
}
