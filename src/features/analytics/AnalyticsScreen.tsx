import React from 'react';
import { useTransactionStore } from '../../store/transactionStore';
import { useTranslation } from '../../i18n';
import { Card } from '../../components/common/Card';
import { SpendingBarChart } from '../../components/charts/SpendingBarChart';
import { CategoryPieChart } from '../../components/charts/CategoryPieChart';
import { formatCurrency } from '../../core/normalization/currency';
import { useSettingsStore } from '../../store/settingsStore';
import { TrendingUp, Award, Calculator, PiggyBank } from 'lucide-react';

export const AnalyticsScreen: React.FC = () => {
  const { t } = useTranslation();
  const { stats, monthlyTrends, categoryBreakdown } = useTransactionStore();
  const currency = useSettingsStore((s) => s.currency);

  return (
    <div className="space-y-5 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* Top Banner */}
      <div>
        <h2 className="text-lg font-bold text-slate-100">{t.analytics.title}</h2>
        <p className="text-xs text-slate-400">Részletes statisztikák a pénzmozgásaidról</p>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 gap-3">
        <Card className="space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Calculator className="w-4 h-4 text-slate-400" />
            <span>{t.analytics.averageExpense}</span>
          </div>
          <span className="text-base sm:text-lg font-extrabold text-slate-100 block">
            {formatCurrency(stats.avgExpenseMinor, currency)}
          </span>
        </Card>

        <Card className="space-y-1">
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Award className="w-4 h-4 text-amber-400" />
            <span>{t.analytics.largestExpense}</span>
          </div>
          <span className="text-base sm:text-lg font-extrabold text-amber-400 block truncate" title={stats.maxExpenseItem.description}>
            {formatCurrency(stats.maxExpenseItem.amountMinor, currency)}
          </span>
          <span className="text-[10px] text-slate-400 truncate block">
            {stats.maxExpenseItem.description}
          </span>
        </Card>
      </div>

      {/* 6-Month Cash Flow Trends */}
      <SpendingBarChart data={monthlyTrends} />

      {/* Category Expense Breakdown */}
      <CategoryPieChart categories={categoryBreakdown} />
    </div>
  );
};
