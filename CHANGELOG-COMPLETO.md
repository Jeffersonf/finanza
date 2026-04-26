# Finanza - Changelog completo

Documento consolidado do que ja foi construido na linha principal do `Finanza`, com base no estado atual do repositorio, nos roadmaps existentes e no historico recente de commits.

Observacao importante:

- `ROADMAP-COMPLETO.md` usa `[x]` como "quero fazer", nao como "feito".
- Este changelog registra o que realmente entrou no produto.
- A versao alinhada no repositorio ao fechar esta fase e `4.3.1`.

## Pos-4.0.0 - Rodadas entregues sem bump de versao

### 4.3.1 - Fechamento da fase web principal (`2026-04-26`)

- [x] Linha web principal tratada como base consolidada para operacao financeira.
- [x] Setup atualizado para refletir login por `URL + usuario + senha + 2FA opcional`, com `api_key` preservada como detalhe tecnico da API.
- [x] Roadmap atualizado para marcar a `4.11` como ciclo concluido e a base atual como manutencao ativa.
- [x] Changelog e narrativa do produto alinhados ao fechamento desta fase antes do foco seguir para `Finvita` e `FinClinica`.

### 4.1 - Usabilidade sem atrito

- [x] Cadastro principal de transacao exigindo so o valor.
- [x] Defaults inteligentes para descricao, categoria, data e conta.
- [x] Busca global com acoes diretas como editar, pagar, duplicar e abrir contexto relacionado.
- [x] Edicao inline de vencimentos.
- [x] `Enter` para salvar nos fluxos principais.
- [x] Melhoria de acessibilidade, foco e estados vazios.
- [x] Polimento mobile nas telas principais.

### 4.2 - Inteligencia financeira pessoal

- [x] Simuladores, projecoes, comparativos e leituras financeiras chegaram a existir em versoes experimentais desta linha.
- [x] Alertas, ranking e sugestoes de acao tambem tiveram exploracoes iniciais.
- [x] Widget experimental de inteligencia foi implementado e depois removido por decisao de foco e qualidade.
- [x] A frente `4.2` ficou registrada como experimento relevante, mas nao consolidado como nucleo estavel do app atual.

### 4.3 - Dados, importadores e reconciliacao

- [x] Importador CSV com mapeamento assistido, perfis e regra de deduplicacao.
- [x] Importacao OFX.
- [x] Importacao por texto colado para extrato, PDF simples, OCR, comprovante Pix e QR/NFC-e convertidos em texto.
- [x] Importacao em lote por pasta local.
- [x] Revisao em lote com estados de nova, duplicada e conciliavel.
- [x] Tela de conflito visual com opcoes de manter atual, usar importado ou mesclar.
- [x] Regras salvas de categoria e assinatura.
- [x] Caixa de entrada para transacoes importadas sem categoria.
- [x] Snapshots compactos para restauracao rapida antes de importar.
- [x] Marcacao de transacoes conciliadas e metadados de importacao.

### 4.10 - Fundacao confiavel

- [x] Suite de testes para parser de transacao.
- [x] Testes de permissoes.
- [x] Testes e schema para backup/importacao JSON.
- [x] Migracoes de backup por versao.
- [x] Fixture demo importavel.
- [x] Padronizacao de `npm run check` e `npm test`.
- [x] Endurecimento de permissoes defensivas e retornos de importacao.

### 4.11 - Performance e foco no controle de gastos

- [x] Dashboard reorganizada para priorizar o fluxo diario.
- [x] Widgets classificados em `Essencial`, `Apoio` e `Analise`.
- [x] Ordem padrao mais focada em captura, revisao e decisao de gasto.
- [x] Reducao de re-render completo em ajustes pontuais de widgets.
- [x] `financas.html` reduzido a entrada leve apontando para `index.html`.
- [x] Home com texto e organizacao mais orientados ao controle de gastos.

## 4.0.0 - Linha principal web + Capacitor + API

Status: concluida e pronta para evolucao incremental.

### Produto

- [x] Finanza consolidado como app web responsivo e app mobile via Capacitor.
- [x] Dashboard principal com visao geral das financas.
- [x] Menu lateral no desktop e navegacao inferior no mobile.
- [x] Modo escuro como experiencia principal.
- [x] Identidade visual `finanza`, favicon embutido e metadados PWA/mobile.
- [x] Tela inicial de escolha entre modo online e modo local.
- [x] Modo local para uso sem servidor, com dados salvos no dispositivo.
- [x] Modo online com sincronizacao entre dispositivos via API.
- [x] Fluxo de login por usuario e senha no modo online.
- [x] Criacao de usuario, redefinicao de senha com chave admin ou codigo de recuperacao e 2FA opcional.
- [x] Area de conta com nome, papel, backup, exportacao e troca de conta.

