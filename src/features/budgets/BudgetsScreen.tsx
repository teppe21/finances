import React, { useState, useEffect } from 'react';
import { useTranslation } from '../../i18n';
import { useTransactionStore } from '../../store/transactionStore';
import { BudgetRepository } from '../../database/repositories/budgetRepository';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { formatCurrency, fromMinorUnits, toMinorUnits } from '../../core/normalization/currency';
import { Budget, Category } from '../../types';
import { generateId } from '../../utils/hashing';
import { Target, Plus, AlertCircle, CheckCircle2, X } from 'lucide-react';

export const BudgetsScreen: React.FC = () => {
  const { t } = useTranslation();
  const { categoryBreakdown } = useTransactionStore();

  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isAddOpen, setIsAddOpen] = useState(false);

  const [selectedCatId, setSelectedCatId] = useState('food');
  const [budgetMajor, setBudgetMajor] = useState('100000');

  const budgetRepo = new BudgetRepository();
  const catRepo = new CategoryRepository();

  const loadData = async () => {
    const bList = await budgetRepo.getAll();
    const cList = await catRepo.getAllCategories();
    setBudgets(bList);
    setCategories(cList);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSaveBudget = async (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseFloat(budgetMajor) || 0;
    if (val <= 0) return;

    const b: Budget = {
      id: generateId('bgt'),
      categoryId: selectedCatId,
      amountMinor: toMinorUnits(val, 'HUF'),
      period: 'monthly',
    };

    await budgetRepo.saveBudget(b);
    await loadData();
    setIsAddOpen(false);
  };

  const handleDeleteBudget = async (id: string) => {
    await budgetRepo.deleteBudget(id);
    await loadData();
  };

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-lg font-bold text-slate-100">Havi Költségkeretek</h2>
          <p className="text-xs text-slate-400">Kövesd nyomon költési limitjeidet kategóriánként</p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setIsAddOpen(true)}
          icon={<Plus className="w-4 h-4" />}
        >
          Új Keret
        </Button>
      </div>

      <div className="space-y-3">
        {budgets.length > 0 ? (
          budgets.map((b) => {
            const cat = categories.find((c) => c.id === b.categoryId);
            const spentSummary = categoryBreakdown.find((c) => c.categoryId === b.categoryId);
            const spentMinor = spentSummary?.totalMinor || 0;
            const percent = (spentMinor / b.amountMinor) * 100;
            const remainingMinor = b.amountMinor - spentMinor;
            const isOverBudget = remainingMinor < 0;

            return (
              <Card key={b.id} className="p-4 space-y-3">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-bold text-sm text-slate-100">
                      {cat?.name || b.categoryId}
                    </h3>
                    <span className="text-xs text-slate-400">
                      {formatCurrency(spentMinor)} / {formatCurrency(b.amountMinor)}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <Badge variant={isOverBudget ? 'danger' : percent > 80 ? 'warning' : 'success'}>
                      {percent.toFixed(0)}%
                    </Badge>
                    <button
                      onClick={() => handleDeleteBudget(b.id)}
                      className="text-slate-500 hover:text-rose-400 p-1"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Progress bar */}
                <div className="w-full h-2 bg-slate-900 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${
                      isOverBudget ? 'bg-rose-500' : percent > 80 ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}
                    style={{ width: `${Math.min(100, Math.max(2, percent))}%` }}
                  />
                </div>

                <div className="flex justify-between items-center text-[11px]">
                  <span className="text-slate-400">
                    {isOverBudget ? 'Túllépés:' : 'Fennmaradó keret:'}
                  </span>
                  <span
                    className={`font-bold ${isOverBudget ? 'text-rose-400' : 'text-emerald-400'}`}
                  >
                    {formatCurrency(Math.abs(remainingMinor))}
                  </span>
                </div>
              </Card>
            );
          })
        ) : (
          <div className="text-center py-12 text-slate-500 text-xs">
            Még nem hoztál létre havi költségkeretet.
          </div>
        )}
      </div>

      {/* Add Budget Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-md p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-700 pb-3">
              <h3 className="text-base font-bold text-slate-100">Új Keret Létrehozása</h3>
              <button onClick={() => setIsAddOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveBudget} className="space-y-3.5 text-xs sm:text-sm">
              <div>
                <label className="block text-slate-400 mb-1 font-medium">Kategória</label>
                <select
                  value={selectedCatId}
                  onChange={(e) => setSelectedCatId(e.target.value)}
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
                <label className="block text-slate-400 mb-1 font-medium">Havi limit (HUF)</label>
                <input
                  type="number"
                  step="any"
                  value={budgetMajor}
                  onChange={(e) => setBudgetMajor(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500 font-bold"
                  required
                />
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-slate-700">
                <Button variant="ghost" onClick={() => setIsAddOpen(false)}>
                  {t.common.cancel}
                </Button>
                <Button type="submit" variant="primary">
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
