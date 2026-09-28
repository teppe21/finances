import { describe, it, expect, beforeEach } from 'vitest';
import { MemoryDatabaseDriver, setDatabaseDriver, initializeDatabase } from '../../src/database/database';
import { TransactionRepository } from '../../src/database/repositories/transactionRepository';
import { AccountRepository } from '../../src/database/repositories/accountRepository';
import { CategoryRepository } from '../../src/database/repositories/categoryRepository';
import { TransactionIngestionService } from '../../src/services/ingestion/transactionIngestionService';
import { processNotification, DEFAULT_NOTIFICATION_SOURCES } from '../../src/core/notifications/notificationEngine';
import { parseReceiptText } from '../../src/core/receipts/receiptParser';
import { calculateFinancialStats } from '../../src/core/calculations/financialMath';

describe('End-to-End Transaction Ingestion Pipeline', () => {
  let db: MemoryDatabaseDriver;
  let txRepo: TransactionRepository;
  let accountRepo: AccountRepository;
  let categoryRepo: CategoryRepository;
  let ingestionService: TransactionIngestionService;

  beforeEach(async () => {
    db = new MemoryDatabaseDriver();
    setDatabaseDriver(db);
    await initializeDatabase(db);

    txRepo = new TransactionRepository(db);
    accountRepo = new AccountRepository(db);
    categoryRepo = new CategoryRepository(db);
    ingestionService = new TransactionIngestionService(txRepo, accountRepo, categoryRepo);
  });

  it('successfully ingests a bank notification from Revolut', async () => {
    const rawNotif = {
      packageName: 'com.revolut.revolut',
      applicationLabel: 'Revolut',
      title: 'Fizetés a következőnek: LIDL',
      text: '14 500 Ft értékben',
      postedAt: '2026-03-01T14:32:00Z',
    };

    // 1. Process notification via engine
    const notifResult = processNotification(rawNotif, DEFAULT_NOTIFICATION_SOURCES);
    expect(notifResult.status).toBe('parsed');
    expect(notifResult.transaction).toBeDefined();

    const parsed = notifResult.transaction!;

    // 2. Feed into unified ingestion service
    const ingestResult = await ingestionService.ingest({
      date: parsed.date,
      valueDate: parsed.valueDate,
      amountMinor: parsed.amountMinor,
      currency: parsed.currency,
      direction: parsed.direction,
      description: parsed.description,
      merchant: parsed.merchant,
      source: 'notification',
      suggestedAccountName: parsed.suggestedAccountName,
    });

    expect(ingestResult.status).toBe('created');
    expect(ingestResult.transaction).toBeDefined();
    expect(ingestResult.transaction?.categoryId).toBe('food'); // Auto-categorized as food
    expect(ingestResult.transaction?.amountMinor).toBe(-1450000);

    // 3. Verify in repository
    const stored = await txRepo.getAll();
    expect(stored.length).toBe(1);
    expect(stored[0].merchant).toContain('LIDL');
  });

  it('successfully ingests a cash receipt OCR scan and selects Cash account', async () => {
    const rawReceipt = `
      LIDL MAGYARORSZÁG KFT.
      NYUGTA
      2026.03.27 16:45
      TEJ 2,8% 1L 399 Ft
      KENYÉR 1KG 850 Ft
      ÖSSZESEN: 1 249 Ft
      FIZETENDŐ: 1 249 Ft
      KÉSZPÉNZ: 2 000 Ft
      VISSZAJÁRÓ: 751 Ft
    `;

    // 1. Parse receipt OCR text
    const ocrResult = parseReceiptText(rawReceipt);
    expect(ocrResult.paymentMethod).toBe('cash');
    expect(ocrResult.totalMinor).toBe(124900);
    expect(ocrResult.merchant).toBe('Lidl');

    // 2. Feed into ingestion service
    const ingestResult = await ingestionService.ingest({
      date: ocrResult.date!,
      amountMinor: -ocrResult.totalMinor!,
      currency: ocrResult.currency,
      direction: 'expense',
      description: 'Lidl bevásárlás készpénz',
      merchant: ocrResult.merchant,
      source: 'receipt',
    });

    expect(ingestResult.status).toBe('created');
    const cashAcc = await accountRepo.getCashAccount();
    expect(ingestResult.transaction?.accountId).toBe(cashAcc?.id);
    expect(ingestResult.transaction?.categoryId).toBe('food');
  });

  it('prevents cross-source duplicate when CSV is imported after bank notification', async () => {
    // Step 1: Bank notification arrives and is ingested
    await ingestionService.ingest({
      date: '2026-03-01',
      amountMinor: -1450000,
      currency: 'HUF',
      direction: 'expense',
      description: 'Lidl Debrecen Kishegyesi út',
      merchant: 'Lidl',
      source: 'notification',
    });

    // Step 2: Same purchase arrives later via CSV bank statement
    const dupResult = await ingestionService.ingest({
      date: '2026-03-01',
      amountMinor: -1450000,
      currency: 'HUF',
      direction: 'expense',
      description: 'LIDL MAGYARORSZAG KFT',
      merchant: 'Lidl',
      source: 'csv',
    });

    expect(dupResult.status).toBe('duplicate');
    const all = await txRepo.getAll();
    expect(all.length).toBe(1); // Still only 1 transaction, duplicate prevented!
  });

  it('updates dashboard statistics accurately across multiple sources', async () => {
    // 1. Notification purchase
    await ingestionService.ingest({
      date: '2026-03-01',
      amountMinor: -1450000, // -14,500 HUF
      currency: 'HUF',
      direction: 'expense',
      description: 'Lidl élelmiszer',
      source: 'notification',
    });

    // 2. Receipt cash purchase
    await ingestionService.ingest({
      date: '2026-03-02',
      amountMinor: -890000, // -8,900 HUF
      currency: 'HUF',
      direction: 'expense',
      description: 'Éttermi vacsora',
      source: 'receipt',
    });

    // 3. Salary via bank CSV
    await ingestionService.ingest({
      date: '2026-03-03',
      amountMinor: 65000000, // +650,000 HUF
      currency: 'HUF',
      direction: 'income',
      description: 'Fizetés utalás',
      source: 'csv',
    });

    const all = await txRepo.getAll();
    const stats = calculateFinancialStats(all);

    expect(stats.transactionCount).toBe(3);
    expect(stats.incomeMinor).toBe(65000000);
    expect(stats.expenseMinor).toBe(2340000); // 14,500 + 8,900 = 23,400 HUF = 2,340,000 minor
    expect(stats.balanceMinor).toBe(65000000 - 2340000);
  });
});
