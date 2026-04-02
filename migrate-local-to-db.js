#!/usr/bin/env node
// ═══════════════════════════════════════════════════════════════
// Finanza — Migração: localStorage → PostgreSQL
//
// Como usar:
// 1. Abra o financas.html no Chrome
// 2. F12 → Console → cole e rode:
//       copy(localStorage.getItem('finanza_local'))
// 3. Cole o resultado num arquivo chamado: dados-locais.json
// 4. Rode: node migrate-local-to-db.js
// ═══════════════════════════════════════════════════════════════

const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');

// ── Configuração ────────────────────────────────────────────────
// Cole aqui a DATABASE_URL do Render (Settings > Environment)
const DATABASE_URL = process.env.DATABASE_URL || 'postgresql://finanza_user:SENHA@HOST/finanza';
const DATA_FILE = path.join(__dirname, 'dados-locais.json');

// ── Validações ──────────────────────────────────────────────────
if (!fs.existsSync(DATA_FILE)) {
  console.error('❌ Arquivo dados-locais.json não encontrado!');
  console.error('');
  console.error('Como exportar do navegador:');
  console.error('  1. Abra financas.html no Chrome');
  console.error('  2. F12 → Console');
  console.error('  3. Cole: copy(localStorage.getItem("finanza_local"))');
  console.error('  4. Salve o resultado como dados-locais.json');
  process.exit(1);
}

let data;
try {
  data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
} catch (e) {
  console.error('❌ Erro ao ler dados-locais.json:', e.message);
  process.exit(1);
}

const pool = new Pool({ connectionString: DATABASE_URL, ssl: { rejectUnauthorized: false } });

async function migrate() {
  console.log('🚀 Iniciando migração para PostgreSQL...\n');

  const { transactions = [], budgets = [], goals = [] } = data;
  let txOk = 0, budgOk = 0, goalOk = 0, skip = 0;

  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    // ── Transações ─────────────────────────────────────────────
    console.log(`📦 Migrando ${transactions.length} transações...`);
    for (const tx of transactions) {
      if (!tx.desc || !tx.amount || !tx.date || !tx.type) { skip++; continue; }
      try {
        await client.query(
          `INSERT INTO transactions (id, type, description, amount, category, date, note)
           VALUES ($1, $2, $3, $4, $5, $6, $7)
           ON CONFLICT (id) DO UPDATE SET
             type=$2, description=$3, amount=$4, category=$5, date=$6, note=$7`,
          [
            tx.id || undefined,
            tx.type,
            tx.desc || tx.description,
            parseFloat(tx.amount),
            tx.category || 'Outros',
            tx.date.substring(0, 10),
            tx.note || ''
          ]
        );
        txOk++;
        process.stdout.write(`  ✓ ${tx.desc} (${tx.date})\n`);
      } catch (e) {
        console.error(`  ⚠️  Erro em transação "${tx.desc}": ${e.message}`);
      }
    }

    // ── Orçamentos ─────────────────────────────────────────────
    console.log(`\n📦 Migrando ${budgets.length} orçamentos...`);
    for (const b of budgets) {
      if (!b.category || !b.limit) { skip++; continue; }
      try {
        await client.query(
          `INSERT INTO budgets (id, category, "limit")
           VALUES ($1, $2, $3)
           ON CONFLICT (category) DO UPDATE SET "limit"=$3`,
          [b.id || undefined, b.category, parseFloat(b.limit)]
        );
        budgOk++;
        console.log(`  ✓ ${b.category}: R$ ${b.limit}`);
      } catch (e) {
        console.error(`  ⚠️  Erro em orçamento "${b.category}": ${e.message}`);
      }
    }

    // ── Metas ──────────────────────────────────────────────────
    console.log(`\n📦 Migrando ${goals.length} metas...`);
    for (const g of goals) {
      if (!g.name || !g.target) { skip++; continue; }
      try {
        await client.query(
          `INSERT INTO goals (id, name, icon, target, current, deadline, description)
           VALUES ($1, $2, $3, $4, $5, $6, $7)
           ON CONFLICT (id) DO UPDATE SET
             name=$2, icon=$3, target=$4, current=$5, deadline=$6, description=$7`,
          [
            g.id || undefined,
            g.name,
            g.icon || '🎯',
            parseFloat(g.target),
            parseFloat(g.current || 0),
            g.deadline.substring(0, 10),
            g.desc || g.description || ''
          ]
        );
        goalOk++;
        console.log(`  ✓ ${g.icon || '🎯'} ${g.name}`);
      } catch (e) {
        console.error(`  ⚠️  Erro em meta "${g.name}": ${e.message}`);
      }
    }

    await client.query('COMMIT');

    console.log('\n═══════════════════════════════════');
    console.log('✅ Migração concluída!');
    console.log(`   Transações:  ${txOk}/${transactions.length}`);
    console.log(`   Orçamentos:  ${budgOk}/${budgets.length}`);
    console.log(`   Metas:       ${goalOk}/${goals.length}`);
    if (skip > 0) console.log(`   Ignorados:   ${skip} (dados inválidos)`);
    console.log('═══════════════════════════════════\n');

  } catch (e) {
    await client.query('ROLLBACK');
    console.error('\n❌ Erro na migração, rollback realizado:', e.message);
  } finally {
    client.release();
    await pool.end();
  }
}

migrate().catch(console.error);
