import React from 'react';
import { Card } from '../../components/common/Card';
import {
  Target,
  Repeat,
  Tag,
  BellRing,
  Camera,
  FileSpreadsheet,
  Settings,
  Database,
  ChevronRight,
  Shield,
} from 'lucide-react';

interface MoreScreenProps {
  onNavigate: (screen: string) => void;
}

export const MoreScreen: React.FC<MoreScreenProps> = ({ onNavigate }) => {
  const menuItems = [
    {
      id: 'budgets',
      title: 'Havi Költségkeretek',
      subtitle: 'Kategória limitek és megtakarítások',
      icon: <Target className="w-5 h-5 text-blue-400" />,
    },
    {
      id: 'recurring',
      title: 'Fix és Rendszeres Kiadások',
      subtitle: 'Előfizetések, számlák és fizetések',
      icon: <Repeat className="w-5 h-5 text-amber-400" />,
    },
    {
      id: 'categories',
      title: 'Kategóriák és Szabályok',
      subtitle: 'Automatikus kategorizálási kulcsszavak',
      icon: <Tag className="w-5 h-5 text-emerald-400" />,
    },
    {
      id: 'notifications',
      title: 'Banki Értesítés Import & Diagnosztika',
      subtitle: 'Revolut, OTP, Erste, MBH, Wise és figyelés',
      icon: <BellRing className="w-5 h-5 text-indigo-400" />,
    },
    {
      id: 'scan-receipt',
      title: 'Nyugta Beolvasás (OCR)',
      subtitle: 'Készpénzes vásárlások fényképezése',
      icon: <Camera className="w-5 h-5 text-blue-400" />,
    },
    {
      id: 'import',
      title: 'Banki Kivonat Import (CSV)',
      subtitle: 'Kivonatok és számlatörténet beolvasása',
      icon: <FileSpreadsheet className="w-5 h-5 text-teal-400" />,
    },
    {
      id: 'settings',
      title: 'Beállítások & Biztonság',
      subtitle: 'Téma, nyelv, PIN védelem, adatmentés',
      icon: <Settings className="w-5 h-5 text-slate-300" />,
    },
  ];

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      <div>
        <h2 className="text-lg font-bold text-slate-100">További Funkciók</h2>
        <p className="text-xs text-slate-400">Automatizáció, szabályok és rendszerbeállítások</p>
      </div>

      <Card className="divide-y divide-slate-800/70 p-2">
        {menuItems.map((item) => (
          <button
            key={item.id}
            onClick={() => onNavigate(item.id)}
            className="w-full p-3.5 flex items-center justify-between hover:bg-slate-800/50 active:bg-slate-800/80 rounded-xl transition-colors text-left"
          >
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-slate-900 border border-slate-700/60 flex items-center justify-center flex-shrink-0">
                {item.icon}
              </div>
              <div>
                <h3 className="font-bold text-sm text-slate-200">{item.title}</h3>
                <span className="text-xs text-slate-400">{item.subtitle}</span>
              </div>
            </div>

            <ChevronRight className="w-5 h-5 text-slate-500" />
          </button>
        ))}
      </Card>
    </div>
  );
};
