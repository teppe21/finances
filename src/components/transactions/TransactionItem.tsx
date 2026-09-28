import React from 'react';
import { Transaction } from '../../types';
import { formatCurrency } from '../../core/normalization/currency';
import { Badge } from '../common/Badge';
import { useSettingsStore } from '../../store/settingsStore';
import {
  ShoppingCart,
  Utensils,
  Car,
  Gamepad2,
  Home,
  Film,
  PiggyBank,
  Wallet,
  HeartPulse,
  ShoppingBag,
  HelpCircle,
  Repeat,
  Camera,
  BellRing,
  FileSpreadsheet,
  HandCoins,
} from 'lucide-react';

interface TransactionItemProps {
  transaction: Transaction;
  isRecurring?: boolean;
  onClick?: () => void;
}

const CATEGORY_ICONS: Record<string, React.ReactNode> = {
  food: <ShoppingCart className="w-4 h-4" />,
  dining: <Utensils className="w-4 h-4" />,
  transport: <Car className="w-4 h-4" />,
  subscriptions: <Gamepad2 className="w-4 h-4" />,
  housing: <Home className="w-4 h-4" />,
  entertainment: <Film className="w-4 h-4" />,
  savings: <PiggyBank className="w-4 h-4" />,
  income: <Wallet className="w-4 h-4" />,
  health: <HeartPulse className="w-4 h-4" />,
  shopping: <ShoppingBag className="w-4 h-4" />,
  other: <HelpCircle className="w-4 h-4" />,
};

const SOURCE_ICONS: Record<string, React.ReactNode> = {
  notification: <BellRing className="w-3 h-3 text-blue-400" />,
  receipt: <Camera className="w-3 h-3 text-slate-400" />,
  csv: <FileSpreadsheet className="w-3 h-3 text-emerald-400" />,
  manual: <HandCoins className="w-3 h-3 text-amber-400" />,
};

export const TransactionItem: React.FC<TransactionItemProps> = ({
  transaction,
  isRecurring,
  onClick,
}) => {
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';

  const isIncome = transaction.direction === 'income';
  const isRefund = transaction.direction === 'refund';
  const catId = transaction.categoryId || 'other';

  const icon = CATEGORY_ICONS[catId] || <HelpCircle className="w-4 h-4" />;
  const sourceIcon = SOURCE_ICONS[transaction.source] || null;

  return (
    <div
      onClick={onClick}
      className={`flex items-center justify-between p-3 sm:p-3.5 rounded-xl transition-all cursor-pointer border border-transparent ${
        isLight
          ? 'hover:bg-slate-100/80 active:bg-slate-200/70 hover:border-slate-200'
          : 'hover:bg-slate-800/40 active:bg-slate-800/80 hover:border-slate-800/60'
      }`}
    >
      <div className="flex items-center gap-3 min-w-0 flex-1">
        <div
          className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 transition-colors ${
            isIncome
              ? 'bg-emerald-500/15 text-emerald-500'
              : isLight
              ? 'bg-slate-100 text-slate-700 border border-slate-200'
              : 'bg-slate-800 text-slate-300 border border-slate-700'
          }`}
        >
          {icon}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-1.5">
            <span
              className={`font-semibold text-sm truncate ${
                isLight ? 'text-slate-900' : 'text-slate-100'
              }`}
            >
              {transaction.merchant || transaction.description}
            </span>
            {isRecurring && (
              <span title="Rendszeres kiadás" className="text-amber-500 flex-shrink-0">
                <Repeat className="w-3 h-3" />
              </span>
            )}
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
            <span className="font-mono">{transaction.date}</span>
            <span>•</span>
            <span className="flex items-center gap-1 truncate">
              {sourceIcon}
              <span className="capitalize">{transaction.source}</span>
            </span>
          </div>
        </div>
      </div>

      <div className="text-right flex-shrink-0 ml-3">
        <span
          className={`font-bold font-mono text-sm block ${
            isIncome || isRefund
              ? 'text-emerald-500'
              : isLight
              ? 'text-slate-900'
              : 'text-slate-100'
          }`}
        >
          {isIncome
            ? `+${formatCurrency(transaction.amountMinor, transaction.currency)}`
            : formatCurrency(transaction.amountMinor, transaction.currency)}
        </span>
        {transaction.pending && (
          <Badge variant="warning" size="sm">
            Ellenőrzésre vár
          </Badge>
        )}
      </div>
    </div>
  );
};
