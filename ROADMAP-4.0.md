# Finanza 4.0 Roadmap

Este documento congelou a release `3.9 final`, guiou a trilha principal da `4.0` e agora passa a registrar o fechamento dessa etapa.

## Estado atual

- Web + app Capacitor: `4.0 concluida`
- API Node/Express + PostgreSQL: linha principal da `4.0`
- Android Kotlin separado em `android-v4/`: continua existindo, mas nao e a frente principal agora

## Decisao de produto

A linha principal do Finanza nesta etapa foi a **versao 4.0**.

Essa fase vai manter:

- o frontend web atual
- o shell mobile com **Capacitor**
- a API atual em Node/Express
- o banco PostgreSQL atual

O app Android nativo em Kotlin sera renomeado para **5.0**, mas ficara para uma etapa posterior.

## Fechamento da 4.0

Status geral: `concluida e pronta para evolucao incremental`

### Entregas consolidadas

- confiabilidade de dados com estado local / online / sincronizado
- historico de sincronizacao e fila offline visivel
- backup JSON/CSV mais acessivel, com importacao, validacao e preview
- deduplicacao em importacoes JSON e CSV do carro
- desfazer exclusoes nas areas principais
- comparativo mensal e previsao ate o fim do mes
- central de pendencias, busca global, recorrencias e parcelamentos
- modulo do carro com manutencao, comparativos, postos/oficinas e historico mais rico
- multiusuario funcional com login, usuarios e separacao por `user_id`
- atividade importante visivel no app para acoes principais

### Itens que ficam como acabamento ou proxima iteracao

- papeis alem de `admin`
- auditoria completa no backend com autor por registro e trilha persistente no servidor
- reconciliacao offline com resolucao de conflito por item

## Escopo original da 4.0

### 1. Confiabilidade dos dados

- indicador claro de salvo local / online / sincronizado
- historico de sincronizacao
- exportacao de backup mais acessivel
- restauracao com validacao e preview
- versionamento mais claro de backup

### 2. Blindagem contra perda e duplicacao

- deduplicacao em importacoes CSV e JSON
- preview antes de importar
- lixeira temporaria / desfazer exclusoes
- reconciliacao de sync offline

### 3. Fluxo financeiro

- comparativo mes atual vs mes anterior
- previsao ate o fim do mes
- central de pendencias
- busca global
- melhorias em recorrencias e parcelamentos

### 4. Modulo do carro

- manutencao por data e km
- comparativo por veiculo
- custo por posto / oficina
- filtros mais fortes para tanque cheio
- visualizacao historica mais rica

### 5. Multiusuario

- separacao mais clara entre contas e perfis
- papeis basicos
- melhor visibilidade de quem alterou/importou dados importantes

## Depois da 4.0

### Kotlin 5.0

Quando a trilha principal 4.0 estiver estavel no web + Capacitor, o app nativo em Kotlin passa a ser tratado como uma nova linha:

- nome alvo: `5.0`
- foco: experiencia Android premium, widgets, notificacoes, performance e recursos locais
- migracao de dados mantendo compatibilidade com os backups da linha 3.9/4.0

## Regra de versao daqui para frente

- `3.9 final`: congelada como base estavel
- `4.0`: concluida como linha principal web + Capacitor + API
- `4.1`: proxima etapa incremental de refinamento e profundidade
- `5.0`: app Kotlin nativo, planejado para depois
