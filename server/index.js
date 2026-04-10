// ============================================
// Finanza API v3 — Multi-usuário
// Node.js + Express + PostgreSQL (Supabase/Render/Neon)
// ============================================

'use strict';

const express = require('express');
const cors    = require('cors');
const { Pool } = require('pg');
const crypto  = require('crypto');

const app  = express();
const PORT = process.env.PORT || 3000;

// ── DB ──────────────────────────────────────
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_URL?.includes('localhost') ? false : { rejectUnauthorized: false },
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

pool.connect()
  .then(c => { console.log('✅ PostgreSQL conectado'); c.release(); })
  .catch(e => { console.error('❌ DB erro:', e.message); process.exit(1); });

// ── MIDDLEWARES ─────────────────────────────
app.use(cors({ origin: '*', methods: ['GET','POST','PUT','PATCH','DELETE','OPTIONS'] }));
app.use(express.json({ limit: '2mb' }));
app.use((req, _, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

// ── AUTH ─────────────────────────────────────
const ADMIN_SECRET = process.env.API_SECRET || 'TROQUE-ISSO-EM-PRODUCAO';

function adminAuth(req, res, next) {
  if (req.headers['x-api-key'] !== ADMIN_SECRET)
    return res.status(401).json({ error: 'Admin key inválida' });
  next();
}

async function userAuth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key) return res.status(401).json({ error: 'Chave não informada' });

  // Admin key funciona como super-user
  if (key === ADMIN_SECRET) {
    try {
      const { rows } = await pool.query('SELECT * FROM users WHERE is_admin = TRUE LIMIT 1');
      if (!rows.length) return res.status(401).json({ error: 'Admin user não configurado. Use POST /api/setup' });
      req.user = rows[0];
      return next();
    } catch (e) { return res.status(500).json({ error: e.message }); }
  }

  try {
    const { rows } = await pool.query('SELECT * FROM users WHERE api_key = $1', [key]);
    if (!rows.length) return res.status(401).json({ error: 'Chave inválida' });
    req.user = rows[0];
    next();
  } catch (e) { res.status(500).json({ error: e.message }); }
}

// ── HEALTH ───────────────────────────────────
app.get('/health', async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT COUNT(*) FROM users');
    res.json({ status: 'ok', users: parseInt(rows[0].count), timestamp: new Date() });
  } catch {
    res.status(500).json({ status: 'error' });
  }
});

// ════════════════════════════════════════════
// SETUP INICIAL
// ════════════════════════════════════════════

