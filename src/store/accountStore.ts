import { create } from 'zustand';
import { Account } from '../types';
import { AccountRepository } from '../database/repositories/accountRepository';
import { TransactionRepository } from '../database/repositories/transactionRepository';

interface AccountState {
  accounts: Account[];
  isLoading: boolean;
  loadAccounts: () => Promise<void>;
  addAccount: (account: Account) => Promise<void>;
  updateAccount: (account: Account) => Promise<void>;
}

const accountRepo = new AccountRepository();
const txRepo = new TransactionRepository();

export const useAccountStore = create<AccountState>((set, get) => ({
  accounts: [],
  isLoading: false,

  loadAccounts: async () => {
    set({ isLoading: true });
    try {
      const accounts = await accountRepo.getAll();
      const allTx = await txRepo.getAll();

      // Compute current balances
      const enriched = accounts.map((acc) => {
        const txs = allTx.filter((t) => t.accountId === acc.id);
        const netDelta = txs.reduce((sum, t) => {
          if (t.direction === 'income' || t.direction === 'refund') {
            return sum + Math.abs(t.amountMinor);
          } else if (t.direction === 'expense') {
            return sum - Math.abs(t.amountMinor);
          }
          return sum;
        }, 0);

        return {
          ...acc,
          currentBalanceMinor: acc.openingBalanceMinor + netDelta,
        };
      });

      set({ accounts: enriched, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  addAccount: async (account) => {
    await accountRepo.create(account);
    await get().loadAccounts();
  },

  updateAccount: async (account) => {
    await accountRepo.update(account);
    await get().loadAccounts();
  },
}));
