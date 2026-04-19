// ============================================
// Finanza API - Multi-Usuario v2
// Node.js + Express + PostgreSQL
// ============================================

const express = require('express');
const cors    = require('cors');
const { Pool } = require('pg');
const crypto  = require('crypto');
const fs      = require('fs');
const path    = require('path');

const app  = express();
const PORT = process.env.PORT || 3000;
const isProd = process.env.NODE_ENV === 'production';

// ── DB ──────────────────────────────────────
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: isProd ? { rejectUnauthorized: false } : false
});
pool.connect()
  .then(c => { console.log('✅ PostgreSQL conectado'); c.release(); })
  .catch(e => { console.error('❌ DB erro:', e.message); process.exit(1); });

// ── MIDDLEWARES ─────────────────────────────
const allowedOrigins = (process.env.CORS_ORIGIN || '')
  .split(',')
  .map(s => s.trim())
  .filter(Boolean);

app.use(cors({
  origin(origin, cb) {
    if (!origin || !allowedOrigins.length || allowedOrigins.includes(origin)) return cb(null, true);
    cb(new Error('Origem nao permitida'));
  },
  methods: ['GET','POST','PUT','PATCH','DELETE'],
  allowedHeaders: ['Content-Type','x-api-key']
}));
app.use(express.json({ limit: '1mb' }));

function cleanText(v, fallback = '') {
  return typeof v === 'string' ? v : fallback;
}

async function replaceRows(client, table, userId, rows, insertSql, mapper) {
  await client.query(`DELETE FROM ${table} WHERE user_id=$1`, [userId]);
  for (const row of rows || []) {
    await client.query(insertSql, mapper(row, userId));
  }
}

// ── ADMIN KEY ───────────────────────────────
// Usada apenas para criar/listar usuarios
if (isProd && !process.env.API_SECRET) {
  throw new Error('Defina API_SECRET em producao');
}
const ADMIN_KEY = process.env.API_SECRET || 'admin-key-troque-isso';

function adminAuth(req, res, next) {
  if (req.headers['x-api-key'] !== ADMIN_KEY)
    return res.status(401).json({ error: 'Admin key invalida' });
  next();
}

// ── USER AUTH ────────────────────────────────
// Cada requisicao de dados usa a api_key do usuario
async function userAuth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key) return res.status(401).json({ error: 'Chave nao informada' });

  // Admin key tambem funciona como usuario admin
  if (key === ADMIN_KEY) {
    try {
      const { rows } = await pool.query(
        'SELECT * FROM users WHERE is_admin = TRUE LIMIT 1'
      );
      if (!rows.length) return res.status(401).json({ error: 'Admin user nao existe' });
      req.user = rows[0];
      return next();
    } catch (e) {
      return res.status(500).json({ error: e.message });
    }
  }

  try {
    const { rows } = await pool.query(
      'SELECT * FROM users WHERE api_key = $1', [key]
    );
    if (!rows.length) return res.status(401).json({ error: 'Chave invalida' });
    req.user = rows[0];
    next();
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}

// ── HEALTH ───────────────────────────────────
app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', timestamp: new Date(), multiUser: true });
  } catch {
    res.status(500).json({ status: 'error' });
  }
});

// ════════════════════════════════════════════
// USUARIOS
// ════════════════════════════════════════════

// Verificar propria chave e retornar info do usuario
app.get('/api/me', userAuth, (req, res) => {
  res.json({ id: req.user.id, name: req.user.name, is_admin: req.user.is_admin });
});

