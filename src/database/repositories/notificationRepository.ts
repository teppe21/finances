import { NotificationEvent, NotificationSourceConfig } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';

export class NotificationRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getSources(): Promise<NotificationSourceConfig[]> {
    return this.db.query<NotificationSourceConfig>('SELECT * FROM notification_sources');
  }

  async updateSourceEnabled(packageName: string, enabled: boolean): Promise<void> {
    const now = new Date().toISOString();
    await this.db.execute(
      'UPDATE notification_sources SET enabled = ?, updatedAt = ? WHERE packageName = ?',
      [enabled ? 1 : 0, now, packageName]
    );
  }

  async addSource(src: NotificationSourceConfig): Promise<void> {
    await this.db.execute(
      `INSERT INTO notification_sources (packageName, displayName, enabled, bankProfileId, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [src.packageName, src.displayName, src.enabled ? 1 : 0, src.bankProfileId, src.createdAt, src.updatedAt]
    );
  }

  async logEvent(evt: NotificationEvent): Promise<void> {
    await this.db.execute(
      `INSERT INTO notification_events (
        id, packageName, applicationLabel, title, text, bigText, subText,
        postedAt, notificationKey, sourceBankProfile, processed, parseStatus,
        transactionId, fingerprint, failureReason
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        evt.id,
        evt.packageName,
        evt.applicationLabel || '',
        evt.title || '',
        evt.text || '',
        evt.bigText || '',
        evt.subText || '',
        evt.postedAt,
        evt.notificationKey || '',
        evt.sourceBankProfile || '',
        evt.processed ? 1 : 0,
        evt.parseStatus,
        evt.transactionId || '',
        evt.fingerprint || '',
        evt.failureReason || '',
      ]
    );
  }

  async getPendingReviewEvents(): Promise<NotificationEvent[]> {
    const all = await this.db.query<NotificationEvent>('SELECT * FROM notification_events ORDER BY postedAt DESC');
    return all.filter((e) => e.parseStatus === 'needs_review' && !e.processed);
  }

  async markEventProcessed(id: string, transactionId?: string): Promise<void> {
    await this.db.execute(
      'UPDATE notification_events SET processed = 1, transactionId = ? WHERE id = ?',
      [transactionId || '', id]
    );
  }
}
