// ============================================
// Finanza — API Server (Node.js + Express)
// ============================================

const express = require('express');
const cors    = require('cors');
const { Pool } = require('pg');
const fs      = require('fs');
const path    = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// ── Banco de dados ──────────────────────────
const pool = new Pool({ connectionString: process.env.DATABASE_URL });

pool.connect()
  .then(() => console.log('✅ Conectado ao PostgreSQL'))
  .catch(err => { console.error('❌ Erro ao conectar:', err.message); process.exit(1); });

// ── Middlewares ─────────────────────────────
app.use(cors());
app.use(express.json());

// Chave simples de acesso (sem login completo)
const API_SECRET = process.env.API_SECRET || 'minha-chave-secreta';

function auth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (key !== API_SECRET) return res.status(401).json({ error: 'Não autorizado' });
  next();
}

// ── Health check ────────────────────────────
app.get('/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.json({ status: 'ok', timestamp: new Date() });
  } catch {
    res.status(500).json({ status: 'error' });
  }
});

// ════════════════════════════════════════════
// TRANSAÇÕES
// ════════════════════════════════════════════

// Listar (com filtros)
app.get('/api/transactions', auth, async (req, res) => {
  try {
    const { type, category, month, year, search, limit = 200, offset = 0 } = req.query;
    const conditions = [];
    const params = [];
    let i = 1;

    if (type)     { conditions.push(`type = $${i++}`);     params.push(type); }
    if (category) { conditions.push(`category = $${i++}`); params.push(category); }
    if (month && year) {
      conditions.push(`EXTRACT(MONTH FROM date) = $${i++}`); params.push(month);
      conditions.push(`EXTRACT(YEAR  FROM date) = $${i++}`); params.push(year);
    }
    if (search) {
      conditions.push(`(description ILIKE $${i++} OR category ILIKE $${i-1})`);
      params.push(`%${search}%`);
    }

    const where = conditions.length ? 'WHERE ' + conditions.join(' AND ') : '';
    const query = `
      SELECT * FROM transactions
      ${where}
      ORDER BY date DESC, created_at DESC
      LIMIT $${i++} OFFSET $${i++}
    `;
    params.push(limit, offset);

    const { rows } = await pool.query(query, params);
    const total = await pool.query(`SELECT COUNT(*) FROM transactions ${where}`, params.slice(0, -2));
    res.json({ data: rows, total: parseInt(total.rows[0].count) });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Criar
app.post('/api/transactions', auth, async (req, res) => {
  try {
    const { type, description, amount, category, date, note = '' } = req.body;
    if (!type || !description || !amount || !category || !date)
      return res.status(400).json({ error: 'Campos obrigatórios: type, description, amount, category, date' });

    const { rows } = await pool.query(
      `INSERT INTO transactions (type, description, amount, category, date, note)
       VALUES ($1,$2,$3,$4,$5,$6) RETURNING *`,
      [type, description, amount, category, date, note]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Atualizar
app.put('/api/transactions/:id', auth, async (req, res) => {
  try {
    const { type, description, amount, category, date, note } = req.body;
    const { rows } = await pool.query(
      `UPDATE transactions SET type=$1, description=$2, amount=$3,
       category=$4, date=$5, note=$6 WHERE id=$7 RETURNING *`,
      [type, description, amount, category, date, note, req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Deletar
app.delete('/api/transactions/:id', auth, async (req, res) => {
  try {
    const { rowCount } = await pool.query('DELETE FROM transactions WHERE id=$1', [req.params.id]);
    if (!rowCount) return res.status(404).json({ error: 'Não encontrado' });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════
// ORÇAMENTOS
// ════════════════════════════════════════════

app.get('/api/budgets', auth, async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM budgets ORDER BY category');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/budgets', auth, async (req, res) => {
  try {
    const { category, limit } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO budgets (category, "limit") VALUES ($1,$2)
       ON CONFLICT (category) DO UPDATE SET "limit"=$2, updated_at=NOW()
       RETURNING *`,
      [category, limit]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/budgets/:id', auth, async (req, res) => {
  try {
    await pool.query('DELETE FROM budgets WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════
// METAS
// ════════════════════════════════════════════

app.get('/api/goals', auth, async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM goals ORDER BY deadline ASC');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/goals', auth, async (req, res) => {
  try {
    const { name, icon = '🎯', target, current = 0, deadline, description = '' } = req.body;
    const { rows } = await pool.query(
      `INSERT INTO goals (name, icon, target, current, deadline, description)
       VALUES ($1,$2,$3,$4,$5,$6) RETURNING *`,
      [name, icon, target, current, deadline, description]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.patch('/api/goals/:id/add', auth, async (req, res) => {
  try {
    const { amount } = req.body;
    const { rows } = await pool.query(
      `UPDATE goals SET current = LEAST(current + $1, target)
       WHERE id=$2 RETURNING *`,
      [amount, req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Não encontrado' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/goals/:id', auth, async (req, res) => {
  try {
    await pool.query('DELETE FROM goals WHERE id=$1', [req.params.id]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════
// RELATÓRIOS / RESUMO
// ════════════════════════════════════════════

app.get('/api/summary', auth, async (req, res) => {
  try {
    const { month, year } = req.query;
    const m = month || new Date().getMonth() + 1;
    const y = year  || new Date().getFullYear();

    const [summary, byCategory, lastMonths] = await Promise.all([
      pool.query(`
        SELECT
          SUM(CASE WHEN type='income'  THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense,
          SUM(CASE WHEN type='income'  THEN amount ELSE -amount END) AS net
        FROM transactions
        WHERE EXTRACT(MONTH FROM date)=$1 AND EXTRACT(YEAR FROM date)=$2
      `, [m, y]),

      pool.query(`
        SELECT category,
               SUM(amount) AS total,
               COUNT(*) AS count
        FROM transactions
        WHERE type='expense'
          AND EXTRACT(MONTH FROM date)=$1
          AND EXTRACT(YEAR FROM date)=$2
        GROUP BY category ORDER BY total DESC
      `, [m, y]),

      pool.query(`
        SELECT
          EXTRACT(MONTH FROM date)::int AS month,
          EXTRACT(YEAR  FROM date)::int AS year,
          SUM(CASE WHEN type='income'  THEN amount ELSE 0 END) AS income,
          SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense
        FROM transactions
        WHERE date >= NOW() - INTERVAL '6 months'
        GROUP BY year, month ORDER BY year, month
      `)
    ]);

    res.json({
      period: { month: m, year: y },
      summary: summary.rows[0],
      byCategory: byCategory.rows,
      lastMonths: lastMonths.rows
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════
// BACKUP MANUAL
// ════════════════════════════════════════════

app.post('/api/backup', auth, async (req, res) => {
  try {
    const { execSync } = require('child_process');
    const backupDir = process.env.BACKUP_DIR || './backups';
    const filename  = `finanza_manual_${Date.now()}.sql.gz`;
    const filepath  = path.join(backupDir, filename);

    fs.mkdirSync(backupDir, { recursive: true });

    const url = new URL(process.env.DATABASE_URL);
    const cmd = `PGPASSWORD="${url.password}" pg_dump -h ${url.hostname} -U ${url.username} ${url.pathname.slice(1)} | gzip > ${filepath}`;
    execSync(cmd);

    const size = fs.statSync(filepath).size;
    await pool.query('INSERT INTO backup_log (filename, size_bytes) VALUES ($1,$2)', [filename, size]);

    res.json({ success: true, filename, size_bytes: size });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.get('/api/backup/list', auth, async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM backup_log ORDER BY created_at DESC LIMIT 30');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── Start ────────────────────────────────────
app.listen(PORT, () => {
  console.log(`🚀 Finanza API rodando na porta ${PORT}`);
});
