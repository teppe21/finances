import React, { useState } from 'react';
import { CategorySummary } from '../../types';
import { formatCurrency } from '../../core/normalization/currency';
import { Card } from '../common/Card';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { PieChart as PieIcon } from 'lucide-react';

interface CategoryPieChartProps {
  categories: CategorySummary[];
  onSelectCategory?: (categoryId: string) => void;
}

export const CategoryPieChart: React.FC<CategoryPieChartProps> = ({
  categories,
  onSelectCategory,
}) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';
  const [activeCat, setActiveCat] = useState<string | null>(null);

  return (
    <Card className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className={`text-sm font-semibold flex items-center gap-2 ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>
          <PieIcon className="w-4 h-4 text-emerald-500" />
          {t.analytics.categoryShare}
        </h3>
        <span className="text-xs text-slate-400">{categories.length} kategória</span>
      </div>

      {categories.length > 0 ? (
        <div className="space-y-2.5">
          {categories.slice(0, 6).map((cat) => {
            const isSelected = activeCat === cat.categoryId;
            const barColor = cat.color || '#3B82F6';

            return (
              <div
                key={cat.categoryId}
                onClick={() => {
                  setActiveCat(isSelected ? null : cat.categoryId);
                  if (onSelectCategory) onSelectCategory(cat.categoryId);
                }}
                className={`p-2.5 rounded-xl transition-all cursor-pointer ${
                  isSelected
                    ? isLight
                      ? 'bg-slate-100'
                      : 'bg-slate-700/40'
                    : isLight
                    ? 'hover:bg-slate-50'
                    : 'hover:bg-slate-800/40'
                }`}
              >
                <div className="flex justify-between items-center text-xs mb-1.5">
                  <div className="flex items-center gap-2 min-w-0 pr-2">
                    <span
                      className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                      style={{ backgroundColor: barColor }}
                    />
                    <span className={`font-medium truncate ${isLight ? 'text-slate-800' : 'text-slate-200'}`}>
                      {cat.categoryName}
                    </span>
                    <span className="text-[10px] text-slate-400 flex-shrink-0">
                      ({cat.transactionCount} db)
                    </span>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <span className={`font-bold font-mono block ${isLight ? 'text-slate-900' : 'text-slate-100'}`}>
                      {formatCurrency(cat.totalMinor)}
                    </span>
                    <span className="text-[10px] text-slate-400 font-semibold font-mono">
                      {cat.percentage.toFixed(1)}%
                    </span>
                  </div>
                </div>

                {/* Progress track */}
                <div className={`w-full h-1.5 rounded-full overflow-hidden ${isLight ? 'bg-slate-100' : 'bg-slate-900'}`}>
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{
                      width: `${Math.max(2, cat.percentage)}%`,
                      backgroundColor: barColor,
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-6 text-slate-500 text-xs">
          Nincsenek kiadási adatok ebben az időszakban.
        </div>
      )}
    </Card>
  );
};
