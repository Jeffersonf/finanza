-- ============================================
-- Finanza - Schema Multi-Usuario v2
-- ============================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- USUARIOS: cada um tem sua api_key unica
CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL DEFAULT 'Usuario',
    api_key     VARCHAR(128) NOT NULL UNIQUE,
    is_admin    BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- TRANSACOES por usuario
CREATE TABLE IF NOT EXISTS transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type              VARCHAR(10) NOT NULL CHECK (type IN ('income', 'expense')),
    description       TEXT NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    category          VARCHAR(50) NOT NULL,
    date              DATE NOT NULL,
    note              TEXT DEFAULT '',
    account_id        TEXT,
    paid              BOOLEAN DEFAULT FALSE,
    pending           BOOLEAN DEFAULT FALSE,
    installment_group UUID,
    installment_num   INT,
    installment_total INT,
    recur_group       UUID,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS account_id TEXT;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS paid BOOLEAN DEFAULT FALSE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS pending BOOLEAN DEFAULT FALSE;

-- ORCAMENTOS por usuario
CREATE TABLE IF NOT EXISTS budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category    VARCHAR(50) NOT NULL,
    "limit"     NUMERIC(12, 2) NOT NULL CHECK ("limit" > 0),
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, category)
);

-- METAS por usuario
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

-- CONTAS por usuario
CREATE TABLE IF NOT EXISTS accounts (
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id           TEXT NOT NULL,
    name         VARCHAR(100) NOT NULL,
    icon         VARCHAR(20) DEFAULT '',
    type         VARCHAR(30) NOT NULL DEFAULT 'checking',
    balance      NUMERIC(12, 2) DEFAULT 0,
    yield_rate   NUMERIC(12, 4) DEFAULT 0,
    yield_type   VARCHAR(30) DEFAULT 'manual',
    yield_val    NUMERIC(12, 4) DEFAULT 0,
    calc_base    VARCHAR(10) DEFAULT 'du',
    start_date   DATE,
    note         TEXT DEFAULT '',
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

-- CATEGORIAS personalizadas por usuario
CREATE TABLE IF NOT EXISTS categories (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id          TEXT NOT NULL,
    icon        VARCHAR(20) DEFAULT '',
    name        VARCHAR(80) NOT NULL,
    color       VARCHAR(20) DEFAULT '#888',
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, id),
    UNIQUE(user_id, name)
);

-- LISTAS DE COMPRAS por usuario
CREATE TABLE IF NOT EXISTS shopping_lists (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id          TEXT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(20) DEFAULT '',
    position    INT DEFAULT 0,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

CREATE TABLE IF NOT EXISTS shopping_items (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id          TEXT NOT NULL,
    list_id     TEXT NOT NULL,
    name        TEXT NOT NULL,
    qty         VARCHAR(50) DEFAULT '',
    category    VARCHAR(100) DEFAULT '',
    bought      BOOLEAN DEFAULT FALSE,
    created_ms  BIGINT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, id)
);

-- PREFERENCIAS do app por usuario
CREATE TABLE IF NOT EXISTS user_settings (
    user_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme         VARCHAR(20) DEFAULT 'dark',
    rates         JSONB DEFAULT '{}'::jsonb,
    widget_prefs  JSONB DEFAULT '{}'::jsonb,
    widget_order  JSONB DEFAULT '[]'::jsonb,
    tx_view       VARCHAR(20) DEFAULT 'n',
    active_list   TEXT,
    updated_at    TIMESTAMPTZ DEFAULT NOW()
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
DROP TRIGGER IF EXISTS trg_acc ON accounts;
DROP TRIGGER IF EXISTS trg_cat ON categories;
DROP TRIGGER IF EXISTS trg_sl  ON shopping_lists;
DROP TRIGGER IF EXISTS trg_si  ON shopping_items;
DROP TRIGGER IF EXISTS trg_set ON user_settings;
CREATE TRIGGER trg_tx  BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_bud BEFORE UPDATE ON budgets      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_goa BEFORE UPDATE ON goals        FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_acc BEFORE UPDATE ON accounts       FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_cat BEFORE UPDATE ON categories     FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_sl  BEFORE UPDATE ON shopping_lists FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_si  BEFORE UPDATE ON shopping_items FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_set BEFORE UPDATE ON user_settings  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- INDICES
CREATE INDEX IF NOT EXISTS idx_tx_user   ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_tx_date   ON transactions(date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_type   ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_tx_cat    ON transactions(category);
CREATE INDEX IF NOT EXISTS idx_tx_user_date      ON transactions(user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_type_date ON transactions(user_id, type, date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_user_cat_date  ON transactions(user_id, category, date DESC);
CREATE INDEX IF NOT EXISTS idx_bud_user  ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_goal_user ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_acc_user  ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_cat_user  ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_sl_user   ON shopping_lists(user_id);
CREATE INDEX IF NOT EXISTS idx_si_user   ON shopping_items(user_id);
CREATE INDEX IF NOT EXISTS idx_si_list   ON shopping_items(user_id, list_id);
