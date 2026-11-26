# Guia operacional do ChatGPT para integração com o NEXUS

> **Estado: ESTRUTURA INICIAL — NÃO USAR AINDA PARA GERAR JSON DE PRODUÇÃO.**
>
> Este documento deverá ser completado pelo Google AI Studio após a estabilização dos modelos e fluxos da Rodada 4. O conteúdo final precisa ser extraído do código real do repositório `enzonunezmachado/PROJETO-NEXUS-`, especialmente DTOs, enums, validadores, relações, processador de entrada e testes. Não inventar nomes de campos, valores de enum, identificadores ou formatos.

## 1. Objetivo

Ensinar o ChatGPT a converter pedidos em linguagem natural em operações estruturadas que o NEXUS realmente aceite, sem inventar campos ou simular recursos ausentes.

O guia é um manual de uso do contrato, não uma especificação independente. O código e os testes são a fonte de verdade. Sempre indicar a versão mínima do aplicativo e do contrato compatível. O Google AI Studio trabalha somente no workspace local disponível: não acessa GitHub, não faz Pull/Push nem executa GitHub Actions. O usuário sincroniza manualmente o workspace com o repositório; o ChatGPT pode então verificar os arquivos e a CI no GitHub.

## 2. Regras obrigatórias para quem prepara entradas

- Ler este guia e verificar sua versão antes de produzir JSON.
- Confirmar que a funcionalidade solicitada é suportada pela versão instalada do NEXUS.
- Não transformar uma série recorrente em vários registros avulsos.
- Não inventar IDs de entidades relacionadas; usar referências resolvíveis e operações na ordem correta.
- Não confundir checklist com tarefas independentes, notas ou lembretes.
- Respeitar prévia, validação, confirmação, idempotência, conflitos e rollback.
- Não incluir credenciais, tokens ou dados pessoais que não sejam necessários.
- Quando faltar informação essencial, perguntar antes de gerar a operação.
- Quando o pedido exceder o contrato atual, explicar a limitação e não criar JSON que finja suportá-la.

## 3. Matriz de capacidades

> **A preencher pelo Google AI Studio com base no código.** Para cada entidade/ação, registrar versão do app, versão do contrato, campos obrigatórios/opcionais, enumerações, relações, limites e operações suportadas.

| Domínio | Capacidades atuais | Capacidades após Rodada 4 | Observações |
|---|---|---|---|
| Tarefas e checklists | A confirmar no contrato/código atual | Checklist independente por ocorrência recorrente | Preencher com evidência |
| Compromissos | Registros únicos no contrato v1 | Recorrência e checklist opcional | Preencher após implementação |
| Eventos | Registros únicos no contrato v1 | Recorrência e checklist opcional | Preencher após implementação |
| Projetos e objetivos | A confirmar | Relações completas suportadas | Preencher com evidência |
| Finanças | A confirmar | Vínculos e regras financeiras reais | Preencher com evidência |
| Ícones/contexto | Não presumir campo disponível | Catálogo controlado se aprovado/implementado | Nunca inventar identificadores |

## 4. Contrato JSON

> **A preencher pelo Google AI Studio.** Copiar a estrutura real de `StructuredInputModels.kt` e os validadores. Documentar envelope, versão, operações, entidades, ações, tipos, formatos de data/hora, IDs, erros e limites.

## 5. Exemplos completos e validados

Cada exemplo final deve ser validado pelos mesmos parsers/testes utilizados pelo aplicativo. Incluir, no mínimo:

1. Tarefa simples.
2. Tarefa com checklist.
3. Rotina semanal de academia com atividades em dias diferentes, horários, recorrência e checklists por atividade.
4. Compromisso único e compromisso recorrente.
5. Compromisso com checklist próprio (preparação/itens necessários).
6. Evento único e evento recorrente.
7. Evento com checklist próprio e compromissos vinculados.
8. Projeto com tarefas relacionadas.
9. Objetivo pessoal com relações suportadas.
10. Despesa vinculada a tarefa, evento ou projeto, respeitando regras financeiras.
11. Atualização de registro existente.
12. Exceção: alterar apenas uma ocorrência ou a série inteira.
13. Operações encadeadas no mesmo lote e ordem de dependências.
14. Exemplo de conflito preservando dados locais.
15. Exemplo de prévia, confirmação, descarte e retentativa idempotente.

Para cada caso, explicar a intenção em linguagem natural, o JSON integral, os campos importantes, a ordem de processamento, o resultado esperado e os erros que devem ser evitados. Não usar dados pessoais reais como exemplo sem necessidade.

## 6. Guia de decisão: qual entidade usar?

> **A preencher pelo Google AI Studio**, com exemplos positivos e negativos para diferenciar Tarefa, Checklist, Compromisso, Evento, Projeto, Objetivo e Transação.

## 7. Recorrência, séries e exceções

> Documentar o modelo real após Rodada 4: frequências, fuso horário, âncora, limites, exceções, identidade de ocorrência, edição de uma ocorrência/série, checklists-base e estado independente por ocorrência.

## 8. Checklists por tipo de registro

Documentar separadamente:
- Checklist de Tarefa.
- Checklist de cada ocorrência de Tarefa recorrente.
- Checklist opcional de Compromisso.
- Checklist opcional de Evento.
- Relação entre checklist e tarefas independentes vinculadas.
- Regras de alteração do modelo, histórico, backup, merge e sincronização.

## 9. Ícones e identidade contextual

> Documentar o catálogo real permitido pelo aplicativo, se implementado. Incluir contexto (ex.: academia, corrida, consulta, viagem, show e reunião), fallback e regras de apresentação. O ChatGPT só pode emitir identificadores presentes no catálogo versionado.

## 10. Erros comuns e validação

Listar erros reais encontrados nos testes e formas corretas de evitá-los. Incluir campos ausentes, enums inválidos, IDs inexistentes, datas/horários, referências circulares, duplicação, conflitos e incompatibilidade de versão.

## 11. Compatibilidade e histórico de versões

Registrar versões do app, do contrato e do próprio guia. Explicar como o ChatGPT deve agir quando o guia e a versão do aplicativo não forem compatíveis.

## 12. Checklist de manutenção do guia

A cada mudança em modelos, DTOs, enums, validadores, relações ou operações:
1. Atualizar este guia no mesmo lote de implementação.
2. Validar cada JSON de exemplo com testes reais do processador.
3. Atualizar matriz de capacidades e versão mínima.
4. Sincronizar o espelho documental.
5. Registrar alteração no histórico.
6. Não declarar o guia operacional até que os exemplos estejam validados.

## 13. Limite de escopo

Este guia cobre a preparação de entradas para a ponte atual e futura. O catálogo exportável de capacidades e o snapshot do estado real do NEXUS serão definidos separadamente na Rodada 6. Não confundir este manual estático com os dados atuais do usuário.
