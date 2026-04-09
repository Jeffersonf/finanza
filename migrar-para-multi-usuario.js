#!/usr/bin/env node
// ═══════════════════════════════════════════════════════
// Finanza — Migração: banco antigo → multi-usuário
//
// Use se você já tinha dados no banco anterior.
// Cria um usuário "admin" e vincula todos os dados a ele.
//
// Uso:
//   node migrar-para-multi-usuario.js "postgresql://URL_DO_RENDER"
// ═══════════════════════════════════════════════════════

const { Client } = require('pg');
const crypto = require('crypto');

const DATABASE_URL = process.argv[2];
if (!DATABASE_URL) {
  console.error('\n❌ Informe a External Database URL do Render:');
  console.error('   node migrar-para-multi-usuario.js "postgresql://..."\n');
  process.exit(1);
}

const client = new Client({ connectionString: DATABASE_URL, ssl: { rejectUnauthorized: false } });

async function run() {
  await client.connect();
  console.log('\n✅ Conectado ao banco\n');

  await client.query('BEGIN');
  try {
    // 1. Adicionar coluna user_id nas tabelas se não existir
    console.log('📦 Atualizando schema...');

    // Criar extensão
    await client.query(`CREATE EXTENSION IF NOT EXISTS "pgcrypto"`);

    // Criar tabela de usuários
    await client.query(`
      CREATE TABLE IF NOT EXISTS users (
        id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        name       VARCHAR(100) NOT NULL DEFAULT 'Usuário',
        api_key    VARCHAR(128) NOT NULL UNIQUE,
        is_admin   BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMPTZ DEFAULT NOW()
      )
    `);

    // Criar usuário admin
    const existingAdmin = await client.query(`SELECT id, api_key FROM users WHERE is_admin = TRUE LIMIT 1`);
    let adminId, adminKey;

    if (existingAdmin.rows.length) {
      adminId  = existingAdmin.rows[0].id;
      adminKey = existingAdmin.rows[0].api_key;
      console.log('  ↳ Admin já existe, reutilizando');
    } else {
      adminKey = crypto.randomBytes(32).toString('hex');
      const { rows } = await client.query(
        `INSERT INTO users (name, api_key, is_admin) VALUES ('Admin', $1, TRUE) RETURNING id, api_key`,
        [adminKey]
      );
      adminId  = rows[0].id;
      adminKey = rows[0].api_key;
      console.log('  ↳ Usuário admin criado');
    }

    // Adicionar user_id em transactions se não existir
    const txCols = await client.query(`
      SELECT column_name FROM information_schema.columns
      WHERE table_name='transactions' AND column_name='user_id'
    `);
    if (!txCols.rows.length) {
      await client.query(`ALTER TABLE transactions ADD COLUMN user_id UUID`);
      await client.query(`UPDATE transactions SET user_id = $1 WHERE user_id IS NULL`, [adminId]);
      await client.query(`ALTER TABLE transactions ALTER COLUMN user_id SET NOT NULL`);

      // Adicionar FK
      await client.query(`
        ALTER TABLE transactions
        ADD CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      `);
      console.log('  ↳ transactions.user_id adicionado e vinculado ao admin');
    }

    // Adicionar colunas de parcelamento/recorrência se não existir
    const extraCols = ['installment_group', 'installment_num', 'installment_total', 'recur_group'];
    for (const col of extraCols) {
      const exists = await client.query(`
        SELECT column_name FROM information_schema.columns
        WHERE table_name='transactions' AND column_name=$1`, [col]);
      if (!exists.rows.length) {
        const colType = col.includes('num') || col.includes('total') ? 'INT' : 'UUID';
        await client.query(`ALTER TABLE transactions ADD COLUMN ${col} ${colType}`);
        console.log(`  ↳ transactions.${col} adicionado`);
      }
    }

    // Adicionar user_id em budgets
    const budCols = await client.query(`
      SELECT column_name FROM information_schema.columns
      WHERE table_name='budgets' AND column_name='user_id'
    `);
    if (!budCols.rows.length) {
      // Remover UNIQUE antigo em category
      await client.query(`
        ALTER TABLE budgets DROP CONSTRAINT IF EXISTS budgets_category_key
      `);
      await client.query(`ALTER TABLE budgets ADD COLUMN user_id UUID`);
      await client.query(`UPDATE budgets SET user_id = $1 WHERE user_id IS NULL`, [adminId]);
      await client.query(`ALTER TABLE budgets ALTER COLUMN user_id SET NOT NULL`);
      await client.query(`
        ALTER TABLE budgets
        ADD CONSTRAINT fk_bud_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      `);
      await client.query(`
        ALTER TABLE budgets ADD CONSTRAINT budgets_user_cat UNIQUE (user_id, category)
      `);
      console.log('  ↳ budgets.user_id adicionado e vinculado ao admin');
    }

    // Adicionar user_id em goals
    const goalCols = await client.query(`
      SELECT column_name FROM information_schema.columns
      WHERE table_name='goals' AND column_name='user_id'
    `);
    if (!goalCols.rows.length) {
      await client.query(`ALTER TABLE goals ADD COLUMN user_id UUID`);
      await client.query(`UPDATE goals SET user_id = $1 WHERE user_id IS NULL`, [adminId]);
      await client.query(`ALTER TABLE goals ALTER COLUMN user_id SET NOT NULL`);
      await client.query(`
        ALTER TABLE goals
        ADD CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      `);
      console.log('  ↳ goals.user_id adicionado e vinculado ao admin');
    }

    // Adicionar campo monthly em goals se não existir
    const mCols = await client.query(`
      SELECT column_name FROM information_schema.columns
      WHERE table_name='goals' AND column_name='monthly'
    `);
    if (!mCols.rows.length) {
      await client.query(`ALTER TABLE goals ADD COLUMN monthly NUMERIC(12,2) DEFAULT 0`);
      console.log('  ↳ goals.monthly adicionado');
    }

    // Criar índices
    await client.query(`CREATE INDEX IF NOT EXISTS idx_tx_user   ON transactions(user_id)`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_bud_user  ON budgets(user_id)`);
    await client.query(`CREATE INDEX IF NOT EXISTS idx_goal_user ON goals(user_id)`);

    await client.query('COMMIT');

    console.log('\n═══════════════════════════════════════════════');
    console.log('✅ Migração concluída com sucesso!\n');
    console.log('🔑 Sua chave de acesso (API_KEY do admin):');
    console.log(`\n   ${adminKey}\n`);
    console.log('⚠️  IMPORTANTE:');
    console.log('   1. Salve essa chave em local seguro');
    console.log('   2. No app, use ESSA chave no campo "Chave de Acesso"');
    console.log('   3. A API_SECRET do Render agora é só para criar usuários');
    console.log('═══════════════════════════════════════════════\n');

  } catch (e) {
    await client.query('ROLLBACK');
    console.error('\n❌ Erro — rollback realizado:', e.message);
    throw e;
  } finally {
    await client.end();
  }
}

run().catch(() => process.exit(1));
