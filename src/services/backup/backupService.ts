import { Account, Transaction, Category, CategoryRule, Budget, SavedPeriod } from '../../types';
import { AccountRepository } from '../../database/repositories/accountRepository';
import { TransactionRepository } from '../../database/repositories/transactionRepository';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { BudgetRepository, SavedPeriodRepository } from '../../database/repositories/budgetRepository';
import { toMinorUnits } from '../../core/normalization/currency';
import { normalizeDate } from '../../core/normalization/date';
import { cleanMerchantName } from '../../core/normalization/text';
import { generateTransactionFingerprint } from '../../core/deduplication/fingerprint';
import { generateId } from '../../utils/hashing';

export interface BackupData {
  version: '1.0.0';
  exportedAt: string;
  accounts: Account[];
  transactions: Transaction[];
  categories: Category[];
  rules: CategoryRule[];
  budgets: Budget[];
  savedPeriods: SavedPeriod[];
}

export class BackupService {
  constructor(
    private accountRepo: AccountRepository = new AccountRepository(),
    private txRepo: TransactionRepository = new TransactionRepository(),
    private categoryRepo: CategoryRepository = new CategoryRepository(),
    private budgetRepo: BudgetRepository = new BudgetRepository(),
    private periodRepo: SavedPeriodRepository = new SavedPeriodRepository()
  ) {}

  async createBackup(): Promise<string> {
    const backup: BackupData = {
      version: '1.0.0',
      exportedAt: new Date().toISOString(),
      accounts: await this.accountRepo.getAll(),
      transactions: await this.txRepo.getAll(),
      categories: await this.categoryRepo.getAllCategories(),
      rules: await this.categoryRepo.getAllRules(),
      budgets: await this.budgetRepo.getAll(),
      savedPeriods: await this.periodRepo.getAll(),
    };
    return JSON.stringify(backup, null, 2);
  }

  async restoreBackup(backupJson: string): Promise<{ restoredCount: number; errors: string[] }> {
    const errors: string[] = [];
    let count = 0;

    try {
      const data = JSON.parse(backupJson) as BackupData;
      if (!data.version || !Array.isArray(data.transactions)) {
        throw new Error('Érvénytelen biztonsági mentés fájl.');
      }

      for (const acc of data.accounts || []) {
        await this.accountRepo.create(acc);
      }
      for (const cat of data.categories || []) {
        await this.categoryRepo.createCategory(cat);
      }
      for (const r of data.rules || []) {
        await this.categoryRepo.createRule(r);
      }
      for (const b of data.budgets || []) {
        await this.budgetRepo.saveBudget(b);
      }
      for (const p of data.savedPeriods || []) {
        await this.periodRepo.savePeriod(p);
      }
      for (const tx of data.transactions || []) {
        await this.txRepo.create(tx);
        count++;
      }
    } catch (e: any) {
      errors.push(e.message || 'Hiba történt a visszaállítás közben.');
    }

    return { restoredCount: count, errors };
  }

  /**
   * Migrates legacy web localStorage data format into the current SQLite repository.
   */
  async migrateLegacyWebData(legacyObj: {
    financial_current_transactions?: any[];
    financial_saved_months?: Record<string, any>;
    financial_custom_categories?: any[];
  }): Promise<{ importedCount: number; errors: string[] }> {
    let count = 0;
    const errors: string[] = [];

    const cashAcc = await this.accountRepo.getCashAccount();
    const defaultAccountId = cashAcc?.id || 'acc_cash';

    // 1. Migrate custom categories if present
    if (Array.isArray(legacyObj.financial_custom_categories)) {
      for (const cat of legacyObj.financial_custom_categories) {
        const catId = `legacy_${cleanMerchantName(cat.name).replace(/\s+/g, '_').toLowerCase()}`;
        await this.categoryRepo.createCategory({
          id: catId,
          name: cat.name,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        });

        if (Array.isArray(cat.keywords)) {
          for (let i = 0; i < cat.keywords.length; i++) {
            await this.categoryRepo.createRule({
              id: `rule_${catId}_${i}`,
              categoryId: catId,
              pattern: cat.keywords[i],
              matchType: 'contains',
              priority: 20,
              isActive: true,
              createdAt: new Date().toISOString(),
            });
          }
        }
      }
    }

    // 2. Migrate transactions
    if (Array.isArray(legacyObj.financial_current_transactions)) {
      for (const oldTx of legacyObj.financial_current_transactions) {
        try {
          const date = normalizeDate(oldTx.date);
          const amountMajor = typeof oldTx.amount === 'number' ? oldTx.amount : parseFloat(oldTx.amount || '0');
          const amountMinor = toMinorUnits(amountMajor, 'HUF');
          const direction = amountMajor >= 0 ? 'income' : 'expense';
          const description = oldTx.description || 'Tranzakció';
          const merchant = cleanMerchantName(description);

          const fp = generateTransactionFingerprint({
            date,
            amountMinor,
            currency: 'HUF',
            description,
            merchant,
          });

          await this.txRepo.create({
            id: generateId('tx_legacy'),
            accountId: defaultAccountId,
            date,
            amountMinor,
            currency: 'HUF',
            direction,
            description,
            merchant,
            categoryId: oldTx.category ? `legacy_${cleanMerchantName(oldTx.category).toLowerCase()}` : undefined,
            source: 'backup',
            fingerprint: fp,
            importedAt: new Date().toISOString(),
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          });
          count++;
        } catch (err: any) {
          errors.push(`Hiba tétel migrációjakor: ${err.message}`);
        }
      }
    }

    // 3. Migrate saved months
    if (legacyObj.financial_saved_months) {
      for (const [monthKey, monthData] of Object.entries(legacyObj.financial_saved_months)) {
        await this.periodRepo.savePeriod({
          id: `saved_${monthKey}`,
          periodKey: monthKey,
          name: `${monthKey} Mentés`,
          savedAt: monthData.savedAt || new Date().toISOString(),
          transactionCount: Array.isArray(monthData.transactions) ? monthData.transactions.length : 0,
          totalIncomeMinor: toMinorUnits(monthData.stats?.income || 0, 'HUF'),
          totalExpenseMinor: toMinorUnits(monthData.stats?.expense || 0, 'HUF'),
          balanceMinor: toMinorUnits(monthData.stats?.balance || 0, 'HUF'),
          dataJson: JSON.stringify(monthData),
        });
      }
    }

    return { importedCount: count, errors };
  }
}
