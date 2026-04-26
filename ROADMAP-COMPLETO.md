# Finanza - Roadmap completo para sonhar sem pedir licenca

Use este arquivo como quadro de decisao e acompanhamento. Marque com check o que ja foi entregue, deixe vazio o que ainda falta e risque/remova o que quiser descartar.

Legenda sugerida:

- `[ ]` ainda nao feito
- `[x]` entregue / concluido
- `[-]` descartar
- `[~]` parcial / incubando

## Status atual de versao

- [x] Versao atual alinhada em frontend, pacotes e Android: `4.3.1`.
- [x] Fechamento desta fase registrado em `2026-04-26`.
- Ciclo de produto concluido nesta linha: `4.11 - Performance e foco no controle de gastos`.
- Escopo fechado da `4.11`: reduzir re-render da dashboard, priorizar widgets essenciais, diminuir duplicacao estrutural no frontend e manter a home orientada a captura, revisao e decisao de gasto.
- Estado recomendado desta base apos `2026-04-26`: manutencao ativa, correcoes, polimento e alinhamento de docs; sem abrir outra frente grande dentro do `Finanza` agora.
- Linha futura planejada: `5.0 - Android nativo premium`.
- Proximo foco estrategico fora desta base: `Finvita` como organizacao pessoal e `FinClinica` como frente visual/operacional separada.

## Leitura rapida de onde estamos

- [x] Base web principal ativa e em uso, com frontend separado em multiplos arquivos e dashboard reorganizada para foco no dia a dia.
- [x] Ciclos `4.1`, `4.3`, `4.10` e `4.11` tem evidencias fortes no codigo atual.
- [x] `4.4` fechado na base atual com papeis, auditoria, espaco compartilhado, acertos, convite por link, recuperacao por codigo e 2FA opcional.
- [x] Setup e narrativa do produto agora refletem o fluxo real de login por usuario/senha, com `api_key` ficando como detalhe tecnico e nao como entrada principal da interface.
- [~] `4.5`, `4.6` e `4.9` tem partes implementadas, mas ainda pedem fechamento de escopo.
- [ ] `4.2`, `4.7`, `4.8`, `5.0` e boa parte de `6.0` ainda estao mais como direcao do que como entrega fechada.
- [x] Decisao de fase tomada: o `Finanza` fica como base financeira consolidada enquanto a expansao de organizacao pessoal segue no `Finvita`.

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

Status de implementacao: frente ainda em aberto. Houve experimento de inteligencia financeira, mas ele nao esta consolidado no app atual.

- [ ] Simulador de metas: "se eu guardar X por mes, chego quando?".
- [ ] Simulador inverso: "quanto preciso guardar por mes para chegar ate tal data?".
- [ ] Projecao por categoria com sazonalidade.
- [ ] Detectar anomalias: gasto subiu muito contra media de 3/6/12 meses.
- [ ] Alertas inteligentes por categoria.
- [ ] Calendario financeiro mensal com entradas, vencimentos, parcelas e metas.
- [ ] Planejamento por objetivo: viagem, reserva, carro, reforma, curso, computador.
- [ ] Resumo semanal automatico em linguagem humana.
- [ ] Resumo mensal automatico com aprendizados e proximas decisoes.
- [ ] Benchmark pessoal: melhor mes, pior mes, media de 12 meses, recordes.
- [ ] Ranking de categorias que mais estao drenando dinheiro.
- [ ] Detector de assinaturas recorrentes.
- [ ] Detector de gastos esquecidos.
- [ ] Detector de "mes atipico".
- [ ] Detector de risco de fechar o mes negativo.
- [ ] Previsao de saldo para 30, 60, 90 e 180 dias.
- [ ] Modo conservador, realista e otimista nas projecoes.
- [ ] Explicacao do motivo de cada previsao.
- [ ] Painel "o que posso fazer hoje?".
- [ ] Sugestoes de corte sem moralismo: trocar, pausar, renegociar, planejar.
- [ ] Diario financeiro: notas e eventos que explicam o mes.

## 4.3 - Dados, importadores e reconciliacao

Status de implementacao: centro de importacao consolidado com CSV, OFX, texto colado, PDF/OCR/Pix/QR via texto extraido, revisao em lote, regras, snapshots rapidos, caixa de entrada e tela visual de conflito para reconciliacao manual.

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

