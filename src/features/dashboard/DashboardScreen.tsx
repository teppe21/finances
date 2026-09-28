import React, { useState } from 'react';
import { useTransactionStore } from '../../store/transactionStore';
import { useAccountStore } from '../../store/accountStore';
import { useTranslation } from '../../i18n';
import { SummaryCards } from '../../components/dashboard/SummaryCards';
import { QuickActions } from '../../components/dashboard/QuickActions';
import { RecentActivity } from '../../components/dashboard/RecentActivity';
import { SpendingBarChart } from '../../components/charts/SpendingBarChart';
import { CategoryPieChart } from '../../components/charts/CategoryPieChart';
import { TransactionEditModal } from '../../components/transactions/TransactionEditModal';
import { Transaction } from '../../types';
import { CategoryRepository } from '../../database/repositories/categoryRepository';

interface DashboardScreenProps {
  onNavigate: (tab: string) => void;
  onOpenScanReceipt: () => void;
  onOpenImport: () => void;
  onOpenAddTransaction: () => void;
  onOpenNotifications: () => void;
  pendingNotificationCount?: number;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  onNavigate,
  onOpenScanReceipt,
  onOpenImport,
  onOpenAddTransaction,
  onOpenNotifications,
  pendingNotificationCount = 0,
}) => {
  const { t } = useTranslation();
  const { stats, filteredTransactions, monthlyTrends, categoryBreakdown, recurringIds, updateTransaction, deleteTransaction } = useTransactionStore();
  const { accounts } = useAccountStore();
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);
  const [categories, setCategories] = useState<any[]>([]);

  React.useEffect(() => {
    new CategoryRepository().getAllCategories().then(setCategories);
  }, []);

  return (
    <div className="space-y-6 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* 1. Headline Balance & Income/Expense Cards */}
      <SummaryCards stats={stats} />

      {/* 2. Quick Actions Priority Bar */}
      <QuickActions
        onScanReceipt={onOpenScanReceipt}
        onImportStatement={onOpenImport}
        onAddTransaction={onOpenAddTransaction}
        onOpenNotifications={onOpenNotifications}
        pendingCount={pendingNotificationCount}
      />

      {/* 3. 6-Month Cash Flow Trends */}
      <SpendingBarChart data={monthlyTrends} />

      {/* 4. Category Expense Breakdown */}
      <CategoryPieChart
        categories={categoryBreakdown}
        onSelectCategory={() => onNavigate('analytics')}
      />

      {/* 5. Recent Activity List */}
      <RecentActivity
        transactions={filteredTransactions}
        recurringIds={recurringIds}
        onSeeAll={() => onNavigate('transactions')}
        onSelectTransaction={(tx) => setSelectedTx(tx)}
      />

      {/* Edit Modal */}
      {selectedTx && (
        <TransactionEditModal
          transaction={selectedTx}
          categories={categories}
          accounts={accounts}
          onSave={(updated) => {
            updateTransaction(updated);
            setSelectedTx(null);
          }}
          onDelete={(id) => {
            deleteTransaction(id);
            setSelectedTx(null);
          }}
          onClose={() => setSelectedTx(null)}
        />
      )}
    </div>
  );
};
