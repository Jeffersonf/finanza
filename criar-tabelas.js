// ================================================
// Finanza — Criar tabelas no banco (sem psql)
// Rode: node criar-tabelas.js
// ================================================

const { Client } = require('pg');

// Cole aqui a External Database URL do Render
// Render → finanza-db → Info → External Database URL
const DATABASE_URL = process.argv[2];

if (!DATABASE_URL) {
  console.log('');
  console.log('❌ Faltou a URL do banco!');
  console.log('');
  console.log('Uso correto:');
  console.log('  node criar-tabelas.js "postgresql://finanza_user:SENHA@HOST/finanza_6wlc"');
  console.log('');
  console.log('Onde pegar a URL:');
  console.log('  Render → finanza-db → Info → External Database URL');
  process.exit(1);
}

const client = new Client({
  connectionString: DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

async function criarTabelas() {
  console.log('🔌 Conectando ao banco...');
  await client.connect();
  console.log('✅ Conectado!\n');

  const sql = `
    CREATE EXTENSION IF NOT EXISTS "pgcrypto";

    CREATE TABLE IF NOT EXISTS transactions (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      type        VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense')),
      description TEXT NOT NULL,
      amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
      category    VARCHAR(50) NOT NULL,
      date        DATE NOT NULL,
      note        TEXT DEFAULT '',
      created_at  TIMESTAMPTZ DEFAULT NOW(),
      updated_at  TIMESTAMPTZ DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS budgets (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      category    VARCHAR(50) NOT NULL UNIQUE,
      "limit"     NUMERIC(12, 2) NOT NULL CHECK ("limit" > 0),
      created_at  TIMESTAMPTZ DEFAULT NOW(),
      updated_at  TIMESTAMPTZ DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS goals (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name        VARCHAR(100) NOT NULL,
      icon        VARCHAR(10) DEFAULT '🎯',
      target      NUMERIC(12, 2) NOT NULL CHECK (target > 0),
      current     NUMERIC(12, 2) DEFAULT 0 CHECK (current >= 0),
      deadline    DATE NOT NULL,
      description TEXT DEFAULT '',
      created_at  TIMESTAMPTZ DEFAULT NOW(),
      updated_at  TIMESTAMPTZ DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS backup_log (
      id         SERIAL PRIMARY KEY,
      filename   TEXT NOT NULL,
      size_bytes BIGINT,
      created_at TIMESTAMPTZ DEFAULT NOW()
    );

    CREATE OR REPLACE FUNCTION update_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
      NEW.updated_at = NOW();
      RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    DROP TRIGGER IF EXISTS trg_transactions_updated ON transactions;
    CREATE TRIGGER trg_transactions_updated
      BEFORE UPDATE ON transactions
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();

    DROP TRIGGER IF EXISTS trg_budgets_updated ON budgets;
    CREATE TRIGGER trg_budgets_updated
      BEFORE UPDATE ON budgets
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();

    DROP TRIGGER IF EXISTS trg_goals_updated ON goals;
    CREATE TRIGGER trg_goals_updated
      BEFORE UPDATE ON goals
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();

    CREATE INDEX IF NOT EXISTS idx_transactions_date     ON transactions(date DESC);
    CREATE INDEX IF NOT EXISTS idx_transactions_type     ON transactions(type);
    CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
    CREATE INDEX IF NOT EXISTS idx_goals_deadline        ON goals(deadline);
  `;

  console.log('📦 Criando tabelas...');
  await client.query(sql);

  // Verifica o que foi criado
  const res = await client.query(`
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = 'public'
    ORDER BY table_name
  `);

  console.log('\n✅ Tabelas criadas com sucesso!\n');
  console.log('Tabelas no banco:');
  res.rows.forEach(r => console.log('  ✓', r.table_name));
  console.log('\n🎉 Banco pronto! Agora atualize o site e tudo vai funcionar.');

  await client.end();
}

criarTabelas().catch(e => {
  console.error('\n❌ Erro:', e.message);
  console.error('\nDica: verifique se a URL está entre aspas e é a External URL do Render.');
  process.exit(1);
});
