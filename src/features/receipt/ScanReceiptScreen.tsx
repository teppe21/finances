import React, { useState, useRef } from 'react';
import { useTransactionStore } from '../../store/transactionStore';
import { useAccountStore } from '../../store/accountStore';
import { useTranslation } from '../../i18n';
import { ocrBridge } from '../../native/ocr';
import { parseReceiptText, ParsedReceiptResult } from '../../core/receipts/receiptParser';
import { ReceiptReviewModal } from '../../components/receipt/ReceiptReviewModal';
import { Button } from '../../components/common/Button';
import { ReceiptRepository } from '../../database/repositories/receiptRepository';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { Camera, Image as ImageIcon, Zap, ZapOff, ArrowLeft, RefreshCw, Sparkles } from 'lucide-react';
import { generateId } from '../../utils/hashing';

interface ScanReceiptScreenProps {
  onBack: () => void;
  onSuccess: () => void;
}

export const ScanReceiptScreen: React.FC<ScanReceiptScreenProps> = ({ onBack, onSuccess }) => {
  const { t } = useTranslation();
  const { addTransaction } = useTransactionStore();
  const { accounts } = useAccountStore();
  const [categories, setCategories] = useState<any[]>([]);

  const [torch, setTorch] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [parsedResult, setParsedResult] = useState<ParsedReceiptResult | null>(null);
  const [capturedUri, setCapturedUri] = useState<string | undefined>(undefined);

  const fileInputRef = useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    new CategoryRepository().getAllCategories().then(setCategories);
  }, []);

  const handleCaptureSimulation = async (sampleText?: string) => {
    setIsProcessing(true);
    try {
      let ocrText = sampleText;
      if (!ocrText) {
        // Run ML Kit OCR via bridge
        const res = await ocrBridge.recognizeTextFromUri('simulated_receipt.jpg');
        ocrText = res.text;
      }

      const parsed = parseReceiptText(ocrText);
      setParsedResult(parsed);
      setCapturedUri('simulated_receipt.jpg');
    } catch (e: any) {
      alert(`OCR hiba: ${e.message}`);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    const reader = new FileReader();
    reader.onload = async () => {
      // In web/preview, simulate text or parse mock
      await handleCaptureSimulation();
    };
    reader.readAsDataURL(file);
  };

  const handleConfirmSave = async (data: {
    merchant: string;
    totalMinor: number;
    date: string;
    categoryId: string;
    accountId: string;
    paymentMethod: 'cash' | 'card' | 'unknown';
    deleteImageAfterSave: boolean;
  }) => {
    try {
      const receiptId = generateId('rcpt');

      // 1. Save receipt scan metadata to SQLite
      const receiptRepo = new ReceiptRepository();
      await receiptRepo.create({
        id: receiptId,
        imageUri: data.deleteImageAfterSave ? undefined : capturedUri,
        scannedAt: new Date().toISOString(),
        merchant: data.merchant,
        date: data.date,
        totalMinor: data.totalMinor,
        currency: 'HUF',
        paymentMethod: data.paymentMethod,
        items: parsedResult?.items || [],
        rawOcrAvailable: true,
        rawOcrText: parsedResult?.rawText || '',
        confidence: parsedResult?.confidence || 0.85,
        processedLocally: true,
      });

      // 2. Ingest transaction linked to account (Cash or selected bank)
      await addTransaction({
        accountId: data.accountId,
        date: data.date,
        amountMinor: -Math.abs(data.totalMinor),
        currency: 'HUF',
        direction: 'expense',
        description: `${data.merchant} (Nyugta)`,
        merchant: data.merchant,
        categoryId: data.categoryId,
        source: 'receipt',
        receiptId,
      });

      onSuccess();
    } catch (e: any) {
      alert(`Hiba a mentés során: ${e.message}`);
    }
  };

  return (
    <div
      className="fixed inset-0 bg-slate-950 text-white z-50 flex flex-col justify-between p-4"
      style={{
        paddingTop: 'calc(1rem + env(safe-area-inset-top, 0px))',
        paddingBottom: 'calc(1.5rem + env(safe-area-inset-bottom, 0px))',
      }}
    >
      {/* Top Bar */}
      <div className="flex justify-between items-center z-10">
        <button
          onClick={onBack}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900/90 backdrop-blur-md border border-slate-700 text-slate-100 text-xs sm:text-sm font-semibold active:scale-95 transition-all shadow-sm"
          title="Vissza"
        >
          <ArrowLeft className="w-4 h-4 text-slate-200" />
          <span>Vissza</span>
        </button>

        <span className="font-bold text-sm tracking-wide bg-slate-900/80 px-4 py-1.5 rounded-full border border-slate-700/60">
          {t.receipts.title}
        </span>

        <button
          onClick={() => setTorch(!torch)}
          className={`p-2.5 rounded-full backdrop-blur-md border transition-colors ${
            torch
              ? 'bg-amber-500/20 text-amber-400 border-amber-500/50'
              : 'bg-slate-900/80 text-slate-300 border-slate-700/60'
          }`}
        >
          {torch ? <Zap className="w-5 h-5" /> : <ZapOff className="w-5 h-5" />}
        </button>
      </div>

      {/* Camera Viewfinder View */}
      <div className="flex-1 flex flex-col items-center justify-center relative my-4">
        {/* Receipt Framing Guide */}
        <div className="w-72 sm:w-80 h-[380px] sm:h-[420px] rounded-3xl border-2 border-dashed border-slate-500/80 relative flex flex-col justify-between p-4 bg-slate-900/40 backdrop-blur-[2px] shadow-2xl overflow-hidden">
          {/* Corner Guides */}
          <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-blue-400 rounded-tl-xl" />
          <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-blue-400 rounded-tr-xl" />
          <div className="absolute bottom-0 left-0 w-8 h-8 border-b-4 border-l-4 border-blue-400 rounded-bl-xl" />
          <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-blue-400 rounded-br-xl" />

          {/* Scanning Animation line */}
          <div className="w-full h-0.5 bg-gradient-to-r from-transparent via-blue-400 to-transparent animate-pulse my-auto opacity-75" />

          <div className="text-center bg-slate-950/70 py-1.5 px-3 rounded-full mx-auto text-[11px] font-medium text-slate-300 backdrop-blur-md border border-slate-800">
            {t.receipts.alignFrame}
          </div>
        </div>

        {isProcessing && (
          <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm flex flex-col items-center justify-center gap-3 rounded-3xl">
            <RefreshCw className="w-8 h-8 text-blue-400 animate-spin" />
            <span className="text-xs font-semibold text-slate-200">
              Optikai szövegfelismerés (ML Kit OCR) folyamatban...
            </span>
          </div>
        )}
      </div>

      {/* Bottom Controls */}
      <div className="flex items-center justify-around pb-6 max-w-sm mx-auto w-full z-10">
        {/* Gallery upload */}
        <input
          type="file"
          accept="image/*"
          ref={fileInputRef}
          onChange={handleFileUpload}
          className="hidden"
        />
        <button
          onClick={() => fileInputRef.current?.click()}
          className="p-3.5 rounded-full bg-slate-900/80 border border-slate-700/60 text-slate-300 hover:text-white"
          title="Galéria tallózása"
        >
          <ImageIcon className="w-6 h-6" />
        </button>

        {/* Capture Shutter Button */}
        <button
          disabled={isProcessing}
          onClick={() => handleCaptureSimulation()}
          className="w-18 h-18 sm:w-20 sm:h-20 rounded-full border-4 border-slate-400/50 p-1 flex items-center justify-center transition-all active:scale-90"
        >
          <div className="w-full h-full rounded-full bg-blue-600 hover:bg-blue-500 shadow-lg shadow-blue-900/40 flex items-center justify-center">
            <Camera className="w-7 h-7 text-white" />
          </div>
        </button>

        {/* Preset Hungarian Receipt Test Trigger */}
        <button
          onClick={() =>
            handleCaptureSimulation(
              'LIDL MAGYARORSZÁG KFT.\n2026.03.27 16:45\nPILOS TEJ 399 Ft\nKENYÉR 850 Ft\nFIZETENDŐ: 1 249 Ft\nKÉSZPÉNZ: 2000 Ft\nVISSZAJÁRÓ: 751 Ft'
            )
          }
          className="p-3.5 rounded-full bg-slate-900/80 border border-slate-700/60 text-blue-400 hover:text-blue-300"
          title="Demó Nyugta OCR"
        >
          <Sparkles className="w-6 h-6" />
        </button>
      </div>

      {/* Review Modal */}
      {parsedResult && (
        <ReceiptReviewModal
          receipt={parsedResult}
          imageUri={capturedUri}
          accounts={accounts}
          categories={categories}
          onConfirm={handleConfirmSave}
          onRetake={() => {
            setParsedResult(null);
            setCapturedUri(undefined);
          }}
          onClose={() => {
            setParsedResult(null);
            setCapturedUri(undefined);
          }}
        />
      )}
    </div>
  );
};
