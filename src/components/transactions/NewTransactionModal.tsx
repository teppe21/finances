import React, { useState } from 'react';
import { useTranslation } from '../../i18n';
import { Account, Category, CurrencyCode, TransactionDirection } from '../../types';
import { toMinorUnits } from '../../core/normalization/currency';
import { Button } from '../common/Button';
import { X, Check } from 'lucide-react';

interface NewTransactionModalProps {
  accounts: Account[];
  categories: Category[];
  currency: CurrencyCode;
  onSave: (data: {
    accountId: string;
    amountMinor: number;
    currency: CurrencyCode;
    direction: TransactionDirection;
    description: string;
    merchant?: string;
    date: string;
    categoryId?: string;
    notes?: string;
  }) => void;
  onClose: () => void;
}

export const NewTransactionModal: React.FC<NewTransactionModalProps> = ({
  accounts,
  categories,
  currency,
  onSave,
  onClose,
}) => {
  const { t } = useTranslation();
  const [direction, setDirection] = useState<TransactionDirection>('expense');
  const [amountMajor, setAmountMajor] = useState('');
  const [merchant, setMerchant] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(new Date().toISOString().substring(0, 10));
  const [accountId, setAccountId] = useState(accounts[0]?.id || 'acc_cash');
  const [categoryId, setCategoryId] = useState(
    categories.find((c) => c.id === 'food')?.id || 'other'
  );
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(amountMajor) || 0;
    if (val <= 0) {
      alert('Kérlek adj meg 0-nál nagyobb összeget!');
      return;
    }

    const sign = direction === 'expense' ? -1 : 1;
    const amountMinor = sign * toMinorUnits(val, currency);

    onSave({
      accountId,
      amountMinor,
      currency,
      direction,
      description: description.trim() || merchant.trim() || 'Tranzakció',
      merchant: merchant.trim() || undefined,
      date,
      categoryId,
      notes: notes.trim() || undefined,
    });
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-md p-6 space-y-4 shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-slate-700 pb-3">
          <h3 className="text-base font-bold text-slate-100">{t.transactions.newTransaction}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Direction Switcher */}
        <div className="grid grid-cols-2 gap-2 p-1 bg-slate-900 rounded-2xl border border-slate-700/60">
          <button
            type="button"
            onClick={() => setDirection('expense')}
            className={`py-2 rounded-xl text-xs font-bold transition-all ${
              direction === 'expense'
                ? 'bg-rose-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Kiadás
          </button>
          <button
            type="button"
            onClick={() => setDirection('income')}
            className={`py-2 rounded-xl text-xs font-bold transition-all ${
              direction === 'income'
                ? 'bg-emerald-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Bevétel
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3.5 text-xs sm:text-sm">
          <div>
            <label className="block text-slate-400 mb-1 font-medium">{t.transactions.amount} ({currency})</label>
            <input
              type="number"
              step="any"
              autoFocus
              placeholder="0"
              value={amountMajor}
              onChange={(e) => setAmountMajor(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xl font-bold text-slate-100 focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Partner</label>
              <input
                type="text"
                placeholder="pl. Lidl, SPAR, BKK"
                value={merchant}
                onChange={(e) => setMerchant(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-medium">{t.transactions.date}</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">{t.transactions.category}</label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-medium">{t.transactions.account}</label>
              <select
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              >
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} ({a.type})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1 font-medium">Megjegyzés</label>
            <input
              type="text"
              placeholder="Opcionális leírás..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-slate-700">
            <Button variant="ghost" onClick={onClose}>
              {t.common.cancel}
            </Button>
            <Button type="submit" variant="primary" icon={<Check className="w-4 h-4" />}>
              {t.common.save}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
