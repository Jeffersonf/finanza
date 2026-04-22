# Finanza 4.0 Roadmap

Este documento congela a release atual como `3.9 final` e define a proxima trilha principal do produto.

## Estado atual

- Web + app Capacitor: `3.9 final`
- API Node/Express + PostgreSQL: linha `3.9.3`
- Android Kotlin separado em `android-v4/`: continua existindo, mas nao e a frente principal agora

## Decisao de produto

A proxima evolucao principal do Finanza sera a **versao 4.0**.

Essa fase vai manter:

- o frontend web atual
- o shell mobile com **Capacitor**
- a API atual em Node/Express
- o banco PostgreSQL atual

O app Android nativo em Kotlin sera renomeado para **5.0**, mas ficara para uma etapa posterior.

## Escopo proposto para 4.0

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
- `4.0`: proxima linha principal do produto
- `5.0`: app Kotlin nativo, planejado para depois
