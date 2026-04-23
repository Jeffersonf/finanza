'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeBackupPayload, backupImportCounts } = require('../server/backupSchema');

test('normaliza backup completo preservando listas principais', () => {
  const backup = normalizeBackupPayload({
    app: 'Finanza',
    version: '4.0.0',
    transactions: [{ id: 't1', amount: 10 }],
    budgets: [{ category: 'Mercado', limit: 500 }],
    goals: [{ name: 'Reserva', target: 1000 }],
    accounts: [{ id: 'a1', name: 'Principal' }],
    categories: [{ id: 'c1', name: 'Mercado' }],
    shopping: { lists: [{ id: 'l1' }], items: [{ id: 'i1' }] },
    settings: { theme: 'dark', rates: { cdi: 10.4 } },
    dueItems: [{ id: 'd1' }]
  });

  assert.equal(backup.version, '4.0.0');
  assert.equal(backup.transactions.length, 1);
  assert.equal(backup.shopping.lists.length, 1);
  assert.deepEqual(backup.settings.rates.dueItems, [{ id: 'd1' }]);
});

test('aceita aliases antigos de categorias e compras', () => {
  const backup = normalizeBackupPayload({
    transactions: [],
    customCategories: [{ id: 'cat-old', name: 'Antiga' }],
    shoppingLists: [{ id: 'list-old', name: 'Mercado' }],
    shoppingItems: [{ id: 'item-old', name: 'Arroz' }]
  });

  assert.equal(backup.categories[0].id, 'cat-old');
  assert.equal(backup.shopping.lists[0].id, 'list-old');
  assert.equal(backup.shopping.items[0].id, 'item-old');
});

test('conta itens importados a partir do payload normalizado', () => {
  const backup = normalizeBackupPayload({
    transactions: [{ id: 't1' }, { id: 't2' }],
    budgets: [{ id: 'b1' }],
    goals: [],
    accounts: [{ id: 'a1' }, { id: 'a2' }]
  });

  assert.deepEqual(backupImportCounts(backup), {
    transactions: 2,
    budgets: 1,
    goals: 0,
    accounts: 2
  });
});

test('rejeita campos estruturais com tipo invalido', () => {
  assert.throws(
    () => normalizeBackupPayload({ transactions: {} }),
    /transactions deve ser uma lista/
  );
  assert.throws(
    () => normalizeBackupPayload(null),
    /Backup JSON deve ser um objeto/
  );
});
