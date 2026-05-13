#!/usr/bin/env node
'use strict';

const fs = require('fs');
const crypto = require('crypto');
const { Pool } = require('pg');
const { normalizeBackupPayload, backupImportCounts } = require('../server/backupSchema');

const args = process.argv.slice(2);
const getArg = name => {
  const idx = args.indexOf(name);
  return idx >= 0 ? args[idx + 1] : '';
};

const DATABASE_URL = process.env.DATABASE_URL || getArg('--database-url');
const backupPath = getArg('--backup');
const username = (getArg('--username') || '').trim().toLowerCase();
const password = getArg('--password');
const name = getArg('--name') || username || 'Usuario';

if (!DATABASE_URL || !backupPath || !username || !password) {
  console.error('Uso: node tools/restore-backup-to-db.js --backup backup.json --username usuario --password senha');
  console.error('Informe DATABASE_URL no ambiente ou use --database-url.');
  process.exit(1);
}

function hashPassword(value) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(String(value), salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}

function sslFor(url = '') {
  return /sslmode=require/i.test(url) || /neon\.tech/i.test(url) || /render\.com/i.test(url)
    ? { rejectUnauthorized: false }
    : undefined;
}

function cleanText(value, fallback = '') {
  const text = String(value ?? '').replace(/\s+/g, ' ').trim();
  return text || fallback;
}

function normalizeCategoryName(v) {
  return ({
    Salario: 'Salario',
    Alimentacao: 'Alimentacao',
    Saude: 'Saude',
    Educacao: 'Educacao',
    Poupanca: 'Poupanca'
  }[v]) || v;
}

function toJsonb(value, fallback) {
  try {
    return JSON.stringify(value ?? fallback);
  } catch {
    return JSON.stringify(fallback);
  }
}

async function replaceRows(client, table, userId, rows, insertSql, mapper) {
  await client.query(`DELETE FROM ${table} WHERE user_id=$1`, [userId]);
  for (const row of rows || []) {
    await client.query(insertSql, mapper(row, userId));
  }
}

