import React from 'react';
import { Transaction } from '../../types';
import { Card } from '../common/Card';
import { TransactionItem } from '../transactions/TransactionItem';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { ArrowRight, Inbox } from 'lucide-react';

interface RecentActivityProps {
  transactions: Transaction[];
  recurringIds: Set<string>;
  onSeeAll: () => void;
  onSelectTransaction: (tx: Transaction) => void;
}

export const RecentActivity: React.FC<RecentActivityProps> = ({
  transactions,
  recurringIds,
  onSeeAll,
  onSelectTransaction,
}) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';
  const recent = transactions.slice(0, 5);

  return (
    <Card className="space-y-3">
      <div
        className={`flex justify-between items-center pb-2 border-b ${
          isLight ? 'border-slate-100' : 'border-slate-700/40'
        }`}
      >
        <h3 className={`text-sm font-semibold ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>
          {t.dashboard.recentActivity}
        </h3>
        {transactions.length > 5 && (
          <button
            onClick={onSeeAll}
            className="text-xs text-blue-500 hover:text-blue-400 font-semibold flex items-center gap-1 active:scale-95 transition-all"
          >
            <span>{t.common.all}</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {recent.length > 0 ? (
        <div className={`divide-y -mx-1 ${isLight ? 'divide-slate-100' : 'divide-slate-800/60'}`}>
          {recent.map((tx) => (
            <TransactionItem
              key={tx.id}
              transaction={tx}
              isRecurring={recurringIds.has(tx.id)}
              onClick={() => onSelectTransaction(tx)}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-8 text-slate-500 space-y-2">
          <Inbox className="w-8 h-8 mx-auto text-slate-400 opacity-70" />
          <p className="text-xs">{t.dashboard.noTransactions}</p>
        </div>
      )}
    </Card>
  );
};
