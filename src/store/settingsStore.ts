import { create } from 'zustand';
import { SettingsRepository } from '../database/repositories/settingsRepository';
import { CurrencyCode } from '../types';
import { LockType } from '../services/security/securityService';

interface SettingsState {
  language: 'hu' | 'en';
  theme: 'dark' | 'light' | 'system';
  currency: CurrencyCode;
  notificationImportEnabled: boolean;
  receiptOcrEnabled: boolean;
  lockType: LockType;
  isUnlocked: boolean;

  loadSettings: () => Promise<void>;
  setLanguage: (lang: 'hu' | 'en') => Promise<void>;
  setTheme: (theme: 'dark' | 'light' | 'system') => Promise<void>;
  setCurrency: (currency: CurrencyCode) => Promise<void>;
  setNotificationImportEnabled: (enabled: boolean) => Promise<void>;
  setReceiptOcrEnabled: (enabled: boolean) => Promise<void>;
  setLockType: (type: LockType) => Promise<void>;
  unlockApp: () => void;
  lockApp: () => void;
}

const settingsRepo = new SettingsRepository();

export const useSettingsStore = create<SettingsState>((set, get) => ({
  language: 'hu',
  theme: 'dark',
  currency: 'HUF',
  notificationImportEnabled: true,
  receiptOcrEnabled: true,
  lockType: 'off',
  isUnlocked: true,

  loadSettings: async () => {
    const lang = ((await settingsRepo.get('app_language', 'hu')) as 'hu' | 'en');
    const theme = ((await settingsRepo.get('app_theme', 'dark')) as 'dark' | 'light' | 'system');
    const currency = ((await settingsRepo.get('app_currency', 'HUF')) as CurrencyCode);
    const notifEnabled = (await settingsRepo.get('app_notif_enabled', 'true')) === 'true';
    const ocrEnabled = (await settingsRepo.get('app_ocr_enabled', 'true')) === 'true';
    const lockType = ((await settingsRepo.get('security_lock_type', 'off')) as LockType);

    set({
      language: lang,
      theme,
      currency,
      notificationImportEnabled: notifEnabled,
      receiptOcrEnabled: ocrEnabled,
      lockType,
      isUnlocked: lockType === 'off',
    });
  },

  setLanguage: async (lang) => {
    await settingsRepo.set('app_language', lang);
    set({ language: lang });
  },

  setTheme: async (theme) => {
    await settingsRepo.set('app_theme', theme);
    set({ theme });
  },

  setCurrency: async (currency) => {
    await settingsRepo.set('app_currency', currency);
    set({ currency });
  },

  setNotificationImportEnabled: async (enabled) => {
    await settingsRepo.set('app_notif_enabled', enabled ? 'true' : 'false');
    set({ notificationImportEnabled: enabled });
  },

  setReceiptOcrEnabled: async (enabled) => {
    await settingsRepo.set('app_ocr_enabled', enabled ? 'true' : 'false');
    set({ receiptOcrEnabled: enabled });
  },

  setLockType: async (lockType) => {
    await settingsRepo.set('security_lock_type', lockType);
    set({ lockType });
  },

  unlockApp: () => set({ isUnlocked: true }),
  lockApp: () => {
    if (get().lockType !== 'off') {
      set({ isUnlocked: false });
    }
  },
}));