Status de implementacao: ciclo fechado nesta base com papeis online, auditoria persistente, log de atividade, divisao de despesas, acertos, convite por link para espaco compartilhado, recuperacao por codigo e 2FA opcional para a conta.

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
- [x] Modo de gastos por areas, como cachorro, eletrodomesticos, casa, filhos e outros contextos dedicados.

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
- [ ] Modulo de aposentadoria.
- [ ] Simulador de aposentadoria por idade alvo.
- [ ] Projecao de renda passiva mensal na aposentadoria.
- [ ] Comparativo entre INSS, previdencia privada e carteira propria.
- [ ] Meta de independencia financeira com retirada segura.
- [ ] Painel de acumulo vs renda futura.
- [x] Modo "FIRE"/independencia financeira.
- [x] Painel de dividas e patrimonio liquido.

## 4.7 - Assinaturas, dividas e contratos

Status de implementacao: parcial bem avancado nesta base com central nova para assinaturas, dividas e contratos, detector automatico de recorrencias, alertas de renovacao/reajuste, painel de dinheiro comprometido e ordem sugerida para quitacao; ainda faltam anexos e historico formal de renegociacao.

- [x] Central de assinaturas.
- [x] Detector automatico de assinaturas recorrentes.
- [x] Alertar antes de renovacoes.
- [x] Comparar assinatura usada vs esquecida.
- [x] Cadastrar dividas.
- [x] Parcelas de dividas.
- [x] Juros e CET manual.
- [x] Plano de quitacao.
- [~] Simulador bola de neve.
- [~] Simulador avalanche.
- [x] Contratos: aluguel, internet, seguro, financiamento.
- [ ] Arquivos anexos por contrato.
- [x] Alertas de reajuste.
- [ ] Historico de renegociacao.
- [x] Painel de "dinheiro comprometido".

## 4.8 - Automacao e assistente

- [ ] Assistente dentro do app para perguntar sobre os dados.
- [ ] Perguntas: "quanto gastei com mercado este mes?".
- [ ] Perguntas: "posso comprar X agora?".
- [ ] Perguntas: "qual categoria piorou?".
- [ ] Perguntas: "o que vence essa semana?".
- [ ] Criar transacao por comando natural.
- [ ] Criar meta por comando natural.
- [ ] Criar vencimento por comando natural.
- [ ] Gerar resumo semanal automatico.
- [ ] Gerar plano de corte por prioridade.
- [ ] Explicar financas sem julgamento.
- [ ] Modo "coach": me cutuca.
- [ ] Modo "contador": seco e objetivo.
- [ ] Modo "familia": linguagem simples.
- [ ] Comandos por voz.
- [ ] Resposta por voz.
- [ ] Automacoes: se salario caiu, separar percentuais.
- [ ] Automacoes: se gasto passou limite, sugerir ajuste.
- [ ] Automacoes: se sobrou dinheiro, sugerir aporte/meta.

## 4.9 - Experiencia, design e personalizacao

- [x] Temas visuais.
- [x] Tema claro polido.
- [~] Tema alto contraste.
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

Este ciclo organiza itens da fundacao tecnica continua sem mudar a linha principal do produto, e hoje ja tem entregas objetivas no codigo e nos testes.

Itens puxados para este ciclo:

- [x] Criar testes para permissoes de usuario.
- [x] Criar suite de testes para parser de transacao. Entregas atuais: valores com `R$`, salario, conta composta, categorias de saude, recorrencia por texto e entradas invalidas.
- [x] Criar testes para importacao JSON. Entregas atuais: campos estruturais invalidos, vencimentos em `settings.rates`, contadores completos e fixture demo importavel.
- [x] Formalizar schema de backup.
- [x] Criar migracoes de backup por versao.
- [x] Criar fixtures de dados demo. Entrega atual: backup demo com transacoes, vencimento recorrente, lista de compras, meta, orcamento, conta e estado basico de carro.
- [x] Adicionar CI para testes.
- [x] Adicionar validacao de deploy.
- [x] Padronizar comandos `npm run check` e `npm test`.
- [x] Endurecer permissoes defensivas. Entrega atual: `canWrite(null)` bloqueia escrita e `publicUser()` nao quebra nem vaza campos sensiveis.
- [x] Melhorar retorno de importacao. Entrega atual: backend informa contagem de categorias, listas, itens de compra e vencimentos alem de transacoes, orcamentos, metas e contas.

## 4.11 - Performance e foco no controle de gastos (ciclo concluido)

