import { CategoryRule, TransactionDirection } from '../../types';
import { removeDiacritics } from '../normalization/text';
import { DEFAULT_CATEGORY_RULES } from './rules';

export interface CategorizationResult {
  categoryId: string;
  matchedRuleId?: string;
  confidence: number; // 0.0 - 1.0
  source: 'user_rule' | 'keyword_rule' | 'direction_fallback' | 'default_fallback';
}

/**
 * Categorizes a transaction based on description, merchant, direction, and active rules.
 */
export function categorizeTransaction(
  description: string,
  merchant: string | undefined,
  direction: TransactionDirection,
  customRules: CategoryRule[] = DEFAULT_CATEGORY_RULES
): CategorizationResult {
  const combinedText = removeDiacritics(`${merchant || ''} ${description || ''}`);

  // Sort rules by priority descending (user-learned rules have higher priority)
  const activeRules = [...customRules]
    .filter((r) => r.isActive)
    .sort((a, b) => b.priority - a.priority);

  // 1. Check rules
  for (const rule of activeRules) {
    const patternNorm = removeDiacritics(rule.pattern.trim());
    if (!patternNorm) continue;

    let matched = false;
    if (rule.matchType === 'exact') {
      matched = combinedText === patternNorm;
    } else if (rule.matchType === 'regex') {
      try {
        const regex = new RegExp(rule.pattern, 'i');
        matched = regex.test(combinedText);
      } catch {
        matched = false;
      }
    } else {
      // contains
      matched = combinedText.includes(patternNorm);
    }

    if (matched) {
      const isUserRule = rule.priority >= 50;
      return {
        categoryId: rule.categoryId,
        matchedRuleId: rule.id,
        confidence: isUserRule ? 0.98 : 0.85,
        source: isUserRule ? 'user_rule' : 'keyword_rule',
      };
    }
  }

  // 2. Income direction fallback
  if (direction === 'income') {
    return {
      categoryId: 'income',
      confidence: 0.75,
      source: 'direction_fallback',
    };
  }

  // 3. Default fallback
  return {
    categoryId: 'other',
    confidence: 0.2,
    source: 'default_fallback',
  };
}
