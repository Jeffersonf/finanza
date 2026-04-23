# Finanza - Roadmap completo para sonhar sem pedir licenca

Use este arquivo como quadro de decisao. Marque o que voce quiser manter, deixe vazio o que ainda esta em analise e risque/remova o que quiser descartar.

Legenda sugerida:

- `[ ]` ainda nao escolhido
- `[x]` quero fazer
- `[-]` descartar
- `[~]` talvez / incubar

## Status atual de versao

- Versao publicada do app: `4.0.0` (`package.json`, `frontend/app.js` e `frontend/index.html`).
- Ciclo em andamento no roadmap: `4.11 - Performance e foco no controle de gastos`.
- Escopo do ciclo `4.11`: reduzir re-render da dashboard, priorizar widgets essenciais, diminuir duplicacao estrutural no frontend e manter a home orientada a captura, revisao e decisao de gasto.
- Linha futura planejada: `5.0 - Android nativo premium`.
- Observacao: neste roadmap, `[x]` significa "quero fazer", nao "feito".

## Norte do produto

- [x] Fazer o Finanza virar o centro pessoal de decisao financeira, nao so um registrador de gastos.
- [x] Manter o app rapido para uso diario: abrir, lancar, entender, fechar.
- [x] Transformar dados financeiros em linguagem humana: "o que esta acontecendo?", "o que fazer agora?", "onde mora o risco?".
- [x] Priorizar experiencia mobile como uso principal.
- [x] Manter modo local forte para privacidade e autonomia.
- [x] Manter modo online para sincronizar, backup e multi-dispositivo.
- [x] Preparar uma futura linha Android nativa `5.0` sem quebrar a linha web/Capacitor.

## 4.1 - Usabilidade sem atrito

Status de implementacao: fechamento funcional aplicado nesta rodada com cadastro principal de transacao exigindo apenas valor, busca com acoes diretas, edicao inline de vencimentos, Enter nos modais principais, melhorias de acessibilidade/foco e ajustes mobile dos novos fluxos.

- [x] Entrada rapida por texto livre no frontend.
- [x] Aceitar frases como `mercado 87,90 hoje nubank`.
- [x] Aceitar frases como `recebi 350 freela ontem`.
- [x] Aceitar frases como `internet 120 vence dia 10`.
- [x] Mostrar preview antes de salvar transacao parseada.
- [x] Permitir `Enter` para salvar em fluxos rapidos.
- [x] Reduzir campos obrigatorios no cadastro principal.
- [x] Sugerir categoria automaticamente pelo texto.
- [x] Sugerir conta automaticamente pelo historico.
- [x] Sugerir recorrencia quando o mesmo gasto se repete.
- [x] Lembrar filtros de transacoes.
- [x] Lembrar ultima visualizacao usada: normal, compacto ou graficos.
- [x] Lembrar ultimo periodo usado em vencimentos.
- [x] Melhorar busca global com resultados agrupados.
- [x] Incluir acoes dentro da busca: editar, pagar, duplicar, abrir conta, abrir meta.
- [x] Fazer dashboard ter uma faixa de acoes rapidas configuravel.
- [x] Adicionar atalhos visiveis para nova despesa, nova receita, vencimento, abastecimento e busca.
- [x] Adicionar pagar em um toque nos vencimentos.
- [x] Adicionar adiar vencimento em um toque.
- [x] Adicionar editar vencimento inline.
- [x] Adicionar microfeedbacks mais claros para salvar, sincronizar, importar e desfazer.
- [x] Revisar textos de botoes, erros e estados vazios.
- [x] Melhorar estados vazios com proxima acao clara.
- [x] Adicionar modo privacidade mais consistente em todos os graficos e cards.
- [x] Polir layout mobile em todas as paginas.
- [x] Revisar acessibilidade basica: foco, labels, contraste, aria e tamanho de toque.

## 4.2 - Inteligencia financeira pessoal

