import { Budget, SavedPeriod } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';

export class BudgetRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAll(): Promise<Budget[]> {
    return this.db.query<Budget>('SELECT * FROM budgets');
  }

  async saveBudget(budget: Budget): Promise<void> {
    await this.db.execute(
      `INSERT OR REPLACE INTO budgets (id, categoryId, amountMinor, period, startDate, endDate, notes)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [
        budget.id,
        budget.categoryId,
        budget.amountMinor,
        budget.period,
        budget.startDate || '',
        budget.endDate || '',
        budget.notes || '',
      ]
    );
  }

  async deleteBudget(id: string): Promise<void> {
    await this.db.execute('DELETE FROM budgets WHERE id = ?', [id]);
  }
}

export class SavedPeriodRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAll(): Promise<SavedPeriod[]> {
    return this.db.query<SavedPeriod>('SELECT * FROM saved_periods ORDER BY periodKey DESC');
  }

  async savePeriod(period: SavedPeriod): Promise<void> {
    await this.db.execute(
      `INSERT OR REPLACE INTO saved_periods (
        id, periodKey, name, savedAt, transactionCount,
        totalIncomeMinor, totalExpenseMinor, balanceMinor, dataJson
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        period.id,
        period.periodKey,
        period.name,
        period.savedAt,
        period.transactionCount,
        period.totalIncomeMinor,
        period.totalExpenseMinor,
        period.balanceMinor,
        period.dataJson || '',
      ]
    );
  }
}

export { SettingsRepository } from './settingsRepository';
