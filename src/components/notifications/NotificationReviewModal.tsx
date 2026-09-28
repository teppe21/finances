import React from 'react';
import { NotificationEvent } from '../../types';
import { Button } from '../common/Button';
import { Badge } from '../common/Badge';
import { useTranslation } from '../../i18n';
import { BellRing, Check, X, ShieldAlert } from 'lucide-react';

interface NotificationReviewModalProps {
  events: NotificationEvent[];
  onImportEvent: (event: NotificationEvent) => void;
  onIgnoreEvent: (id: string) => void;
  onClose: () => void;
}

export const NotificationReviewModal: React.FC<NotificationReviewModalProps> = ({
  events,
  onImportEvent,
  onIgnoreEvent,
  onClose,
}) => {
  const { t } = useTranslation();

  return (
    <div className="fixed inset-0 bg-slate-950/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-lg p-5 sm:p-6 space-y-4 shadow-2xl max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center border-b border-slate-700 pb-3">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-blue-500/15 text-blue-400 rounded-xl">
              <BellRing className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-100">
                {t.notifications.reviewRequired}
              </h3>
              <p className="text-[11px] text-slate-400">
                {events.length} {t.notifications.detectedCount}
              </p>
            </div>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200 p-1">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 space-y-3 pr-1">
          {events.length > 0 ? (
            events.map((evt) => (
              <div
                key={evt.id}
                className="bg-slate-900/60 border border-slate-700/60 p-4 rounded-2xl space-y-3"
              >
                <div className="flex justify-between items-start">
                  <div>
                    <Badge variant="primary">{evt.applicationLabel || evt.packageName}</Badge>
                    <h4 className="font-semibold text-sm text-slate-100 mt-1.5">
                      {evt.title || 'Banki Értesítés'}
                    </h4>
                    <p className="text-xs text-slate-300 mt-0.5">{evt.text}</p>
                  </div>
                  <span className="text-[10px] text-slate-500">{evt.postedAt.substring(11, 16)}</span>
                </div>

                {evt.failureReason && (
                  <div className="flex items-center gap-1.5 text-[11px] text-amber-400">
                    <ShieldAlert className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>{evt.failureReason}</span>
                  </div>
                )}

                <div className="flex justify-end gap-2 pt-2 border-t border-slate-800">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onIgnoreEvent(evt.id)}
                    icon={<X className="w-3.5 h-3.5" />}
                  >
                    {t.notifications.ignoreButton}
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => onImportEvent(evt)}
                    icon={<Check className="w-3.5 h-3.5" />}
                  >
                    {t.notifications.importButton}
                  </Button>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-10 text-slate-500 text-xs">
              Nincs felülvizsgálatra váró értesítés.
            </div>
          )}
        </div>

        <div className="flex justify-end pt-3 border-t border-slate-700">
          <Button variant="secondary" onClick={onClose}>
            {t.common.close}
          </Button>
        </div>
      </div>
    </div>
  );
};