Status de implementacao: linha marcada no roadmap, mas o widget experimental de inteligencia financeira foi removido do app por decisao de foco e qualidade.

- [x] Simulador de metas: "se eu guardar X por mes, chego quando?".
- [x] Simulador inverso: "quanto preciso guardar por mes para chegar ate tal data?".
- [x] Projecao por categoria com sazonalidade.
- [x] Detectar anomalias: gasto subiu muito contra media de 3/6/12 meses.
- [x] Alertas inteligentes por categoria.
- [x] Calendario financeiro mensal com entradas, vencimentos, parcelas e metas.
- [x] Planejamento por objetivo: viagem, reserva, carro, reforma, curso, computador.
- [x] Resumo semanal automatico em linguagem humana.
- [x] Resumo mensal automatico com aprendizados e proximas decisoes.
- [x] Benchmark pessoal: melhor mes, pior mes, media de 12 meses, recordes.
- [x] Ranking de categorias que mais estao drenando dinheiro.
- [x] Detector de assinaturas recorrentes.
- [x] Detector de gastos esquecidos.
- [x] Detector de "mes atipico".
- [x] Detector de risco de fechar o mes negativo.
- [x] Previsao de saldo para 30, 60, 90 e 180 dias.
- [x] Modo conservador, realista e otimista nas projecoes.
- [x] Explicacao do motivo de cada previsao.
- [x] Painel "o que posso fazer hoje?".
- [x] Sugestoes de corte sem moralismo: trocar, pausar, renegociar, planejar.
- [x] Diario financeiro: notas e eventos que explicam o mes.

## 4.3 - Dados, importadores e reconciliacao

- [x] Importador universal de extrato CSV.
- [x] Mapeamento assistido de colunas.
- [x] Perfis salvos de importacao por banco/cartao.
- [x] Preview de duplicatas antes de importar.
- [x] Regra de deduplicacao configuravel.
- [x] Conciliacao entre extrato importado e transacoes ja cadastradas.
- [x] Marcar transacoes como conciliadas.
- [x] Detectar divergencias entre saldo esperado e saldo real.
- [x] Importar OFX.
- [x] Importar PDF simples de fatura/extrato, se viavel.
- [x] Importar print/recibo por OCR.
- [x] Importar comprovante Pix.
- [x] Importar NFC-e/cupom fiscal por QR code.
- [x] Importar automaticamente anexos de uma pasta local.
- [x] Criar regras: "toda descricao X vira categoria Y".
- [x] Criar regras: "todo valor X no dia Y vira assinatura".
- [x] Criar revisao de importacao em lote.
- [x] Criar modo "caixa de entrada" para transacoes importadas sem categoria.
- [x] Criar snapshots compactos para restauracao rapida.
- [x] Cache local versionado para migracoes mais seguras.
- [x] Reconciliacao offline por item.
- [x] Tela de conflito: manter local, manter servidor, mesclar manualmente.

## 4.4 - Multiusuario, familia e colaboracao

- [x] Papeis: admin, editor, leitura, convidado.
- [x] Perfis separados dentro da mesma familia.
- [x] Carteiras compartilhadas.
- [x] Contas pessoais e contas compartilhadas.
- [x] Metas compartilhadas.
- [x] Orcamentos compartilhados.
- [x] Historico de quem criou/editou/excluiu.
- [x] Auditoria persistente no backend.
- [x] Log de atividade dentro do app.
- [x] Comentarios em transacoes importantes.
- [x] Aprovacao para gasto compartilhado.
- [x] Divisao de despesas.
- [x] Acerto entre pessoas.
- [x] Modo casal.
- [x] Modo familia.
- [x] Modo republica/casa compartilhada.
- [x] Convites por link.
- [x] Recuperacao de conta sem depender so de chave admin.
- [x] 2FA opcional.

## 4.5 - Carro premium