### Transacoes

- [x] Cadastro de receitas e despesas.
- [x] Modal de nova transacao.
- [x] Entrada rapida por botao flutuante.
- [x] Teclado numerico proprio para lancamento rapido.
- [x] Entrada rapida por texto livre com preview antes de preencher/salvar.
- [x] Sugestao automatica de categoria, conta e recorrencia a partir do texto/historico.
- [x] Frases de vencimento como `internet 120 vence dia 10` viram lancamento futuro/pendente.
- [x] Seletores de tipo, categoria, conta, data e descricao.
- [x] Historico completo de transacoes.
- [x] Filtros por periodo, tipo, categoria, conta, texto, valor minimo e valor maximo.
- [x] Ordenacao por data e valor.
- [x] Visualizacao normal, compacta e por graficos.
- [x] Edicao inline de transacoes.
- [x] Exclusao com possibilidade de desfazer em areas principais.
- [x] Suporte a transacoes futuras.
- [x] Suporte a transacoes pagas/pendentes.
- [x] Suporte a recorrencias e parcelamentos.
- [x] Exportacao CSV.
- [x] Parser basico de texto livre no backend para transformar texto em transacao.

### Dashboard

- [x] Cards de resumo financeiro.
- [x] Comparativo mensal.
- [x] Projecao ate o fim do mes.
- [x] Widgets configuraveis no dashboard.
- [x] Controles para ativar, remover, resetar e reordenar widgets.
- [x] Reordenacao por botoes e arrastar.
- [x] Widgets de acoes rapidas.
- [x] Widgets de contas.
- [x] Widgets de metas.
- [x] Widgets de orcamentos.
- [x] Widgets de compras.
- [x] Widgets de carro.
- [x] Widgets de categorias.
- [x] Widgets de economia e estatisticas.
- [x] Filtros de widget persistidos.
- [x] Opcao de restaurar widgets padrao.
- [x] Ajustes recentes para remover redimensionamento problemativo de widgets.
- [x] Melhorias de layout mobile.

### Vencimentos e planejamento

- [x] Pagina dedicada a vencimentos.
- [x] Cadastro de vencimentos.
- [x] Lancamento futuro.
- [x] Filtros de proximos 7 dias, 15 dias, 1 mes, 3 meses, 6 meses e tudo.
- [x] Cards de compromissos, proximo vencimento, a pagar, a receber, saldo futuro e vencidos.
- [x] Separacao visual entre vencimentos, vencidos, a receber e a pagar.
- [x] Central de comando para vencimentos futuros.
- [x] Hub de contas fixas.
- [x] Acoes rapidas para marcar/pagar/adaptar fluxos de vencimentos.
- [x] Notificacoes locais via Capacitor para vencimentos proximos.
- [x] Notificacao persistente com saldo e pendencias no Android/Capacitor.
- [x] Suporte a deep links para abrir areas especificas.

### Orcamentos

- [x] Pagina de limites e orcamentos.
- [x] Cadastro de orcamento por categoria.
- [x] Cards de total orcado, total gasto e disponivel.
- [x] Alertas de orcamento por percentual usado.
- [x] Visualizacao do consumo mensal por categoria.

### Metas

- [x] Pagina de metas financeiras.
- [x] Cadastro de metas com icone, alvo, valor atual, prazo, descricao e aporte mensal.
- [x] Atualizacao de progresso de metas.
- [x] Exclusao de metas.
- [x] Estatisticas de metas.
- [x] Widget de metas no dashboard.

### Contas e rendimento

- [x] Pagina de contas.
- [x] Cadastro de contas.
- [x] Saldos por conta.
- [x] Transferencia entre contas.
- [x] Taxas/rendimentos salvos em configuracoes.
- [x] Calculo de rendimento por conta.
- [x] Inclusao de conta nos filtros de transacoes.
- [x] Inclusao de contas no backup/estado remoto.

### Lista de compras

- [x] Pagina de lista de compras.
- [x] Multiplas listas.
- [x] Criacao de listas com icone.
- [x] Itens com nome, quantidade e categoria.
- [x] Marcacao de item comprado.
- [x] Remocao de itens.
- [x] Limpeza de itens comprados.
- [x] Estatisticas de lista.
- [x] Progresso da lista ativa.
- [x] Agrupamento por categoria.
- [x] Persistencia local e sincronizacao via estado remoto.

