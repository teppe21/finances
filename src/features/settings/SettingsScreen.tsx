import React, { useState } from 'react';
import { useTranslation } from '../../i18n';
import { useSettingsStore } from '../../store/settingsStore';
import { useTransactionStore } from '../../store/transactionStore';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { BackupService } from '../../services/backup/backupService';
import { SecurityService, LockType } from '../../services/security/securityService';
import { generateCsvContent } from '../../services/export/csvExportService';
import {
  Moon,
  Sun,
  Globe,
  Coins,
  Shield,
  Lock,
  Download,
  Upload,
  Database,
  CheckCircle2,
  FileCode,
  Key,
} from 'lucide-react';

export const SettingsScreen: React.FC = () => {
  const { t } = useTranslation();
  const { language, setLanguage, theme, setTheme, currency, setCurrency, lockType, setLockType } = useSettingsStore();
  const { transactions, loadTransactions } = useTransactionStore();

  const [pinInput, setPinInput] = useState('');
  const [isPinModalOpen, setIsPinModalOpen] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');

  const backupService = new BackupService();
  const securityService = new SecurityService();

  const handleDownloadBackup = async () => {
    try {
      const json = await backupService.createBackup();
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `penzugyek_mentes_${new Date().toISOString().substring(0, 10)}.json`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setStatusMsg('Biztonsági mentés letöltve!');
      setTimeout(() => setStatusMsg(''), 3000);
    } catch (e: any) {
      alert(`Hiba a mentésnél: ${e.message}`);
    }
  };

  const handleRestoreBackup = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (evt) => {
      try {
        const text = evt.target?.result as string;
        const res = await backupService.restoreBackup(text);
        if (res.errors.length > 0) {
          alert(`Figyelmeztetés a visszaállításkor: ${res.errors.join('\n')}`);
        } else {
          setStatusMsg(`Sikeres visszaállítás: ${res.restoredCount} tétel!`);
          await loadTransactions();
          setTimeout(() => setStatusMsg(''), 3000);
        }
      } catch (err: any) {
        alert(`Hiba a visszaállításkor: ${err.message}`);
      }
    };
    reader.readAsText(file);
  };

  const handleMigrateLegacyData = async () => {
    try {
      const storedTx = localStorage.getItem('financial_current_transactions');
      const storedSaved = localStorage.getItem('financial_saved_months');
      const storedCat = localStorage.getItem('financial_custom_categories');

      if (!storedTx && !storedSaved && !storedCat) {
        alert('Nem található korábbi webes localStorage adat a böngészőben.');
        return;
      }

      const legacyObj = {
        financial_current_transactions: storedTx ? JSON.parse(storedTx) : [],
        financial_saved_months: storedSaved ? JSON.parse(storedSaved) : {},
        financial_custom_categories: storedCat ? JSON.parse(storedCat) : [],
      };

      const res = await backupService.migrateLegacyWebData(legacyObj);
      setStatusMsg(`Régi webes adatok sikeresen migrálva (${res.importedCount} tétel)!`);
      await loadTransactions();
      setTimeout(() => setStatusMsg(''), 3000);
    } catch (e: any) {
      alert(`Hiba a régi adatok migrációjakor: ${e.message}`);
    }
  };

  const handleSetPin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await securityService.setPin(pinInput);
      await setLockType('pin');
      setIsPinModalOpen(false);
      setPinInput('');
      setStatusMsg('PIN kód sikeresen beállítva!');
      setTimeout(() => setStatusMsg(''), 3000);
    } catch (e: any) {
      alert(e.message);
    }
  };

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      <div>
        <h2 className="text-lg font-bold text-slate-100">{t.settings.title}</h2>
        <p className="text-xs text-slate-400">Alkalmazás testreszabása, védelem és adatmentés</p>
      </div>

      {statusMsg && (
        <div className="p-3 bg-emerald-500/15 border border-emerald-500/30 rounded-2xl flex items-center gap-2 text-xs font-semibold text-emerald-300">
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{statusMsg}</span>
        </div>
      )}

      {/* Appearance */}
      <Card className="p-4 space-y-3">
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-2">
          <Sun className="w-4 h-4 text-amber-400" />
          {t.settings.appearance}
        </h3>
        <div className="grid grid-cols-3 gap-2">
          {(['dark', 'light', 'system'] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setTheme(mode)}
              className={`p-2.5 rounded-xl text-xs font-semibold border transition-all ${
                theme === mode
                  ? 'bg-blue-600 text-white border-blue-500 shadow-md shadow-blue-500/20'
                  : 'bg-slate-900 text-slate-300 border-slate-700/60'
              }`}
            >
              {mode === 'dark' ? t.settings.themeDark : mode === 'light' ? t.settings.themeLight : t.settings.themeSystem}
            </button>
          ))}
        </div>
      </Card>

      {/* Language & Currency */}
      <Card className="p-4 space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2 flex items-center gap-1.5">
              <Globe className="w-3.5 h-3.5 text-blue-400" />
              {t.settings.language}
            </label>
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value as any)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-xs text-slate-100 font-semibold focus:outline-none focus:border-blue-500"
            >
              <option value="hu">Magyar</option>
              <option value="en">English</option>
            </select>
          </div>

          <div>
            <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-2 flex items-center gap-1.5">
              <Coins className="w-3.5 h-3.5 text-amber-400" />
              {t.settings.currency}
            </label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value as any)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-xs text-slate-100 font-semibold focus:outline-none focus:border-blue-500"
            >
              <option value="HUF">HUF (Ft)</option>
              <option value="EUR">EUR (€)</option>
              <option value="USD">USD ($)</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Security & App Lock */}
      <Card className="p-4 space-y-3">
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-2">
          <Shield className="w-4 h-4 text-emerald-400" />
          {t.settings.security}
        </h3>

        <div className="flex items-center justify-between py-1">
          <div>
            <span className="text-sm font-semibold text-slate-200 block">{t.settings.appLock}</span>
            <span className="text-xs text-slate-400">Jelenlegi védelem: {lockType.toUpperCase()}</span>
          </div>

          <div className="flex items-center gap-2">
            {lockType === 'off' ? (
              <Button size="sm" variant="primary" onClick={() => setIsPinModalOpen(true)}>
                PIN beállítása
              </Button>
            ) : (
              <Button size="sm" variant="secondary" onClick={() => setLockType('off')}>
                Kikapcsolás
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Backup, Restore & Migration */}
      <Card className="p-4 space-y-3">
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-2">
          <Database className="w-4 h-4 text-slate-400" />
          {t.settings.backup}
        </h3>

        <div className="space-y-2">
          <Button
            variant="secondary"
            fullWidth
            onClick={handleDownloadBackup}
            icon={<Download className="w-4 h-4 text-blue-400" />}
          >
            {t.settings.createBackup}
          </Button>

          <label className="block cursor-pointer">
            <input
              type="file"
              accept=".json"
              onChange={handleRestoreBackup}
              className="hidden"
            />
            <div className="w-full py-2.5 px-4 rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium flex items-center justify-center gap-2 transition-all">
              <Upload className="w-4 h-4 text-blue-400" />
              <span>{t.settings.restoreBackup}</span>
            </div>
          </label>

          <Button
            variant="ghost"
            fullWidth
            onClick={handleMigrateLegacyData}
            icon={<FileCode className="w-4 h-4 text-emerald-400" />}
          >
            {t.settings.migrateLegacy}
          </Button>
        </div>
      </Card>

      {/* PIN Modal */}
      {isPinModalOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-sm p-6 space-y-4 shadow-2xl">
            <h3 className="text-base font-bold text-slate-100 flex items-center gap-2">
              <Key className="w-5 h-5 text-blue-400" />
              Új PIN kód megadása
            </h3>

            <form onSubmit={handleSetPin} className="space-y-4">
              <input
                type="password"
                maxLength={6}
                value={pinInput}
                onChange={(e) => setPinInput(e.target.value.replace(/\D/g, ''))}
                placeholder="****"
                className="w-full bg-slate-900 border border-slate-700 rounded-2xl p-3 text-center text-2xl tracking-widest text-slate-100 focus:outline-none focus:border-blue-500"
                required
              />

              <div className="flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setIsPinModalOpen(false)}>
                  Mégse
                </Button>
                <Button type="submit" variant="primary">
                  Mentés
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
