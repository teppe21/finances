import { create } from 'zustand';
import { Transaction, FinancialStats, MonthlyTrendData, CategorySummary } from '../types';
import { TransactionRepository } from '../database/repositories/transactionRepository';
import { CategoryRepository } from '../database/repositories/categoryRepository';
import { calculateFinancialStats, calculateMonthlyTrends, calculateCategoryBreakdown } from '../core/calculations/financialMath';
import { detectRecurringTransactions } from '../core/recurring/detector';
import { TransactionIngestionService, IngestionInput } from '../services/ingestion/transactionIngestionService';

interface TransactionState {
  transactions: Transaction[];
  filteredTransactions: Transaction[];
  isLoading: boolean;
  selectedPeriod: 'All' | 'this_month' | 'last_month' | 'last_3_months' | 'last_6_months';
  selectedCategory: string;
  selectedSource: string;
  searchTerm: string;
  onlyRecurring: boolean;
  recurringIds: Set<string>;
  stats: FinancialStats;
  monthlyTrends: MonthlyTrendData[];
  categoryBreakdown: CategorySummary[];

  // Actions
  loadTransactions: () => Promise<void>;
  setSelectedPeriod: (period: 'All' | 'this_month' | 'last_month' | 'last_3_months' | 'last_6_months') => void;
  setSelectedCategory: (catId: string) => void;
  setSelectedSource: (source: string) => void;
  setSearchTerm: (term: string) => void;
  setOnlyRecurring: (only: boolean) => void;
  addTransaction: (input: IngestionInput) => Promise<any>;
  updateTransaction: (tx: Transaction) => Promise<void>;
  deleteTransaction: (id: string) => Promise<void>;
}

const txRepo = new TransactionRepository();
const catRepo = new CategoryRepository();
const ingestionService = new TransactionIngestionService();

export const useTransactionStore = create<TransactionState>((set, get) => ({
  transactions: [],
  filteredTransactions: [],
  isLoading: false,
  selectedPeriod: 'this_month',
  selectedCategory: 'All',
  selectedSource: 'All',
  searchTerm: '',
  onlyRecurring: false,
  recurringIds: new Set<string>(),
  stats: {
    incomeMinor: 0,
    expenseMinor: 0,
    balanceMinor: 0,
    transfersMinor: 0,
    savingsRate: 0,
    transactionCount: 0,
    maxExpenseItem: { description: '-', amountMinor: 0 },
    avgExpenseMinor: 0,
  },
  monthlyTrends: [],
  categoryBreakdown: [],

  loadTransactions: async () => {
    set({ isLoading: true });
    try {
      const all = await txRepo.getAll();
      const categories = await catRepo.getAllCategories();
      const { recurringTransactionIds } = detectRecurringTransactions(all);

      const { selectedPeriod, selectedCategory, selectedSource, searchTerm, onlyRecurring } = get();

      const filtered = await txRepo.filter({
        period: selectedPeriod,
        categoryId: selectedCategory,
        source: selectedSource,
        searchTerm,
        onlyRecurring,
        recurringIds: recurringTransactionIds,
      });

      const stats = calculateFinancialStats(filtered);
      const monthlyTrends = calculateMonthlyTrends(all, 6);
      const categoryBreakdown = calculateCategoryBreakdown(filtered, categories);

      set({
        transactions: all,
        filteredTransactions: filtered,
        recurringIds: recurringTransactionIds,
        stats,
        monthlyTrends,
        categoryBreakdown,
        isLoading: false,
      });
    } catch (e) {
      set({ isLoading: false });
    }
  },

  setSelectedPeriod: (period) => {
    set({ selectedPeriod: period });
    get().loadTransactions();
  },

  setSelectedCategory: (catId) => {
    set({ selectedCategory: catId });
    get().loadTransactions();
  },

  setSelectedSource: (src) => {
    set({ selectedSource: src });
    get().loadTransactions();
  },

  setSearchTerm: (term) => {
    set({ searchTerm: term });
    get().loadTransactions();
  },

  setOnlyRecurring: (only) => {
    set({ onlyRecurring: only });
    get().loadTransactions();
  },

  addTransaction: async (input) => {
    const res = await ingestionService.ingest(input);
    await get().loadTransactions();
    return res;
  },

  updateTransaction: async (tx) => {
    await txRepo.update(tx);
    await get().loadTransactions();
  },

  deleteTransaction: async (id) => {
    await txRepo.delete(id);
    await get().loadTransactions();
  },
}));
