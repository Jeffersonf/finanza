// ============================================
// Finanza API - Multi-Usuário v2
// Node.js + Express + PostgreSQL
// ============================================

const express = require('express');
const cors    = require('cors');
const { Pool } = require('pg');
const crypto  = require('crypto');
const fs      = require('fs');
const path    = require('path');
const { cleanText, normalizeRole, userRole, canWrite, publicUser } = require('./permissions');
const { parseTransactionText } = require('./transactionParser');
const { normalizeBackupPayload, backupImportCounts } = require('./backupSchema');

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
async function ensureOperationalSchema() {
  await pool.query(`
    ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'editor';
    UPDATE users SET role = 'admin' WHERE is_admin = TRUE AND role <> 'admin';
    CREATE TABLE IF NOT EXISTS audit_events (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
      actor_name  TEXT DEFAULT '',
      actor_role  VARCHAR(20) DEFAULT '',
      action      VARCHAR(80) NOT NULL,
      entity      VARCHAR(80) NOT NULL,
      entity_id   TEXT,
      detail      TEXT DEFAULT '',
      metadata    JSONB DEFAULT '{}'::jsonb,
      created_at  TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE INDEX IF NOT EXISTS idx_audit_user_time ON audit_events(user_id, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_events(entity, entity_id);
  `);
}

ensureOperationalSchema().catch(e => console.error('Schema bootstrap erro:', e.message));

const allowedOrigins = (process.env.CORS_ORIGIN || '')
  .split(',')
  .map(s => s.trim())
  .filter(Boolean);

app.use(cors({
  origin(origin, cb) {
    if (!origin || !allowedOrigins.length || allowedOrigins.includes(origin)) return cb(null, true);
    cb(new Error('Origem não permitida'));
  },
  methods: ['GET','POST','PUT','PATCH','DELETE'],
  allowedHeaders: ['Content-Type','x-api-key']
}));
app.use(express.json({ limit: '1mb' }));

function toJsonb(value, fallback) {
  try {
    return JSON.stringify(value ?? fallback);
  } catch {
    return JSON.stringify(fallback);
  }
}

function normalizeCategoryName(v) {
  return ({
    Salario: 'Salário',
    Alimentacao: 'Alimentação',
    Saude: 'Saúde',
    Educacao: 'Educação',
    Poupanca: 'Poupança'
  }[v]) || v;
}

function normalizeUsername(v) {
  return cleanText(v).trim().toLowerCase();
}

function requireWrite(req, res, next) {
  if (!canWrite(req.user)) return res.status(403).json({ error: 'Perfil sem permissao de edicao' });
  next();
}

async function auditEvent(db, user, action, entity, entityId = null, detail = '', metadata = {}) {
  try {
    await db.query(
      `INSERT INTO audit_events
        (user_id, actor_name, actor_role, action, entity, entity_id, detail, metadata)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8::jsonb)`,
      [
        user?.id || null,
        cleanText(user?.name || user?.username || ''),
        userRole(user),
        cleanText(action, 'acao'),
        cleanText(entity, 'registro'),
        entityId == null ? null : String(entityId),
        cleanText(detail),
        toJsonb(metadata, {})
      ]
    );
  } catch (e) {
    console.error('Falha ao gravar auditoria:', e.message);
  }
}

function inferCategoryFromText(text, type) {
  if (type === 'income') {
    if (/(sal[aá]rio|pagamento)/.test(text)) return 'SalÃ¡rio';
    if (/(freela|freelance)/.test(text)) return 'Freelance';
    if (/(invest|rendimento)/.test(text)) return 'Investimentos';
    return 'Outros';
  }
  if (/(mercado|ifood|restaurante|lanche|padaria|comida|delivery)/.test(text)) return 'AlimentaÃ§Ã£o';
  if (/(uber|99|taxi|gasolina|combust[ií]vel|[oô]nibus)/.test(text)) return 'Transporte';
  if (/(aluguel|luz|[aá]gua|internet|condom[ií]nio|casa)/.test(text)) return 'Casa';
  if (/(farm[aá]cia|rem[eé]dio|m[eé]dico|sa[uú]de)/.test(text)) return 'SaÃºde';
  if (/(curso|livro|faculdade|educa[cç][aã]o)/.test(text)) return 'EducaÃ§Ã£o';
  if (/(cinema|bar|jogo|show|lazer)/.test(text)) return 'Lazer';
  return 'Outros';
}