- [x] Dashboard exclusivo do carro.
- [x] Comparar custo mensal por veiculo.
- [x] Calcular custo por km real.
- [x] Calcular autonomia por combustivel.
- [x] Detectar abastecimento fora do padrao.
- [x] Historico de postos favoritos.
- [x] Ranking de posto por preco, consumo aparente e confiabilidade.
- [x] Agenda de manutencao preventiva.
- [x] Alertas por km.
- [x] Alertas por data.
- [x] Cadastro de pneus, oleo, filtros, bateria e seguro.
- [x] Custo total de propriedade.
- [x] Valor de revenda estimado manualmente.
- [x] Simulador: manter carro atual vs trocar.
- [x] Simulador: carro, moto, app, transporte publico.
- [x] Registro de multas, pedagios, estacionamento e seguro.
- [x] Importar abastecimento por foto do recibo.
- [x] Exportar relatorio do carro.
- [x] Linha do tempo visual do veiculo.

## 4.6 - Investimentos e patrimonio

- [x] Painel de patrimonio consolidado.
- [x] Contas, metas e investimentos no mesmo resumo.
- [x] Cadastro manual de investimentos.
- [x] Carteiras por objetivo.
- [x] Rentabilidade mensal.
- [x] Aportes e resgates.
- [x] Evolucao patrimonial.
- [x] Distribuicao por classe: renda fixa, renda variavel, cripto, caixa, outros.
- [x] Metas atreladas a investimentos.
- [x] Simulador de juros compostos.
- [x] Simulador de reserva de emergencia.
- [x] Calculo de meses de seguranca.
- [x] Importar cotacoes automaticamente.
- [x] Alertas de concentracao.
- [x] Relatorio anual.
- [x] Modo "FIRE"/independencia financeira.
- [x] Painel de dividas e patrimonio liquido.

## 4.7 - Assinaturas, dividas e contratos

- [x] Central de assinaturas.
- [x] Detector automatico de assinaturas recorrentes.
- [x] Alertar antes de renovacoes.
- [x] Comparar assinatura usada vs esquecida.
- [x] Cadastrar dividas.
- [x] Parcelas de dividas.
- [x] Juros e CET manual.
- [x] Plano de quitacao.
- [x] Simulador bola de neve.
- [x] Simulador avalanche.
- [x] Contratos: aluguel, internet, seguro, financiamento.
- [x] Arquivos anexos por contrato.
- [x] Alertas de reajuste.
- [x] Historico de renegociacao.
- [x] Painel de "dinheiro comprometido".

## 4.8 - Automacao e assistente

- [x] Assistente dentro do app para perguntar sobre os dados.
- [x] Perguntas: "quanto gastei com mercado este mes?".
- [x] Perguntas: "posso comprar X agora?".
- [x] Perguntas: "qual categoria piorou?".
- [x] Perguntas: "o que vence essa semana?".
- [x] Criar transacao por comando natural.
- [x] Criar meta por comando natural.
- [x] Criar vencimento por comando natural.
- [x] Gerar resumo semanal automatico.
- [x] Gerar plano de corte por prioridade.
- [x] Explicar financas sem julgamento.
- [x] Modo "coach": me cutuca.
- [x] Modo "contador": seco e objetivo.
- [x] Modo "familia": linguagem simples.
- [x] Comandos por voz.
- [x] Resposta por voz.
- [x] Automacoes: se salario caiu, separar percentuais.
- [x] Automacoes: se gasto passou limite, sugerir ajuste.
- [x] Automacoes: se sobrou dinheiro, sugerir aporte/meta.

## 4.9 - Experiencia, design e personalizacao

- [x] Temas visuais.
- [x] Tema claro polido.
- [ ] Tema alto contraste.
- [x] Icones configuraveis por categoria.
- [x] Cores configuraveis por categoria.
- [x] Dashboard por contexto: dia a dia, familia, carro, metas, patrimonio.
- [x] Layout compacto para quem lanca muito.
- [x] Layout executivo para visao geral.
- [x] Modo foco: so lancar gasto.
- [x] Modo revisao mensal.
- [x] Calendario arrastavel.
- [x] Timeline financeira.
- [x] Animacoes sutis de transicao.
- [x] Melhorar experiencia de instalacao PWA.
- [x] Onboarding guiado com dados demo opcionais.
- [x] Sandbox de exemplo para testar sem mexer nos dados reais.
- [x] Sistema de comandos/atalhos no desktop.

