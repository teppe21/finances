import { Account, AccountType, CurrencyCode } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';

export class AccountRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAll(): Promise<Account[]> {
    return this.db.query<Account>('SELECT * FROM accounts WHERE isActive = 1');
  }

  async getById(id: string): Promise<Account | null> {
    return this.db.queryOne<Account>('SELECT * FROM accounts WHERE id = ?', [id]);
  }

  async getCashAccount(): Promise<Account | null> {
    const accounts = await this.getAll();
    return accounts.find((a) => a.type === 'cash') || null;
  }

  async findByInstitution(institution: string): Promise<Account | null> {
    const accounts = await this.getAll();
    const norm = institution.toLowerCase();
    return (
      accounts.find(
        (a) =>
          a.institution.toLowerCase().includes(norm) ||
          a.name.toLowerCase().includes(norm)
      ) || null
    );
  }

  async create(account: Account): Promise<void> {
    await this.db.execute(
      `INSERT INTO accounts (id, name, institution, type, currency, openingBalanceMinor, isActive, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        account.id,
        account.name,
        account.institution,
        account.type,
        account.currency,
        account.openingBalanceMinor,
        account.isActive ? 1 : 0,
        account.createdAt,
        account.updatedAt,
      ]
    );
  }

  async update(account: Account): Promise<void> {
    await this.db.execute(
      `UPDATE accounts SET name = ?, institution = ?, type = ?, currency = ?, openingBalanceMinor = ?, isActive = ?, updatedAt = ?
       WHERE id = ?`,
      [
        account.name,
        account.institution,
        account.type,
        account.currency,
        account.openingBalanceMinor,
        account.isActive ? 1 : 0,
        account.updatedAt,
        account.id,
      ]
    );
  }
}
