export interface ThemeColors {
  background: string;
  surface: string;
  surfaceElevated: string;
  surfaceSubtle: string;
  primary: string;
  primaryHover: string;
  success: string;
  successSubtle: string;
  warning: string;
  warningSubtle: string;
  danger: string;
  dangerSubtle: string;
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  border: string;
  borderSubtle: string;
  income: string;
  expense: string;
  transfer: string;
}

export const darkColors: ThemeColors = {
  background: '#0F172A', // slate-900
  surface: '#1E293B',    // slate-800
  surfaceElevated: '#334155', // slate-700
  surfaceSubtle: 'rgba(30, 41, 59, 0.6)',
  primary: '#2563EB',    // blue-600
  primaryHover: '#3B82F6', // blue-500
  success: '#10B981',    // emerald-500
  successSubtle: 'rgba(16, 185, 129, 0.15)',
  warning: '#F59E0B',    // amber-500
  warningSubtle: 'rgba(245, 158, 11, 0.15)',
  danger: '#EF4444',     // red-500
  dangerSubtle: 'rgba(239, 68, 68, 0.15)',
  textPrimary: '#F8FAFC', // slate-50
  textSecondary: '#94A3B8', // slate-400
  textMuted: '#64748B',  // slate-500
  border: '#334155',     // slate-700
  borderSubtle: 'rgba(51, 65, 85, 0.5)',
  income: '#10B981',
  expense: '#F87171',
  transfer: '#38BDF8',
};

export const lightColors: ThemeColors = {
  background: '#F8FAFC',
  surface: '#FFFFFF',
  surfaceElevated: '#F1F5F9',
  surfaceSubtle: '#F8FAFC',
  primary: '#2563EB',
  primaryHover: '#1D4ED8',
  success: '#059669',
  successSubtle: '#ECFDF5',
  warning: '#D97706',
  warningSubtle: '#FFFBEB',
  danger: '#DC2626',
  dangerSubtle: '#FEF2F2',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  border: '#E2E8F0',
  borderSubtle: '#F1F5F9',
  income: '#059669',
  expense: '#DC2626',
  transfer: '#0284C7',
};

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
};

export const borderRadius = {
  sm: 6,
  md: 10,
  lg: 16,
  xl: 24,
  full: 9999,
};
