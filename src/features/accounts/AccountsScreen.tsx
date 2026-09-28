import React, { useState } from 'react';
import { useAccountStore } from '../../store/accountStore';
import { useTranslation } from '../../i18n';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { formatCurrency, toMinorUnits } from '../../core/normalization/currency';
import { Account, AccountType, CurrencyCode } from '../../types';
import { generateId } from '../../utils/hashing';
import {
  Wallet,
  Building2,
  PiggyBank,
  CreditCard,
  TrendingUp,
  Banknote,
  Plus,
  X,
  Check,
} from 'lucide-react';

const ACCOUNT_ICONS: Record<AccountType, React.ReactNode> = {
  cash: <Banknote className="w-5 h-5 text-emerald-400" />,
  bank: <Building2 className="w-5 h-5 text-blue-400" />,
  savings: <PiggyBank className="w-5 h-5 text-teal-400" />,
  credit_card: <CreditCard className="w-5 h-5 text-rose-400" />,
  investment: <TrendingUp className="w-5 h-5 text-amber-400" />,
  other: <Wallet className="w-5 h-5 text-slate-400" />,
};

export const AccountsScreen: React.FC = () => {
  const { t } = useTranslation();
  const { accounts, addAccount } = useAccountStore();
  const [isAddOpen, setIsAddOpen] = useState(false);

  const [name, setName] = useState('');
  const [institution, setInstitution] = useState('');
  const [type, setType] = useState<AccountType>('bank');
  const [currency, setCurrency] = useState<CurrencyCode>('HUF');
  const [openingBalance, setOpeningBalance] = useState('0');

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    const opMinor = toMinorUnits(parseFloat(openingBalance) || 0, currency);
    const now = new Date().toISOString();

    const newAcc: Account = {
      id: generateId('acc'),
      name: name.trim(),
      institution: institution.trim() || name.trim(),
      type,
      currency,
      openingBalanceMinor: opMinor,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    };

    await addAccount(newAcc);
    setIsAddOpen(false);
    setName('');
    setInstitution('');
    setOpeningBalance('0');
  };

  const totalBalance = accounts.reduce(
    (sum, a) => sum + (a.currentBalanceMinor || a.openingBalanceMinor),
    0
  );

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* Top Banner */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-lg font-bold text-slate-100">{t.accounts.title}</h2>
          <span className="text-xs text-slate-400">
            Összesen {accounts.length} számla és készpénz tárca
          </span>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setIsAddOpen(true)}
          icon={<Plus className="w-4 h-4" />}
        >
          {t.accounts.addAccount}
        </Button>
      </div>

      {/* Account Cards Grid */}
      <div className="space-y-3">
        {accounts.map((acc) => {
          const balance = acc.currentBalanceMinor !== undefined ? acc.currentBalanceMinor : acc.openingBalanceMinor;
          const icon = ACCOUNT_ICONS[acc.type] || <Wallet className="w-5 h-5" />;

          return (
            <Card key={acc.id} className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                <div className="w-12 h-12 rounded-2xl bg-slate-900 border border-slate-700/60 flex items-center justify-center flex-shrink-0">
                  {icon}
                </div>

                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-sm text-slate-100">{acc.name}</h3>
                    {acc.type === 'cash' && (
                      <Badge variant="success" size="sm">
                        Készpénz
                      </Badge>
                    )}
                  </div>
                  <span className="text-xs text-slate-400">
                    {acc.institution} • {acc.currency}
                  </span>
                </div>
              </div>

              <div className="text-right">
                <span
                  className={`font-extrabold text-base block ${
                    balance >= 0 ? 'text-slate-100' : 'text-rose-400'
                  }`}
                >
                  {formatCurrency(balance, acc.currency)}
                </span>
                <span className="text-[10px] text-slate-500">
                  Kezdő: {formatCurrency(acc.openingBalanceMinor, acc.currency)}
                </span>
              </div>
            </Card>
          );
        })}
      </div>

      {/* Add Account Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-md p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-700 pb-3">
              <h3 className="text-base font-bold text-slate-100">{t.accounts.addAccount}</h3>
              <button onClick={() => setIsAddOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreate} className="space-y-3.5 text-xs sm:text-sm">
              <div>
                <label className="block text-slate-400 mb-1 font-medium">Számla neve</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="pl. Revolut Fő, Készpénz pénztárca"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">Intézmény / Bank</label>
                <input
                  type="text"
                  value={institution}
                  onChange={(e) => setInstitution(e.target.value)}
                  placeholder="pl. OTP Bank, Erste, Revolut, Készpénz"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1 font-medium">Típus</label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value as AccountType)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                  >
                    <option value="bank">{t.accounts.bankAccount}</option>
                    <option value="cash">{t.accounts.cash}</option>
                    <option value="savings">{t.accounts.savings}</option>
                    <option value="credit_card">{t.accounts.creditCard}</option>
                    <option value="investment">Befektetés</option>
                    <option value="other">Egyéb</option>
                  </select>
                </div>

                <div>
                  <label className="block text-slate-400 mb-1 font-medium">Pénznem</label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value as CurrencyCode)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                  >
                    <option value="HUF">HUF (Ft)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="USD">USD ($)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">{t.accounts.openingBalance}</label>
                <input
                  type="number"
                  step="any"
                  value={openingBalance}
                  onChange={(e) => setOpeningBalance(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 font-bold"
                />
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-slate-700">
                <Button variant="ghost" onClick={() => setIsAddOpen(false)}>
                  {t.common.cancel}
                </Button>
                <Button type="submit" variant="primary" icon={<Check className="w-4 h-4" />}>
                  {t.common.save}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
