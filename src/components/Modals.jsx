import { Check, Plus, Trash2, X } from 'lucide-react';
import { useState } from 'react';

// TRANZAKCIÓ SZERKESZTŐ MODÁL
export function EditTransactionModal({ transaction, categories, onSave, onClose }) {
  const [formData, setFormData] = useState({ ...transaction });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({
      ...formData,
      amount: parseFloat(formData.amount)
    });
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-md p-6 space-y-4 shadow-2xl">
        <div className="flex justify-between items-center border-b border-slate-700 pb-3">
          <h3 className="text-lg font-semibold text-slate-100">Tranzakció Szerkesztése</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200"><X className="w-5 h-5" /></button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-sm">
          <div>
            <label className="block text-slate-400 mb-1">Leírás</label>
            <input 
              type="text" 
              value={formData.description} 
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              required 
            />
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Összeg (HUF)</label>
            <input 
              type="number" 
              value={formData.amount} 
              onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              required 
            />
            <span className="text-xs text-slate-500 mt-1 block">Pozitív = Bevétel, Negatív = Kiadás</span>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Kategória</label>
            <select 
              value={formData.category} 
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
            >
              {categories.map(c => (
                <option key={c.name} value={c.name}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Dátum</label>
            <input 
              type="date" 
              value={formData.date} 
              onChange={(e) => setFormData({ ...formData, date: e.target.value })}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              required 
            />
          </div>

          <div>
            <label className="block text-slate-400 mb-1">Forrás / Számla</label>
            <input 
              type="text" 
              value={formData.source || ''} 
              onChange={(e) => setFormData({ ...formData, source: e.target.value })}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-slate-100 focus:outline-none focus:border-blue-500"
              placeholder="pl. Revolut, OTP"
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-slate-700">
            <button type="button" onClick={onClose} className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-slate-300">Mégse</button>
            <button type="submit" className="px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg text-white font-medium">Mentés</button>
          </div>
        </form>
      </div>
    </div>
  );
}

// KATEGÓRIA KEZELŐ MODÁL
export function CategoryManagerModal({ categories, onSaveCategories, onClose }) {
  const [categoryList, setCategoryList] = useState([...categories]);
  const [newCatName, setNewCatName] = useState('');
  const [newCatKeywords, setNewCatKeywords] = useState('');

  const handleAddCategory = (e) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    const newCat = {
      name: newCatName.trim(),
      keywords: newCatKeywords.split(',').map(k => k.trim()).filter(Boolean)
    };

    setCategoryList([...categoryList, newCat]);
    setNewCatName('');
    setNewCatKeywords('');
  };

  const handleDeleteCategory = (name) => {
    setCategoryList(categoryList.filter(c => c.name !== name));
  };

  const handleSave = () => {
    onSaveCategories(categoryList);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-xl w-full max-w-2xl p-6 space-y-6 shadow-2xl max-h-[90vh] flex flex-col">
        <div className="flex justify-between items-center border-b border-slate-700 pb-3">
          <h3 className="text-lg font-semibold text-slate-100">Kategóriák és Szabályok Kezelése</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-200"><X className="w-5 h-5" /></button>
        </div>

        {/* ÚJ KATEGÓRIA FELVÉTELE */}
        <form onSubmit={handleAddCategory} className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/60 space-y-3">
          <h4 className="text-sm font-medium text-blue-400 flex items-center gap-1"><Plus className="w-4 h-4"/> Új Kategória Hozzáadása</h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
            <input 
              type="text" 
              placeholder="Kategória neve (pl. Egészség)" 
              value={newCatName}
              onChange={(e) => setNewCatName(e.target.value)}
              className="bg-slate-900 border border-slate-700 rounded-lg p-2 text-slate-200 focus:outline-none focus:border-blue-500"
            />
            <input 
              type="text" 
              placeholder="Kulcsszavak (vesszővel elválasztva)" 
              value={newCatKeywords}
              onChange={(e) => setNewCatKeywords(e.target.value)}
              className="bg-slate-900 border border-slate-700 rounded-lg p-2 text-slate-200 focus:outline-none focus:border-blue-500"
            />
          </div>
          <button type="submit" className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-1.5 rounded-lg text-sm font-medium transition-all">Hozzáadás</button>
        </form>

        {/* KATEGÓRIÁK LISTÁJA */}
        <div className="overflow-y-auto flex-1 space-y-3 pr-2">
          {categoryList.map((cat) => (
            <div key={cat.name} className="flex justify-between items-start bg-slate-900/40 border border-slate-700/40 p-3 rounded-lg text-sm">
              <div className="space-y-1">
                <span className="font-semibold text-slate-200 block">{cat.name}</span>
                <p className="text-xs text-slate-400">
                  Kulcsszavak: {cat.keywords && cat.keywords.length > 0 ? cat.keywords.join(', ') : 'Nincsenek kulcsszavak'}
                </p>
              </div>
              <button 
                onClick={() => handleDeleteCategory(cat.name)}
                className="text-rose-400 hover:text-rose-300 p-1"
                title="Kategória törlése"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>

        <div className="flex justify-end gap-3 pt-4 border-t border-slate-700">
          <button onClick={onClose} className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-slate-300 text-sm">Mégse</button>
          <button onClick={handleSave} className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 rounded-lg text-white font-medium text-sm flex items-center gap-2">
            <Check className="w-4 h-4" /> Változtatások Mentése
          </button>
        </div>
      </div>
    </div>
  );
}