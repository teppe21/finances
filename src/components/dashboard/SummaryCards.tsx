import React from 'react';
import { Card } from '../common/Card';
import { FinancialStats } from '../../types';
import { formatCurrency } from '../../core/normalization/currency';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { ArrowDownRight, ArrowUpRight, Wallet } from 'lucide-react';

interface SummaryCardsProps {
  stats: FinancialStats;
}

export const SummaryCards: React.FC<SummaryCardsProps> = ({ stats }) => {
  const { t } = useTranslation();
  const currency = useSettingsStore((s) => s.currency);
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';

  const savingsPercent = (stats.savingsRate * 100).toFixed(1);

  return (
    <div className="space-y-3">
      {/* Premium Minimalist Banking Account Card */}
      <div
        className={`rounded-2xl p-4 sm:p-6 border shadow-md relative transition-all ${
          isLight
            ? 'bg-white border-slate-200 text-slate-900 shadow-slate-200/50'
            : 'bg-slate-900 border-slate-700/80 text-white'
        }`}
      >
        <div className="flex justify-between items-start gap-2">
          <div className="min-w-0 flex-1">
            <div
              className={`flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider ${
                isLight ? 'text-slate-500' : 'text-slate-400'
              }`}
            >
              <span>{t.dashboard.totalBalance}</span>
              <span>•</span>
              <span>{currency}</span>
            </div>
            <h2
              className={`text-2xl sm:text-3xl md:text-4xl font-extrabold mt-1.5 tracking-tight font-mono truncate ${
                isLight ? 'text-slate-900' : 'text-slate-50'
              }`}
            >
              {formatCurrency(stats.balanceMinor, currency)}
            </h2>
          </div>
          <div
            className={`p-2.5 rounded-xl border flex-shrink-0 ${
              isLight
                ? 'bg-slate-100 border-slate-200 text-slate-700'
                : 'bg-slate-800 border-slate-700 text-slate-300'
            }`}
          >
            <Wallet className="w-5 h-5" />
          </div>
        </div>

        <div
          className={`mt-4 pt-3 border-t flex items-center justify-between text-xs ${
            isLight ? 'border-slate-100 text-slate-500' : 'border-slate-800 text-slate-400'
          }`}
        >
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 inline-block" />
            <span>
              {t.dashboard.savingsRate}:{' '}
              <strong className={`font-mono font-bold ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>
                {savingsPercent}%
              </strong>
            </span>
          </div>
          <span className="font-mono text-slate-500">{stats.transactionCount} tétel</span>
        </div>
      </div>

      {/* Income vs Expenses Mini Grid */}
      <div className="grid grid-cols-2 gap-2.5 sm:gap-3">
        <Card className="flex flex-col justify-between p-3 sm:p-3.5">
          <div className="flex items-center justify-between">
            <span className={`text-xs font-medium ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
              {t.dashboard.income}
            </span>
            <div className="p-1 bg-emerald-500/10 rounded-md text-emerald-500">
              <ArrowUpRight className="w-3.5 h-3.5" />
            </div>
          </div>
          <p className="text-sm sm:text-base font-bold text-emerald-500 mt-2 font-mono truncate">
            +{formatCurrency(stats.incomeMinor, currency)}
          </p>
        </Card>

        <Card className="flex flex-col justify-between p-3 sm:p-3.5">
          <div className="flex items-center justify-between">
            <span className={`text-xs font-medium ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
              {t.dashboard.expenses}
            </span>
            <div className="p-1 bg-rose-500/10 rounded-md text-rose-500">
              <ArrowDownRight className="w-3.5 h-3.5" />
            </div>
          </div>
          <p
            className={`text-sm sm:text-base font-bold mt-2 font-mono truncate ${
              isLight ? 'text-slate-900' : 'text-slate-100'
            }`}
          >
            -{formatCurrency(stats.expenseMinor, currency)}
          </p>
        </Card>
      </div>
    </div>
  );
};
