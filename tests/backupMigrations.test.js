'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { CURRENT_BACKUP_VERSION, migrateBackupPayload } = require('../server/backupMigrations');

test('migra backup legado para a versao atual', () => {
  const migrated = migrateBackupPayload({
    transactions: [{ id: 't1', desc: 'Mercado', account_id: 'a1', installment_group: 'g1' }],
    customCategories: [{ id: 'c1', name: 'Mercado' }],
    shoppingLists: [{ id: 'l1' }],
    shoppingItems: [{ id: 'i1' }],
    dueItems: [{ id: 'd1' }]
  });

  assert.equal(migrated.app, 'Finanza');
  assert.equal(migrated.version, CURRENT_BACKUP_VERSION);
  assert.equal(migrated.transactions[0].description, 'Mercado');
  assert.equal(migrated.transactions[0].accountId, 'a1');
  assert.equal(migrated.transactions[0].installmentGroup, 'g1');
  assert.equal(migrated.categories[0].id, 'c1');
  assert.equal(migrated.shopping.lists[0].id, 'l1');
  assert.equal(migrated.shopping.items[0].id, 'i1');
  assert.deepEqual(migrated.settings.rates.dueItems, [{ id: 'd1' }]);
});

test('preserva backups versionados sem forcar migracao destrutiva', () => {
  const input = {
    app: 'Finanza',
    version: '4.0.0',
    transactions: [{ id: 't1', description: 'Padaria' }]
  };

  const migrated = migrateBackupPayload(input);
  assert.equal(migrated.version, '4.0.0');
  assert.deepEqual(migrated.transactions, input.transactions);
});

test('migra backup 4.0-preview como legado compativel', () => {
  const migrated = migrateBackupPayload({
    version: '4.0-preview',
    transactions: [{ id: 't1', description: 'Internet', recur_group: 'rec1' }]
  });

  assert.equal(migrated.version, CURRENT_BACKUP_VERSION);
  assert.equal(migrated.transactions[0].desc, 'Internet');
  assert.equal(migrated.transactions[0].recurGroup, 'rec1');
});

test('cria estruturas vazias para backup legado minimo', () => {
  const migrated = migrateBackupPayload({});

  assert.equal(migrated.version, CURRENT_BACKUP_VERSION);
  assert.deepEqual(migrated.transactions, []);
  assert.deepEqual(migrated.shopping, { lists: [], items: [] });
  assert.deepEqual(migrated.settings.rates, {});
});
