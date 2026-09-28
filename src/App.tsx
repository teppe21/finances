import React, { useEffect, useState } from 'react';
import { initializeDatabase } from './database/database';
import { useTransactionStore } from './store/transactionStore';
import { useAccountStore } from './store/accountStore';
import { useSettingsStore } from './store/settingsStore';
import { notificationBridge } from './native/notificationListener';
import { NotificationRepository } from './database/repositories/notificationRepository';
import { CategoryRepository } from './database/repositories/categoryRepository';
import { TopBar } from './components/common/TopBar';
import { BottomNav } from './components/common/BottomNav';
import { DashboardScreen } from './features/dashboard/DashboardScreen';
import { TransactionsScreen } from './features/transactions/TransactionsScreen';
import { AnalyticsScreen } from './features/analytics/AnalyticsScreen';
import { AccountsScreen } from './features/accounts/AccountsScreen';
import { MoreScreen } from './features/more/MoreScreen';
import { ScanReceiptScreen } from './features/receipt/ScanReceiptScreen';
import { NotificationScreen } from './features/notifications/NotificationScreen';
import { ImportScreen } from './features/imports/ImportScreen';
import { BudgetsScreen } from './features/budgets/BudgetsScreen';
import { RecurringScreen } from './features/recurring/RecurringScreen';
import { CategoriesScreen } from './features/categories/CategoriesScreen';
import { SettingsScreen } from './features/settings/SettingsScreen';
import { NewTransactionModal } from './components/transactions/NewTransactionModal';
import { darkColors, lightColors } from './theme';
import { Lock, ShieldCheck, Key } from 'lucide-react';
import { Button } from './components/common/Button';
import { SecurityService } from './services/security/securityService';