### Carro e combustivel

- [x] Modulo de carro e combustivel.
- [x] Cadastro/controle de veiculos.
- [x] Eventos de abastecimento.
- [x] Eventos de manutencao/despesas.
- [x] Controle por data e quilometragem.
- [x] Comparativo por veiculo.
- [x] Custo por posto/oficina.
- [x] Filtros mais fortes para tanque cheio.
- [x] Historico enriquecido.
- [x] Graficos de gasto, combustivel, mix e evolucao.
- [x] Insights de manutencao.
- [x] Importacao CSV de carro com deduplicacao.
- [x] Correcao para persistir configuracoes do carro sem esconder historico.
- [x] Prevencao de importacoes duplicadas de CSV de carro.

### Busca e produtividade

- [x] Busca global.
- [x] Overlay de busca.
- [x] Busca por transacoes, carro, vencimentos e compras.
- [x] Campo de busca no menu lateral.
- [x] Atalho visual para Ctrl+K.
- [x] Escopos de busca.
- [x] Acoes rapidas a partir de resultados.
- [x] Pull to refresh no mobile.
- [x] Botao de atualizar dados.
- [x] Comportamento de botao voltar no Android/mobile.

### Dados, backup e importacao

- [x] Exportacao de backup completo.
- [x] Exportacao JSON em modo local.
- [x] Importacao de backup JSON.
- [x] Validacao e preview de importacao.
- [x] Deduplicacao em importacoes JSON.
- [x] Deduplicacao em importacoes CSV.
- [x] Versionamento basico de backup.
- [x] Lembrete/estado de backup automatico local.
- [x] Endpoint de backup SQL no backend.
- [x] Script de backup em `db/backup.sh`.
- [x] Scripts de migracao de local para banco e multiusuario.

### Sincronizacao e offline

- [x] Fila local de sincronizacao offline.
- [x] Badge de pendencias de sincronizacao.
- [x] Acao manual para sincronizar fila.
- [x] Indicador de conexao online/offline/erro/sincronizando.
- [x] Notificacao de sync mais sutil.
- [x] Estado remoto com contas, categorias, listas, itens de compra e configuracoes.
- [x] Importacao remota atomica por usuario.
- [x] Separacao de dados por `user_id`.

### Backend e banco

- [x] API Node/Express.
- [x] PostgreSQL via `pg`.
- [x] CORS configuravel.
- [x] Health check.
- [x] Autenticacao por chave de usuario.
- [x] Login por usuario e senha.
- [x] Hash de senha com `crypto.scrypt`.
- [x] Registro do primeiro usuario sem chave admin.
- [x] Registro controlado por chave admin quando necessario.
- [x] Regeneracao de chave de API do usuario.
- [x] CRUD de usuarios admin.
- [x] CRUD de transacoes.
- [x] CRUD de orcamentos.
- [x] CRUD de metas.
- [x] Estado geral do app por usuario.
- [x] Resumo financeiro por mes/ano.
- [x] Tabelas para usuarios, transacoes, orcamentos, metas, contas, categorias, listas, itens, configuracoes e log de backup.

### Mobile, Android e deploy

- [x] Configuracao Capacitor.
- [x] Scripts `sync`, `android`, `ios` e `build:apk`.
- [x] Integracao com plugins de notificacoes locais, push, splash screen e status bar.
- [x] Ajustes de viewport e area segura para mobile.
- [x] Estrutura Android em `android/`.
- [x] Linha Android nativa separada em `android-v4/`, preservada para uma futura linha `5.0`.
- [x] Documentacao de setup.
- [x] Documentacao de deploy.
- [x] Configuracao Render.
- [x] Workflow/deploy YAML existente.

## 3.9 final - Base congelada

- [x] Linha anterior congelada como base estavel.
- [x] Modulo de carro ja continha insights e graficos enriquecidos.
- [x] Melhorias em importacao de CSV do carro.
- [x] Persistencia de configuracoes do carro corrigida.

## Pendencias reconhecidas ao fechar a 4.0

- [ ] Papeis alem de `admin`.
- [ ] Auditoria completa no backend com autor por registro e trilha persistente.
- [ ] Reconciliacao offline com resolucao de conflito por item.
- [ ] Testes automatizados mais fortes para importacao, projecoes e sincronizacao.
- [ ] Observabilidade e logs de producao mais claros.
- [ ] Hardening de seguranca antes de uso multiusuario amplo.