// Cria tabelas se não existirem + usuário admin inicial
app.post('/api/setup', adminAuth, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // Criar extensão e tabelas
    await client.query(`CREATE EXTENSION IF NOT EXISTS "pgcrypto"`);

    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        name        VARCHAR(100) NOT NULL DEFAULT 'Usuário',
        email       VARCHAR(255) UNIQUE,
        api_key     VARCHAR(128) NOT NULL UNIQUE,
        is_admin    BOOLEAN DEFAULT FALSE,
        created_at  TIMESTAMPTZ DEFAULT NOW(),
        last_seen   TIMESTAMPTZ
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS transactions (
        id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        type              VARCHAR(10) NOT NULL CHECK (type IN ('income','expense')),
        description       TEXT NOT NULL,
        amount            NUMERIC(12,2) NOT NULL CHECK (amount > 0),
        category          VARCHAR(100) NOT NULL,
        date              DATE NOT NULL,
        note              TEXT DEFAULT '',
        installment_group UUID,
        installment_num   INT,
        installment_total INT,
        recur_group       UUID,
        paid              BOOLEAN DEFAULT FALSE,
        pending           BOOLEAN DEFAULT FALSE,
        created_at        TIMESTAMPTZ DEFAULT NOW(),
        updated_at        TIMESTAMPTZ DEFAULT NOW()
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS budgets (
        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        category    VARCHAR(100) NOT NULL,
        "limit"     NUMERIC(12,2) NOT NULL CHECK ("limit" > 0),
        created_at  TIMESTAMPTZ DEFAULT NOW(),
        UNIQUE(user_id, category)
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS goals (
        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name        VARCHAR(100) NOT NULL,
        icon        VARCHAR(10) DEFAULT '🎯',
        target      NUMERIC(12,2) NOT NULL CHECK (target > 0),
        current     NUMERIC(12,2) DEFAULT 0 CHECK (current >= 0),
        deadline    DATE NOT NULL,
        description TEXT DEFAULT '',
        monthly     NUMERIC(12,2) DEFAULT 0,
        created_at  TIMESTAMPTZ DEFAULT NOW()
      )
    `);

    await client.query(`
      CREATE TABLE IF NOT EXISTS accounts (
        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name        VARCHAR(100) NOT NULL,
        icon        VARCHAR(10) DEFAULT '🏦',
        type        VARCHAR(20) DEFAULT 'checking',
        balance     NUMERIC(12,2) DEFAULT 0,
        created_at  TIMESTAMPTZ DEFAULT NOW()
      )
    `);

    // Índices
    const indexes = [
      'CREATE INDEX IF NOT EXISTS idx_tx_user ON transactions(user_id)',
      'CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions(date DESC)',
      'CREATE INDEX IF NOT EXISTS idx_tx_type ON transactions(type)',
      'CREATE INDEX IF NOT EXISTS idx_bud_user ON budgets(user_id)',
      'CREATE INDEX IF NOT EXISTS idx_goal_user ON goals(user_id)',
      'CREATE INDEX IF NOT EXISTS idx_acc_user ON accounts(user_id)',
    ];
    for (const idx of indexes) await client.query(idx);

    // Trigger updated_at
    await client.query(`
      CREATE OR REPLACE FUNCTION update_updated_at()
      RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql
    `);
    await client.query(`
      DROP TRIGGER IF EXISTS trg_tx ON transactions;
      CREATE TRIGGER trg_tx BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at()
    `);

    // Criar admin se não existir
    const { rows: existing } = await client.query('SELECT id, api_key FROM users WHERE is_admin = TRUE LIMIT 1');
    let adminKey, adminId;

    if (existing.length) {
      adminKey = existing[0].api_key;
      adminId  = existing[0].id;
      await client.query('COMMIT');
      return res.json({ message: 'Setup já feito', admin_key: adminKey, admin_id: adminId, existing: true });
    }

    const { name = 'Admin' } = req.body;
    adminKey = crypto.randomBytes(32).toString('hex');
    const { rows } = await client.query(
      'INSERT INTO users (name, api_key, is_admin) VALUES ($1, $2, TRUE) RETURNING id, api_key',
      [name, adminKey]
    );
    adminId = rows[0].id;

    await client.query('COMMIT');
    res.status(201).json({
      message: '✅ Setup completo!',
      admin_key: adminKey,
      admin_id: adminId,
      instructions: 'Guarde o admin_key. Use x-api-key: <admin_key> para criar novos usuários em POST /api/users'
    });
  } catch (e) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: e.message });
  } finally { client.release(); }
});

// ════════════════════════════════════════════
// USUÁRIOS
// ════════════════════════════════════════════

app.get('/api/me', userAuth, async (req, res) => {
  await pool.query('UPDATE users SET last_seen = NOW() WHERE id = $1', [req.user.id]);
  res.json({ id: req.user.id, name: req.user.name, email: req.user.email, is_admin: req.user.is_admin });
});

app.post('/api/users', adminAuth, async (req, res) => {
  try {
    const { name = 'Usuário', email } = req.body;
    if (!name?.trim()) return res.status(400).json({ error: 'Nome obrigatório' });

    const api_key = crypto.randomBytes(32).toString('hex');
    const { rows } = await pool.query(
      'INSERT INTO users (name, email, api_key) VALUES ($1, $2, $3) RETURNING id, name, email, api_key, created_at',
      [name.trim(), email || null, api_key]
    );
    res.status(201).json(rows[0]);
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Email já existe' });
    res.status(500).json({ error: e.message });
  }
});

app.get('/api/users', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, name, email, is_admin, created_at, last_seen FROM users ORDER BY created_at'
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/users/:id', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT is_admin FROM users WHERE id = $1', [req.params.id]);
    if (!rows.length) return res.status(404).json({ error: 'Usuário não encontrado' });
    if (rows[0].is_admin) return res.status(403).json({ error: 'Não é possível remover o admin' });
    await pool.query('DELETE FROM users WHERE id = $1', [req.params.id]);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/me/regenerate-key', userAuth, async (req, res) => {
  try {
    const new_key = crypto.randomBytes(32).toString('hex');
    await pool.query('UPDATE users SET api_key = $1 WHERE id = $2', [new_key, req.user.id]);
    res.json({ api_key: new_key, message: 'Nova chave gerada. Salve agora — não pode ser recuperada.' });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// TRANSAÇÕES
// ════════════════════════════════════════════

app.get('/api/transactions', userAuth, async (req, res) => {
  try {
    const { type, category, search, limit = 1000, offset = 0 } = req.query;
    const conds = ['user_id = $1'];
    const params = [req.user.id];
    let i = 2;
    if (type)     { conds.push(`type = $${i++}`);              params.push(type); }
    if (category) { conds.push(`category = $${i++}`);          params.push(category); }
    if (search)   { conds.push(`description ILIKE $${i++}`);   params.push(`%${search}%`); }

    const where = 'WHERE ' + conds.join(' AND ');
    const q = `SELECT * FROM transactions ${where} ORDER BY date DESC, created_at DESC LIMIT $${i++} OFFSET $${i++}`;
    params.push(Number(limit), Number(offset));

    const [{ rows }, { rows: tot }] = await Promise.all([
      pool.query(q, params),
      pool.query(`SELECT COUNT(*) FROM transactions ${where}`, params.slice(0, -2))
    ]);
    res.json({ data: rows, total: parseInt(tot[0].count) });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/transactions', userAuth, async (req, res) => {
  try {
    const {
      type, description, amount, category, date, note = '',
      installment_group, installment_num, installment_total,
      recur_group, paid = false, pending = false
    } = req.body;

    if (!type || !description || !amount || !category || !date)
      return res.status(400).json({ error: 'Campos obrigatórios: type, description, amount, category, date' });

    const { rows } = await pool.query(
      `INSERT INTO transactions
        (user_id, type, description, amount, category, date, note,
         installment_group, installment_num, installment_total, recur_group, paid, pending)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13) RETURNING *`,
      [req.user.id, type, description, amount, category, date, note,
       installment_group || null, installment_num || null, installment_total || null,
       recur_group || null, paid, pending]
    );
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.put('/api/transactions/:id', userAuth, async (req, res) => {
  try {
    const { type, description, amount, category, date, note, paid, pending } = req.body;
    const { rows } = await pool.query(
      `UPDATE transactions
       SET type=$1, description=$2, amount=$3, category=$4, date=$5, note=$6,
           paid=COALESCE($7,paid), pending=COALESCE($8,pending)
       WHERE id=$9 AND user_id=$10 RETURNING *`,
      [type, description, amount, category, date, note, paid, pending, req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.patch('/api/transactions/:id/paid', userAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'UPDATE transactions SET paid = NOT paid, pending = false WHERE id=$1 AND user_id=$2 RETURNING *',
      [req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/transactions/:id', userAuth, async (req, res) => {
  try {
    const { rowCount } = await pool.query(
      'DELETE FROM transactions WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]
    );
    if (!rowCount) return res.status(404).json({ error: 'Não encontrado' });
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// ORÇAMENTOS
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
    if (!category || !limit) return res.status(400).json({ error: 'category e limit obrigatórios' });
    const { rows } = await pool.query(
      `INSERT INTO budgets (user_id, category, "limit") VALUES ($1,$2,$3)
       ON CONFLICT (user_id, category) DO UPDATE SET "limit"=$3 RETURNING *`,
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
    const { name, icon = '🎯', target, current = 0, deadline, description = '', monthly = 0 } = req.body;
    if (!name || !target || !deadline) return res.status(400).json({ error: 'name, target, deadline obrigatórios' });
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
    if (!amount || amount <= 0) return res.status(400).json({ error: 'amount inválido' });
    const { rows } = await pool.query(
      `UPDATE goals SET current = LEAST(current + $1, target)
       WHERE id=$2 AND user_id=$3 RETURNING *`,
      [amount, req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
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
// CONTAS BANCÁRIAS (server-side)
// ════════════════════════════════════════════

app.get('/api/accounts', userAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM accounts WHERE user_id=$1 ORDER BY created_at', [req.user.id]
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/accounts', userAuth, async (req, res) => {
  try {
    const { name, icon = '🏦', type = 'checking', balance = 0 } = req.body;
    if (!name) return res.status(400).json({ error: 'name obrigatório' });
    const { rows } = await pool.query(
      'INSERT INTO accounts (user_id,name,icon,type,balance) VALUES ($1,$2,$3,$4,$5) RETURNING *',
      [req.user.id, name, icon, type, balance]
    );
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/accounts/:id', userAuth, async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT id FROM accounts WHERE user_id=$1', [req.user.id]);
    if (rows.length <= 1) return res.status(400).json({ error: 'Mínimo 1 conta' });
    await pool.query('DELETE FROM accounts WHERE id=$1 AND user_id=$2', [req.params.id, req.user.id]);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ════════════════════════════════════════════
// EXPORTAR / BACKUP
// ════════════════════════════════════════════

app.get('/api/export', userAuth, async (req, res) => {
  try {
    const [txs, buds, goals, accs] = await Promise.all([
      pool.query('SELECT * FROM transactions WHERE user_id=$1 ORDER BY date DESC', [req.user.id]),
      pool.query('SELECT * FROM budgets WHERE user_id=$1', [req.user.id]),
      pool.query('SELECT * FROM goals WHERE user_id=$1', [req.user.id]),
      pool.query('SELECT * FROM accounts WHERE user_id=$1', [req.user.id]),
    ]);
    res.json({
      exported_at: new Date().toISOString(),
      user: { id: req.user.id, name: req.user.name },
      transactions: txs.rows,
      budgets: buds.rows,
      goals: goals.rows,
      accounts: accs.rows,
    });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ── START ────────────────────────────────────
app.listen(PORT, () => {
  console.log(`🚀 Finanza API v3 — porta ${PORT}`);
  console.log(`   Multi-usuário | PostgreSQL | Node ${process.version}`);
  console.log(`   Primeiro uso: POST /api/setup com x-api-key: <API_SECRET>`);
});
