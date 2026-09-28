import React from 'react';
import { Camera, FileUp, Plus, BellRing } from 'lucide-react';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';

interface QuickActionsProps {
  onScanReceipt: () => void;
  onImportStatement: () => void;
  onAddTransaction: () => void;
  onOpenNotifications?: () => void;
  pendingCount?: number;
}

export const QuickActions: React.FC<QuickActionsProps> = ({
  onScanReceipt,
  onImportStatement,
  onAddTransaction,
  onOpenNotifications,
  pendingCount = 0,
}) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';

  return (
    <div className="space-y-2.5">
      <div className="flex justify-between items-center">
        <h3 className={`text-xs font-semibold uppercase tracking-wider ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
          {t.dashboard.quickActions}
        </h3>
        {pendingCount > 0 && onOpenNotifications && (
          <button
            onClick={onOpenNotifications}
            className="flex items-center gap-1.5 px-2.5 py-1 bg-amber-500/10 text-amber-500 rounded-lg text-xs font-medium border border-amber-500/20 active:scale-95 transition-all"
          >
            <BellRing className="w-3.5 h-3.5" />
            <span>{pendingCount} észlelve</span>
          </button>
        )}
      </div>

      <div className="grid grid-cols-3 gap-2 sm:gap-3">
        {/* Action 1: Scan Receipt */}
        <button
          onClick={onScanReceipt}
          className={`flex flex-col items-center justify-center p-2.5 sm:p-3 rounded-2xl border transition-all active:scale-95 group text-center shadow-sm min-h-[84px] sm:min-h-[92px] ${
            isLight
              ? 'border-slate-200 bg-white hover:bg-slate-50 text-slate-800'
              : 'border-slate-700/80 bg-slate-800/80 hover:bg-slate-700/80 text-slate-200'
          }`}
        >
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center mb-1.5 transition-colors ${
              isLight ? 'bg-slate-100 text-slate-700' : 'bg-slate-700/60 text-slate-200'
            }`}
          >
            <Camera className="w-4 h-4" />
          </div>
          <span className="text-[11px] sm:text-xs font-semibold tracking-tight leading-tight line-clamp-1">
            {t.dashboard.scanReceipt}
          </span>
        </button>

        {/* Action 2: Import Statement */}
        <button
          onClick={onImportStatement}
          className={`flex flex-col items-center justify-center p-2.5 sm:p-3 rounded-2xl border transition-all active:scale-95 group text-center shadow-sm min-h-[84px] sm:min-h-[92px] ${
            isLight
              ? 'border-slate-200 bg-white hover:bg-slate-50 text-slate-800'
              : 'border-slate-700/80 bg-slate-800/80 hover:bg-slate-700/80 text-slate-200'
          }`}
        >
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center mb-1.5 transition-colors ${
              isLight ? 'bg-slate-100 text-slate-700' : 'bg-slate-700/60 text-slate-200'
            }`}
          >
            <FileUp className="w-4 h-4" />
          </div>
          <span className="text-[11px] sm:text-xs font-semibold tracking-tight leading-tight line-clamp-1">
            {t.dashboard.importStatement}
          </span>
        </button>

        {/* Action 3: Add Manual Transaction */}
        <button
          onClick={onAddTransaction}
          className={`flex flex-col items-center justify-center p-2.5 sm:p-3 rounded-2xl border transition-all active:scale-95 group text-center shadow-sm min-h-[84px] sm:min-h-[92px] ${
            isLight
              ? 'border-blue-200 bg-blue-50/80 hover:bg-blue-100/70 text-blue-700'
              : 'border-blue-500/30 bg-blue-600/15 hover:bg-blue-600/25 text-blue-400'
          }`}
        >
          <div
            className={`w-9 h-9 rounded-xl flex items-center justify-center mb-1.5 transition-colors ${
              isLight ? 'bg-blue-600 text-white' : 'bg-blue-600/40 text-blue-300'
            }`}
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
          </div>
          <span className="text-[11px] sm:text-xs font-semibold tracking-tight leading-tight line-clamp-1">
            {t.dashboard.addTransaction}
          </span>
        </button>
      </div>
    </div>
  );
};
