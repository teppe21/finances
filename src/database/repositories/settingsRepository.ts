import { getDatabase, DatabaseDriver } from '../database';

export class SettingsRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async get(key: string, defaultValue: string = ''): Promise<string> {
    const row = await this.db.queryOne<{ key: string; value: string }>(
      'SELECT * FROM settings WHERE key = ?',
      [key]
    );
    return row ? row.value : defaultValue;
  }

  async set(key: string, value: string): Promise<void> {
    await this.db.execute(
      'INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)',
      [key, value]
    );
  }
}
