import React, { useState, useRef } from 'react';
import { useTranslation } from '../../i18n';
import { useAccountStore } from '../../store/accountStore';
import { useTransactionStore } from '../../store/transactionStore';
import { parseCsvFile, ParsedCsvRow } from '../../core/imports/csvParser';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { formatCurrency } from '../../core/normalization/currency';
import { FileSpreadsheet, Upload, CheckCircle2, AlertCircle, ArrowLeft, RefreshCw } from 'lucide-react';

interface ImportScreenProps {
  onBack: () => void;
  onSuccess: () => void;
}

export const ImportScreen: React.FC<ImportScreenProps> = ({ onBack, onSuccess }) => {
  const { t } = useTranslation();
  const { accounts } = useAccountStore();
  const { addTransaction } = useTransactionStore();

  const [selectedAccountId, setSelectedAccountId] = useState(accounts[0]?.id || 'acc_revolut');
  const [isDragging, setIsDragging] = useState(false);
  const [parsedRows, setParsedRows] = useState<ParsedCsvRow[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [importSummary, setImportSummary] = useState<{
    total: number;
    imported: number;
    duplicates: number;
  } | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleProcessCsvText = (content: string) => {
    try {
      const rows = parseCsvFile(content);
      if (rows.length === 0) {
        alert('Nem sikerült érvényes tranzakció sorokat beolvasni a fájlból.');
        return;
      }
      setParsedRows(rows);
      setImportSummary(null);
    } catch (e: any) {
      alert(`Hiba a CSV feldolgozásakor: ${e.message}`);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt) => {
      const text = evt.target?.result as string;
      handleProcessCsvText(text);
    };
    reader.readAsText(file, 'UTF-8');
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      const reader = new FileReader();
      reader.onload = (evt) => {
        handleProcessCsvText(evt.target?.result as string);
      };
      reader.readAsText(file, 'UTF-8');
    }
  };

  const handleExecuteImport = async () => {
    if (parsedRows.length === 0) return;
    setIsProcessing(true);

    let imported = 0;
    let duplicates = 0;

    for (const row of parsedRows) {
      const result = await addTransaction({
        accountId: selectedAccountId,
        date: row.date,
        amountMinor: row.amountMinor,
        currency: row.currency,
        direction: row.direction,
        description: row.description,
        merchant: row.merchant,
        source: 'csv',
      });

      if (result.status === 'created') {
        imported++;
      } else if (result.status === 'duplicate' || result.status === 'possible_duplicate') {
        duplicates++;
      }
    }

    setIsProcessing(false);
    setImportSummary({
      total: parsedRows.length,
      imported,
      duplicates,
    });
    setParsedRows([]);
  };

  return (
    <div className="space-y-5 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* Top Header */}
      <div>
        <h2 className="text-lg font-bold text-slate-100">{t.dashboard.importStatement}</h2>
        <p className="text-xs text-slate-400">Töltsd fel banki kivonatodat (Revolut, OTP, Erste, stb.)</p>
      </div>

      {/* Target Account Selector */}
      <Card className="space-y-2">
        <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider">
          Célszámla kiválasztása
        </label>
        <select
          value={selectedAccountId}
          onChange={(e) => setSelectedAccountId(e.target.value)}
          className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-sm text-slate-100 focus:outline-none focus:border-blue-500"
        >
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name} ({a.institution} - {a.currency})
            </option>
          ))}
        </select>
      </Card>

      {/* Dropzone */}
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`border-2 border-dashed rounded-3xl p-8 text-center transition-all cursor-pointer ${
          isDragging
            ? 'border-blue-500 bg-blue-500/10'
            : 'border-slate-700 bg-slate-800/40 hover:border-slate-600 hover:bg-slate-800/60'
        }`}
      >
        <input
          type="file"
          accept=".csv,.txt"
          ref={fileInputRef}
          onChange={handleFileUpload}
          className="hidden"
        />

        <div className="flex flex-col items-center gap-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-500/15 text-blue-400 flex items-center justify-center">
            <Upload className="w-7 h-7" />
          </div>
          <div>
            <span className="font-semibold text-sm text-slate-200 block">
              Kattints ide vagy húzd be a CSV fájlt
            </span>
            <span className="text-xs text-slate-400 mt-1 block">
              Támogatott: Revolut, OTP, Erste, Wise, általános banki CSV
            </span>
          </div>
        </div>
      </div>

      {/* Import Result Summary */}
      {importSummary && (
        <Card className="bg-emerald-500/10 border-emerald-500/30 p-5 space-y-3">
          <div className="flex items-center gap-2 text-emerald-400 font-bold text-sm">
            <CheckCircle2 className="w-5 h-5" />
            <span>Sikeres importálás!</span>
          </div>
          <div className="grid grid-cols-3 gap-2 text-center text-xs">
            <div className="p-2 bg-slate-900/60 rounded-xl">
              <span className="text-slate-400 block text-[10px]">Összes tétel</span>
              <strong className="text-slate-100">{importSummary.total} db</strong>
            </div>
            <div className="p-2 bg-slate-900/60 rounded-xl">
              <span className="text-slate-400 block text-[10px]">Importálva</span>
              <strong className="text-emerald-400">{importSummary.imported} db</strong>
            </div>
            <div className="p-2 bg-slate-900/60 rounded-xl">
              <span className="text-slate-400 block text-[10px]">Duplikáció</span>
              <strong className="text-amber-400">{importSummary.duplicates} db</strong>
            </div>
          </div>
          <Button variant="primary" fullWidth onClick={onSuccess}>
            Ugrás a tranzakciókhoz
          </Button>
        </Card>
      )}

      {/* Preview Rows Table */}
      {parsedRows.length > 0 && (
        <Card className="space-y-3">
          <div className="flex justify-between items-center pb-2 border-b border-slate-700/50">
            <span className="text-xs font-bold text-slate-300">
              Beolvasott tételek előnézete ({parsedRows.length} db)
            </span>
            <Button
              variant="success"
              size="sm"
              disabled={isProcessing}
              onClick={handleExecuteImport}
              icon={isProcessing ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Upload className="w-3.5 h-3.5" />}
            >
              {isProcessing ? 'Importálás...' : 'Összes importálása'}
            </Button>
          </div>

          <div className="max-h-72 overflow-y-auto divide-y divide-slate-800/60">
            {parsedRows.slice(0, 15).map((row, idx) => (
              <div key={idx} className="py-2.5 flex justify-between items-center text-xs">
                <div>
                  <span className="font-semibold text-slate-200 block truncate max-w-[200px]">
                    {row.merchant || row.description}
                  </span>
                  <span className="text-slate-400 text-[10px]">{row.date}</span>
                </div>
                <span
                  className={`font-bold ${row.direction === 'income' ? 'text-emerald-400' : 'text-slate-200'}`}
                >
                  {row.direction === 'income' ? '+' : ''}
                  {formatCurrency(row.amountMinor, row.currency)}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
};
