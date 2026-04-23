'use strict';

function asObject(value, fallback = {}) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : fallback;
}

function asArray(value, fieldName) {
  if (value == null) return [];
  if (!Array.isArray(value)) throw new Error(`${fieldName} deve ser uma lista`);
  return value;
}

function normalizeBackupPayload(payload = {}) {
  const data = asObject(payload, null);
  if (!data) throw new Error('Backup JSON deve ser um objeto');

  const transactions = asArray(data.transactions, 'transactions');
  const budgets = asArray(data.budgets, 'budgets');
  const goals = asArray(data.goals, 'goals');
  const accounts = asArray(data.accounts, 'accounts');
  const categories = asArray(data.categories || data.customCategories, 'categories');

  const shopping = asObject(data.shopping, {
    lists: data.shoppingLists || [],
    items: data.shoppingItems || []
  });
  const shoppingLists = asArray(shopping.lists, 'shopping.lists');
  const shoppingItems = asArray(shopping.items, 'shopping.items');

  const settings = asObject(data.settings, {});
  const rates = asObject(settings.rates, {});
  const dueItems = asArray(data.dueItems || rates.dueItems, 'dueItems');

  return {
    app: data.app || 'Finanza',
    version: data.version || '',
    transactions,
    budgets,
    goals,
    accounts,
    categories,
    shopping: { lists: shoppingLists, items: shoppingItems },
    settings: {
      ...settings,
      rates: { ...rates, dueItems }
    },
    dueItems
  };
}

function backupImportCounts(data) {
  return {
    transactions: data.transactions.length,
    budgets: data.budgets.length,
    goals: data.goals.length,
    accounts: data.accounts.length
  };
}

module.exports = { normalizeBackupPayload, backupImportCounts };