Status de implementacao: dashboard reorganizada para priorizar o fluxo diario, com widgets classificados em essencial/apoio/analise, render parcial em interacoes pontuais do menu e filtros, e `financas.html` reduzido a entrada leve apontando para `index.html`.

- [x] Manter um nucleo sagrado: lancar transacao, ver saldo, ver vencimentos e buscar.
- [x] Tratar inteligencia e widgets analiticos como camada secundaria e opcional.
- [x] Otimizar a dashboard para evitar re-render completo em ajustes pontuais.
- [x] Reordenar a home para priorizar widgets essenciais por padrao.
- [x] Esconder por padrao widgets mais analiticos.
- [x] Reduzir duplicacao entre `index.html` e `financas.html`.
- [x] Revisar a home para ficar mais "controle de gastos" e menos "painel de tudo".
- [x] Definir criterio de produto: se uma feature nao melhora captura, revisao ou decisao de gasto, ela entra depois.

## Fechamento da fase web principal

Status de implementacao: decisao registrada em `2026-04-26` para considerar a linha `4.3.1` consolidada como base principal de financas pessoais, mantendo apenas manutencao ativa, correcoes e polimento antes de deslocar energia de produto para `Finvita` e `FinClinica`.

- [x] Congelar expansao grande de escopo dentro do `Finanza`.
- [x] Manter esta base como fonte principal das operacoes financeiras do ecossistema.
- [x] Alinhar setup e documentacao ao fluxo atual de login por usuario/senha.
- [x] Registrar no roadmap a transicao de foco para manutencao e consolidacao.

## 5.0 - Android nativo premium

- [x] Definir se `android-v4/` vira base oficial da linha `5.0`.
- [ ] Migrar modelo de dados mantendo compatibilidade com backup 4.x.
- [ ] App Android nativo com performance e UX de primeira classe.
- [ ] Widgets Android na tela inicial.
- [ ] Widget de saldo.
- [ ] Widget de vencimentos.
- [ ] Widget de lancamento rapido.
- [ ] Quick Settings tile para lancar despesa.
- [ ] Notificacoes com acoes: pagar, adiar, abrir.
- [ ] Offline-first real com banco local.
- [ ] Sincronizacao em background.
- [ ] Biometria.
- [ ] Bloqueio por PIN.
- [ ] Atalhos de app.
- [ ] Compartilhar texto/recibo para o Finanza.
- [ ] OCR local ou hibrido para comprovantes.
- [ ] Integracao com calendario Android.
- [ ] Backup local criptografado.
- [ ] Experiencia tablet/dobravels.
- [ ] Wear OS: ver vencimentos e lancar gasto rapido.

## 6.0 - Plataforma e produto grande

- [x] App web com frontend modular moderno.
- [x] Separar HTML/CSS/JS em arquitetura mais sustentavel.
- [x] Testes unitarios.
- [ ] Testes e2e.
- [ ] Storybook ou catalogo de componentes.
- [ ] Design system proprio.
- [ ] API versionada.
- [ ] Migrations formais de banco.
- [ ] Observabilidade: logs, metricas, tracing.
- [x] Painel admin.
- [ ] Exportacao LGPD: baixar todos os dados.
- [ ] Exclusao definitiva de conta.
- [ ] Criptografia de dados sensiveis.
- [ ] Backups automaticos agendados.
- [ ] Multi-tenant serio.
- [ ] Plano gratuito/premium, se virar produto.
- [ ] Landing page publica.
- [x] Documentacao publica.
- [~] Modo self-hosted.
- [ ] Docker Compose completo.
- [ ] Instalador local.

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

- [x] Criar suite de testes para parser de transacao.
- [x] Criar testes para importacao JSON.
- [ ] Criar testes para importacao CSV de carro.
- [ ] Criar testes para projecoes.
- [ ] Criar testes para recorrencias e parcelamentos.
- [ ] Criar testes para sync offline.
- [x] Criar testes para permissoes de usuario.
- [~] Separar `frontend/app.js` em modulos menores.
- [x] Separar componentes de dashboard.
- [ ] Separar modulo de carro.
- [ ] Separar modulo de compras.
- [ ] Separar modulo de vencimentos.
- [x] Criar camada unica de storage.
- [x] Criar camada unica de API.
- [x] Formalizar schema de backup.
- [x] Criar migracoes de backup por versao.
- [x] Criar fixtures de dados demo.
- [ ] Adicionar lint/format.
- [x] Adicionar CI para testes.
- [x] Adicionar build Android em CI.
- [x] Adicionar validacao de deploy.

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
