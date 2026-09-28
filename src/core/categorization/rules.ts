import { Category, CategoryRule } from '../../types';

export const DEFAULT_CATEGORIES: Category[] = [
  { id: 'food', name: 'Élelmiszer', icon: 'shopping-cart', color: '#10B981', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'dining', name: 'Étkezés / Vendéglátás', icon: 'utensils', color: '#F59E0B', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'transport', name: 'Tankolás / Közlekedés', icon: 'car', color: '#EF4444', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'subscriptions', name: 'Előfizetések / Játék', icon: 'gamepad-2', color: '#0284C7', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'housing', name: 'Rezsi / Szolgáltatás', icon: 'home', color: '#3B82F6', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'entertainment', name: 'Szórakozás / Szabadidő', icon: 'film', color: '#D97706', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'savings', name: 'Utalás / Megtakarítás', icon: 'piggy-bank', color: '#06B6D4', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'income', name: 'Bevétel', icon: 'wallet', color: '#10B981', isIncome: true, isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'health', name: 'Egészség / Gyógyszertár', icon: 'heart-pulse', color: '#14B8A6', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'shopping', name: 'Bevásárlás / Ruházat', icon: 'shopping-bag', color: '#F97316', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'other', name: 'Egyéb / Ismeretlen', icon: 'help-circle', color: '#6B7280', isDefault: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
];

export const DEFAULT_CATEGORY_RULES: CategoryRule[] = [
  // Élelmiszer
  ...['lidl', 'spar', 'interspar', 'aldi', 'tesco', 'auchan', 'penny', 'coop', 'cba', 'pékség', 'pekseg', 'lipóti', 'lipoti', 'pék', 'príma', 'prima', 'groceries'].map((pattern, idx) => ({
    id: `rule_food_${idx}`,
    categoryId: 'food',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Étkezés / Vendéglátás
  ...['wolt', 'foodora', 'mcdonalds', 'kfc', 'burger king', 'starbucks', 'étterem', 'etterem', 'kávézó', 'kavezo', 'roastery', 'coffee', 'büfé', 'bufe', 'pipi', 'pizza', 'pizzéria', 'bistro', 'restaurant', 'subway'].map((pattern, idx) => ({
    id: `rule_dining_${idx}`,
    categoryId: 'dining',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Tankolás / Közlekedés
  ...['mol', 'shell', 'omv', 'orlen', 'lukoil', 'bkk', 'mav', 'máv', 'autópálya', 'autopalya', 'parkolás', 'parkolas', 'uber', 'bolt', 'tankolás', 'benzinkút'].map((pattern, idx) => ({
    id: `rule_transport_${idx}`,
    categoryId: 'transport',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Előfizetések / Játék
  ...['netflix', 'spotify', 'google', 'apple', 'youtube', 'patreon', 'steam', 'riot', 'league', 'epic games', 'playstation', 'xbox', 'disney', 'hbo', 'max', 'chatgpt', 'openai', 'github'].map((pattern, idx) => ({
    id: `rule_subscriptions_${idx}`,
    categoryId: 'subscriptions',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Rezsi / Szolgáltatás
  ...['e.on', 'mvp', 'mvm', 'telekom', 'yettel', 'vodafone', 'díjnet', 'dijnet', 'biztosító', 'biztositas', 'lakbér', 'közös költség', 'kozos koltseg', 'főtáv', 'fotav'].map((pattern, idx) => ({
    id: `rule_housing_${idx}`,
    categoryId: 'housing',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Szórakozás / Szabadidő
  ...['állatkert', 'allatkert', 'vidámpark', 'vidampark', 'strand', 'mozi', 'cinema', 'színház', 'szinhaz', 'feszti', 'aliexpress', 'amazon', 'shein', 'temu'].map((pattern, idx) => ({
    id: `rule_entertainment_${idx}`,
    categoryId: 'entertainment',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Utalás / Megtakarítás
  ...['transfer', 'utalás', 'utalas', 'államkincstár', 'allamkincstar', 'kincstár', 'megtakarítás', 'securities', 'invest', 'trading', 'lightyear', 'etoro'].map((pattern, idx) => ({
    id: `rule_savings_${idx}`,
    categoryId: 'savings',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Bevétel
  ...['fizetés', 'fizetes', 'pénzvisszatérítés', 'penzvisszaterites', 'deposit', 'bér', 'ber', 'salaries', 'salary', 'payroll', 'munkabér'].map((pattern, idx) => ({
    id: `rule_income_${idx}`,
    categoryId: 'income',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),

  // Egészség
  ...['gyógyszertár', 'gyogyszertar', 'patika', 'benu', 'dm drogerie', 'rossmann', 'orvos', 'fogorvos', 'klinika', 'optika'].map((pattern, idx) => ({
    id: `rule_health_${idx}`,
    categoryId: 'health',
    pattern,
    matchType: 'contains' as const,
    priority: 10,
    isActive: true,
    createdAt: '2026-01-01T00:00:00Z',
  })),
];
