import React, { useMemo } from 'react';
import { useTransactionStore } from '../../store/transactionStore';
import { detectRecurringTransactions } from '../../core/recurring/detector';
import { Card } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { formatCurrency } from '../../core/normalization/currency';
import { Repeat, Calendar, ArrowRight, Zap } from 'lucide-react';

export const RecurringScreen: React.FC = () => {
  const { transactions } = useTransactionStore();

  const { detectedRules } = useMemo(() => {
    return detectRecurringTransactions(transactions);
  }, [transactions]);

  const totalMonthlyMinor = detectedRules.reduce(
    (sum, r) => sum + r.estimatedAmountMinor,
    0
  );

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      <div>
        <h2 className="text-lg font-bold text-slate-100">Fix és Rendszeres Kiadások</h2>
        <p className="text-xs text-slate-400">Automatikus előfizetés és számla felismerés</p>
      </div>

      {/* Summary card */}
      <Card className="bg-slate-800/80 border-slate-700/80 p-4 flex justify-between items-center">
        <div>
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
            Várható Havi Fix Kiadások
          </span>
          <span className="text-2xl font-extrabold text-amber-400 mt-1 block">
            {formatCurrency(totalMonthlyMinor)}
          </span>
        </div>
        <div className="p-3 bg-amber-500/15 text-amber-400 rounded-2xl">
          <Repeat className="w-6 h-6" />
        </div>
      </Card>

      {/* Detected recurring list */}
      <div className="space-y-3">
        {detectedRules.length > 0 ? (
          detectedRules.map((rule) => (
            <Card key={rule.id} className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-amber-500/15 text-amber-400 flex items-center justify-center flex-shrink-0">
                  <Zap className="w-5 h-5" />
                </div>

                <div>
                  <h3 className="font-bold text-sm text-slate-100">
                    {rule.merchant || rule.descriptionPattern}
                  </h3>
                  <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3 text-slate-500" />
                      Következő: {rule.nextDate}
                    </span>
                    <span>•</span>
                    <span>{rule.matchedTransactionIds.length} előfordulás</span>
                  </div>
                </div>
              </div>

              <div className="text-right">
                <span className="font-extrabold text-sm text-slate-100 block">
                  {formatCurrency(rule.estimatedAmountMinor, rule.currency)}
                </span>
                <Badge variant="warning" size="sm">
                  Havi rendszeres
                </Badge>
              </div>
            </Card>
          ))
        ) : (
          <div className="text-center py-12 text-slate-500 text-xs">
            Még nem észleltünk ismétlődő havi tranzakciót.
          </div>
        )}
      </div>
    </div>
  );
};
