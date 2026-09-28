import React from 'react';
import { ArrowLeft, BellRing, ShieldCheck, Wallet } from 'lucide-react';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';

interface TopBarProps {
  currentScreen: string;
  screenTitle?: string;
  onGoBack: () => void;
  onOpenNotifications?: () => void;
  pendingNotificationCount?: number;
}

const SCREEN_TITLES: Record<string, string> = {
  dashboard: 'Áttekintés',
  transactions: 'Tranzakciók',
  analytics: 'Elemzések',
  accounts: 'Számlák és Tárcák',
  more: 'Továbbiak',
  'scan-receipt': 'Nyugta Beolvasás (OCR)',
  notifications: 'Banki Értesítések',
  import: 'Kivonat Import (CSV)',
  budgets: 'Havi Költségkeretek',
  recurring: 'Fix és Rendszeres Kiadások',
  categories: 'Kategóriák és Szabályok',
  settings: 'Beállítások & Biztonság',
};

export const TopBar: React.FC<TopBarProps> = ({
  currentScreen,
  screenTitle,
  onGoBack,
  onOpenNotifications,
  pendingNotificationCount = 0,
}) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);

  const isRootDashboard = currentScreen === 'dashboard';
  const title = screenTitle || SCREEN_TITLES[currentScreen] || 'Pénzügyek';

  const isLight = theme === 'light';

  return (
    <header
      className={`sticky top-0 z-40 w-full border-b backdrop-blur-md transition-colors ${
        isLight ? 'bg-white/95 border-slate-200' : 'bg-slate-900/95 border-slate-800'
      }`}
      style={{
        paddingTop: 'env(safe-area-inset-top, 0px)',
      }}
    >
      <div className="max-w-2xl mx-auto px-4 h-14 flex items-center justify-between">
        {/* Left Side: Back button or App Brand */}
        <div className="flex items-center gap-2.5 min-w-0">
          {!isRootDashboard ? (
            <button
              onClick={onGoBack}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs sm:text-sm font-semibold transition-all active:scale-95 shadow-sm min-h-[36px] ${
                isLight
                  ? 'bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-800 border-slate-300'
                  : 'bg-slate-800 hover:bg-slate-700 active:bg-slate-600 text-slate-100 border-slate-700'
              }`}
              title="Vissza"
            >
              <ArrowLeft className={`w-4 h-4 ${isLight ? 'text-slate-700' : 'text-slate-200'}`} />
              <span>Vissza</span>
            </button>
          ) : (
            <div className="flex items-center gap-2.5">
              <div
                className={`w-8 h-8 rounded-xl border flex items-center justify-center ${
                  isLight
                    ? 'bg-slate-100 border-slate-200 text-blue-600'
                    : 'bg-slate-800 border-slate-700 text-blue-400'
                }`}
              >
                <Wallet className="w-4 h-4" />
              </div>
              <span
                className={`font-bold text-sm sm:text-base tracking-tight ${
                  isLight ? 'text-slate-900' : 'text-slate-100'
                }`}
              >
                Pénzügyek
              </span>
            </div>
          )}

          {!isRootDashboard && (
            <h1
              className={`font-bold text-sm sm:text-base truncate ml-1 ${
                isLight ? 'text-slate-900' : 'text-slate-100'
              }`}
            >
              {title}
            </h1>
          )}
        </div>

        {/* Right Side: Notification Icon */}
        <div className="flex items-center gap-2">
          {onOpenNotifications && (
            <button
              onClick={onOpenNotifications}
              className={`relative p-2 rounded-xl border transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center ${
                isLight
                  ? 'bg-slate-100 hover:bg-slate-200 border-slate-300 text-slate-700'
                  : 'bg-slate-800/80 hover:bg-slate-700 border-slate-700/60 text-slate-300'
              }`}
              title="Értesítések"
            >
              <BellRing className="w-4 h-4" />
              {pendingNotificationCount > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-blue-600 text-white text-[10px] font-bold flex items-center justify-center shadow">
                  {pendingNotificationCount}
                </span>
              )}
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
