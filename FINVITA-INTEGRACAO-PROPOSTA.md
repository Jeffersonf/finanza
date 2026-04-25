# Proposta Finvita + Finanza

Documento de referência para uma futura integração entre `Finvita` e `Finanza`.

## Objetivo

Definir o `Finvita` como produto de organização pessoal da vida, complementar ao `Finanza`, que continua focado em operação financeira.

## Visão resumida

- `Finanza`: dinheiro, transações, contas, vencimentos, orçamento, metas financeiras, importação e conciliação.
- `Finvita`: rotina, prioridades, projetos pessoais, hábitos, listas, agenda e contexto de vida.
- Integração: o `Finvita` envia intenção e contexto; o `Finanza` devolve impacto e status financeiro.

## Divisão sugerida

### Fica no Finanza

- transações
- contas, saldos e rendimento
- vencimentos
- orçamento
- metas financeiras
- importação de extrato e conciliação
- relatórios financeiros
- custos do carro quando o foco for financeiro

### Vai para o Finvita

- rotina
- agenda pessoal
- hábitos
- projetos pessoais
- listas e checklists
- prioridades da semana
- áreas da vida
- organização de casa, saúde, estudos e trabalho pessoal
- planejamento de compras como intenção
- objetivos pessoais não necessariamente financeiros

## Exemplos de integração

- lista de compras no `Finvita` pode gerar sugestão de lançamento no `Finanza`
- projeto no `Finvita` pode ler orçamento ou meta do `Finanza`
- meta pessoal no `Finvita` pode exibir progresso financeiro vindo do `Finanza`
- compromisso no `Finvita` pode se vincular a um vencimento no `Finanza`
- viagem, mudança ou reforma no `Finvita` pode ter vínculo financeiro no `Finanza`

## Prompt salvo para uso futuro

```text
Quero que você atue como estrategista de produto, UX designer e arquiteto de software para me ajudar a definir o Finvita.

Contexto geral:
- Eu tenho dois produtos:
  - Finanza: app principal de finanças pessoais
  - Finvita: app de organização pessoal da vida
- O Finanza deve continuar extremamente focado em dinheiro e operação financeira.
- O Finvita deve complementar o Finanza, ajudando a organizar vida, rotina, prioridades e projetos pessoais.
- Eu não quero dois apps disputando a mesma função.
- O objetivo é que o Finvita desafogue o Finanza, absorvendo o que não é núcleo financeiro, mas sem perder integração entre os dois.

Posicionamento desejado:
- Finanza = controle financeiro real
- Finvita = organização pessoal e operacional da vida
- O Finvita deve organizar contexto, intenção, rotina e execução pessoal.
- O Finanza deve registrar, acompanhar e projetar impacto financeiro.

Divisão de responsabilidades esperada:

No Finanza devem continuar:
- transações
- contas, saldos e rendimento
- vencimentos
- orçamento
- metas financeiras
- importação de extrato / conciliação
- relatórios financeiros
- custos com carro quando forem financeiros

No Finvita devem ficar:
- rotina
- agenda pessoal
- hábitos
- projetos pessoais
- listas e checklists
- prioridades da semana
- áreas da vida
- organização de casa, saúde, estudos, trabalho pessoal
- planejamento de compras como intenção
- organização de objetivos pessoais não necessariamente financeiros

Integração desejada entre os apps:
- Finvita envia contexto e intenção para o Finanza
- Finanza devolve status financeiro para o Finvita
- Os apps devem conversar sem se misturar
- Quero uma integração útil, prática e estratégica

Exemplos do que pode acontecer:
- uma lista de compras no Finvita pode gerar ou sugerir lançamento no Finanza
- um projeto no Finvita pode ter orçamento lido do Finanza
- uma meta pessoal no Finvita pode mostrar progresso financeiro vindo do Finanza
- um compromisso no Finvita pode ser vinculado a um vencimento no Finanza
- uma viagem, reforma, mudança ou objetivo de vida pode existir no Finvita com vínculo financeiro no Finanza

Quero que você me entregue uma proposta completa e prática, organizada exatamente nestas seções:

1. Posicionamento do Finvita
- explique claramente qual o papel do Finvita
- explique como ele complementa o Finanza
- explique o que ele nunca deve virar

2. Limites entre Finvita e Finanza
- tabela ou lista clara do que fica em cada produto
- mostre zonas cinzentas e como decidir onde algo deve morar

3. MVP do Finvita
- quais módulos devem entrar na primeira versão
- quais problemas reais essa primeira versão resolve
- o que eu não devo colocar no MVP

4. Estrutura de navegação
- proponha a navegação principal do Finvita
- diga quais seriam as seções e telas principais
- nomeie essas áreas de forma clara e boa para produto real

5. Fluxos principais
- descreva os fluxos mais importantes do usuário
- ex: capturar tarefa, planejar semana, acompanhar projeto, organizar compras, conectar algo ao Finanza

6. Integração com o Finanza
- proponha como os dois apps conversam
- diga quais ações podem ser unidirecionais e quais devem ser bidirecionais
- sugira eventos, vínculos e objetos compartilháveis entre os dois sistemas

7. Modelo de dados inicial
- proponha as entidades principais do Finvita
- ex: projetos, tarefas, hábitos, áreas da vida, listas, eventos, vínculos com Finanza
- inclua os relacionamentos principais entre elas
- não precisa escrever SQL ainda, mas quero um modelo lógico bem claro

8. Banco e arquitetura
- sugira uma arquitetura inicial simples e sustentável
- diga se faz sentido banco separado ou compartilhado com o Finanza
- diga como sincronizar os dados entre os apps
- proponha uma estratégia segura e evolutiva

9. Roadmap
- divida em:
  - V1
  - V1.5
  - V2
- diga o que entra em cada fase e por quê

10. UX e produto
- diga como evitar que o Finvita fique confuso, inchado ou genérico
- diga como fazer ele parecer útil no dia a dia
- proponha princípios de UX para manter foco, leveza e clareza

11. Oportunidades estratégicas
- diga quais recursos podem criar sinergia forte com o Finanza
- diga quais recursos podem aumentar retenção e valor do ecossistema

12. Riscos
- diga os principais riscos de produto, escopo, UX e arquitetura
- diga como reduzir cada um

Quero também que você inclua no final:

13. Proposta objetiva de telas
- lista de telas do Finvita
- uma frase descrevendo a função de cada tela

14. Proposta de integrações concretas com o Finanza
- exemplos reais de uso
- nome do dado compartilhado
- direção do fluxo
- benefício para o usuário

15. Recomendações finais
- um resumo franco do que vale fazer agora
- o que evitar
- o que teria maior impacto

Instruções importantes:
- não escreva resposta genérica
- não trate isso como brainstorming solto
- pense como alguém definindo um produto real
- seja estratégico, prático e claro
- prefira profundidade útil a frases bonitas
- se identificar conflitos de escopo entre Finvita e Finanza, destaque isso explicitamente
- se achar melhor, proponha convenções de nomenclatura e organização entre os dois produtos
- se fizer sentido, sugira uma visão de ecossistema Finanza + Finvita

Depois dessa proposta, quero que você esteja pronto para a próxima etapa, que pode ser uma destas:
- transformar isso em sitemap
- transformar isso em backlog
- transformar isso em arquitetura técnica
- transformar isso em wireframes
- transformar isso em plano de implementação
```

## Próximo uso recomendado

Quando chegar a hora da integração, usar este arquivo como ponto de partida para:

- sitemap do `Finvita`
- backlog inicial
- arquitetura técnica da integração
- modelo de dados compartilhado
- fluxos entre `Finvita` e `Finanza`
