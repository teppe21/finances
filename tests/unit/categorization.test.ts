import { describe, it, expect } from 'vitest';
import { categorizeTransaction } from '../../src/core/categorization/categorizer';
import { DEFAULT_CATEGORY_RULES } from '../../src/core/categorization/rules';

describe('Multi-Tier Categorization Engine', () => {
  it('categorizes known merchants correctly with accents normalized', () => {
    // "MOL tankolás" -> transport
    const mol = categorizeTransaction('MOL tankolás', 'MOL', 'expense', DEFAULT_CATEGORY_RULES);
    expect(mol.categoryId).toBe('transport');

    // "Péksütemény vásárlás" -> food
    const pekseg = categorizeTransaction('Péksütemény vásárlás', undefined, 'expense', DEFAULT_CATEGORY_RULES);
    expect(pekseg.categoryId).toBe('food');

    // "Netflix előfizetés" -> subscriptions
    const netflix = categorizeTransaction('Netflix előfizetés', 'Netflix', 'expense', DEFAULT_CATEGORY_RULES);
    expect(netflix.categoryId).toBe('subscriptions');

    // "Wolt ebéd" -> dining
    const wolt = categorizeTransaction('Wolt ebéd', 'Wolt', 'expense', DEFAULT_CATEGORY_RULES);
    expect(wolt.categoryId).toBe('dining');
  });

  it('respects higher priority user-learned rules over generic rules', () => {
    // Custom user rule: "MOL" -> "car_maintenance" with priority 100
    const customRules = [
      ...DEFAULT_CATEGORY_RULES,
      {
        id: 'user_rule_mol',
        categoryId: 'car_maintenance',
        pattern: 'mol',
        matchType: 'contains' as const,
        priority: 100, // higher priority
        isActive: true,
        createdAt: '2026-03-01T00:00:00Z',
      },
    ];

    const result = categorizeTransaction('MOL tankolás', 'MOL', 'expense', customRules);
    expect(result.categoryId).toBe('car_maintenance');
    expect(result.source).toBe('user_rule');
  });

  it('assigns income category when direction is income', () => {
    const res = categorizeTransaction('Kliens jóváírás', undefined, 'income', DEFAULT_CATEGORY_RULES);
    expect(res.categoryId).toBe('income');
  });

  it('falls back to other when no rule matches', () => {
    const res = categorizeTransaction('Ismeretlen tétel xyz99', undefined, 'expense', DEFAULT_CATEGORY_RULES);
    expect(res.categoryId).toBe('other');
  });
});
