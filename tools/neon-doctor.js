#!/usr/bin/env node
'use strict';

const { Pool } = require('pg');
const crypto = require('crypto');

const args = process.argv.slice(2);
const getArg = name => {
  const idx = args.indexOf(name);
  return idx >= 0 ? args[idx + 1] : '';
};

const usernameFilter = (getArg('--username') || '').trim().toLowerCase();
const resetPassword = getArg('--reset-password') || '';
const listUsers = args.includes('--list-users');
const DATABASE_URL = process.env.DATABASE_URL || getArg('--database-url');

if (!DATABASE_URL) {
  console.error('Informe DATABASE_URL no ambiente ou use --database-url.');
  process.exit(1);
}

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(String(password), salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}

function sslFor(url = '') {
  return /sslmode=require/i.test(url) || /neon\.tech/i.test(url)
    ? { rejectUnauthorized: false }
    : undefined;
}

async function timed(label, fn) {
  const start = Date.now();
  const result = await fn();
  return { label, ms: Date.now() - start, result };
}

async function main() {
  const pool = new Pool({ connectionString: DATABASE_URL, ssl: sslFor(DATABASE_URL) });
  try {
    const ping = await timed('ping', () => pool.query('SELECT NOW() AS now'));
    console.log(`DB ok (${ping.ms}ms) ${ping.result.rows[0].now.toISOString()}`);

    const tables = await timed('table counts', () => pool.query(`
      SELECT 'users' AS table_name, COUNT(*)::int AS total FROM users
      UNION ALL SELECT 'transactions', COUNT(*)::int FROM transactions
      UNION ALL SELECT 'budgets', COUNT(*)::int FROM budgets
      UNION ALL SELECT 'goals', COUNT(*)::int FROM goals
      UNION ALL SELECT 'accounts', COUNT(*)::int FROM accounts
      UNION ALL SELECT 'user_settings', COUNT(*)::int FROM user_settings
      ORDER BY table_name
    `));
    console.log(`Contagens (${tables.ms}ms):`);
    for (const row of tables.result.rows) console.log(`  ${row.table_name}: ${row.total}`);

    const userWhere = usernameFilter ? 'WHERE username=$1' : '';
    const userParams = usernameFilter ? [usernameFilter] : [];
    const users = await pool.query(`
      SELECT id, name, username, role, is_admin, created_at,
             password_hash IS NOT NULL AND password_hash LIKE 'scrypt$%' AS has_password,
             two_factor_enabled
      FROM users
      ${userWhere}
      ORDER BY created_at
    `, userParams);

    if (!users.rows.length) {
      console.log(usernameFilter ? `Usuario "${usernameFilter}" nao encontrado.` : 'Nenhum usuario encontrado.');
    } else if (listUsers || usernameFilter) {
      console.log('Usuarios:');
      for (const user of users.rows) {
        console.log(`  ${user.username || '(sem username)'} | ${user.role}${user.is_admin ? '/admin' : ''} | senha=${user.has_password ? 'ok' : 'faltando'} | 2FA=${user.two_factor_enabled ? 'on' : 'off'} | id=${user.id}`);
      }
    } else {
      const missing = users.rows.filter(user => !user.has_password).length;
      console.log(`Usuarios: ${users.rows.length}; sem senha valida: ${missing}. Use --list-users para detalhar.`);
    }

    if (resetPassword) {
      if (!usernameFilter) throw new Error('Use --username junto com --reset-password.');
      const updated = await pool.query(
        'UPDATE users SET password_hash=$1 WHERE username=$2 RETURNING username',
        [hashPassword(resetPassword), usernameFilter]
      );
      if (!updated.rowCount) throw new Error(`Usuario "${usernameFilter}" nao encontrado para reset.`);
      console.log(`Senha redefinida para ${updated.rows[0].username}.`);
    }
  } finally {
    await pool.end();
  }
}

main().catch(err => {
  console.error(`Erro: ${err.message}`);
  process.exit(1);
});
