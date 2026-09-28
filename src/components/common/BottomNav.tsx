import React from 'react';
import { LayoutDashboard, ReceiptText, PieChart, Wallet, Menu } from 'lucide-react';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { darkColors, lightColors } from '../../theme';

interface BottomNavProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ currentTab, onSelectTab }) => {
  const { t } = useTranslation();
  const theme = useSettingsStore((s) => s.theme);
  const isLight = theme === 'light';
  const colors = isLight ? lightColors : darkColors;

  const tabs = [
    { id: 'dashboard', label: t.tabs.home, icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'transactions', label: t.tabs.transactions, icon: <ReceiptText className="w-5 h-5" /> },
    { id: 'analytics', label: t.tabs.analytics, icon: <PieChart className="w-5 h-5" /> },
    { id: 'accounts', label: t.tabs.accounts, icon: <Wallet className="w-5 h-5" /> },
    { id: 'more', label: t.tabs.more, icon: <Menu className="w-5 h-5" /> },
  ];

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-40 border-t backdrop-blur-lg px-2 pt-1.5 flex items-center justify-around max-w-2xl mx-auto transition-colors"
      style={{
        backgroundColor: isLight ? 'rgba(255, 255, 255, 0.95)' : 'rgba(15, 23, 42, 0.95)',
        borderColor: colors.border,
        paddingBottom: 'calc(0.5rem + env(safe-area-inset-bottom, 0px))',
      }}
    >
      {tabs.map((tab) => {
        const isActive = currentTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onSelectTab(tab.id)}
            className={`flex flex-col items-center justify-center flex-1 py-1 min-h-[48px] transition-all active:scale-95 ${
              isActive
                ? 'text-blue-500 font-bold'
                : isLight
                ? 'text-slate-500 hover:text-slate-800'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <div className={`p-1 rounded-xl transition-all ${isActive ? 'bg-blue-500/15' : ''}`}>
              {tab.icon}
            </div>
            <span className="text-[10px] sm:text-[11px] mt-0.5 tracking-tight">{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
};
