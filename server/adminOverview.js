'use strict';

function safeInt(value) {
  const parsed = parseInt(value, 10);
  return Number.isFinite(parsed) ? parsed : 0;
}

function normalizeRoleCounts(rows = []) {
  const base = { admin: 0, editor: 0, read: 0, guest: 0 };
  for (const row of rows) {
    const role = String(row?.role || '').trim().toLowerCase();
    if (role in base) base[role] = safeInt(row?.total);
  }
  return base;
}

function buildAdminOverview(payload = {}) {
  const roleCounts = normalizeRoleCounts(payload.roleRows);
  const userCount = Object.values(roleCounts).reduce((sum, value) => sum + value, 0);
  const lastBackup = payload.lastBackupRow
    ? {
        filename: payload.lastBackupRow.filename || '',
        sizeBytes: safeInt(payload.lastBackupRow.size_bytes),
        createdAt: payload.lastBackupRow.created_at || null
      }
    : null;

  return {
    users: {
      total: userCount,
      byRole: roleCounts
    },
    activity: {
      auditCount: safeInt(payload.auditRow?.total),
      lastAuditAt: payload.auditRow?.last_at || null
    },
    transactions: {
      total: safeInt(payload.transactionRow?.total)
    },
    backup: {
      available: !!lastBackup,
      last: lastBackup
    },
    server: {
      status: payload.serverOk === false ? 'error' : 'ok',
      checkedAt: payload.checkedAt || null
    }
  };
}

module.exports = { normalizeRoleCounts, buildAdminOverview };
