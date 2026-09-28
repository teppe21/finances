import React, { useState } from 'react';
import { ParsedReceiptResult } from '../../core/receipts/receiptParser';
import { Account, Category } from '../../types';
import { formatCurrency, fromMinorUnits, toMinorUnits } from '../../core/normalization/currency';
import { Button } from '../common/Button';
import { Badge } from '../common/Badge';
import { useTranslation } from '../../i18n';
import { Check, X, Camera, Sparkles, AlertCircle, ShoppingBag, Banknote, CreditCard } from 'lucide-react';

interface ReceiptReviewModalProps {
  receipt: ParsedReceiptResult;
  imageUri?: string;
  accounts: Account[];
  categories: Category[];
  onConfirm: (data: {
    merchant: string;
    totalMinor: number;
    date: string;
    categoryId: string;
    accountId: string;
    paymentMethod: 'cash' | 'card' | 'unknown';
    deleteImageAfterSave: boolean;
  }) => void;
  onRetake: () => void;
  onClose: () => void;
}

export const ReceiptReviewModal: React.FC<ReceiptReviewModalProps> = ({
  receipt,
  imageUri,
  accounts,
  categories,
  onConfirm,
  onRetake,
  onClose,
}) => {
  const { t } = useTranslation();
  const [merchant, setMerchant] = useState(receipt.merchant || 'Bolt');
  const [totalMajor, setTotalMajor] = useState(
    receipt.totalMinor ? fromMinorUnits(receipt.totalMinor, receipt.currency).toString() : '0'
  );
  const [date, setDate] = useState(receipt.date || new Date().toISOString().substring(0, 10));
  const [paymentMethod, setPaymentMethod] = useState(receipt.paymentMethod);
  const [deleteImage, setDeleteImage] = useState(false);

  // Auto-detect default category
  const defaultCategory = categories.find((c) => c.id === 'food')?.id || 'other';
  const [categoryId, setCategoryId] = useState(defaultCategory);

  // Auto-detect Cash account if payment was cash
  const cashAcc = accounts.find((a) => a.type === 'cash') || accounts[0];
  const [accountId, setAccountId] = useState(
    receipt.paymentMethod === 'cash' ? cashAcc?.id || '' : accounts[0]?.id || ''
  );

  const handleSave = () => {
    const val = parseFloat(totalMajor) || 0;
    const totalMinor = toMinorUnits(val, receipt.currency);

    onConfirm({
      merchant: merchant.trim(),
      totalMinor,
      date,
      categoryId,
      accountId,
      paymentMethod,
      deleteImageAfterSave: deleteImage,
    });
  };

  return (
    <div className="fixed inset-0 bg-slate-950/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-lg p-5 sm:p-6 space-y-4 shadow-2xl max-h-[92vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-slate-700/60 pb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-blue-500/15 text-blue-400 rounded-xl">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">
                {t.receipts.detectedTitle}
              </h3>
              <p className="text-[11px] text-slate-400">
                Megbízhatóság: {(receipt.confidence * 100).toFixed(0)}%
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Payment hint badge */}
        {receipt.paymentMethod === 'cash' && (
          <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center gap-2 text-xs text-emerald-300">
            <Banknote className="w-4 h-4 flex-shrink-0" />
            <span>{t.receipts.cashDetected}</span>
          </div>
        )}

        <div className="space-y-3.5 text-xs sm:text-sm">
          <div>
            <label className="block text-slate-400 mb-1 font-medium">{t.receipts.merchant}</label>
            <input
              type="text"
              value={merchant}
              onChange={(e) => setMerchant(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 font-semibold focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 mb-1 font-medium">{t.receipts.total} (HUF)</label>
              <input
                type="number"
                step="any"
                value={totalMajor}
                onChange={(e) => setTotalMajor(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 text-base font-bold text-rose-400 focus:outline-none focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-medium">{t.transactions.date}</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
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

          {/* Detected items snippet */}
          {receipt.items.length > 0 && (
            <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-700/50 space-y-1.5 max-h-36 overflow-y-auto">
              <span className="text-[11px] font-semibold text-slate-400 block uppercase">
                Beolvasott tételek ({receipt.items.length} db)
              </span>
              {receipt.items.map((item, idx) => (
                <div key={idx} className="flex justify-between text-xs text-slate-300">
                  <span className="truncate pr-2">{item.name}</span>
                  <span className="font-semibold text-slate-200 flex-shrink-0">
                    {item.totalPriceMinor ? formatCurrency(item.totalPriceMinor) : ''}
                  </span>
                </div>
              ))}
            </div>
          )}

          {/* Privacy Toggle: Delete receipt image after creation */}
          <div className="pt-2">
            <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-400 hover:text-slate-300">
              <input
                type="checkbox"
                checked={deleteImage}
                onChange={(e) => setDeleteImage(e.target.checked)}
                className="rounded border-slate-700 bg-slate-900 text-blue-600 focus:ring-0"
              />
              <span>{t.receipts.deleteImageKeepRecord}</span>
            </label>
          </div>
        </div>

        <div className="flex justify-between items-center pt-4 border-t border-slate-700/60 gap-3">
          <Button variant="secondary" onClick={onRetake} icon={<Camera className="w-4 h-4" />}>
            {t.receipts.retake}
          </Button>

          <Button variant="primary" onClick={handleSave} icon={<Check className="w-4 h-4" />}>
            {t.receipts.confirm}
          </Button>
        </div>
      </div>
    </div>
  );
};
