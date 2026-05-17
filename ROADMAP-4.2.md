# Finanza 4.2 Sugestoes

Ideias guardadas para uma etapa posterior, depois do aprofundamento da previsao na `4.1`.

## Prioridade alta registrada em 2026-05-13

- corrigir lag alto percebido no site, com investigacao de boot, dashboard, graficos, listas, localStorage e sincronizacao
- reformular a barra lateral como ferramenta de trabalho diaria, com hierarquia melhor, atalhos configuraveis, indicadores de estado e comportamento mobile/desktop consistente
- reconstruir a previsao de gastos como pilar do produto, usando dados reais e separando fixos, recorrentes, parcelados, vencimentos, variaveis por categoria e entradas esperadas
- entregar previsao com cenarios conservador, realista e otimista, explicacao dos numeros, margem segura para gastar hoje e risco de fechar o mes negativo
- transformar previsao em decisao: cortes sugeridos, limites por categoria, alertas de compromisso futuro e proximas acoes claras

## Sugestoes de produto

- metas com simulador: "se eu guardar X por mes, chego quando?"
- previsao por categoria com sazonalidade: mercado, carro, assinaturas e saude
- alertas de anomalia melhores: gasto subiu 40% contra media de 6 meses
- calendario financeiro mensal com vencimentos, entradas e parcelas no mesmo lugar
- planejamento por objetivo: viagem, reserva, carro, reforma
- dashboard com "modo familia" e "modo carro"
- consolidado de patrimonio: contas, metas e investimentos no mesmo painel
- benchmark pessoal: melhor mes, pior mes, media de 12 meses, recordes
- importador financeiro mais universal para extratos bancarios simples
- resumo semanal automatico com linguagem humana

## Sugestoes tecnicas

- cache local versionado para migracoes mais seguras
- trilha de auditoria no backend por entidade
- snapshots compactos para restauracao rapida
- testes automatizados para importacao e projeções
