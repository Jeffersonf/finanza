# Finanza v3 — Setup Multi-Usuário

## 1. Banco de dados (escolha um)

### Opção A — Supabase (RECOMENDADO, free, sem expirar)
1. Acesse supabase.com → New Project
2. Copie a **Connection string** (Settings → Database → URI)
3. Formato: `postgresql://postgres:[senha]@db.[ref].supabase.co:5432/postgres`

### Opção B — Neon (PostgreSQL serverless, free)
1. Acesse neon.tech → New Project
2. Copie a **Connection string**

### Opção C — Render Database (expira em 90 dias no free)
1. No Render: New → PostgreSQL
2. Copie a **External Database URL**

---

## 2. Deploy no Render

1. Faça push do repo no GitHub
2. render.com → New → Blueprint
3. Cole a DATABASE_URL do banco escolhido
4. A API_SECRET é gerada automaticamente — **anote ela após o deploy**

---

## 3. Configurar o banco (primeira vez)

Após o deploy, faça UMA chamada:

```bash
curl -X POST https://SEU-APP.onrender.com/api/setup \
  -H "x-api-key: SUA_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"name": "Seu Nome"}'
```

A resposta retorna sua `admin_key` pessoal. **Guarde-a.**

---

## 4. Criar novos usuários

```bash
curl -X POST https://SEU-APP.onrender.com/api/users \
  -H "x-api-key: SUA_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"name": "João", "email": "joao@email.com"}'
```

A resposta inclui a `api_key` do usuário — envie para ele.

---

## 5. No app Finanza

- Tela inicial → Online
- URL: `https://SEU-APP.onrender.com`
- Chave: a `api_key` recebida

---

## Endpoints disponíveis

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| POST | /api/setup | Admin | Cria tabelas + admin |
| GET | /health | — | Status da API |
| GET | /api/me | User | Dados do usuário logado |
| POST | /api/users | Admin | Cria novo usuário |
| GET | /api/users | Admin | Lista usuários |
| DELETE | /api/users/:id | Admin | Remove usuário |
| POST | /api/me/regenerate-key | User | Nova api_key |
| GET/POST/PUT/DELETE | /api/transactions | User | CRUD transações |
| GET/POST/DELETE | /api/budgets | User | CRUD orçamentos |
| GET/POST/DELETE | /api/goals | User | CRUD metas |
| PATCH | /api/goals/:id/add | User | Adiciona valor à meta |
| GET/POST/DELETE | /api/accounts | User | CRUD contas bancárias |
| GET | /api/export | User | Exporta todos os dados |