// Criar usuario (requer admin key)
app.post('/api/users', adminAuth, async (req, res) => {
  try {
    const { name = 'Usuario' } = req.body;
    const api_key = crypto.randomBytes(32).toString('hex');
    const { rows } = await pool.query(
      'INSERT INTO users (name, api_key) VALUES ($1, $2) RETURNING id, name, api_key, created_at',
      [name, api_key]
    );
    res.status(201).json(rows[0]);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Criar usuario admin inicial (so funciona se nao existir nenhum admin)
app.post('/api/setup', adminAuth, async (req, res) => {
  try {
    const { rows: existing } = await pool.query('SELECT id FROM users WHERE is_admin = TRUE');
    if (existing.length) return res.status(409).json({ error: 'Admin ja existe', id: existing[0].id });

    const { name = 'Admin' } = req.body;
    const api_key = crypto.randomBytes(32).toString('hex');
    const { rows } = await pool.query(
      'INSERT INTO users (name, api_key, is_admin) VALUES ($1, $2, TRUE) RETURNING id, name, api_key',
      [name, api_key]
    );
    res.status(201).json({ message: 'Admin criado', ...rows[0] });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Listar usuarios (admin)
app.get('/api/users', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, name, is_admin, created_at FROM users ORDER BY created_at'
    );
    res.json(rows);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Deletar usuario (admin)
app.delete('/api/users/:id', adminAuth, async (req, res) => {
  try {
    await pool.query('DELETE FROM users WHERE id = $1', [req.params.id]);
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Regenerar api_key do proprio usuario
app.post('/api/me/regenerate-key', userAuth, async (req, res) => {
  try {
    const new_key = crypto.randomBytes(32).toString('hex');
    await pool.query('UPDATE users SET api_key = $1 WHERE id = $2', [new_key, req.user.id]);
    res.json({ api_key: new_key });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ════════════════════════════════════════════
// TRANSACOES
// ════════════════════════════════════════════

app.get('/api/transactions', userAuth, async (req, res) => {
  try {
    const { type, category, month, year, search } = req.query;
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 500, 1), 1000);
    const offset = Math.max(parseInt(req.query.offset, 10) || 0, 0);
    const conds = ['user_id = $1'];
    const params = [req.user.id];
    let i = 2;

    if (type)     { conds.push(`type = $${i++}`);     params.push(type); }
    if (category) { conds.push(`category = $${i++}`); params.push(category); }
    if (month && year) {
      conds.push(`EXTRACT(MONTH FROM date) = $${i++}`); params.push(month);
      conds.push(`EXTRACT(YEAR FROM date)  = $${i++}`); params.push(year);
    }
    if (search) { conds.push(`description ILIKE $${i++}`); params.push(`%${search}%`); }

    const where = 'WHERE ' + conds.join(' AND ');
    const q = `SELECT * FROM transactions ${where} ORDER BY date DESC, created_at DESC LIMIT $${i++} OFFSET $${i++}`;
    params.push(limit, offset);

    const [{ rows }, { rows: tot }] = await Promise.all([
      pool.query(q, params),
      pool.query(`SELECT COUNT(*) FROM transactions ${where}`, params.slice(0, -2))
    ]);
    res.json({ data: rows, total: parseInt(tot[0].count) });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/transactions', userAuth, async (req, res) => {
  try {
    const { type, description, amount, category, date, note = '',
            account_id, paid=false, pending=false,
            installment_group, installment_num, installment_total, recur_group } = req.body;
    if (!type || !description || !amount || !category || !date)
      return res.status(400).json({ error: 'Campos obrigatorios faltando' });

    const { rows } = await pool.query(
      `INSERT INTO transactions
        (user_id, type, description, amount, category, date, note,
         account_id, paid, pending, installment_group, installment_num, installment_total, recur_group)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14) RETURNING *`,
      [req.user.id, type, description, amount, category, date, note,
       account_id||null, !!paid, !!pending,
       installment_group||null, installment_num||null, installment_total||null, recur_group||null]
    );
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.put('/api/transactions/:id', userAuth, async (req, res) => {
  try {
    const { type, description, amount, category, date, note, account_id, paid, pending } = req.body;
    const { rows } = await pool.query(
      `UPDATE transactions
       SET type=$1,description=$2,amount=$3,category=$4,date=$5,note=$6,
           account_id=$7,paid=COALESCE($8,paid),pending=COALESCE($9,pending)
       WHERE id=$10 AND user_id=$11 RETURNING *`,
      [type, description, amount, category, date, note, account_id||null,
       typeof paid === 'boolean' ? paid : null,
       typeof pending === 'boolean' ? pending : null,
       req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Nao encontrado' });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/transactions/:id', userAuth, async (req, res) => {
  try {
    const { rowCount } = await pool.query(
      'DELETE FROM transactions WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]
    );
    if (!rowCount) return res.status(404).json({ error: 'Nao encontrado' });
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ============================================================================
// ESTADO COMPLETO DO APP
// ============================================================================

app.get('/api/state', userAuth, async (req, res) => {
  try {
    const uid = req.user.id;
    const [accounts, categories, lists, items, settings] = await Promise.all([
      pool.query('SELECT * FROM accounts WHERE user_id=$1 ORDER BY created_at, name', [uid]),
      pool.query('SELECT * FROM categories WHERE user_id=$1 ORDER BY created_at, name', [uid]),
      pool.query('SELECT * FROM shopping_lists WHERE user_id=$1 ORDER BY position, created_at', [uid]),
      pool.query('SELECT * FROM shopping_items WHERE user_id=$1 ORDER BY created_at', [uid]),
      pool.query('SELECT * FROM user_settings WHERE user_id=$1', [uid])
    ]);
    res.json({
      accounts: accounts.rows,
      categories: categories.rows,
      shopping: { lists: lists.rows, items: items.rows },
      settings: settings.rows[0] || {}
    });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.put('/api/state', userAuth, async (req, res) => {
  const client = await pool.connect();
  try {
    const uid = req.user.id;
    const { accounts=[], categories=[], shopping={}, settings={} } = req.body || {};
    await client.query('BEGIN');

    await replaceRows(client, 'accounts', uid, accounts,
      `INSERT INTO accounts
       (user_id,id,name,icon,type,balance,yield_rate,yield_type,yield_val,calc_base,start_date,note)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)`,
      (a, userId) => [userId, String(a.id), cleanText(a.name, 'Conta'), cleanText(a.icon),
        cleanText(a.type, 'checking'), Number(a.balance)||0, Number(a.yieldRate ?? a.yield_rate)||0,
        cleanText(a.yieldType ?? a.yield_type, 'manual'), Number(a.yieldVal ?? a.yield_val)||0,
        cleanText(a.calcBase ?? a.calc_base, 'du'), a.startDate || a.start_date || null, cleanText(a.note)]
    );

    await replaceRows(client, 'categories', uid, categories,
      `INSERT INTO categories (user_id,id,icon,name,color) VALUES ($1,$2,$3,$4,$5)`,
      (c, userId) => [userId, String(c.id), cleanText(c.ico || c.icon), cleanText(c.name, 'Categoria'), cleanText(c.col || c.color, '#888')]
    );

    await replaceRows(client, 'shopping_items', uid, [], 'SELECT $1', () => [uid]);
    await replaceRows(client, 'shopping_lists', uid, shopping.lists || [],
      `INSERT INTO shopping_lists (user_id,id,name,icon,position) VALUES ($1,$2,$3,$4,$5)`,
      (l, userId) => [userId, String(l.id), cleanText(l.name, 'Lista'), cleanText(l.ico || l.icon), Number(l.position)||0]
    );
    for (const item of shopping.items || []) {
      await client.query(
        `INSERT INTO shopping_items (user_id,id,list_id,name,qty,category,bought,created_ms)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
        [uid, String(item.id), String(item.listId || item.list_id), cleanText(item.name, 'Item'),
         cleanText(item.qty), cleanText(item.cat || item.category), !!item.bought, Number(item.createdAt || item.created_ms)||Date.now()]
      );
    }

    await client.query(
      `INSERT INTO user_settings (user_id,theme,rates,widget_prefs,widget_order,tx_view,active_list)
       VALUES ($1,$2,$3,$4,$5,$6,$7)
       ON CONFLICT (user_id) DO UPDATE SET
         theme=EXCLUDED.theme,
         rates=EXCLUDED.rates,
         widget_prefs=EXCLUDED.widget_prefs,
         widget_order=EXCLUDED.widget_order,
         tx_view=EXCLUDED.tx_view,
         active_list=EXCLUDED.active_list,
         updated_at=NOW()`,
      [uid, cleanText(settings.theme, 'dark'), settings.rates || {},
       settings.widgetPrefs || settings.widget_prefs || {},
       settings.widgetOrder || settings.widget_order || [],
       cleanText(settings.txView || settings.tx_view, 'n'),
       settings.activeList || settings.active_list || null]
    );

    await client.query('COMMIT');
    res.json({ success: true });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: e.message });
  } finally {
    client.release();
  }
});

// ════════════════════════════════════════════
// ORCAMENTOS
// ════════════════════════════════════════════

app.get('/api/budgets', userAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM budgets WHERE user_id=$1 ORDER BY category', [req.user.id]
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/budgets', userAuth, async (req, res) => {
  try {
    const { category, limit } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO budgets (user_id, category, "limit") VALUES ($1,$2,$3)
       ON CONFLICT (user_id, category) DO UPDATE SET "limit"=$3, updated_at=NOW()
       RETURNING *`,
      [req.user.id, category, limit]
    );
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/budgets/:id', userAuth, async (req, res) => {
  try {
    await pool.query('DELETE FROM budgets WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// METAS
// ════════════════════════════════════════════

app.get('/api/goals', userAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM goals WHERE user_id=$1 ORDER BY deadline', [req.user.id]
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/goals', userAuth, async (req, res) => {
  try {
    const { name, icon='🎯', target, current=0, deadline, description='', monthly=0 } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO goals (user_id,name,icon,target,current,deadline,description,monthly)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,
      [req.user.id, name, icon, target, current, deadline, description, monthly]
    );
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.patch('/api/goals/:id/add', userAuth, async (req, res) => {
  try {
    const { amount } = req.body;
    const { rows } = await pool.query(
      `UPDATE goals SET current = LEAST(current+$1, target)
       WHERE id=$2 AND user_id=$3 RETURNING *`,
      [amount, req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Nao encontrado' });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/goals/:id', userAuth, async (req, res) => {
  try {
    await pool.query('DELETE FROM goals WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// RESUMO / DASHBOARD
// ════════════════════════════════════════════

app.get('/api/summary', userAuth, async (req, res) => {
  try {
    const uid = req.user.id;
    const m = req.query.month || new Date().getMonth() + 1;
    const y = req.query.year  || new Date().getFullYear();

    const [summary, byCategory, lastMonths] = await Promise.all([
      pool.query(`
        SELECT
          SUM(CASE WHEN type='income'  THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense,
          SUM(CASE WHEN type='income'  THEN amount ELSE -amount END) AS net
        FROM transactions
        WHERE user_id=$1
          AND EXTRACT(MONTH FROM date)=$2
          AND EXTRACT(YEAR FROM date)=$3
      `, [uid, m, y]),

      pool.query(`
        SELECT category, SUM(amount) AS total, COUNT(*) AS count
        FROM transactions
        WHERE user_id=$1 AND type='expense'
          AND EXTRACT(MONTH FROM date)=$2
          AND EXTRACT(YEAR FROM date)=$3
        GROUP BY category ORDER BY total DESC
      `, [uid, m, y]),

      pool.query(`
        SELECT
          EXTRACT(MONTH FROM date)::int AS month,
          EXTRACT(YEAR FROM date)::int  AS year,
          SUM(CASE WHEN type='income'  THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE user_id=$1 AND date >= NOW() - INTERVAL '6 months'
        GROUP BY year, month ORDER BY year, month
      `, [uid])
    ]);

    res.json({
      period: { month: m, year: y },
      summary: summary.rows[0],
      byCategory: byCategory.rows,
      lastMonths: lastMonths.rows
    });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// BACKUP
// ════════════════════════════════════════════

app.post('/api/backup', userAuth, async (req, res) => {
  if (!req.user.is_admin) return res.status(403).json({ error: 'Apenas admin pode fazer backup' });
  try {
    const { execSync } = require('child_process');
    const dir  = process.env.BACKUP_DIR || './backups';
    const file = `finanza_${Date.now()}.sql.gz`;
    const fp   = path.join(dir, file);
    fs.mkdirSync(dir, { recursive: true });
    const url  = new URL(process.env.DATABASE_URL);
    execSync(`PGPASSWORD="${url.password}" pg_dump -h ${url.hostname} -U ${url.username} ${url.pathname.slice(1)} | gzip > ${fp}`);
    const size = fs.statSync(fp).size;
    await pool.query('INSERT INTO backup_log (filename, size_bytes) VALUES ($1,$2)', [file, size]);
    res.json({ success: true, filename: file, size_bytes: size });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.listen(PORT, () => console.log(`Finanza API na porta ${PORT} - multi-usuario`));
