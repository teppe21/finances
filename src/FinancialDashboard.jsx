import {
    Check, Download,
    Edit2,
    FileText,
    FolderOpen, LayoutDashboard,
    Plus,
    RefreshCw,
    Repeat,
    Save, Search,
    Settings,
    Trash2, Upload,
    X
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
    Bar, BarChart, Cell, Legend, Pie, PieChart, ResponsiveContainer,
    Tooltip, XAxis, YAxis
} from 'recharts';

// --- 1. ALAPÉRTELMEZETT KATEGÓRIÁK ---
const DEFAULT_CATEGORIES = [
  {
    name: 'Élelmiszer',
    keywords: ['lidl', 'spar', 'aldi', 'tesco', 'auchan', 'penny', 'coop', 'pékség', 'pekseg', 'lipóti', 'pék']
  },
  {
    name: 'Étkezés / Vendéglátás',
    keywords: ['wolt', 'foodora', 'mcdonalds', 'kfc', 'starbucks', 'uber', 'bolt', 'étterem', 'kávézó', 'roastery', 'coffee', 'büfé', 'bufe', 'pipi', 'pizz']
  },
  {
    name: 'Tankolás / Közlekedés',
    keywords: ['mol', 'shell', 'omv', 'bkk', 'mav', 'máv', 'autópálya', 'parkolás']
  },
  {
    name: 'Előfizetések / Játék',
    keywords: ['netflix', 'spotify', 'google', 'apple', 'youtube', 'patreon', 'steam', 'riot', 'league', 'epic games', 'playstation', 'xbox']
  },
  {
    name: 'Rezsi / Szolgáltatás',
    keywords: ['e.on', 'mvp', 'mvm', 'telekom', 'yettel', 'vodafone', 'díj', 'dij', 'biztosító']
  },
  {
    name: 'Szórakozás / Szabadidő',
    keywords: ['állatkert', 'allatkert', 'vidámpark', 'vidampark', 'strand', 'mozi', 'színház', 'feszti', 'aliexpress', 'amazon', 'shein']
  },
  {
    name: 'Utalás / Megtakarítás',
    keywords: ['transfer', 'utalás', 'utalas', 'államkincstár', 'allamkincstar', 'kincstár', 'megtakarítás', 'securities']
  },
  {
    name: 'Bevétel',
    keywords: ['fizetés', 'fizetes', 'pénzvisszatérítés', 'deposit', 'bér', 'salaries', 'salary']
  }
];

// --- 2. LOGIKAI SEGÉDFÜGGVÉNYEK ---
const categorizeTransaction = (description, amount, customCategories = DEFAULT_CATEGORIES) => {
  const desc = (description || '').toLowerCase();

  if (amount > 0) {
    const matchedCategory = customCategories.find(c => 
      c.keywords && c.keywords.some(kw => desc.includes(kw.toLowerCase()))
    );
    return matchedCategory ? matchedCategory.name : 'Bevétel';
  }

  for (const catObj of customCategories) {
    if (catObj.keywords && catObj.keywords.some(keyword => desc.includes(keyword.toLowerCase()))) {
      return catObj.name;
    }
  }

  return 'Egyéb / Ismeretlen';
};

const detectRecurringTransactions = (transactions) => {
  if (!transactions || transactions.length < 2) return new Set();

  const recurringIds = new Set();
  const groups = {};

  transactions.forEach(t => {
    const key = t.description.trim().toLowerCase();
    if (!groups[key]) groups[key] = [];
    groups[key].push(t);
  });

  Object.values(groups).forEach(items => {
    if (items.length < 2) return;
    items.sort((a, b) => new Date(a.date) - new Date(b.date));

    for (let i = 0; i < items.length - 1; i++) {
      for (let j = i + 1; j < items.length; j++) {
        const d1 = new Date(items[i].date);
        const d2 = new Date(items[j].date);
        const monthDiff = (d2.getFullYear() - d1.getFullYear()) * 12 + (d2.getMonth() - d1.getMonth());
        
        if (monthDiff >= 1 && monthDiff <= 2) {
          const dayDiff = Math.abs(d1.getDate() - d2.getDate());
          const amount1 = Math.abs(items[i].amount);
          const amount2 = Math.abs(items[j].amount);
          const maxAmount = Math.max(amount1, amount2);
          const amountDiffRatio = maxAmount === 0 ? 0 : Math.abs(amount1 - amount2) / maxAmount;

          if (dayDiff <= 3 && amountDiffRatio <= 0.10) {
            recurringIds.add(items[i].id);
            recurringIds.add(items[j].id);
          }
        }
      }
    }
  });

  return recurringIds;
};