function parseTransactionTextLegacy(text) {
  const raw = cleanText(text).trim();
  if (!raw) return null;
  const amountMatch = raw.match(/\d+(?:[.,]\d{1,2})?/);
  if (!amountMatch) return null;
  const amount = moneyToNumber(amountMatch[0]);
  if (!amount) return null;
  const lower = raw.toLowerCase();
  const type = /(recebi|receita|sal[aá]rio|freela|ganhei|entrada)/.test(lower) ? 'income' : 'expense';
  const now = new Date();
  const date = new Date(now);
  if (lower.includes('ontem')) date.setDate(date.getDate() - 1);
  if (lower.includes('amanha') || lower.includes('amanhÃ£')) date.setDate(date.getDate() + 1);
  const isoDate = date.toISOString().slice(0, 10);
  const pending = type === 'expense' && /(vence|venc|pagar|a pagar|amanha|amanhÃ£)/.test(lower);
  const category = inferCategoryFromText(lower, type);
  const description = raw
    .replace(amountMatch[0], '')
    .replace(/\b(gastei|paguei|comprei|recebi|hoje|ontem|amanh[aã]|no|na|em|de|r\$)\b/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim() || category;
  return {
    type,
    amount,
    amountCents: Math.round(amount * 100),
    description: description.charAt(0).toUpperCase() + description.slice(1),
    category,
    date: isoDate,
    pending
  };
}

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(String(password), salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}

function verifyPassword(password, stored) {
  if (!stored) return false;
  const [algo, salt, hash] = String(stored).split('$');
  if (algo !== 'scrypt' || !salt || !hash) return false;
  const calc = crypto.scryptSync(String(password), salt, 64);
  return crypto.timingSafeEqual(Buffer.from(hash, 'hex'), calc);
}

async function replaceRows(client, table, userId, rows, insertSql, mapper) {
  await client.query(`DELETE FROM ${table} WHERE user_id=$1`, [userId]);
  for (const row of rows || []) {
    await client.query(insertSql, mapper(row, userId));
  }
}

async function replaceAppState(client, userId, state = {}) {
  const { accounts=[], categories=[], shopping={}, settings={} } = state || {};
  const rates = { ...(settings.rates || {}) };
  if (Array.isArray(state.dueItems) && !Array.isArray(rates.dueItems)) rates.dueItems = state.dueItems;

  await replaceRows(client, 'accounts', userId, accounts,
    `INSERT INTO accounts
     (user_id,id,name,icon,type,balance,yield_rate,yield_type,yield_val,calc_base,start_date,note)
     VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)`,
    (a, uid) => [uid, String(a.id), cleanText(a.name, 'Conta'), cleanText(a.icon),
      cleanText(a.type, 'checking'), Number(a.balance)||0, Number(a.yieldRate ?? a.yield_rate)||0,
      cleanText(a.yieldType ?? a.yield_type, 'manual'), Number(a.yieldVal ?? a.yield_val)||0,
      cleanText(a.calcBase ?? a.calc_base, 'du'), a.startDate || a.start_date || null, cleanText(a.note)]
  );

  await replaceRows(client, 'categories', userId, categories,
    `INSERT INTO categories (user_id,id,icon,name,color) VALUES ($1,$2,$3,$4,$5)`,
    (c, uid) => [uid, String(c.id), cleanText(c.ico || c.icon), normalizeCategoryName(cleanText(c.name, 'Categoria')), cleanText(c.col || c.color, '#888')]
  );

  await replaceRows(client, 'shopping_items', userId, [], 'SELECT $1', uid => [uid]);
  await replaceRows(client, 'shopping_lists', userId, shopping.lists || [],
    `INSERT INTO shopping_lists (user_id,id,name,icon,position) VALUES ($1,$2,$3,$4,$5)`,
    (l, uid) => [uid, String(l.id), cleanText(l.name, 'Lista'), cleanText(l.ico || l.icon), Number(l.position)||0]
  );
  for (const item of shopping.items || []) {
    await client.query(
      `INSERT INTO shopping_items (user_id,id,list_id,name,qty,category,bought,created_ms)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      [userId, String(item.id), String(item.listId || item.list_id), cleanText(item.name, 'Item'),
       cleanText(item.qty), cleanText(item.cat || item.category), !!item.bought, Number(item.createdAt || item.created_ms)||Date.now()]
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

// ── ADMIN KEY ───────────────────────────────
// Usada apenas para criar/listar usuários
if (isProd && !process.env.API_SECRET) {
  throw new Error('Defina API_SECRET em produção');
}
const ADMIN_KEY = process.env.API_SECRET || 'admin-key-troque-isso';

function adminAuth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key) return res.status(401).json({ error: 'Chave admin nao informada' });

  if (key === ADMIN_KEY) {
    return pool.query('SELECT * FROM users WHERE is_admin = TRUE LIMIT 1')
      .then(({ rows }) => {
        req.adminActor = rows[0] || { name: 'admin-key', role: 'admin', is_admin: true };
        next();
      })
      .catch(e => res.status(500).json({ error: e.message }));
  }

  return pool.query('SELECT * FROM users WHERE api_key = $1', [key])
    .then(({ rows }) => {
      if (!rows.length) return res.status(401).json({ error: 'Chave invalida' });
      if (userRole(rows[0]) !== 'admin') return res.status(403).json({ error: 'Apenas admin pode gerenciar usuarios' });
      req.user = rows[0];
      req.adminActor = rows[0];
      next();
    })
    .catch(e => res.status(500).json({ error: e.message }));

  if (req.headers['x-api-key'] !== ADMIN_KEY)
    return res.status(401).json({ error: 'Admin key inválida' });
  next();
}

// ── USER AUTH ────────────────────────────────
// Cada requisição de dados usa a api_key do usuário
async function userAuth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key) return res.status(401).json({ error: 'Chave não informada' });

  // Admin key também funciona como usuário admin
  if (key === ADMIN_KEY) {
    try {
      const { rows } = await pool.query(
        'SELECT * FROM users WHERE is_admin = TRUE LIMIT 1'
      );
      if (!rows.length) return res.status(401).json({ error: 'Admin user não existe' });
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
    if (!rows.length) return res.status(401).json({ error: 'Chave inválida' });
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

// Verificar própria chave e retornar info do usuário
app.get('/api/me', userAuth, (req, res) => {
  res.json(publicUser(req.user));
});

app.post('/api/login', async (req, res) => {
  try {
    const username = normalizeUsername(req.body?.username);
    const password = req.body?.password || '';
    if (!username || !password) return res.status(400).json({ error: 'Usuário e senha são obrigatórios' });
    const { rows } = await pool.query('SELECT * FROM users WHERE username=$1', [username]);
    if (!rows.length || !verifyPassword(password, rows[0].password_hash)) {
      return res.status(401).json({ error: 'Usuário ou senha inválidos' });
    }
    res.json({ ...publicUser(rows[0]), api_key: rows[0].api_key });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/password-reset', adminAuth, async (req, res) => {
  try {
    const username = normalizeUsername(req.body?.username);
    const password = req.body?.password || '';
    if (!username || !password) return res.status(400).json({ error: 'Usuário e nova senha são obrigatórios' });
    const { rows } = await pool.query(
      'UPDATE users SET password_hash=$1 WHERE username=$2 RETURNING id, name, username, role, is_admin, created_at',
      [hashPassword(password), username]
    );
    if (!rows.length) return res.status(404).json({ error: 'Usuário não encontrado' });
    await auditEvent(pool, req.adminActor, 'senha_redefinida', 'user', rows[0].id, rows[0].username || rows[0].name);
    res.json({ success: true, user: publicUser(rows[0]) });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Criar usuário sem admin key apenas no primeiro acesso ou quando ALLOW_SIGNUP=true
app.post('/api/register', async (req, res) => {
  try {
    const { rows: totalRows } = await pool.query('SELECT COUNT(*)::int AS total FROM users');
    const isFirstUser = totalRows[0].total === 0;
    if (!isFirstUser && process.env.ALLOW_SIGNUP !== 'true') {
      return res.status(403).json({ error: 'Cadastro direto desativado. Use a chave admin do servidor.' });
    }

    const { name = 'Usuário', password = '' } = req.body || {};
    const username = normalizeUsername(req.body?.username || name);
    if (!username || !password) return res.status(400).json({ error: 'Usuário e senha são obrigatórios' });
    const api_key = crypto.randomBytes(32).toString('hex');
    const role = normalizeRole(req.body?.role, isFirstUser);
    const { rows } = await pool.query(
      'INSERT INTO users (name, username, password_hash, api_key, role, is_admin) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, name, username, api_key, role, is_admin, created_at',
      [name, username, hashPassword(password), api_key, role, isFirstUser]
    );
    await auditEvent(pool, rows[0], 'usuario_criado', 'user', rows[0].id, isFirstUser ? 'primeiro admin' : 'cadastro direto');
    res.status(201).json(rows[0]);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Criar usuário (requer admin key)
app.post('/api/users', adminAuth, async (req, res) => {
  try {
    const { name = 'Usuário', password = '' } = req.body;
    const username = normalizeUsername(req.body?.username || name);
    if (!username || !password) return res.status(400).json({ error: 'Usuário e senha são obrigatórios' });
    const api_key = crypto.randomBytes(32).toString('hex');
    const role = normalizeRole(req.body?.role, false);
    const { rows } = await pool.query(
      'INSERT INTO users (name, username, password_hash, api_key, role, is_admin) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, name, username, api_key, role, is_admin, created_at',
      [name, username, hashPassword(password), api_key, role, role === 'admin']
    );
    await auditEvent(pool, req.adminActor, 'usuario_criado', 'user', rows[0].id, `${rows[0].username || rows[0].name} (${userRole(rows[0])})`);
    res.status(201).json(rows[0]);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Criar usuário admin inicial (só funciona se não existir nenhum admin)
app.post('/api/setup', adminAuth, async (req, res) => {
  try {
    const { rows: existing } = await pool.query('SELECT id FROM users WHERE is_admin = TRUE');
    if (existing.length) return res.status(409).json({ error: 'Admin já existe', id: existing[0].id });

    const { name = 'Admin', password = '' } = req.body;
    const username = normalizeUsername(req.body?.username || name);
    if (!username || !password) return res.status(400).json({ error: 'Usuário e senha são obrigatórios' });
    const { rows: existingUser } = await pool.query('SELECT id FROM users WHERE username=$1', [username]);
    if (existingUser.length) {
      const { rows } = await pool.query(
        `UPDATE users
         SET name=COALESCE(NULLIF($1,''), name),
             password_hash=$2,
             role='admin',
             is_admin=TRUE
         WHERE username=$3
         RETURNING id, name, username, api_key, role, is_admin, created_at`,
        [name, hashPassword(password), username]
      );
      await auditEvent(pool, req.adminActor || rows[0], 'usuario_promovido_admin', 'user', rows[0].id, rows[0].username || rows[0].name);
      return res.status(200).json({ message: 'Usuario promovido a admin', ...rows[0] });
    }

    const api_key = crypto.randomBytes(32).toString('hex');
    const { rows } = await pool.query(
      'INSERT INTO users (name, username, password_hash, api_key, role, is_admin) VALUES ($1, $2, $3, $4, $5, TRUE) RETURNING id, name, username, api_key, role, is_admin, created_at',
      [name, username, hashPassword(password), api_key, 'admin']
    );
    await auditEvent(pool, rows[0], 'admin_inicial_criado', 'user', rows[0].id, rows[0].username || rows[0].name);
    res.status(201).json({ message: 'Admin criado', ...rows[0] });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Listar usuários (admin)
app.get('/api/users', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, name, username, role, is_admin, created_at FROM users ORDER BY created_at'
    );
    res.json(rows.map(publicUser));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Deletar usuário (admin)
app.patch('/api/users/:id/role', adminAuth, async (req, res) => {
  try {
    const role = normalizeRole(req.body?.role, false);
    const { rows } = await pool.query(
      `UPDATE users
       SET role=$1, is_admin=$2
       WHERE id=$3
       RETURNING id, name, username, role, is_admin, created_at`,
      [role, role === 'admin', req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Usuario nao encontrado' });
    await auditEvent(pool, req.adminActor, 'papel_atualizado', 'user', rows[0].id, `${rows[0].username || rows[0].name} -> ${userRole(rows[0])}`);
    res.json(publicUser(rows[0]));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.delete('/api/users/:id', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query('DELETE FROM users WHERE id = $1 RETURNING id, name, username, role, is_admin', [req.params.id]);
    if (rows.length) await auditEvent(pool, req.adminActor, 'usuario_removido', 'user', rows[0].id, rows[0].username || rows[0].name);
    res.json({ success: true });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// Regenerar api_key do próprio usuário
app.post('/api/me/regenerate-key', userAuth, async (req, res) => {
  try {
    const new_key = crypto.randomBytes(32).toString('hex');
    await pool.query('UPDATE users SET api_key = $1 WHERE id = $2', [new_key, req.user.id]);
    await auditEvent(pool, req.user, 'api_key_regenerada', 'user', req.user.id);
    res.json({ api_key: new_key });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ════════════════════════════════════════════
// TRANSACOES
// ════════════════════════════════════════════

app.post('/api/transactions/parse', userAuth, async (req, res) => {
  const parsed = parseTransactionText(req.body?.text);
  if (!parsed) return res.status(400).json({ error: 'Texto nÃ£o reconhecido' });
  res.json(parsed);
});

app.get('/api/audit-log', userAuth, async (req, res) => {
  try {
    const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 80, 1), 200);
    const params = [limit];
    const where = userRole(req.user) === 'admin' ? '' : 'WHERE user_id=$2';
    if (where) params.push(req.user.id);
    const { rows } = await pool.query(
      `SELECT id,user_id,actor_name,actor_role,action,entity,entity_id,detail,metadata,created_at
       FROM audit_events ${where}
       ORDER BY created_at DESC
       LIMIT $1`,
      params
    );
    res.json(rows);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

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

app.post('/api/transactions', userAuth, requireWrite, async (req, res) => {
  try {
    const { type, description, amount, category, date, note = '',
            account_id, paid=false, pending=false,
            installment_group, installment_num, installment_total, recur_group } = req.body;
    if (!type || !description || !amount || !category || !date)
      return res.status(400).json({ error: 'Campos obrigatórios faltando' });

    const { rows } = await pool.query(
      `INSERT INTO transactions
        (user_id, type, description, amount, category, date, note,
         account_id, paid, pending, installment_group, installment_num, installment_total, recur_group)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14) RETURNING *`,
      [req.user.id, type, description, amount, normalizeCategoryName(category), date, note,
       account_id||null, !!paid, !!pending,
       installment_group||null, installment_num||null, installment_total||null, recur_group||null]
    );
    await auditEvent(pool, req.user, 'transacao_criada', 'transaction', rows[0].id, rows[0].description, { amount: rows[0].amount, category: rows[0].category });
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.put('/api/transactions/:id', userAuth, requireWrite, async (req, res) => {
  try {
    const { type, description, amount, category, date, note, account_id, paid, pending } = req.body;
    const { rows } = await pool.query(
      `UPDATE transactions
       SET type=$1,description=$2,amount=$3,category=$4,date=$5,note=$6,
           account_id=$7,paid=COALESCE($8,paid),pending=COALESCE($9,pending)
       WHERE id=$10 AND user_id=$11 RETURNING *`,
      [type, description, amount, normalizeCategoryName(category), date, note, account_id||null,
       typeof paid === 'boolean' ? paid : null,
       typeof pending === 'boolean' ? pending : null,
       req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    await auditEvent(pool, req.user, 'transacao_atualizada', 'transaction', rows[0].id, rows[0].description, { amount: rows[0].amount, category: rows[0].category });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/transactions/:id', userAuth, requireWrite, async (req, res) => {
  try {
    const { rows, rowCount } = await pool.query(
      'DELETE FROM transactions WHERE id=$1 AND user_id=$2 RETURNING id, description, amount, category', [req.params.id, req.user.id]
    );
    if (!rowCount) return res.status(404).json({ error: 'Não encontrado' });
    await auditEvent(pool, req.user, 'transacao_removida', 'transaction', rows[0].id, rows[0].description, { amount: rows[0].amount, category: rows[0].category });
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

app.put('/api/state', userAuth, requireWrite, async (req, res) => {
  const client = await pool.connect();
  try {
    const uid = req.user.id;
    await client.query('BEGIN');
    await replaceAppState(client, uid, req.body || {});
    await auditEvent(client, req.user, 'estado_salvo', 'state', uid, 'preferencias e dados auxiliares');
    await client.query('COMMIT');
    res.json({ success: true });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: e.message });
  } finally {
    client.release();
  }
});

app.put('/api/import', userAuth, requireWrite, async (req, res) => {
  const client = await pool.connect();
  try {
    const uid = req.user.id;
    const data = normalizeBackupPayload(req.body || {});
    const imported = backupImportCounts(data);
    await client.query('BEGIN');

    await replaceRows(client, 'transactions', uid, data.transactions || [],
      `INSERT INTO transactions
        (user_id,type,description,amount,category,date,note,account_id,paid,pending,
         installment_group,installment_num,installment_total,recur_group)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)`,
      (t, userId) => [userId, cleanText(t.type, 'expense'), cleanText(t.desc || t.description, 'Lançamento'),
        Number(t.amount)||0, normalizeCategoryName(cleanText(t.category, 'A classificar')), t.date, cleanText(t.note),
        t.accountId || t.account_id || null, !!t.paid, !!t.pending,
        t.installmentGroup || t.installment_group || null, t.installmentNum || t.installment_num || null,
        t.installmentTotal || t.installment_total || null, t.recurGroup || t.recur_group || null]
    );

    await replaceRows(client, 'budgets', uid, data.budgets || [],
      `INSERT INTO budgets (user_id,category,"limit") VALUES ($1,$2,$3)
       ON CONFLICT (user_id, category) DO UPDATE SET "limit"=EXCLUDED."limit", updated_at=NOW()`,
      (b, userId) => [userId, normalizeCategoryName(cleanText(b.category, 'Categoria')), Number(b.limit)||1]
    );

    await replaceRows(client, 'goals', uid, data.goals || [],
      `INSERT INTO goals (user_id,name,icon,target,current,deadline,description,monthly)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
      (g, userId) => [userId, cleanText(g.name, 'Meta'), cleanText(g.icon, 'goal'),
        Number(g.target)||1, Number(g.current)||0, g.deadline || null,
        cleanText(g.desc || g.description), Number(g.monthly)||0]
    );

    await replaceAppState(client, uid, {
      accounts: data.accounts,
      categories: data.categories,
      shopping: data.shopping,
      settings: data.settings,
      dueItems: data.dueItems
    });

    await auditEvent(client, req.user, 'backup_importado', 'backup', uid, 'importacao completa', {
      ...imported
    });
    await client.query('COMMIT');
    res.json({
      success: true,
      imported
    });
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

app.post('/api/budgets', userAuth, requireWrite, async (req, res) => {
  try {
    const { category, limit } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO budgets (user_id, category, "limit") VALUES ($1,$2,$3)
       ON CONFLICT (user_id, category) DO UPDATE SET "limit"=$3, updated_at=NOW()
       RETURNING *`,
      [req.user.id, normalizeCategoryName(category), limit]
    );
    await auditEvent(pool, req.user, 'orcamento_salvo', 'budget', rows[0].id, rows[0].category, { limit: rows[0].limit });
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/budgets/:id', userAuth, requireWrite, async (req, res) => {
  try {
    const { rows } = await pool.query('DELETE FROM budgets WHERE id=$1 AND user_id=$2 RETURNING id, category, "limit"', [req.params.id, req.user.id]);
    if (rows.length) await auditEvent(pool, req.user, 'orcamento_removido', 'budget', rows[0].id, rows[0].category, { limit: rows[0].limit });
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

app.post('/api/goals', userAuth, requireWrite, async (req, res) => {
  try {
    const { name, icon='🎯', target, current=0, deadline, description='', monthly=0 } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO goals (user_id,name,icon,target,current,deadline,description,monthly)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING *`,
      [req.user.id, name, icon, target, current, deadline, description, monthly]
    );
    await auditEvent(pool, req.user, 'meta_criada', 'goal', rows[0].id, rows[0].name, { target: rows[0].target });
    res.status(201).json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.patch('/api/goals/:id/add', userAuth, requireWrite, async (req, res) => {
  try {
    const { amount } = req.body;
    const { rows } = await pool.query(
      `UPDATE goals SET current = LEAST(current+$1, target)
       WHERE id=$2 AND user_id=$3 RETURNING *`,
      [amount, req.params.id, req.user.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    await auditEvent(pool, req.user, 'meta_atualizada', 'goal', rows[0].id, rows[0].name, { current: rows[0].current, target: rows[0].target });
    res.json(rows[0]);
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.delete('/api/goals/:id', userAuth, requireWrite, async (req, res) => {
  try {
    const { rows } = await pool.query('DELETE FROM goals WHERE id=$1 AND user_id=$2 RETURNING id, name, target', [req.params.id, req.user.id]);
    if (rows.length) await auditEvent(pool, req.user, 'meta_removida', 'goal', rows[0].id, rows[0].name, { target: rows[0].target });
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
    await auditEvent(pool, req.user, 'backup_sql_criado', 'backup', file, file, { size_bytes: size });
    res.json({ success: true, filename: file, size_bytes: size });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

app.listen(PORT, () => console.log(`Finanza API na porta ${PORT} - multi-usuário`));
