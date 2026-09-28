import { Category, CategoryRule } from '../../types';
import { getDatabase, DatabaseDriver } from '../database';

export class CategoryRepository {
  constructor(private db: DatabaseDriver = getDatabase()) {}

  async getAllCategories(): Promise<Category[]> {
    return this.db.query<Category>('SELECT * FROM categories');
  }

  async getAllRules(): Promise<CategoryRule[]> {
    return this.db.query<CategoryRule>('SELECT * FROM category_rules WHERE isActive = 1 ORDER BY priority DESC');
  }

  async createCategory(cat: Category): Promise<void> {
    await this.db.execute(
      `INSERT INTO categories (id, name, icon, color, isIncome, isDefault, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [cat.id, cat.name, cat.icon || '', cat.color || '', cat.isIncome ? 1 : 0, cat.isDefault ? 1 : 0, cat.createdAt, cat.updatedAt]
    );
  }

  async createRule(rule: CategoryRule): Promise<void> {
    await this.db.execute(
      `INSERT INTO category_rules (id, categoryId, pattern, matchType, priority, isActive, createdAt)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [rule.id, rule.categoryId, rule.pattern, rule.matchType, rule.priority, rule.isActive ? 1 : 0, rule.createdAt]
    );
  }

  async deleteRule(id: string): Promise<void> {
    await this.db.execute('DELETE FROM category_rules WHERE id = ?', [id]);
  }
}
