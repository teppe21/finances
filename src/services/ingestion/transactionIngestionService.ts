import {
  Transaction,
  TransactionSource,
  CurrencyCode,
  TransactionDirection,
  IngestionResult,
} from '../../types';
import { normalizeDate } from '../../core/normalization/date';
import { cleanMerchantName } from '../../core/normalization/text';
import { generateTransactionFingerprint } from '../../core/deduplication/fingerprint';
import { checkDuplicate } from '../../core/deduplication/deduplicator';
import { categorizeTransaction } from '../../core/categorization/categorizer';
import { validateTransaction } from '../../core/validation/transactionValidator';
import { TransactionRepository } from '../../database/repositories/transactionRepository';
import { AccountRepository } from '../../database/repositories/accountRepository';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { generateId } from '../../utils/hashing';

export interface IngestionInput {
  accountId?: string;
  suggestedAccountName?: string;
  date: string;
  valueDate?: string;
  amountMinor: number;
  currency: CurrencyCode;
  direction?: TransactionDirection;
  description: string;
  merchant?: string;
  categoryId?: string;
  source: TransactionSource;
  sourceAppPackage?: string;
  externalId?: string;
  receiptId?: string;
  notificationEventId?: string;
  notes?: string;
  confidence?: number;
}

export class TransactionIngestionService {
  constructor(
    private txRepo: TransactionRepository = new TransactionRepository(),
    private accountRepo: AccountRepository = new AccountRepository(),
    private categoryRepo: CategoryRepository = new CategoryRepository()
  ) {}

  /**
   * Unified transaction ingestion method for all input sources.
   */
  async ingest(input: IngestionInput): Promise<IngestionResult> {
    const warnings: string[] = [];

    // 1. Normalization
    const date = normalizeDate(input.date);
    const merchant = input.merchant ? cleanMerchantName(input.merchant) : undefined;
    const description = input.description.trim() || merchant || 'Tranzakció';
    const amountMinor = input.amountMinor;
    const currency = (input.currency || 'HUF').toUpperCase() as CurrencyCode;

    let direction: TransactionDirection = input.direction || (amountMinor >= 0 ? 'income' : 'expense');
    if (amountMinor < 0 && direction === 'income') direction = 'expense';
    if (amountMinor > 0 && direction === 'expense') direction = 'income';

    // 2. Resolve Account (Default to Cash account for receipts, or institution match, or first account)
    let accountId = input.accountId;
    if (!accountId) {
      if (input.source === 'receipt') {
        const cashAcc = await this.accountRepo.getCashAccount();
        accountId = cashAcc?.id;
      } else if (input.suggestedAccountName) {
        const instAcc = await this.accountRepo.findByInstitution(input.suggestedAccountName);
        accountId = instAcc?.id;
      }

      if (!accountId) {
        const allAccs = await this.accountRepo.getAll();
        if (allAccs.length > 0) {
          accountId = allAccs[0].id;
        } else {
          return {
            status: 'rejected',
            warnings: ['Nem található aktív számla a tranzakció mentéséhez.'],
          };
        }
      }
    }

    // 3. Categorization (if not manually specified)
    let categoryId = input.categoryId;
    let categorizationConfidence = input.confidence || 0.85;

    if (!categoryId) {
      const rules = await this.categoryRepo.getAllRules();
      const catResult = categorizeTransaction(description, merchant, direction, rules);
      categoryId = catResult.categoryId;
      categorizationConfidence = Math.min(categorizationConfidence, catResult.confidence);
    }

    // 4. Fingerprinting
    const fingerprint = generateTransactionFingerprint({
      date,
      amountMinor,
      currency,
      description,
      merchant,
      externalId: input.externalId,
    });

    // 5. Cross-Source Deduplication Check
    const existingTransactions = await this.txRepo.getAll();
    const dupCheck = checkDuplicate(
      {
        date,
        amountMinor,
        currency,
        description,
        merchant,
        externalId: input.externalId,
        fingerprint,
      },
      existingTransactions
    );

    if (dupCheck.status === 'exact_duplicate' && dupCheck.matchedTransaction) {
      return {
        status: 'duplicate',
        transactionId: dupCheck.matchedTransaction.id,
        duplicateOf: dupCheck.matchedTransaction.id,
        warnings: [dupCheck.reason || 'Tranzakció duplikáció észlelve.'],
        confidence: dupCheck.confidence,
      };
    }

    if (dupCheck.status === 'possible_duplicate' && dupCheck.matchedTransaction) {
      warnings.push(dupCheck.reason || 'Lehetséges tranzakció duplikáció.');
    }

    // 6. Validation
    const now = new Date().toISOString();
    const tx: Transaction = {
      id: generateId('tx'),
      accountId,
      date,
      valueDate: input.valueDate || now,
      amountMinor,
      currency,
      direction,
      description,
      merchant,
      categoryId,
      source: input.source,
      sourceAppPackage: input.sourceAppPackage,
      externalId: input.externalId,
      fingerprint,
      receiptId: input.receiptId,
      notificationEventId: input.notificationEventId,
      notes: input.notes,
      pending: dupCheck.status === 'possible_duplicate',
      confidence: categorizationConfidence,
      importedAt: now,
      createdAt: now,
      updatedAt: now,
    };

    const validation = validateTransaction(tx);
    if (!validation.isValid) {
      return {
        status: 'rejected',
        warnings: [...warnings, ...validation.errors],
      };
    }

    // 7. Check if needs user review (low confidence)
    if (categorizationConfidence < 0.5 || dupCheck.status === 'possible_duplicate') {
      await this.txRepo.create(tx);
      return {
        status: dupCheck.status === 'possible_duplicate' ? 'possible_duplicate' : 'needs_review',
        transaction: tx,
        transactionId: tx.id,
        duplicateOf: dupCheck.matchedTransaction?.id,
        warnings,
        confidence: categorizationConfidence,
      };
    }

    // 8. Commit to Database
    await this.txRepo.create(tx);

    return {
      status: 'created',
      transaction: tx,
      transactionId: tx.id,
      warnings,
      confidence: categorizationConfidence,
    };
  }
}