async function replaceAppState(client, userId, state = {}) {
  const { accounts = [], categories = [], shopping = {}, settings = {} } = state || {};
  const rates = { ...(settings.rates || {}) };
  if (Array.isArray(state.dueItems) && !Array.isArray(rates.dueItems)) rates.dueItems = state.dueItems;
  if (state.car && !rates.car) rates.car = state.car;

  await replaceRows(client, 'accounts', userId, accounts,
    `INSERT INTO accounts
     (user_id,id,name,icon,type,balance,yield_rate,yield_type,yield_val,calc_base,start_date,note)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)`,
    (a, uid) => [uid, String(a.id), cleanText(a.name, 'Conta'), cleanText(a.icon),
      cleanText(a.type, 'checking'), Number(a.balance) || 0, Number(a.yieldRate ?? a.yield_rate) || 0,
      cleanText(a.yieldType ?? a.yield_type, 'manual'), Number(a.yieldVal ?? a.yield_val) || 0,
      cleanText(a.calcBase ?? a.calc_base, 'du'), a.startDate || a.start_date || null, cleanText(a.note)]
  );

  await replaceRows(client, 'categories', userId, categories,
    `INSERT INTO categories (user_id,id,icon,name,color) VALUES ($1,$2,$3,$4,$5)`,
    (c, uid) => [uid, String(c.id), cleanText(c.ico || c.icon), normalizeCategoryName(cleanText(c.name, 'Categoria')), cleanText(c.col || c.color, '#888')]
  );

  await replaceRows(client, 'shopping_items', userId, [], 'SELECT $1', uid => [uid]);
  await replaceRows(client, 'shopping_lists', userId, shopping.lists || [],
    `INSERT INTO shopping_lists (user_id,id,name,icon,position) VALUES ($1,$2,$3,$4,$5)`,
    (l, uid) => [uid, String(l.id), cleanText(l.name, 'Lista'), cleanText(l.ico || l.icon), Number(l.position) || 0]
  );
  for (const item of shopping.items || []) {
    await client.query(
      `INSERT INTO shopping_items (user_id,id,list_id,name,qty,category,bought,created_ms)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      [userId, String(item.id), String(item.listId || item.list_id), cleanText(item.name, 'Item'),
       cleanText(item.qty), cleanText(item.cat || item.category), !!item.bought, Number(item.createdAt || item.created_ms) || Date.now()]
    );
  }

  await client.query(
    `INSERT INTO user_settings (user_id,theme,rates,widget_prefs,widget_order,tx_view,active_list)
     VALUES ($1,$2,$3::jsonb,$4::jsonb,$5::jsonb,$6,$7)
     ON CONFLICT (user_id) DO UPDATE SET
       theme=EXCLUDED.theme,
       rates=EXCLUDED.rates,
       widget_prefs=EXCLUDED.widget_prefs,
       widget_order=EXCLUDED.widget_order,
       tx_view=EXCLUDED.tx_view,
       active_list=EXCLUDED.active_list,
       updated_at=NOW()`,
    [userId, cleanText(settings.theme, 'dark'), toJsonb(rates, {}),
     toJsonb(settings.widgetPrefs || settings.widget_prefs || {}, {}),
     toJsonb(settings.widgetOrder || settings.widget_order || [], []),
     cleanText(settings.txView || settings.tx_view, 'n'),
     settings.activeList || settings.active_list || null]
  );
}

async function main() {
  const raw = JSON.parse(fs.readFileSync(backupPath, 'utf8'));
  const data = normalizeBackupPayload(raw);
  const pool = new Pool({ connectionString: DATABASE_URL, ssl: sslFor(DATABASE_URL) });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const apiKey = crypto.randomBytes(32).toString('hex');
    const userResult = await client.query(
      `INSERT INTO users (name, username, password_hash, api_key, role, is_admin)
       VALUES ($1,$2,$3,$4,'admin',TRUE)
       ON CONFLICT (username) DO UPDATE SET
         name=EXCLUDED.name,
         password_hash=EXCLUDED.password_hash,
         role='admin',
         is_admin=TRUE
       RETURNING id, username`,
      [name, username, hashPassword(password), apiKey]
    );
    const userId = userResult.rows[0].id;

    await replaceRows(client, 'transactions', userId, data.transactions || [],
      `INSERT INTO transactions
        (id,user_id,type,description,amount,category,date,purchase_date,note,account_id,paid,pending,
         installment_group,installment_num,installment_total,recur_group,split_meta)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17::jsonb)`,
      (t, uid) => [t.id || crypto.randomUUID(), uid, cleanText(t.type, 'expense'), cleanText(t.desc || t.description, 'Lancamento'),
        Number(t.amount) || 0, normalizeCategoryName(cleanText(t.category, 'A classificar')), t.date,
        t.purchaseDate || t.purchase_date || t.date, cleanText(t.note), t.accountId || t.account_id || null,
        !!t.paid, !!t.pending, t.installmentGroup || t.installment_group || null,
        t.installmentNum || t.installment_num || null, t.installmentTotal || t.installment_total || null,
        t.recurGroup || t.recur_group || null, toJsonb(t.splitMeta || t.split_meta || {}, {})]
    );

    await replaceRows(client, 'budgets', userId, data.budgets || [],
      `INSERT INTO budgets (user_id,category,"limit") VALUES ($1,$2,$3)
       ON CONFLICT (user_id, category) DO UPDATE SET "limit"=EXCLUDED."limit", updated_at=NOW()`,
      (b, uid) => [uid, normalizeCategoryName(cleanText(b.category, 'Categoria')), Number(b.limit) || 1]
    );

    await replaceRows(client, 'goals', userId, data.goals || [],
      `INSERT INTO goals (user_id,name,icon,target,current,deadline,description,monthly)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      (g, uid) => [uid, cleanText(g.name, 'Meta'), cleanText(g.icon, 'goal'),
        Number(g.target) || 1, Number(g.current) || 0, g.deadline || null,
        cleanText(g.desc || g.description), Number(g.monthly) || 0]
    );

    await replaceAppState(client, userId, {
      accounts: data.accounts,
      categories: data.categories,
      shopping: data.shopping,
      car: raw.car,
      settings: data.settings,
      dueItems: data.dueItems
    });

    await client.query('COMMIT');
    console.log(`Restaurado para ${userResult.rows[0].username}:`, backupImportCounts(data));
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch(err => {
  console.error(`Erro: ${err.message}`);
  process.exit(1);
});
