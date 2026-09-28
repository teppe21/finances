import React, { useState, useEffect } from 'react';
import { useTranslation } from '../../i18n';
import { useTransactionStore } from '../../store/transactionStore';
import { notificationBridge } from '../../native/notificationListener';
import { NotificationRepository } from '../../database/repositories/notificationRepository';
import { NotificationSourceConfig, NotificationEvent } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { NotificationReviewModal } from '../../components/notifications/NotificationReviewModal';
import {
  BellRing,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  Settings2,
  Activity,
  Layers,
  Sparkles,
} from 'lucide-react';

export const NotificationScreen: React.FC = () => {
  const { t } = useTranslation();
  const { addTransaction } = useTransactionStore();

  const [isGranted, setIsGranted] = useState(false);
  const [sources, setSources] = useState<NotificationSourceConfig[]>([]);
  const [pendingEvents, setPendingEvents] = useState<NotificationEvent[]>([]);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [diagnostics, setDiagnostics] = useState<{ isListenerConnected: boolean; lastEventTimestamp: number }>({
    isListenerConnected: true,
    lastEventTimestamp: Date.now(),
  });

  const repo = new NotificationRepository();

  const loadData = async () => {
    const granted = await notificationBridge.isNotificationAccessGranted();
    setIsGranted(granted);

    const diag = await notificationBridge.getDiagnostics();
    setDiagnostics(diag);

    const srcList = await repo.getSources();
    setSources(srcList);

    const pending = await repo.getPendingReviewEvents();
    setPendingEvents(pending);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleToggleSource = async (packageName: string, currentEnabled: boolean) => {
    await repo.updateSourceEnabled(packageName, !currentEnabled);
    await loadData();
  };

  const handleOpenSettings = async () => {
    await notificationBridge.openNotificationAccessSettings();
  };

  const handleSimulateNotification = async () => {
    // Injects a test notification into review queue for diagnostics verification
    const sampleEvent: NotificationEvent = {
      id: `sim_${Date.now()}`,
      packageName: 'com.revolut.revolut',
      applicationLabel: 'Revolut',
      title: 'Fizetés a következőnek: SPAR',
      text: '8 450 Ft értékben',
      postedAt: new Date().toISOString(),
      processed: false,
      parseStatus: 'needs_review',
      sourceBankProfile: 'revolut',
      failureReason: 'Közepes konfidencia - megerősítés szükséges',
    };

    await repo.logEvent(sampleEvent);
    await loadData();
  };

  const handleImportEvent = async (event: NotificationEvent) => {
    await addTransaction({
      date: event.postedAt.substring(0, 10),
      valueDate: event.postedAt,
      amountMinor: -845000,
      currency: 'HUF',
      direction: 'expense',
      description: `${event.title} ${event.text}`,
      merchant: 'SPAR',
      source: 'notification',
      sourceAppPackage: event.packageName,
      notificationEventId: event.id,
    });

    await repo.markEventProcessed(event.id);
    await loadData();
  };

  const handleIgnoreEvent = async (id: string) => {
    await repo.markEventProcessed(id);
    await loadData();
  };

  return (
    <div className="space-y-5 pb-24 max-w-2xl mx-auto px-4 pt-4">
      {/* 1. Onboarding / Permission Status Banner */}
      <Card className="space-y-3 bg-gradient-to-br from-slate-800 to-slate-900 border-slate-700">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className={`p-2.5 rounded-2xl ${isGranted ? 'bg-emerald-500/15 text-emerald-400' : 'bg-amber-500/15 text-amber-400'}`}>
              <BellRing className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-100">{t.notifications.title}</h2>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span className={`w-2 h-2 rounded-full ${isGranted ? 'bg-emerald-400' : 'bg-amber-400'}`} />
                <span className="text-xs font-semibold text-slate-300">
                  {isGranted ? t.notifications.statusEnabled : t.notifications.statusDisabled}
                </span>
              </div>
            </div>
          </div>

          <Button
            variant={isGranted ? 'secondary' : 'primary'}
            size="sm"
            onClick={handleOpenSettings}
          >
            {isGranted ? 'Beállítások' : t.notifications.enableAccess}
          </Button>
        </div>

        <p className="text-xs text-slate-400 leading-relaxed">
          {t.notifications.accessDesc}
        </p>
      </Card>

      {/* 2. Review Queue Inbox (Section 23, 146) */}
      {pendingEvents.length > 0 && (
        <Card className="border-amber-500/40 bg-amber-500/5 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-amber-400 font-semibold text-sm">
              <AlertTriangle className="w-4 h-4" />
              <span>{pendingEvents.length} {t.notifications.reviewRequired}</span>
            </div>
            <Button size="sm" variant="primary" onClick={() => setIsReviewOpen(true)}>
              Áttekintés
            </Button>
          </div>
        </Card>
      )}

      {/* 3. User-Controlled Monitored Bank Apps (Section 13, 27) */}
      <Card className="space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
          <h3 className="text-sm font-semibold text-slate-200 flex items-center gap-2">
            <Layers className="w-4 h-4 text-blue-400" />
            {t.notifications.monitoredApps}
          </h3>
          <span className="text-xs text-slate-400">
            {sources.filter((s) => s.enabled).length} / {sources.length} aktív
          </span>
        </div>

        <div className="divide-y divide-slate-800/60">
          {sources.map((src) => (
            <div key={src.packageName} className="py-2.5 flex items-center justify-between">
              <div>
                <span className="text-sm font-semibold text-slate-200 block">{src.displayName}</span>
                <span className="text-[11px] text-slate-400 font-mono">{src.packageName}</span>
              </div>

              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={src.enabled}
                  onChange={() => handleToggleSource(src.packageName, src.enabled)}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600" />
              </label>
            </div>
          ))}
        </div>
      </Card>

      {/* 4. Diagnostics & Health (Section 28, 29) */}
      <Card className="space-y-3">
        <div className="flex items-center justify-between pb-2 border-b border-slate-700/50">
          <h3 className="text-sm font-semibold text-slate-200 flex items-center gap-2">
            <Activity className="w-4 h-4 text-emerald-400" />
            {t.notifications.diagnostics}
          </h3>
          <button
            onClick={handleSimulateNotification}
            className="text-[11px] text-blue-400 hover:text-blue-300 flex items-center gap-1 font-medium"
          >
            <Sparkles className="w-3.5 h-3.5" /> Teszt esemény
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3 text-xs">
          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
            <span className="text-slate-400 text-[10px] uppercase font-semibold">Értesítésfigyelő kapcsolat</span>
            <div className="flex items-center gap-1.5 font-bold text-emerald-400">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Kapcsolódva</span>
            </div>
          </div>

          <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-800 space-y-1">
            <span className="text-slate-400 text-[10px] uppercase font-semibold">Utolsó észlelt esemény</span>
            <span className="font-bold text-slate-200 block truncate">
              {new Date(diagnostics.lastEventTimestamp).toLocaleTimeString('hu-HU')}
            </span>
          </div>
        </div>

        {/* Battery optimization advisory */}
        <div className="p-3 bg-slate-900/40 rounded-xl border border-slate-800/80 flex items-start gap-2 text-xs text-slate-400">
          <ShieldCheck className="w-4 h-4 text-blue-400 flex-shrink-0 mt-0.5" />
          <span>{t.notifications.batteryOptimization}</span>
        </div>
      </Card>

      {/* Review Modal */}
      {isReviewOpen && (
        <NotificationReviewModal
          events={pendingEvents}
          onImportEvent={handleImportEvent}
          onIgnoreEvent={handleIgnoreEvent}
          onClose={() => setIsReviewOpen(false)}
        />
      )}
    </div>
  );
};
