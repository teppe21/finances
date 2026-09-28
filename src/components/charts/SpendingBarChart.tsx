import React, { useState } from 'react';
import { MonthlyTrendData } from '../../types';
import { formatCurrency } from '../../core/normalization/currency';
import { Card } from '../common/Card';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { BarChart3 } from 'lucide-react';

interface SpendingBarChartProps {
  data: MonthlyTrendData[];
}

export const SpendingBarChart: React.FC<SpendingBarChartProps> = ({ data }) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';

  const [selectedMonth, setSelectedMonth] = useState<MonthlyTrendData | null>(
    data.length > 0 ? data[data.length - 1] : null
  );

  const maxVal = Math.max(
    ...data.map((d) => Math.max(d.expenseMinor, d.incomeMinor)),
    1
  );

  return (
    <Card className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className={`text-sm font-semibold flex items-center gap-2 ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>
          <BarChart3 className="w-4 h-4 text-slate-400" />
          {t.analytics.sixMonthsTitle}
        </h3>
        {selectedMonth && (
          <span className="text-xs text-slate-400 font-medium">{selectedMonth.month}</span>
        )}
      </div>

      {/* Selected month breakdown pill */}
      {selectedMonth && (
        <div
          className={`flex items-center justify-around p-2.5 rounded-xl border text-xs transition-colors ${
            isLight ? 'bg-slate-50 border-slate-200' : 'bg-slate-900/60 border-slate-700/40'
          }`}
        >
          <div className="text-center">
            <span className="text-slate-400 block text-[10px]">Bevétel</span>
            <span className="text-emerald-500 font-bold font-mono">
              +{formatCurrency(selectedMonth.incomeMinor)}
            </span>
          </div>
          <div className={`h-6 w-px ${isLight ? 'bg-slate-200' : 'bg-slate-700'}`} />
          <div className="text-center">
            <span className="text-slate-400 block text-[10px]">Kiadás</span>
            <span className="text-rose-500 font-bold font-mono">
              -{formatCurrency(selectedMonth.expenseMinor)}
            </span>
          </div>
          <div className={`h-6 w-px ${isLight ? 'bg-slate-200' : 'bg-slate-700'}`} />
          <div className="text-center">
            <span className="text-slate-400 block text-[10px]">Egyenleg</span>
            <span
              className={`font-bold font-mono ${
                selectedMonth.balanceMinor >= 0 ? 'text-emerald-500' : 'text-rose-500'
              }`}
            >
              {formatCurrency(selectedMonth.balanceMinor)}
            </span>
          </div>
        </div>
      )}

      {/* Touch-Friendly Bar Graph */}
      <div className="h-40 flex items-end justify-between gap-1.5 pt-4 px-1">
        {data.map((item) => {
          const isSelected = selectedMonth?.month === item.month;
          const expHeight = Math.max(6, Math.round((item.expenseMinor / maxVal) * 96));
          const incHeight = Math.max(6, Math.round((item.incomeMinor / maxVal) * 96));

          return (
            <div
              key={item.month}
              onClick={() => setSelectedMonth(item)}
              className={`flex-1 flex flex-col items-center gap-1.5 cursor-pointer transition-all active:scale-95 ${
                isSelected ? 'opacity-100 scale-105' : 'opacity-70 hover:opacity-90'
              }`}
            >
              <div className="flex items-end gap-1 h-28 w-full justify-center">
                {/* Income bar */}
                <div
                  style={{ height: `${incHeight}px` }}
                  className={`w-2.5 sm:w-3.5 rounded-t-md transition-all ${
                    isSelected ? 'bg-emerald-500 shadow-md shadow-emerald-500/30' : 'bg-emerald-500/70'
                  }`}
                  title={`Bevétel: ${formatCurrency(item.incomeMinor)}`}
                />
                {/* Expense bar */}
                <div
                  style={{ height: `${expHeight}px` }}
                  className={`w-2.5 sm:w-3.5 rounded-t-md transition-all ${
                    isSelected ? 'bg-rose-500 shadow-md shadow-rose-500/20' : 'bg-rose-500/70'
                  }`}
                  title={`Kiadás: ${formatCurrency(item.expenseMinor)}`}
                />
              </div>

              <span
                className={`text-[10px] font-medium ${
                  isSelected
                    ? isLight
                      ? 'text-slate-900 font-bold'
                      : 'text-white font-bold'
                    : 'text-slate-400'
                }`}
              >
                {item.month.substring(5)}
              </span>
            </div>
          );
        })}
      </div>
    </Card>
  );
};
