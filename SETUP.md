# Finanza v3.9 final - Setup com Supabase

Este projeto usa:

- **API**: Node.js/Express no Render.
- **Banco**: PostgreSQL no Supabase.
- **Frontend**: arquivos estaticos em `frontend/`, geralmente publicados no GitHub Pages.

## Android SDK local

Se voce for compilar os apps Android (`android/` v3 e `android-v4/` v4), configure o `local.properties` das duas pastas com:

```powershell
.\configure-android-sdk.ps1
```

O script tenta encontrar o SDK nesta ordem:

- `ANDROID_SDK_ROOT`
- `ANDROID_HOME`
- `C:\Users\SEU_USUARIO\AppData\Local\Android\Sdk`

Depois disso, para build local, use tambem o Java do Android Studio:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## 1. Criar o banco no Supabase

1. Acesse <https://supabase.com>.
2. Crie um projeto novo.
3. Va em **Project Settings** > **Database** > **Connection string**.
4. Copie uma URL PostgreSQL.

Use preferencialmente uma string no formato:

```text
postgresql://postgres:[SENHA]@db.[PROJECT_REF].supabase.co:5432/postgres
```

Se o host onde a API roda tiver problema com IPv6, use a connection string do **pooler/session pooler** do Supabase.

## 2. Criar as tabelas

No Supabase:

1. Abra **SQL Editor**.
2. Cole o conteudo de `db/init.sql`.
3. Execute.

Isso cria as tabelas de usuarios, transacoes, orcamentos, metas, contas, categorias personalizadas, lista de compras, preferencias do app e log de backup.

## 3. Deploy da API no Render

O `render.yaml` agora cria apenas o servico da API. O banco fica no Supabase.

No Render, configure as variaveis:

```text
DATABASE_URL=postgresql://postgres:[SENHA]@db.[PROJECT_REF].supabase.co:5432/postgres
API_SECRET=uma-chave-admin-bem-grande
NODE_ENV=production
CORS_ORIGIN=https://SEU_USUARIO.github.io
```

`CORS_ORIGIN` pode receber mais de uma origem separada por virgula.

## 4. Criar o usuario admin

Depois que a API estiver no ar:

```bash
curl -X POST https://SEU-APP.onrender.com/api/setup \
  -H "x-api-key: SUA_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"name": "Seu Nome"}'
```

A resposta retorna a `api_key` do admin. Guarde essa chave.

## 5. Criar usuarios

```bash
curl -X POST https://SEU-APP.onrender.com/api/users \
  -H "x-api-key: SUA_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"name": "Joao"}'
```

A resposta inclui a `api_key` do usuario.

## 6. Entrar no app

Na tela inicial do Finanza:

- Escolha **Online**.
- URL: `https://SEU-APP.onrender.com`
- Chave: a `api_key` do usuario.

## Endpoints principais

| Metodo | Endpoint | Auth | Descricao |
| --- | --- | --- | --- |
| POST | `/api/setup` | Admin | Cria o admin inicial |
| GET | `/health` | - | Status da API |
| GET | `/api/me` | User | Dados do usuario logado |
| POST | `/api/users` | Admin | Cria usuario |
| GET | `/api/users` | Admin | Lista usuarios |
| DELETE | `/api/users/:id` | Admin | Remove usuario |
| POST | `/api/me/regenerate-key` | User | Gera nova `api_key` |
| GET/POST/PUT/DELETE | `/api/transactions` | User | CRUD de transacoes |
| GET/POST/DELETE | `/api/budgets` | User | CRUD de orcamentos |
| GET/POST/DELETE | `/api/goals` | User | CRUD de metas |
| PATCH | `/api/goals/:id/add` | User | Adiciona valor a meta |
| GET/PUT | `/api/state` | User | Carrega/salva contas, categorias, lista e preferencias |
| POST | `/api/backup` | Admin | Backup via `pg_dump` quando disponivel no host |

O Supabase agora também recebe contas bancárias, vínculo de transação com conta, status pago/pendente, categorias personalizadas, lista de compras, preferências do dashboard, tema e taxas de referência.
