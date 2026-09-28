import React, { useState, useEffect } from 'react';
import { CategoryRepository } from '../../database/repositories/categoryRepository';
import { Category, CategoryRule } from '../../types';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { generateId } from '../../utils/hashing';
import { Settings, Plus, Trash2, Tag, Check, X } from 'lucide-react';

export const CategoriesScreen: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [rules, setRules] = useState<CategoryRule[]>([]);
  const [isAddOpen, setIsAddOpen] = useState(false);

  const [newCatName, setNewCatName] = useState('');
  const [newCatKeywords, setNewCatKeywords] = useState('');

  const repo = new CategoryRepository();

  const loadData = async () => {
    const c = await repo.getAllCategories();
    const r = await repo.getAllRules();
    setCategories(c);
    setRules(r);
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    const catId = `cat_${Date.now()}`;
    const now = new Date().toISOString();

    await repo.createCategory({
      id: catId,
      name: newCatName.trim(),
      createdAt: now,
      updatedAt: now,
    });

    const kwList = newCatKeywords
      .split(',')
      .map((k) => k.trim())
      .filter(Boolean);

    for (let i = 0; i < kwList.length; i++) {
      await repo.createRule({
        id: `rule_${catId}_${i}`,
        categoryId: catId,
        pattern: kwList[i],
        matchType: 'contains',
        priority: 50, // user-created rule has higher priority
        isActive: true,
        createdAt: now,
      });
    }

    setNewCatName('');
    setNewCatKeywords('');
    setIsAddOpen(false);
    await loadData();
  };

  const handleDeleteRule = async (ruleId: string) => {
    await repo.deleteRule(ruleId);
    await loadData();
  };

  return (
    <div className="space-y-4 pb-24 max-w-2xl mx-auto px-4 pt-4">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-lg font-bold text-slate-100">Kategóriák és Szabályok</h2>
          <p className="text-xs text-slate-400">Automatikus besorolási kulcsszavak kezelése</p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setIsAddOpen(true)}
          icon={<Plus className="w-4 h-4" />}
        >
          Új Kategória
        </Button>
      </div>

      <div className="space-y-3">
        {categories.map((cat) => {
          const catRules = rules.filter((r) => r.categoryId === cat.id);

          return (
            <Card key={cat.id} className="p-4 space-y-2.5">
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <span
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: cat.color || '#3B82F6' }}
                  />
                  <h3 className="font-bold text-sm text-slate-100">{cat.name}</h3>
                  {cat.isIncome && <Badge variant="success">Bevétel</Badge>}
                </div>
                <span className="text-[11px] text-slate-400 font-medium">
                  {catRules.length} kulcsszó
                </span>
              </div>

              {catRules.length > 0 ? (
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {catRules.map((rule) => (
                    <span
                      key={rule.id}
                      className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-lg bg-slate-900 border border-slate-700/80 text-[11px] text-slate-300"
                    >
                      <span>{rule.pattern}</span>
                      <button
                        onClick={() => handleDeleteRule(rule.id)}
                        className="text-slate-500 hover:text-rose-400 ml-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  ))}
                </div>
              ) : (
                <span className="text-[11px] text-slate-500 italic block">
                  Nincsenek kulcsszavak rendelve ehhez a kategóriához.
                </span>
              )}
            </Card>
          );
        })}
      </div>

      {/* Add Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-3xl w-full max-w-md p-6 space-y-4 shadow-2xl">
            <div className="flex justify-between items-center border-b border-slate-700 pb-3">
              <h3 className="text-base font-bold text-slate-100">Új Kategória Hozzáadása</h3>
              <button onClick={() => setIsAddOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddCategory} className="space-y-3.5 text-xs sm:text-sm">
              <div>
                <label className="block text-slate-400 mb-1 font-medium">Kategória neve</label>
                <input
                  type="text"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  placeholder="pl. Állatorvos, Hobbi"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-medium">
                  Kulcsszavak (vesszővel elválasztva)
                </label>
                <input
                  type="text"
                  value={newCatKeywords}
                  onChange={(e) => setNewCatKeywords(e.target.value)}
                  placeholder="pl. allat, orvos, fressnapf"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex justify-end gap-2 pt-4 border-t border-slate-700">
                <Button variant="ghost" onClick={() => setIsAddOpen(false)}>
                  Mégse
                </Button>
                <Button type="submit" variant="primary">
                  Hozzáadás
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
