'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { normalizeBackupPayload, backupImportCounts } = require('../server/backupSchema');
const { CURRENT_BACKUP_VERSION } = require('../server/backupMigrations');

function loadDemoFixture() {
  const file = path.join(__dirname, '..', 'fixtures', 'demo-backup.json');
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

test('fixture demo e um backup importavel pela versao atual', () => {
  const backup = normalizeBackupPayload(loadDemoFixture());

  assert.equal(backup.app, 'Finanza');
  assert.equal(backup.version, CURRENT_BACKUP_VERSION);
  assert.ok(backup.transactions.length >= 3);
  assert.ok(backup.accounts.length >= 1);
  assert.ok(backup.shopping.lists.length >= 1);
  assert.ok(backup.settings.rates.car.vehicles.length >= 1);
});

test('fixture demo produz contadores completos de importacao', () => {
  const backup = normalizeBackupPayload(loadDemoFixture());
  const counts = backupImportCounts(backup);

  assert.equal(counts.transactions, backup.transactions.length);
  assert.equal(counts.accounts, backup.accounts.length);
  assert.equal(counts.categories, backup.categories.length);
  assert.equal(counts.shoppingLists, backup.shopping.lists.length);
  assert.equal(counts.shoppingItems, backup.shopping.items.length);
  assert.equal(counts.dueItems, backup.dueItems.length);
});
