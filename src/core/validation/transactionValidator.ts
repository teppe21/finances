import { Transaction } from '../../types';

export interface ValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
}

export function validateTransaction(transaction: Partial<Transaction>): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  if (!transaction.date || !/^\d{4}-\d{2}-\d{2}$/.test(transaction.date)) {
    errors.push('Érvénytelen dátum formátum (elvárás: YYYY-MM-DD).');
  }

  if (transaction.amountMinor === undefined || isNaN(transaction.amountMinor)) {
    errors.push('Hiányzó vagy érvénytelen tranzakció összeg.');
  } else if (transaction.amountMinor === 0) {
    warnings.push('A tranzakció összege nulla.');
  }

  if (!transaction.currency) {
    errors.push('A pénznem megadása kötelező.');
  }

  if (!transaction.accountId) {
    errors.push('A számla azonosító megadása kötelező.');
  }

  if (!transaction.description && !transaction.merchant) {
    errors.push('A tranzakció leírása vagy partnere kötelező.');
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}