const parseCSV = (text, sourceName = 'Alapértelmezett', customCategories) => {
  const lines = text.split(/\r\n|\n/).filter(line => line.trim() !== '');
  if (lines.length < 2) return [];

  const delimiter = lines[0].includes(';') ? ';' : ',';
  const headers = lines[0].split(delimiter).map(h => h.trim().replace(/^["']|["']$/g, '').toLowerCase());

  const dateIdx = headers.findIndex(h => h.includes('dátum') || h.includes('date') || h.includes('started date'));
  const descIdx = headers.findIndex(h => h.includes('leírás') || h.includes('description') || h.includes('partner') || h.includes('név') || h.includes('megjegyzés'));
  const amountIdx = headers.findIndex(h => h.includes('összeg') || h.includes('amount') || h.includes('érték'));

  const parsedData = [];

  for (let i = 1; i < lines.length; i++) {
    const row = lines[i].split(delimiter).map(cell => cell.trim().replace(/^["']|["']$/g, ''));
    if (row.length < headers.length) continue;

    const rawDate = dateIdx !== -1 ? row[dateIdx] : new Date().toISOString().split('T')[0];
    const rawDesc = descIdx !== -1 ? row[descIdx] : 'Ismeretlen tranzakció';
    const rawAmount = amountIdx !== -1 ? row[amountIdx] : '0';

    let cleanStr = rawAmount.replace(/\s+/g, '').replace(/Ft|HUF/gi, '');
    if (cleanStr.includes(',') && cleanStr.includes('.')) {
      cleanStr = cleanStr.replace(/\./g, '').replace(',', '.');
    } else if (cleanStr.includes(',')) {
      cleanStr = cleanStr.replace(',', '.');
    }
    
    let parsedAmount = parseFloat(cleanStr);
    if (isNaN(parsedAmount)) parsedAmount = 0;

    parsedData.push({
      id: `csv-${i}-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      date: rawDate.substring(0, 10),
      description: rawDesc,
      amount: parsedAmount,
      category: categorizeTransaction(rawDesc, parsedAmount, customCategories),
      source: sourceName
    });
  }

  return parsedData;
};

const exportToCSV = (transactions) => {
  if (!transactions || transactions.length === 0) {
    alert("Nincs exportálható adat!");
    return;
  }

  const headers = ['Dátum', 'Leírás', 'Összeg', 'Kategória', 'Forrás'];
  const rows = transactions.map(t => [
    `"${t.date}"`,
    `"${t.description.replace(/"/g, '""')}"`,
    t.amount,
    `"${t.category}"`,
    `"${t.source || 'Alapértelmezett'}"`
  ]);

  const csvContent = [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `tranzakciok_export_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// --- 3. MODÁL KOMPONENSEK ---
function EditTransactionModal({ transaction, categories, onSave, onClose }) {
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

function CategoryManagerModal({ categories, onSaveCategories, onClose }) {
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

// --- 4. DEMÓ ADATOK ---
const DEMO_TRANSACTIONS = [
  { id: '1', date: '2026-03-01', description: 'Lidl élelmiszer vásárlás', amount: -14500, category: 'Élelmiszer', source: 'Revolut' },
  { id: '2', date: '2026-03-01', description: 'Fizetés utalás - ACME Corp', amount: 650000, category: 'Bevétel', source: 'OTP' },
  { id: '3', date: '2026-03-02', description: 'Wolt ebéd rendelés', amount: -4200, category: 'Étkezés / Vendéglátás', source: 'Revolut' },
  { id: '4', date: '2026-03-03', description: 'Netflix előfizetés', amount: -4490, category: 'Előfizetések / Játék', source: 'Revolut' },
  { id: '5', date: '2026-02-03', description: 'Netflix előfizetés', amount: -4490, category: 'Előfizetések / Játék', source: 'Revolut' },
  { id: '6', date: '2026-01-03', description: 'Netflix előfizetés', amount: -4490, category: 'Előfizetések / Játék', source: 'Revolut' },
  { id: '7', date: '2026-03-04', description: 'MOL tankolás', amount: -22000, category: 'Tankolás / Közlekedés', source: 'OTP' },
  { id: '8', date: '2026-03-05', description: 'Yettel mobil számla', amount: -8900, category: 'Rezsi / Szolgáltatás', source: 'OTP' },
  { id: '9', date: '2026-02-05', description: 'Yettel mobil számla', amount: -8900, category: 'Rezsi / Szolgáltatás', source: 'OTP' },
  { id: '10', date: '2026-03-06', description: 'McDonalds vacsora', amount: -3800, category: 'Étkezés / Vendéglátás', source: 'Revolut' },
  { id: '11', date: '2026-03-07', description: 'Debreceni Állatkert belépő', amount: -6500, category: 'Szórakozás / Szabadidő', source: 'Készpénz' },
];

const COLORS = ['#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#3B82F6', '#06B6D4', '#EC4899', '#14B8A6', '#6B7280'];

// --- 5. FŐ KOMPONENS ---
export default function FinancialDashboard() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState(DEFAULT_CATEGORIES);
  
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedSource, setSelectedSource] = useState('All');
  const [selectedPeriod, setSelectedPeriod] = useState('All');
  const [onlyRecurring, setOnlyRecurring] = useState(false);

  const [sourceNameInput, setSourceNameInput] = useState('Revolut');
  const [isDragging, setIsDragging] = useState(false);
  const [savedMonths, setSavedMonths] = useState({});
  const [saveSuccessMsg, setSaveSuccessMsg] = useState(false);

  const [editingTransaction, setEditingTransaction] = useState(null);
  const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);

  // Hover állapotok a diagram kiemelésekhez
  const [hoveredBarIndex, setHoveredBarIndex] = useState(null);

  useEffect(() => {
    const storedTransactions = localStorage.getItem('financial_current_transactions');
    if (storedTransactions) {
      try { setTransactions(JSON.parse(storedTransactions)); } catch (e) {}
    }

    const storedSaved = localStorage.getItem('financial_saved_months');
    if (storedSaved) {
      try { setSavedMonths(JSON.parse(storedSaved)); } catch (e) {}
    }

    const storedCategories = localStorage.getItem('financial_custom_categories');
    if (storedCategories) {
      try { setCategories(JSON.parse(storedCategories)); } catch (e) {}
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('financial_current_transactions', JSON.stringify(transactions));
  }, [transactions]);

  useEffect(() => {
    localStorage.setItem('financial_custom_categories', JSON.stringify(categories));
  }, [categories]);

  const recurringIds = useMemo(() => {
    return detectRecurringTransactions(transactions);
  }, [transactions]);

  const availableSources = useMemo(() => {
    const sources = new Set(transactions.map(t => t.source || 'Alapértelmezett'));
    return Array.from(sources);
  }, [transactions]);

  const periodFilteredTransactions = useMemo(() => {
    if (selectedPeriod === 'All') return transactions;

    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth();

    return transactions.filter(t => {
      const tDate = new Date(t.date);
      if (isNaN(tDate.getTime())) return true;

      if (selectedPeriod === 'this_month') {
        return tDate.getFullYear() === currentYear && tDate.getMonth() === currentMonth;
      }
      if (selectedPeriod === 'last_month') {
        const lastMonthDate = new Date(currentYear, currentMonth - 1, 1);
        return tDate.getFullYear() === lastMonthDate.getFullYear() && tDate.getMonth() === lastMonthDate.getMonth();
      }
      if (selectedPeriod === 'last_3_months') {
        const threeMonthsAgo = new Date(currentYear, currentMonth - 3, 1);
        return tDate >= threeMonthsAgo;
      }
      if (selectedPeriod === 'last_6_months') {
        const sixMonthsAgo = new Date(currentYear, currentMonth - 6, 1);
        return tDate >= sixMonthsAgo;
      }
      return true;
    });
  }, [transactions, selectedPeriod]);

  const finalFilteredTransactions = useMemo(() => {
    return periodFilteredTransactions.filter(t => {
      const matchesSearch = t.description.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesCategory = selectedCategory === 'All' || t.category === selectedCategory;
      const matchesSource = selectedSource === 'All' || (t.source || 'Alapértelmezett') === selectedSource;
      const matchesRecurring = !onlyRecurring || recurringIds.has(t.id);
      return matchesSearch && matchesCategory && matchesSource && matchesRecurring;
    });
  }, [periodFilteredTransactions, searchTerm, selectedCategory, selectedSource, onlyRecurring, recurringIds]);

  const stats = useMemo(() => {
    let income = 0;
    let expense = 0;
    let maxExpenseItem = { description: '-', amount: 0 };
    let expenseCount = 0;

    periodFilteredTransactions.forEach(t => {
      if (t.amount > 0) {
        income += t.amount;
      } else {
        const absAmt = Math.abs(t.amount);
        expense += absAmt;
        expenseCount++;
        if (absAmt > maxExpenseItem.amount) {
          maxExpenseItem = { description: t.description, amount: absAmt };
        }
      }
    });

    const avgExpense = expenseCount > 0 ? expense / expenseCount : 0;

    return {
      income,
      expense,
      balance: income - expense,
      count: periodFilteredTransactions.length,
      maxExpenseItem,
      avgExpense
    };
  }, [periodFilteredTransactions]);

  // HAVI KIADÁSOK ÉS BEVÉTELEK ALAKULÁSA (6 HÓNAP)
  const monthlyComparisonData = useMemo(() => {
    const monthlyTotals = {};
    const now = new Date();

    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      monthlyTotals[key] = { expense: 0, income: 0 };
    }

    transactions.forEach(t => {
      const key = t.date.substring(0, 7);
      if (monthlyTotals.hasOwnProperty(key)) {
        if (t.amount < 0) {
          monthlyTotals[key].expense += Math.abs(t.amount);
        } else {
          monthlyTotals[key].income += t.amount;
        }
      }
    });

    return Object.keys(monthlyTotals).map(monthKey => ({
      month: monthKey,
      expense: monthlyTotals[monthKey].expense,
      income: monthlyTotals[monthKey].income
    }));
  }, [transactions]);

  const chartData = useMemo(() => {
    const categoryTotals = {};

    periodFilteredTransactions.forEach(t => {
      if (t.amount < 0) {
        const cat = t.category;
        categoryTotals[cat] = (categoryTotals[cat] || 0) + Math.abs(t.amount);
      }
    });

    return Object.keys(categoryTotals).map(cat => ({
      name: cat,
      value: categoryTotals[cat]
    }));
  }, [periodFilteredTransactions]);

  const handleFileUpload = (file) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      const content = e.target.result;
      const parsed = parseCSV(content, sourceNameInput || 'Alapértelmezett', categories);
      if (parsed.length > 0) {
        setTransactions(prev => [...parsed, ...prev]);
      } else {
        alert('Nem sikerült érvényes tranzakciókat beolvasni.');
      }
    };
    reader.readAsText(file, 'UTF-8');
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleDeleteTransaction = (id) => {
    if (confirm("Biztosan törölni szeretnéd ezt a tranzakciót?")) {
      setTransactions(prev => prev.filter(t => t.id !== id));
    }
  };

  const handleSaveEditedTransaction = (updated) => {
    setTransactions(prev => prev.map(t => t.id === updated.id ? updated : t));
    setEditingTransaction(null);
  };

  const handleSaveCategories = (newCategories) => {
    setCategories(newCategories);
    setTransactions(prev => prev.map(t => ({
      ...t,
      category: categorizeTransaction(t.description, t.amount, newCategories)
    })));
  };

  const currentMonthKey = useMemo(() => {
    if (transactions.length === 0) return 'Nincs betöltve adat';
    return transactions[0].date.substring(0, 7);
  }, [transactions]);

  const handleSaveCurrentMonth = () => {
    if (transactions.length === 0) return;
    const updated = {
      ...savedMonths,
      [currentMonthKey]: {
        savedAt: new Date().toLocaleString('hu-HU'),
        transactions: transactions,
        stats: stats
      }
    };
    setSavedMonths(updated);
    localStorage.setItem('financial_saved_months', JSON.stringify(updated));
    setSaveSuccessMsg(true);
    setTimeout(() => setSaveSuccessMsg(false), 3000);
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('hu-HU', { style: 'currency', currency: 'HUF', maximumFractionDigits: 0 }).format(val);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-4 md:p-8 font-sans">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* TOP FEJLÉC */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-800 pb-6">
          <div>
            <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
              Személyes Pénzügyi Dashboard
            </h1>
            <p className="text-slate-400 text-sm mt-1">
              Kövesd nyomon és elemezd kiadásaidat egyetlen felületen.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => setIsCategoryModalOpen(true)}
              className="flex items-center gap-2 bg-slate-800 hover:bg-slate-700 text-slate-300 px-3.5 py-2 rounded-xl text-sm border border-slate-700 transition-all"
            >
              <Settings className="w-4 h-4 text-blue-400" /> Kategóriák kezelése
            </button>

            <div className="flex items-center gap-1 bg-slate-800/80 p-1.5 rounded-xl border border-slate-700/60">
              <button
                onClick={() => setActiveTab('dashboard')}
                className={`flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
                  activeTab === 'dashboard' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <LayoutDashboard className="w-4 h-4" /> Dashboard
              </button>
              <button
                onClick={() => setActiveTab('saved')}
                className={`flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all ${
                  activeTab === 'saved' ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <FolderOpen className="w-4 h-4" /> Elmentett Hónapok ({Object.keys(savedMonths).length})
              </button>
            </div>
          </div>
        </div>

        {activeTab === 'dashboard' && (
          <>
            <div className="flex flex-col lg:flex-row justify-between items-stretch lg:items-center bg-slate-800/50 p-4 rounded-xl border border-slate-700/50 gap-4">
              <div className="flex items-center gap-1 overflow-x-auto pb-2 lg:pb-0">
                {[
                  { id: 'All', label: 'Összes' },
                  { id: 'this_month', label: 'Ez a hónap' },
                  { id: 'last_month', label: 'Előző hónap' },
                  { id: 'last_3_months', label: 'Utolsó 3 hónap' },
                  { id: 'last_6_months', label: 'Utolsó 6 hónap' }
                ].map(p => (
                  <button
                    key={p.id}
                    onClick={() => setSelectedPeriod(p.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                      selectedPeriod === p.id 
                        ? 'bg-blue-500/20 text-blue-400 border border-blue-500/30' 
                        : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>

              <div className="flex items-center gap-2 flex-wrap">
                <button
                  onClick={() => exportToCSV(periodFilteredTransactions)}
                  className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-700 transition-all"
                >
                  <Download className="w-3.5 h-3.5 text-emerald-400" /> Export CSV
                </button>

                {transactions.length > 0 ? (
                  <button
                    onClick={handleSaveCurrentMonth}
                    className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-white px-3 py-1.5 rounded-lg text-xs font-medium transition-all"
                  >
                    {saveSuccessMsg ? <Check className="w-3.5 h-3.5" /> : <Save className="w-3.5 h-3.5" />}
                    {saveSuccessMsg ? 'Elmentve!' : 'Hónap Mentése'}
                  </button>
                ) : (
                  <button 
                    onClick={() => setTransactions(DEMO_TRANSACTIONS)}
                    className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 px-3 py-1.5 rounded-lg text-xs border border-slate-700"
                  >
                    <RefreshCw className="w-3.5 h-3.5" /> Demó Adatok
                  </button>
                )}
              </div>
            </div>

            {/* ÖSSZEGZŐ KÁRTYÁK GRID */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-4 flex flex-col justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase">Összes Kiadás</span>
                <p className="text-xl font-bold text-rose-400 mt-2">{formatCurrency(stats.expense)}</p>
              </div>

              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-4 flex flex-col justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase">Tranzakciók Száma</span>
                <p className="text-xl font-bold text-slate-200 mt-2">{stats.count} db</p>
              </div>

              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-4 flex flex-col justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase">Legnagyobb Tétel</span>
                <p className="text-sm font-bold text-amber-400 mt-2 truncate" title={stats.maxExpenseItem.description}>
                  {stats.maxExpenseItem.description}
                </p>
                <span className="text-xs text-amber-400/80">{formatCurrency(stats.maxExpenseItem.amount)}</span>
              </div>

              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-4 flex flex-col justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase">Átlagos Tétel</span>
                <p className="text-xl font-bold text-purple-400 mt-2">{formatCurrency(stats.avgExpense)}</p>
              </div>

              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-4 flex flex-col justify-between sm:col-span-2 lg:col-span-1">
                <span className="text-xs font-medium text-slate-400 uppercase">Nettó Egyenleg</span>
                <p className={`text-xl font-bold mt-2 ${stats.balance >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {formatCurrency(stats.balance)}
                </p>
              </div>
            </div>

            {/* CSV IMPORT DROPZONE */}
            <div 
              onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-xl p-4 text-center transition-all ${
                isDragging ? 'border-blue-500 bg-blue-500/10' : 'border-slate-700 bg-slate-800/40 hover:border-slate-600'
              }`}
            >
              <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
                <div className="flex items-center gap-2 text-xs text-slate-400">
                  <span>Számlanév:</span>
                  <input 
                    type="text" 
                    value={sourceNameInput}
                    onChange={(e) => setSourceNameInput(e.target.value)}
                    className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-slate-200 focus:outline-none focus:border-blue-500 w-28 text-center"
                    placeholder="pl. Revolut"
                  />
                </div>
                
                <input type="file" accept=".csv" id="csvInput" className="hidden" onChange={(e) => handleFileUpload(e.target.files[0])} />
                <label htmlFor="csvInput" className="cursor-pointer text-xs text-blue-400 hover:underline flex items-center gap-1">
                  <Upload className="w-3.5 h-3.5" /> CSV fájl tallózása vagy áthúzása ide
                </label>
              </div>
            </div>

            {/* 6 HAVI DIAGRAM (KIADÁSOK ÉS BEVÉTELEK) */}
            <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-6">
              <h2 className="text-base font-semibold text-slate-200 mb-4 flex items-center gap-2">
                <BarChart className="w-4 h-4 text-purple-400" /> Elmúlt 6 Hónap Pénzmozgásai
              </h2>
              <div className="h-56 w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlyComparisonData}>
                    <XAxis dataKey="month" stroke="#94A3B8" fontSize={11} />
                    <YAxis stroke="#94A3B8" fontSize={11} tickFormatter={(v) => `${v / 1000}k`} />
                    <Tooltip 
                      formatter={(val, name) => [
                        formatCurrency(val), 
                        name === 'expense' ? 'Kiadás' : 'Bevétel'
                      ]} 
                      contentStyle={{ backgroundColor: '#1E293B', borderColor: '#475569', borderRadius: '8px' }}
                      cursor={{ fill: 'rgba(255, 255, 255, 0.05)' }}
                    />
                    <Legend formatter={(value) => value === 'expense' ? 'Kiadás' : 'Bevétel'} />
                    <Bar dataKey="expense" name="expense" fill="#8B5CF6" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="income" name="income" fill="#10B981" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* DONUT & BAR DIAGRAMOK */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-6">
                <h2 className="text-base font-semibold text-slate-200 mb-4 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-400" /> Kiadások Kategóriánként
                </h2>
                <div className="h-72 w-full">
                  {chartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie data={chartData} cx="50%" cy="50%" innerRadius={55} outerRadius={75} paddingAngle={4} dataKey="value">
                          {chartData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(val) => [formatCurrency(val), 'Kiadás']} contentStyle={{ backgroundColor: '#1E293B', borderColor: '#475569', borderRadius: '8px' }} />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-slate-500 text-sm">Nincs kiadási adat</div>
                  )}
                </div>
              </div>

              <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-6">
                <h2 className="text-base font-semibold text-slate-200 mb-4 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-emerald-400" /> Kategória eloszlás
                </h2>
                <div className="h-72 w-full">
                  {chartData.length > 0 ? (
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart 
                        data={chartData}
                        margin={{ top: 10, right: 10, left: 10, bottom: 65 }}
                      >
                        <XAxis 
                          dataKey="name" 
                          stroke="#94A3B8" 
                          fontSize={11} 
                          angle={-25} 
                          textAnchor="end"
                          interval={0}
                        />
                        <YAxis stroke="#94A3B8" fontSize={11} tickFormatter={(v) => `${v / 1000}k`} />
                        <Tooltip 
                          formatter={(val) => [formatCurrency(val), 'Kiadás']} 
                          contentStyle={{ backgroundColor: '#1E293B', borderColor: '#475569', borderRadius: '8px' }}
                          cursor={{ fill: 'rgba(255, 255, 255, 0.05)' }}
                        />
                        <Bar 
                          dataKey="value" 
                          radius={[4, 4, 0, 0]}
                          onMouseEnter={(_, index) => setHoveredBarIndex(index)}
                          onMouseLeave={() => setHoveredBarIndex(null)}
                        >
                          {chartData.map((entry, index) => {
                            const isHovered = hoveredBarIndex === index;
                            const isAnyHovered = hoveredBarIndex !== null;
                            const opacity = isAnyHovered ? (isHovered ? 1 : 0.4) : 1;
                            return (
                              <Cell 
                                key={`bar-${index}`} 
                                fill={COLORS[index % COLORS.length]} 
                                fillOpacity={opacity}
                                style={{ transition: 'all 0.2s ease-in-out', cursor: 'pointer' }}
                              />
                            );
                          })}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  ) : (
                    <div className="h-full flex items-center justify-center text-slate-500 text-sm">Nincs kiadási adat</div>
                  )}
                </div>
              </div>
            </div>

            {/* TÁBLÁZAT */}
            <div className="bg-slate-800/80 border border-slate-700/50 rounded-xl p-6 space-y-4">
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <h2 className="text-lg font-semibold text-slate-200">Tranzakciók</h2>
                
                <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
                  <div className="relative flex-1 md:w-48">
                    <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input 
                      type="text" 
                      placeholder="Keresés..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full pl-8 pr-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                    />
                  </div>

                  <select 
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="All">Minden kategória</option>
                    {categories.map(c => (
                      <option key={c.name} value={c.name}>{c.name}</option>
                    ))}
                  </select>

                  <select 
                    value={selectedSource}
                    onChange={(e) => setSelectedSource(e.target.value)}
                    className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                  >
                    <option value="All">Minden forrás</option>
                    {availableSources.map(s => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>

                  <button
                    onClick={() => setOnlyRecurring(!onlyRecurring)}
                    className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                      onlyRecurring 
                        ? 'bg-amber-500/20 text-amber-400 border-amber-500/30' 
                        : 'bg-slate-900 text-slate-400 border-slate-700 hover:text-slate-200'
                    }`}
                  >
                    <Repeat className="w-3 h-3" /> Fix kiadások
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto rounded-lg border border-slate-700/50">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-slate-900/80 text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-700/50">
                      <th className="p-3">Dátum</th>
                      <th className="p-3">Leírás</th>
                      <th className="p-3">Forrás</th>
                      <th className="p-3">Kategória</th>
                      <th className="p-3 text-right">Összeg</th>
                      <th className="p-3 text-center">Műveletek</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-700/30 text-xs">
                    {finalFilteredTransactions.length > 0 ? (
                      finalFilteredTransactions.map(t => {
                        const isRecurring = recurringIds.has(t.id);
                        const isIncome = t.amount > 0;

                        return (
                          <tr key={t.id} className="hover:bg-slate-700/20 transition-colors">
                            <td className="p-3 text-slate-400 whitespace-nowrap">{t.date}</td>
                            <td className="p-3 font-medium text-slate-200 flex items-center gap-1.5">
                              {t.description}
                              {isRecurring && (
                                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] bg-amber-500/10 text-amber-400 border border-amber-500/20" title="Rendszeres / Fix kiadás">
                                  <Repeat className="w-2.5 h-2.5 mr-0.5" /> Fix
                                </span>
                              )}
                            </td>
                            <td className="p-3 text-slate-400">{t.source || 'Alapértelmezett'}</td>
                            <td className="p-3 text-slate-300">{t.category}</td>
                            <td className={`p-3 text-right font-semibold whitespace-nowrap ${
                              isIncome ? 'text-emerald-400' : 'text-slate-200'
                            }`}>
                              {isIncome ? `+${formatCurrency(t.amount)}` : formatCurrency(t.amount)}
                            </td>
                            <td className="p-3 text-center">
                              <div className="flex items-center justify-center gap-2">
                                <button onClick={() => setEditingTransaction(t)} className="text-slate-400 hover:text-blue-400"><Edit2 className="w-3.5 h-3.5" /></button>
                                <button onClick={() => handleDeleteTransaction(t.id)} className="text-slate-400 hover:text-rose-400"><Trash2 className="w-3.5 h-3.5" /></button>
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    ) : (
                      <tr>
                        <td colSpan="6" className="text-center p-8 text-slate-500">
                          Nincs a szűrésnek megfelelő tranzakció.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

        {activeTab === 'saved' && (
          <div className="space-y-6">
            <h2 className="text-lg font-semibold text-slate-200 flex items-center gap-2">
              <FolderOpen className="w-5 h-5 text-blue-400" /> Elmentett Hónapok Gyűjteménye
            </h2>

            {Object.keys(savedMonths).length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {Object.entries(savedMonths).map(([monthKey, data]) => (
                  <div key={monthKey} className="bg-slate-800/80 border border-slate-700/60 rounded-xl p-5 space-y-3">
                    <div className="flex justify-between items-start border-b border-slate-700/50 pb-2">
                      <div>
                        <h3 className="text-lg font-bold text-blue-400">{monthKey}</h3>
                        <p className="text-[10px] text-slate-400">Mentve: {data.savedAt}</p>
                      </div>
                      <span className="text-xs px-2 py-0.5 bg-slate-700 text-slate-300 rounded border border-slate-600">
                        {data.transactions.length} db
                      </span>
                    </div>

                    <div className="space-y-1 text-xs">
                      <div className="flex justify-between"><span className="text-slate-400">Kiadás:</span><span className="text-rose-400">{formatCurrency(data.stats.expense)}</span></div>
                      <div className="flex justify-between"><span className="text-slate-400">Egyenleg:</span><span className="text-emerald-400">{formatCurrency(data.stats.balance)}</span></div>
                    </div>

                    <button
                      onClick={() => { setTransactions(data.transactions); setActiveTab('dashboard'); }}
                      className="w-full bg-blue-600 hover:bg-blue-500 text-white py-1.5 rounded text-xs font-medium"
                    >
                      Megnyitás
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-slate-500 text-sm">Még nincs elmentett hónap.</p>
            )}
          </div>
        )}

      </div>

      {editingTransaction && (
        <EditTransactionModal 
          transaction={editingTransaction} 
          categories={categories} 
          onSave={handleSaveEditedTransaction} 
          onClose={() => setEditingTransaction(null)} 
        />
      )}

      {isCategoryModalOpen && (
        <CategoryManagerModal 
          categories={categories} 
          onSaveCategories={handleSaveCategories} 
          onClose={() => setIsCategoryModalOpen(false)} 
        />
      )}
    </div>
  );
}