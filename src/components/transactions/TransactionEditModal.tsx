import React, { useState } from 'react';
import { Transaction, Category, Account } from '../../types';
import { fromMinorUnits, toMinorUnits } from '../../core/normalization/currency';
import { Button } from '../common/Button';
import { useTranslation } from '../../i18n';
import { X, Trash2, Check } from 'lucide-react';

interface TransactionEditModalProps {
  transaction: Transaction;
  categories: Category[];
  accounts: Account[];
  onSave: (updated: Transaction) => void;
  onDelete: (id: string) => void;
  onClose: () => void;
}

export const TransactionEditModal: React.FC<TransactionEditModalProps> = ({
  transaction,
  categories,
  accounts,
  onSave,
  onDelete,
  onClose,
}) => {
  const { t } = useTranslation();
  const [description, setDescription] = useState(transaction.description);
  const [merchant, setMerchant] = useState(transaction.merchant || '');
  const [amountMajor, setAmountMajor] = useState(
    fromMinorUnits(transaction.amountMinor, transaction.currency).toString()
  );
  const [date, setDate] = useState(transaction.date);
  const [categoryId, setCategoryId] = useState(transaction.categoryId || 'other');
  const [accountId, setAccountId] = useState(transaction.accountId);
  const [notes, setNotes] = useState(transaction.notes || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(amountMajor) || 0;
    const amountMinor = toMinorUnits(val, transaction.currency);
    const direction = val > 0 ? 'income' : val < 0 ? 'expense' : 'adjustment';

    onSave({
      ...transaction,
      description: description.trim(),
      merchant: merchant.trim() || undefined,
      amountMinor,
      direction,
      date,
      categoryId,
      accountId,
      notes: notes.trim() || undefined,
      updatedAt: new Date().toISOString(),
    });
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-lg p-5 sm:p-6 space-y-4 shadow-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-slate-700 pb-3">
          <h3 className="text-base font-bold text-slate-100">
            {t.transactions.editTransaction}
          </h3>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-200 p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs sm:text-sm">
          <div>
            <label className="block text-slate-400 mb-1 font-medium">Partner / Kereskedő</label>
            <input
              type="text"
              value={merchant}
              onChange={(e) => setMerchant(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              placeholder="pl. Lidl, MOL, Spar"
            />
          </div>

          <div>
            <label className="block text-slate-400 mb-1 font-medium">Leírás</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">Összeg ({transaction.currency})</label>
              <input
                type="number"
                step="any"
                value={amountMajor}
                onChange={(e) => setAmountMajor(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 font-bold"
                required
              />
              <span className="text-[10px] text-slate-500 mt-1 block">Pozitív = Bevétel, Negatív = Kiadás</span>
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-medium">Dátum</label>
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
              <label className="block text-slate-400 mb-1 font-medium">Kategória</label>
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
              <label className="block text-slate-400 mb-1 font-medium">Számla</label>
              <select
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              >
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} ({a.institution})
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-slate-400 mb-1 font-medium">Megjegyzések</label>
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 text-xs"
              placeholder="Opcionális feljegyzés..."
            />
          </div>

          <div className="flex justify-between items-center pt-4 border-t border-slate-700">
            <button
              type="button"
              onClick={() => {
                if (confirm(t.transactions.deleteConfirm)) {
                  onDelete(transaction.id);
                  onClose();
                }
              }}
              className="p-2.5 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-xl flex items-center gap-1.5 font-medium"
            >
              <Trash2 className="w-4 h-4" />
              <span>{t.common.delete}</span>
            </button>

            <div className="flex items-center gap-2">
              <Button variant="ghost" onClick={onClose}>
                {t.common.cancel}
              </Button>
              <Button type="submit" variant="primary" icon={<Check className="w-4 h-4" />}>
                {t.common.save}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
