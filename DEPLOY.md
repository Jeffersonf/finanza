# Finanza - Deploy com Supabase + Render

Este guia assume que o banco de dados sera o **Supabase PostgreSQL** e que o Render hospedara somente a API Node.js.

## Arquitetura

```text
Frontend/GitHub Pages ou App Android
        |
        v
Render Web Service: finanza-api
        |
        v
Supabase PostgreSQL
```

## 1. Supabase

1. Crie um projeto em <https://supabase.com>.
2. Abra **Project Settings** > **Database** > **Connection string**.
3. Copie a connection string PostgreSQL.
4. No **SQL Editor**, execute o conteudo de `db/init.sql`.

O schema salva usuarios, transacoes, contas, metas, orcamentos, categorias personalizadas, lista de compras e preferencias do app.

Use a connection string direta se funcionar no Render:

```text
postgresql://postgres:[SENHA]@db.[PROJECT_REF].supabase.co:5432/postgres
```

Se houver erro de rede/IPv6, use a connection string do pooler do Supabase.

## 2. Render

O arquivo `render.yaml` cria apenas o servico `finanza-api`. Ele nao cria banco Render.

No Render:

1. Clique em **New** > **Blueprint**.
2. Selecione o repositorio.
3. Aplique o `render.yaml`.
4. Abra o servico `finanza-api` > **Environment**.
5. Configure:

```text
DATABASE_URL=postgresql://postgres:[SENHA]@db.[PROJECT_REF].supabase.co:5432/postgres
API_SECRET=uma-chave-admin-bem-grande
NODE_ENV=production
CORS_ORIGIN=https://SEU_USUARIO.github.io
```

Depois salve e deixe o Render redeployar.

Teste:

```bash
curl https://SEU-APP.onrender.com/health
```

Resposta esperada:

```json
{"status":"ok","multiUser":true}
```

## 3. Usuario admin

Crie o admin inicial:

```bash
curl -X POST https://SEU-APP.onrender.com/api/setup \
  -H "x-api-key: SUA_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"name": "Seu Nome"}'
```

Guarde a `api_key` retornada.

## 4. Frontend

Publique a pasta `frontend/` no GitHub Pages ou use o app Android/Capacitor.

Na tela inicial:

- Modo: **Online**
- URL da API: `https://SEU-APP.onrender.com`
- Chave: a `api_key` do usuario

## 5. Migrar dados locais

Se voce tem dados no modo local:

1. Exporte um backup JSON pelo app.
2. Use a opcao de migrar para online dentro do proprio app, ou rode o script `migrate-local-to-db.js` com `DATABASE_URL` apontando para o Supabase.

Exemplo:

```bash
$env:DATABASE_URL="postgresql://postgres:[SENHA]@db.[PROJECT_REF].supabase.co:5432/postgres"
node migrate-local-to-db.js
```

## Observacoes importantes

- Render Free pode dormir apos inatividade; isso deixa a primeira requisicao mais lenta, mas nao apaga dados do Supabase.
- Render Postgres Free nao sera mais usado.
- Supabase Free e bom para comecar, mas confira limites de armazenamento e pausa de projeto conforme seu uso.
- Para dados financeiros, mantenha backups JSON/CSV periodicos.

## Checklist

- [ ] Projeto criado no Supabase
- [ ] `db/init.sql` executado no SQL Editor
- [ ] `DATABASE_URL` do Supabase configurada no Render
- [ ] `API_SECRET` configurado
- [ ] `/health` retorna ok
- [ ] Admin criado com `/api/setup`
- [ ] App conectado no modo Online