export default function App() {
  const [isReady, setIsReady] = useState(false);
  const [navHistory, setNavHistory] = useState<string[]>(['dashboard']);
  const currentTab = navHistory[navHistory.length - 1] || 'dashboard';

  const navigateTo = (screen: string) => {
    if (screen === currentTab) return;
    try {
      window.history.pushState({ screen }, '', `#${screen}`);
    } catch {
      // history API fallback
    }
    setNavHistory((prev) => [...prev, screen]);
  };

  const handleGoBack = () => {
    if (navHistory.length > 1) {
      try {
        window.history.back();
      } catch {
        setNavHistory((prev) => prev.slice(0, prev.length - 1));
      }
    } else {
      setNavHistory(['dashboard']);
    }
  };

  useEffect(() => {
    const handlePopState = (e: PopStateEvent) => {
      if (e.state && e.state.screen) {
        setNavHistory((prev) => {
          if (prev[prev.length - 1] === e.state.screen) return prev;
          return [...prev.slice(0, -1), e.state.screen];
        });
      } else {
        setNavHistory(['dashboard']);
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const [isAddTxOpen, setIsAddTxOpen] = useState(false);
  const [categories, setCategories] = useState<any[]>([]);
  const [pendingNotifCount, setPendingNotifCount] = useState(0);

  const { loadTransactions, addTransaction } = useTransactionStore();
  const { accounts, loadAccounts } = useAccountStore();
  const { theme, currency, isUnlocked, lockType, unlockApp, loadSettings } = useSettingsStore();

  const [enteredPin, setEnteredPin] = useState('');
  const [pinError, setPinError] = useState(false);

  useEffect(() => {
    async function boot() {
      await initializeDatabase();
      await loadSettings();
      await loadAccounts();
      await loadTransactions();

      const cats = await new CategoryRepository().getAllCategories();
      setCategories(cats);

      const notifRepo = new NotificationRepository();
      const pending = await notifRepo.getPendingReviewEvents();
      setPendingNotifCount(pending.length);

      setIsReady(true);
    }
    boot();

    // Listen for live background bank notifications via native Android bridge
    const unsubscribe = notificationBridge.addListener(async (notif) => {
      await loadTransactions();
      const notifRepo = new NotificationRepository();
      const pending = await notifRepo.getPendingReviewEvents();
      setPendingNotifCount(pending.length);
    });

    return () => unsubscribe();
  }, []);

  const handleVerifyPin = async (e: React.FormEvent) => {
    e.preventDefault();
    const sec = new SecurityService();
    const ok = await sec.verifyPin(enteredPin);
    if (ok) {
      unlockApp();
      setPinError(false);
      setEnteredPin('');
    } else {
      setPinError(true);
      setEnteredPin('');
    }
  };

  const colors = theme === 'light' ? lightColors : darkColors;

  if (!isReady) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-white space-y-3">
        <div className="w-12 h-12 rounded-2xl bg-blue-600/20 text-blue-400 flex items-center justify-center animate-pulse">
          <ShieldCheck className="w-6 h-6" />
        </div>
        <span className="text-sm font-semibold tracking-wide text-slate-300">Pénzügyek betöltése...</span>
      </div>
    );
  }

  // App Lock PIN Screen
  if (!isUnlocked && lockType !== 'off') {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 text-white">
        <div className="w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 text-center space-y-5 shadow-2xl">
          <div className="w-14 h-14 rounded-2xl bg-blue-500/15 text-blue-400 mx-auto flex items-center justify-center">
            <Lock className="w-7 h-7" />
          </div>

          <div>
            <h2 className="text-lg font-bold text-slate-100">Alkalmazás Zárolva</h2>
            <p className="text-xs text-slate-400 mt-1">Add meg a PIN kódodat a belépéshez</p>
          </div>

          <form onSubmit={handleVerifyPin} className="space-y-4">
            <input
              type="password"
              maxLength={6}
              autoFocus
              value={enteredPin}
              onChange={(e) => setEnteredPin(e.target.value.replace(/\D/g, ''))}
              placeholder="••••"
              className="w-full bg-slate-950 border border-slate-700 rounded-2xl p-3 text-center text-2xl tracking-widest text-slate-100 focus:outline-none focus:border-blue-500"
            />

            {pinError && (
              <span className="text-xs font-semibold text-rose-400 block">
                Helytelen PIN kód!
              </span>
            )}

            <Button type="submit" variant="primary" fullWidth icon={<Key className="w-4 h-4" />}>
              Feloldás
            </Button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div
      className="min-h-screen font-sans transition-colors"
      style={{ backgroundColor: colors.background, color: colors.textPrimary }}
    >
      {/* Top Bar with Prominent Top-Left Back Button for all non-root screens */}
      {currentTab !== 'scan-receipt' && (
        <TopBar
          currentScreen={currentTab}
          onGoBack={handleGoBack}
          onOpenNotifications={() => navigateTo('notifications')}
          pendingNotificationCount={pendingNotifCount}
        />
      )}

      <main className="min-h-[calc(100vh-3.5rem)]">
        {currentTab === 'dashboard' && (
          <DashboardScreen
            onNavigate={(tab) => navigateTo(tab)}
            onOpenScanReceipt={() => navigateTo('scan-receipt')}
            onOpenImport={() => navigateTo('import')}
            onOpenAddTransaction={() => setIsAddTxOpen(true)}
            onOpenNotifications={() => navigateTo('notifications')}
            pendingNotificationCount={pendingNotifCount}
          />
        )}

        {currentTab === 'transactions' && (
          <TransactionsScreen onOpenAddTransaction={() => setIsAddTxOpen(true)} />
        )}

        {currentTab === 'analytics' && <AnalyticsScreen />}

        {currentTab === 'accounts' && <AccountsScreen />}

        {currentTab === 'more' && <MoreScreen onNavigate={(screen) => navigateTo(screen)} />}

        {currentTab === 'scan-receipt' && (
          <ScanReceiptScreen
            onBack={handleGoBack}
            onSuccess={() => {
              handleGoBack();
              loadTransactions();
            }}
          />
        )}

        {currentTab === 'notifications' && <NotificationScreen />}

        {currentTab === 'import' && (
          <ImportScreen
            onBack={handleGoBack}
            onSuccess={() => {
              navigateTo('transactions');
              loadTransactions();
            }}
          />
        )}

        {currentTab === 'budgets' && <BudgetsScreen />}

        {currentTab === 'recurring' && <RecurringScreen />}

        {currentTab === 'categories' && <CategoriesScreen />}

        {currentTab === 'settings' && <SettingsScreen />}
      </main>

      {/* Floating quick modal for New Transaction */}
      {isAddTxOpen && (
        <NewTransactionModal
          accounts={accounts}
          categories={categories}
          currency={currency}
          onSave={async (data) => {
            await addTransaction({ ...data, source: 'manual' });
            setIsAddTxOpen(false);
          }}
          onClose={() => setIsAddTxOpen(false)}
        />
      )}

      {/* Bottom Navigation */}
      {currentTab !== 'scan-receipt' && (
        <BottomNav
          currentTab={
            ['dashboard', 'transactions', 'analytics', 'accounts'].includes(currentTab)
              ? currentTab
              : 'more'
          }
          onSelectTab={(tab) => {
            if (tab === 'dashboard') {
              setNavHistory(['dashboard']);
            } else {
              navigateTo(tab);
            }
          }}
        />
      )}
    </div>
  );
}