## 4.10 - Fundacao confiavel

Este ciclo organiza itens da fundacao tecnica continua sem mudar a versao publicada `4.0.0` ainda.

Itens puxados para este ciclo:

- Criar testes para permissoes de usuario.
- Criar suite de testes para parser de transacao. Entregas atuais: valores com `R$`, salario, conta composta, categorias de saude, recorrencia por texto e entradas invalidas.
- Criar testes para importacao JSON. Entregas atuais: campos estruturais invalidos, vencimentos em `settings.rates`, contadores completos e fixture demo importavel.
- Formalizar schema de backup.
- Criar migracoes de backup por versao.
- Criar fixtures de dados demo. Entrega atual: backup demo com transacoes, vencimento recorrente, lista de compras, meta, orcamento, conta e estado basico de carro.
- Adicionar CI para testes.
- Adicionar validacao de deploy.
- Padronizar comandos `npm run check` e `npm test`.
- Endurecer permissoes defensivas. Entrega atual: `canWrite(null)` bloqueia escrita e `publicUser()` nao quebra nem vaza campos sensiveis.
- Melhorar retorno de importacao. Entrega atual: backend informa contagem de categorias, listas, itens de compra e vencimentos alem de transacoes, orcamentos, metas e contas.

## 4.11 - Performance e foco no controle de gastos (ciclo atual)

Status de implementacao: dashboard reorganizada para priorizar o fluxo diario, com widgets classificados em essencial/apoio/analise, render parcial em interacoes pontuais do menu e filtros, e `financas.html` reduzido a entrada leve apontando para `index.html`.

- [x] Manter um nucleo sagrado: lancar transacao, ver saldo, ver vencimentos e buscar.
- [x] Tratar inteligencia e widgets analiticos como camada secundaria e opcional.
- [x] Otimizar a dashboard para evitar re-render completo em ajustes pontuais.
- [x] Reordenar a home para priorizar widgets essenciais por padrao.
- [x] Esconder por padrao widgets mais analiticos.
- [x] Reduzir duplicacao entre `index.html` e `financas.html`.
- [x] Revisar a home para ficar mais "controle de gastos" e menos "painel de tudo".
- [x] Definir criterio de produto: se uma feature nao melhora captura, revisao ou decisao de gasto, ela entra depois.

## 5.0 - Android nativo premium

- [x] Definir se `android-v4/` vira base oficial da linha `5.0`.
- [x] Migrar modelo de dados mantendo compatibilidade com backup 4.x.
- [x] App Android nativo com performance e UX de primeira classe.
- [x] Widgets Android na tela inicial.
- [x] Widget de saldo.
- [x] Widget de vencimentos.
- [x] Widget de lancamento rapido.
- [x] Quick Settings tile para lancar despesa.
- [x] Notificacoes com acoes: pagar, adiar, abrir.
- [x] Offline-first real com banco local.
- [x] Sincronizacao em background.
- [x] Biometria.
- [x] Bloqueio por PIN.
- [x] Atalhos de app.
- [x] Compartilhar texto/recibo para o Finanza.
- [x] OCR local ou hibrido para comprovantes.
- [x] Integracao com calendario Android.
- [x] Backup local criptografado.
- [x] Experiencia tablet/dobravels.
- [ ] Wear OS: ver vencimentos e lancar gasto rapido.

## 6.0 - Plataforma e produto grande

