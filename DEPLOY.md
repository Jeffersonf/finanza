# 🚀 Finanza — Guia Completo de Deploy

Tudo que você precisa para colocar o Finanza no ar:
- **Backend** no Render (Node + PostgreSQL grátis)
- **Frontend** no GitHub Pages
- **App Android** com Capacitor
- **Migrar dados** do notebook para o banco

---

## 📁 Estrutura final do projeto

```
finanza/                          ← repositório no GitHub
├── frontend/
│   └── financas.html             ← seu app web
├── server/
│   ├── index.js                  ← API Node.js
│   ├── package.json
│   └── Dockerfile
├── db/
│   ├── init.sql
│   └── backup.sh
├── capacitor.config.json         ← config do app mobile
├── package.json                  ← dependências Capacitor
├── render.yaml                   ← config automática do Render
├── migrate-local-to-db.js        ← script de migração
├── .github/
│   └── workflows/
│       └── deploy.yml            ← CI/CD automático
└── .gitignore
```

---

## ETAPA 1 — Preparar o repositório no GitHub

### 1.1 Criar o repositório

1. Acesse [github.com](https://github.com) e faça login
2. Clique em **New repository**
3. Nome: `finanza`
4. Visibilidade: **Private** (seus dados financeiros!)
5. Clique em **Create repository**

### 1.2 Organizar os arquivos localmente

```bash
# No seu notebook, crie a estrutura:
mkdir finanza
cd finanza
mkdir frontend server db .github/workflows

# Mova os arquivos:
# financas.html → frontend/financas.html
# server/index.js → server/index.js
# server/package.json → server/package.json
# server/Dockerfile → server/Dockerfile
# db/init.sql → db/init.sql
# db/backup.sh → db/backup.sh

# Cole os arquivos deste pacote:
# render.yaml → raiz
# capacitor.config.json → raiz
# package.json (root) → raiz
# migrate-local-to-db.js → raiz
# .github/workflows/deploy.yml → .github/workflows/
```

### 1.3 Criar o .gitignore

```bash
cat > .gitignore << 'EOF'
.env
node_modules/
android/
ios/
backup/*.sql.gz
backup/*.log
dados-locais.json
EOF
```

### 1.4 Subir para o GitHub

```bash
git init
git add .
git commit -m "feat: finanza app inicial"
git branch -M main
git remote add origin https://github.com/SEU_USUARIO/finanza.git
git push -u origin main
```

---

## ETAPA 2 — Backend no Render

### 2.1 Criar conta no Render

1. Acesse [render.com](https://render.com)
2. **Sign up with GitHub** (já conecta seu repositório)

### 2.2 Fazer deploy com render.yaml (automático)

O arquivo `render.yaml` já configura tudo. Siga:

1. No Render, clique em **New** → **Blueprint**
2. Conecte o repositório `finanza`
3. O Render detecta o `render.yaml` automaticamente
4. Clique em **Apply**
5. Aguarde ~3 minutos — ele vai criar:
   - O banco PostgreSQL (`finanza-db`)
   - O serviço Node.js (`finanza-api`)

### 2.3 Definir o API_SECRET

1. No Render, vá em **finanza-api** → **Environment**
2. Adicione a variável:
   ```
   API_SECRET = qualquer-texto-longo-e-secreto-aqui
   ```
3. Clique em **Save Changes** — o serviço reinicia automaticamente

### 2.4 Testar o backend

```bash
# Substitua pela URL do Render (aparece no dashboard)
curl https://finanza-api.onrender.com/health
# Resposta esperada: {"status":"ok","timestamp":"..."}
```

> ⚠️ **Atenção:** O plano grátis do Render dorme após 15 minutos de inatividade.
> A primeira requisição do dia pode demorar ~30 segundos para acordar.
> Isso é normal e não perde dados.

### 2.5 Anotar suas credenciais

Salve em local seguro:
```
API_URL    = https://finanza-api.onrender.com
API_SECRET = (o que você definiu acima)
```

---

## ETAPA 3 — Frontend no GitHub Pages

### 3.1 Ativar GitHub Pages

1. No GitHub, vá no repositório `finanza`
2. **Settings** → **Pages**
3. Source: **GitHub Actions**
4. Salve

### 3.2 Adicionar o secret do Render (para CI/CD)

1. No Render, vá em **finanza-api** → **Settings** → **Deploy Hook**
2. Copie a URL do webhook
3. No GitHub: **Settings** → **Secrets and variables** → **Actions**
4. Clique em **New repository secret**:
   ```
   Name:  RENDER_DEPLOY_HOOK
   Value: (URL copiada do Render)
   ```

### 3.3 Fazer deploy

```bash
# Qualquer push na branch main faz deploy automático
git push origin main
```

Após ~2 minutos, seu app estará em:
```
https://SEU_USUARIO.github.io/finanza/
```

### 3.4 Configurar o app no navegador

1. Acesse a URL do GitHub Pages
2. Na tela de setup, preencha:
   - **URL da API:** `https://finanza-api.onrender.com`
   - **Chave:** seu `API_SECRET`
3. Clique em **Conectar e continuar**

---

## ETAPA 4 — Migrar dados do notebook para o banco

### 4.1 Exportar dados do navegador

1. Abra `financas.html` no Chrome do notebook
2. Pressione `F12` → aba **Console**
3. Cole e execute:
   ```javascript
   copy(localStorage.getItem('finanza_local'))
   ```
4. Abra qualquer editor de texto e cole (`Ctrl+V`)
5. Salve como `dados-locais.json` na pasta raiz do projeto

### 4.2 Configurar a DATABASE_URL

1. No Render, vá em **finanza-db** → **Info**
2. Copie a **External Database URL**
3. No seu notebook, crie um `.env` temporário:
   ```bash
   export DATABASE_URL="postgresql://finanza_user:SENHA@HOST/finanza"
   ```

### 4.3 Rodar a migração

```bash
# Na pasta raiz do projeto
npm install pg

# Com a DATABASE_URL exportada:
DATABASE_URL="postgresql://..." node migrate-local-to-db.js
```

Saída esperada:
```
🚀 Iniciando migração para PostgreSQL...

📦 Migrando 47 transações...
  ✓ Salário (2026-01-05)
  ✓ Aluguel (2026-01-07)
  ...

📦 Migrando 5 orçamentos...
  ✓ Alimentação: R$ 600
  ...

═══════════════════════════════════
✅ Migração concluída!
   Transações:  47/47
   Orçamentos:  5/5
   Metas:       2/2
═══════════════════════════════════
```

### 4.4 Verificar no app

1. Acesse o GitHub Pages
2. Configure a conexão com a API do Render
3. Todos os seus dados estarão lá ✅

---

## ETAPA 5 — App Android com Capacitor

### 5.1 Pré-requisitos

- [Node.js](https://nodejs.org) instalado
- [Android Studio](https://developer.android.com/studio) instalado
- Java 17+ (vem com o Android Studio)

### 5.2 Instalar dependências

```bash
# Na pasta raiz do projeto finanza/
npm install
```

### 5.3 Preparar o frontend para o Capacitor

O Capacitor vai empacotar o `frontend/financas.html` como app nativo.

```bash
# Sincronizar com o Android
npx cap sync android
```

Se for a primeira vez:
```bash
npx cap add android
npx cap sync android
```

### 5.4 Abrir no Android Studio

```bash
npx cap open android
```

O Android Studio vai abrir. Aguarde indexar (pode demorar alguns minutos na primeira vez).

### 5.5 Gerar o APK (para instalar no celular)

**Opção A — Pelo terminal:**
```bash
npm run build:apk
# APK gerado em: android/app/build/outputs/apk/debug/app-debug.apk
```

**Opção B — Pelo Android Studio:**
1. Menu **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Clique em **locate** quando terminar
3. Transfira o `.apk` para o celular e instale

### 5.6 Instalar no celular Android

```bash
# Via USB (com depuração USB ativada no celular):
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

Ou: transfira o arquivo `.apk` para o celular e abra com o gerenciador de arquivos.

> ⚠️ Para instalar APK fora da Play Store, ative em:
> **Configurações → Segurança → Fontes desconhecidas**

### 5.7 iPhone (iOS)

Requer Mac + Xcode. Se tiver:
```bash
npx cap add ios
npx cap open ios
# No Xcode: Product → Run
```

---

## ETAPA 6 — Usar no celular via navegador (mais simples)

Se não quiser instalar o APK, instale como PWA:

**Android (Chrome):**
1. Acesse `https://SEU_USUARIO.github.io/finanza/`
2. Menu ⋮ → **Adicionar à tela inicial**
3. Confirme → vira ícone na tela inicial

**iPhone (Safari):**
1. Acesse o link
2. Botão compartilhar ↑ → **Adicionar à Tela de Início**
3. Confirme → ícone na tela inicial

---

## 🔄 Fluxo de sincronização (como os dados ficam)

```
Notebook (Chrome)  ──┐
                     ├──► Render API ──► PostgreSQL ──► Backup diário
Celular (App/PWA)  ──┘
```

Todos os dispositivos leem e escrevem no mesmo banco. Mudança em um aparece no outro instantaneamente.

---

## ✅ Checklist final

- [ ] Repositório criado no GitHub (privado)
- [ ] Backend no Render funcionando (`/health` retorna ok)
- [ ] `API_SECRET` configurado no Render
- [ ] GitHub Pages publicado
- [ ] App configurado com URL + chave
- [ ] Dados migrados do localStorage
- [ ] Testado no notebook ✓
- [ ] Testado no celular ✓
- [ ] APK instalado (opcional)

---

## 🆘 Problemas comuns

**"CORS error" no console:**
Adicione no `server/index.js`:
```javascript
app.use(cors({ origin: 'https://SEU_USUARIO.github.io' }));
```

**App demora muito pra carregar:**
Normal no Render grátis — servidor dormindo. Aguarde ~30 segundos na primeira vez.

**APK não instala:**
Ative "Fontes desconhecidas" nas configurações do Android.

**Dados não aparecem depois da migração:**
Verifique se a `DATABASE_URL` usada na migração é a **External** (não a Internal) do Render.
