import { Transaction, FinancialStats, MonthlyTrendData, CategorySummary, Category } from '../../types';
import { getMonthKey } from '../normalization/date';

/**
 * Calculates sum of income in minor units (excluding transfers).
 */
export function calculateIncome(transactions: Transaction[]): number {
  return transactions.reduce((sum, t) => {
    if (t.direction === 'income') {
      return sum + Math.abs(t.amountMinor);
    }
    // Refunds can be treated either as income or as offset
    if (t.direction === 'refund' && t.amountMinor > 0) {
      return sum + t.amountMinor;
    }
    return sum;
  }, 0);
}

/**
 * Calculates sum of expenses in minor units (excluding transfers).
 */
export function calculateExpenses(transactions: Transaction[]): number {
  return transactions.reduce((sum, t) => {
    if (t.direction === 'expense') {
      return sum + Math.abs(t.amountMinor);
    }
    return sum;
  }, 0);
}

/**
 * Calculates total transfer volume in minor units.
 */
export function calculateTransfers(transactions: Transaction[]): number {
  return transactions.reduce((sum, t) => {
    if (t.direction === 'transfer') {
      return sum + Math.abs(t.amountMinor);
    }
    return sum;
  }, 0);
}

/**
 * Calculates net balance (income - expenses + adjustments).
 */
export function calculateBalance(transactions: Transaction[]): number {
  const income = calculateIncome(transactions);
  const expense = calculateExpenses(transactions);
  return income - expense;
}

/**
 * Calculates savings rate (percentage of income saved, 0.0 - 1.0).
 */
export function calculateSavingsRate(incomeMinor: number, expenseMinor: number): number {
  if (incomeMinor <= 0) return 0;
  const savings = incomeMinor - expenseMinor;
  const rate = savings / incomeMinor;
  return Math.max(-1, Math.min(1, rate));
}

/**
 * Computes all headline dashboard stats from transactions.
 */
export function calculateFinancialStats(transactions: Transaction[]): FinancialStats {
  const incomeMinor = calculateIncome(transactions);
  const expenseMinor = calculateExpenses(transactions);
  const balanceMinor = incomeMinor - expenseMinor;
  const transfersMinor = calculateTransfers(transactions);
  const savingsRate = calculateSavingsRate(incomeMinor, expenseMinor);

  let expenseCount = 0;
  let maxExpenseItem = { description: '-', amountMinor: 0 };

  transactions.forEach((t) => {
    if (t.direction === 'expense') {
      const absAmount = Math.abs(t.amountMinor);
      expenseCount++;
      if (absAmount > maxExpenseItem.amountMinor) {
        maxExpenseItem = {
          description: t.description || t.merchant || 'Kiadás',
          amountMinor: absAmount,
        };
      }
    }
  });

  const avgExpenseMinor = expenseCount > 0 ? Math.round(expenseMinor / expenseCount) : 0;

  return {
    incomeMinor,
    expenseMinor,
    balanceMinor,
    transfersMinor,
    savingsRate,
    transactionCount: transactions.length,
    maxExpenseItem,
    avgExpenseMinor,
  };
}

/**
 * Computes monthly comparison data for the last N months (default 6).
 */
export function calculateMonthlyTrends(
  transactions: Transaction[],
  monthCount: number = 6,
  referenceDate: Date = new Date()
): MonthlyTrendData[] {
  const monthlyTotals: Record<string, { incomeMinor: number; expenseMinor: number }> = {};

  for (let i = monthCount - 1; i >= 0; i--) {
    const d = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - i, 1);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    monthlyTotals[key] = { incomeMinor: 0, expenseMinor: 0 };
  }

  transactions.forEach((t) => {
    const key = getMonthKey(t.date);
    if (monthlyTotals[key]) {
      if (t.direction === 'income') {
        monthlyTotals[key].incomeMinor += Math.abs(t.amountMinor);
      } else if (t.direction === 'expense') {
        monthlyTotals[key].expenseMinor += Math.abs(t.amountMinor);
      }
    }
  });

  return Object.keys(monthlyTotals).map((monthKey) => {
    const { incomeMinor, expenseMinor } = monthlyTotals[monthKey];
    return {
      month: monthKey,
      incomeMinor,
      expenseMinor,
      balanceMinor: incomeMinor - expenseMinor,
    };
  });
}

/**
 * Calculates category-wise expense breakdown.
 */
export function calculateCategoryBreakdown(
  transactions: Transaction[],
  categories: Category[]
): CategorySummary[] {
  const categoryMap = new Map<string, Category>();
  categories.forEach((c) => categoryMap.set(c.id, c));

  const totals: Record<string, { totalMinor: number; count: number }> = {};
  let totalExpenseMinor = 0;

  transactions.forEach((t) => {
    if (t.direction === 'expense') {
      const catId = t.categoryId || 'other';
      const absAmount = Math.abs(t.amountMinor);
      totalExpenseMinor += absAmount;

      if (!totals[catId]) {
        totals[catId] = { totalMinor: 0, count: 0 };
      }
      totals[catId].totalMinor += absAmount;
      totals[catId].count++;
    }
  });

  return Object.keys(totals)
    .map((catId) => {
      const cat = categoryMap.get(catId);
      const catTotal = totals[catId].totalMinor;
      return {
        categoryId: catId,
        categoryName: cat?.name || (catId === 'other' ? 'Egyéb / Ismeretlen' : catId),
        color: cat?.color,
        icon: cat?.icon,
        totalMinor: catTotal,
        percentage: totalExpenseMinor > 0 ? (catTotal / totalExpenseMinor) * 100 : 0,
        transactionCount: totals[catId].count,
      };
    })
    .sort((a, b) => b.totalMinor - a.totalMinor);
}
