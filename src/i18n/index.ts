import { hu } from './hu';
import { en } from './en';
import { useSettingsStore } from '../store/settingsStore';

export const translations = {
  hu,
  en,
};

export function useTranslation() {
  const language = useSettingsStore((s) => s.language);
  const t = translations[language] || translations.hu;
  return { t, language };
}
