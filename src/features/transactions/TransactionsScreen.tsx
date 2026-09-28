import React, { useState, useEffect } from 'react';
import { useTransactionStore } from '../../store/transactionStore';
import { useAccountStore } from '../../store/accountStore';
import { useTranslation } from '../../i18n';
import { TransactionItem } from '../../components/transactions/TransactionItem';
import { TransactionEditModal } from '../../components/transactions/TransactionEditModal';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { generateCsvContent } from '../../services/export/csvExportService';
import { Transaction, Category } from '../../types';
import { Search, Download, Plus, Repeat, Filter, Inbox } from 'lucide-react';

interface TransactionsScreenProps {
  onOpenAddTransaction: () => void;
}

export const TransactionsScreen: React.FC<TransactionsScreenProps> = ({
  onOpenAddTransaction,
}) => {
  const { t } = useTranslation();
  const {
    filteredTransactions,
    selectedPeriod,
    setSelectedPeriod,
    selectedCategory,
    setSelectedCategory,
    selectedSource,
    setSelectedSource,
    searchTerm,
    setSearchTerm,
    onlyRecurring,
    setOnlyRecurring,
    recurringIds,
    updateTransaction,
    deleteTransaction,
  } = useTransactionStore();

  const { accounts } = useAccountStore();
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedTx, setSelectedTx] = useState<Transaction | null>(null);

  useEffect(() => {
    new CategoryRepository().getAllCategories().then(setCategories);
  }, []);

  const handleExportCsv = () => {
    const csv = generateCsvContent(filteredTransactions);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `tranzakciok_${new Date().toISOString().substring(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* Top Header & Search Bar */}
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={t.transactions.searchPlaceholder}
            className="w-full pl-10 pr-3 py-2.5 bg-slate-800 border border-slate-700/80 rounded-2xl text-xs sm:text-sm text-slate-100 focus:outline-none focus:border-blue-500"
          />
        </div>

        <Button
          variant="secondary"
          size="sm"
          onClick={handleExportCsv}
          icon={<Download className="w-4 h-4 text-emerald-400" />}
          className="p-2.5 rounded-2xl"
        >
          <span className="hidden sm:inline">CSV</span>
        </Button>

        <Button
          variant="primary"
          size="sm"
          onClick={onOpenAddTransaction}
          icon={<Plus className="w-4 h-4" />}
          className="p-2.5 rounded-2xl"
        >
          <span className="hidden sm:inline">{t.common.save}</span>
        </Button>
      </div>

      {/* Filter Row 1: Periods */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
        {[
          { id: 'this_month', label: t.transactions.periodThisMonth },
          { id: 'last_month', label: t.transactions.periodLastMonth },
          { id: 'last_3_months', label: t.transactions.period3Months },
          { id: 'last_6_months', label: t.transactions.period6Months },
          { id: 'All', label: t.common.all },
        ].map((p) => (
          <button
            key={p.id}
            onClick={() => setSelectedPeriod(p.id as any)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
              selectedPeriod === p.id
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                : 'bg-slate-800 text-slate-400 hover:text-slate-200 border border-slate-700/50'
            }`}
          >
            {p.label}
          </button>
        ))}
      </div>

      {/* Filter Row 2: Category, Source & Recurring Toggle */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value)}
          className="bg-slate-800 border border-slate-700/70 text-slate-300 text-xs rounded-xl px-2.5 py-1.5 focus:outline-none focus:border-blue-500"
        >
          <option value="All">{t.transactions.allCategories}</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>

        <select
          value={selectedSource}
          onChange={(e) => setSelectedSource(e.target.value)}
          className="bg-slate-800 border border-slate-700/70 text-slate-300 text-xs rounded-xl px-2.5 py-1.5 focus:outline-none focus:border-blue-500"
        >
          <option value="All">{t.transactions.allSources}</option>
          <option value="notification">Értesítés</option>
          <option value="receipt">Nyugta</option>
          <option value="csv">CSV Kivonat</option>
          <option value="manual">Kézi rögzítés</option>
        </select>

        <button
          onClick={() => setOnlyRecurring(!onlyRecurring)}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all border ${
            onlyRecurring
              ? 'bg-amber-500/20 text-amber-400 border-amber-500/40'
              : 'bg-slate-800 text-slate-400 border-slate-700/50'
          }`}
        >
          <Repeat className="w-3.5 h-3.5" />
          <span>{t.transactions.recurringOnly}</span>
        </button>
      </div>

      {/* Transactions List */}
      <Card className="p-2 sm:p-3">
        {filteredTransactions.length > 0 ? (
          <div className="divide-y divide-slate-800/60">
            {filteredTransactions.map((tx) => (
              <TransactionItem
                key={tx.id}
                transaction={tx}
                isRecurring={recurringIds.has(tx.id)}
                onClick={() => setSelectedTx(tx)}
              />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-slate-500 space-y-2">
            <Inbox className="w-10 h-10 mx-auto text-slate-600" />
            <p className="text-xs font-medium">Nincs a szűrésnek megfelelő tranzakció.</p>
          </div>
        )}
      </Card>

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