- [x] App web com frontend modular moderno.
- [x] Separar HTML/CSS/JS em arquitetura mais sustentavel.
- [x] Testes unitarios.
- [x] Testes e2e.
- [x] Storybook ou catalogo de componentes.
- [x] Design system proprio.
- [x] API versionada.
- [x] Migrations formais de banco.
- [x] Observabilidade: logs, metricas, tracing.
- [x] Painel admin.
- [x] Exportacao LGPD: baixar todos os dados.
- [x] Exclusao definitiva de conta.
- [x] Criptografia de dados sensiveis.
- [x] Backups automaticos agendados.
- [x] Multi-tenant serio.
- [x] Plano gratuito/premium, se virar produto.
- [x] Landing page publica.
- [x] Documentacao publica.
- [x] Modo self-hosted.
- [x] Docker Compose completo.
- [x] Instalador local.

## Sonhos deliberadamente irrealistas

- [ ] Finanza prever seu stress financeiro antes de voce perceber.
- [ ] Finanza montar um "mapa de vida" cruzando dinheiro, tempo, metas e energia.
- [ ] Finanza negociar contas automaticamente.
- [ ] Finanza trocar plano de internet/celular sozinho apos sua aprovacao.
- [ ] Finanza comprar melhores produtos de mercado baseado no historico da sua casa.
- [ ] Finanza criar um plano de carreira financeiro baseado em renda, metas e risco.
- [ ] Finanza simular varios futuros: conservador, ousado, familia, mudanca de cidade, empreender.
- [ ] Finanza virar um copiloto de decisoes: "posso assumir esse financiamento?".
- [ ] Finanza construir um plano para quitar tudo e investir sem voce precisar montar planilha.
- [ ] Finanza detectar vazamento de dinheiro invisivel.
- [ ] Finanza comparar custo de vida entre bairros/cidades usando seus padroes.
- [ ] Finanza criar "missoes" mensais para melhorar sua vida financeira.
- [ ] Finanza conversar com banco/cartao/servicos via integracoes oficiais.
- [ ] Finanza ter modo "crise": congelar gastos, renegociar, priorizar vencimentos e preservar caixa.
- [ ] Finanza ter modo "sonho grande": casa, viagem, liberdade, empresa, patrimonio.
- [ ] Finanza criar uma autobiografia financeira visual.
- [ ] Finanza gerar um relatorio bonito tipo "Spotify Wrapped" do seu dinheiro.
- [ ] Finanza virar uma IA financeira local, privada, rodando no seu dispositivo.
- [ ] Finanza conectar familia inteira sem virar bagunca.
- [ ] Finanza ser bom o bastante para voce esquecer que um dia usou planilha.

## Fundacao tecnica continua

- [ ] Criar suite de testes para parser de transacao.
- [ ] Criar testes para importacao JSON.
- [ ] Criar testes para importacao CSV de carro.
- [ ] Criar testes para projecoes.
- [ ] Criar testes para recorrencias e parcelamentos.
- [ ] Criar testes para sync offline.
- [ ] Criar testes para permissoes de usuario.
- [ ] Separar `frontend/app.js` em modulos menores.
- [ ] Separar componentes de dashboard.
- [ ] Separar modulo de carro.
- [ ] Separar modulo de compras.
- [ ] Separar modulo de vencimentos.
- [ ] Criar camada unica de storage.
- [ ] Criar camada unica de API.
- [ ] Formalizar schema de backup.
- [ ] Criar migracoes de backup por versao.
- [ ] Criar fixtures de dados demo.
- [ ] Adicionar lint/format.
- [ ] Adicionar CI para testes.
- [ ] Adicionar build Android em CI.
- [ ] Adicionar validacao de deploy.

## Criterios para escolher o proximo ciclo

- [ ] Aumenta uso diario?
- [ ] Reduz chance de perda de dado?
- [ ] Ajuda a tomar decisao melhor?
- [ ] Evita trabalho manual chato?
- [ ] Funciona bem no celular?
- [ ] Nao quebra modo local?
- [ ] Nao cria complexidade desnecessaria agora?
- [ ] Deixa o projeto mais bonito de usar?
- [ ] Deixa o projeto mais confiavel?
- [ ] Faz voce querer abrir o app amanha?
