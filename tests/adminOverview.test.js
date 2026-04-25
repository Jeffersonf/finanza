'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeRoleCounts, buildAdminOverview } = require('../server/adminOverview');

test('normaliza contagem de papeis ausentes para zero', () => {
  assert.deepEqual(normalizeRoleCounts([
    { role: 'admin', total: '2' },
    { role: 'editor', total: '3' }
  ]), {
    admin: 2,
    editor: 3,
    read: 0,
    guest: 0
  });
});

test('monta overview administrativo com metricas principais', () => {
  const overview = buildAdminOverview({
    roleRows: [
      { role: 'admin', total: '1' },
      { role: 'editor', total: '2' },
      { role: 'guest', total: '4' }
    ],
    auditRow: { total: '17', last_at: '2026-04-25T13:10:00.000Z' },
    transactionRow: { total: '189' },
    lastBackupRow: { filename: 'finanza_1.sql.gz', size_bytes: '2048', created_at: '2026-04-24T23:00:00.000Z' },
    checkedAt: '2026-04-25T13:11:00.000Z'
  });

  assert.equal(overview.users.total, 7);
  assert.deepEqual(overview.users.byRole, {
    admin: 1,
    editor: 2,
    read: 0,
    guest: 4
  });
  assert.equal(overview.activity.auditCount, 17);
  assert.equal(overview.activity.lastAuditAt, '2026-04-25T13:10:00.000Z');
  assert.equal(overview.transactions.total, 189);
  assert.equal(overview.backup.available, true);
  assert.deepEqual(overview.backup.last, {
    filename: 'finanza_1.sql.gz',
    sizeBytes: 2048,
    createdAt: '2026-04-24T23:00:00.000Z'
  });
  assert.equal(overview.server.status, 'ok');
  assert.equal(overview.server.checkedAt, '2026-04-25T13:11:00.000Z');
});

test('mantem overview resiliente sem backup registrado', () => {
  const overview = buildAdminOverview({
    roleRows: [],
    auditRow: { total: '0', last_at: null },
    transactionRow: { total: '0' },
    checkedAt: null,
    serverOk: false
  });

  assert.equal(overview.users.total, 0);
  assert.equal(overview.backup.available, false);
  assert.equal(overview.backup.last, null);
  assert.equal(overview.server.status, 'error');
});
