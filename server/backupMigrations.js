'use strict';

const CURRENT_BACKUP_VERSION = '4.0.0';

function clone(value) {
  return JSON.parse(JSON.stringify(value || {}));
}

function migrateLegacyBackup(input = {}) {
  const data = clone(input);
  data.app = data.app || 'Finanza';
  data.version = data.version || 'legacy';

  if (data.transactions == null) data.transactions = [];
  if (Array.isArray(data.transactions)) {
    data.transactions = data.transactions.map(tx => ({
      ...tx,
      description: tx.description || tx.desc || 'Lancamento',
      desc: tx.desc || tx.description || 'Lancamento',
      accountId: tx.accountId || tx.account_id || null,
      account_id: tx.account_id || tx.accountId || null,
      installmentGroup: tx.installmentGroup || tx.installment_group || null,
      installmentNum: tx.installmentNum || tx.installment_num || null,
      installmentTotal: tx.installmentTotal || tx.installment_total || null,
      recurGroup: tx.recurGroup || tx.recur_group || null
    }));
  }

  if (!Array.isArray(data.categories) && Array.isArray(data.customCategories)) {
    data.categories = data.customCategories;
  }

  if (!data.shopping || typeof data.shopping !== 'object' || Array.isArray(data.shopping)) {
    data.shopping = {
      lists: data.shoppingLists || [],
      items: data.shoppingItems || []
    };
  }

  data.settings = data.settings && typeof data.settings === 'object' && !Array.isArray(data.settings) ? data.settings : {};
  data.settings.rates = data.settings.rates && typeof data.settings.rates === 'object' && !Array.isArray(data.settings.rates) ? data.settings.rates : {};
  if (Array.isArray(data.dueItems) && !Array.isArray(data.settings.rates.dueItems)) {
    data.settings.rates.dueItems = data.dueItems;
  }

  data.version = CURRENT_BACKUP_VERSION;
  return data;
}

function migrateBackupPayload(input = {}) {
  const data = clone(input);
  if (!data.version || data.version === 'legacy' || data.version.startsWith('4.0-preview')) {
    return migrateLegacyBackup(data);
  }
  return { ...data, version: data.version || CURRENT_BACKUP_VERSION };
}

module.exports = { CURRENT_BACKUP_VERSION, migrateBackupPayload, migrateLegacyBackup };
