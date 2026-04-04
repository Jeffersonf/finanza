-- ============================================
-- Finanza — Schema Multi-Usuário v2
-- ============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- USUÁRIOS: cada um tem sua api_key única
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL DEFAULT 'Usuário',
    api_key     VARCHAR(128) NOT NULL UNIQUE,
    is_admin    BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- TRANSAÇÕES por usuário
CREATE TABLE IF NOT EXISTS transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type              VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense')),
    description       TEXT NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    category          VARCHAR(50) NOT NULL,
    date              DATE NOT NULL,
    note              TEXT DEFAULT '',
    installment_group UUID,
    installment_num   INT,
    installment_total INT,
    recur_group       UUID,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

-- ORÇAMENTOS por usuário
CREATE TABLE IF NOT EXISTS budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(50) NOT NULL,
    "limit"     NUMERIC(12, 2) NOT NULL CHECK ("limit" > 0),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, category)
);

-- METAS por usuário
CREATE TABLE IF NOT EXISTS goals (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(10) DEFAULT '🎯',
    target      NUMERIC(12, 2) NOT NULL CHECK (target > 0),
    current     NUMERIC(12, 2) DEFAULT 0 CHECK (current >= 0),
    deadline    DATE NOT NULL,
    description TEXT DEFAULT '',
    monthly     NUMERIC(12, 2) DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- BACKUP LOG
CREATE TABLE IF NOT EXISTS backup_log (
    id         SERIAL PRIMARY KEY,
    filename   TEXT NOT NULL,
    size_bytes BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- TRIGGERS
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tx  ON transactions;
DROP TRIGGER IF EXISTS trg_bud ON budgets;
DROP TRIGGER IF EXISTS trg_goa ON goals;
CREATE TRIGGER trg_tx  BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_bud BEFORE UPDATE ON budgets      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_goa BEFORE UPDATE ON goals        FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ÍNDICES
CREATE INDEX IF NOT EXISTS idx_tx_user   ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_tx_date   ON transactions(date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_type   ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_tx_cat    ON transactions(category);
CREATE INDEX IF NOT EXISTS idx_bud_user  ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_goal_user ON goals(user_id);
