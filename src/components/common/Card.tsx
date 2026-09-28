import React from 'react';
import { darkColors, lightColors } from '../../theme';
import { useSettingsStore } from '../../store/settingsStore';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  style?: any;
  onClick?: () => void;
}

export const Card: React.FC<CardProps> = ({ children, className = '', style = {}, onClick }) => {
  const theme = useSettingsStore((s) => s.theme);
  const colors = theme === 'light' ? lightColors : darkColors;

  return (
    <div
      onClick={onClick}
      className={`rounded-2xl p-4 transition-all ${onClick ? 'cursor-pointer hover:opacity-95 active:scale-[0.99]' : ''} ${className}`}
      style={{
        backgroundColor: colors.surface,
        borderColor: colors.border,
        borderWidth: '1px',
        borderStyle: 'solid',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
        ...style,
      }}
    >
      {children}
    </div>
  );
};
