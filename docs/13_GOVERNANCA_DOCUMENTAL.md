# 13 — Governança Documental, Ideias e Evolução do Projeto

> Documento normativo para governança do conhecimento, decisões e continuidade do NEXUS. Não substitui especificações, ADRs ou evidências do código.

## 1. Objetivo

A leitura operacional deve começar por [`00_ORIENTACAO_E_AUTORIDADE.md`](./00_ORIENTACAO_E_AUTORIDADE.md). Esse arquivo define a hierarquia de autoridade e a interpretação obrigatória para agentes; este documento define o processo de governança.

Definir como ideias e decisões são capturadas, investigadas, avaliadas, aprovadas, planejadas, implementadas, revistas e encerradas. A documentação deve explicar não apenas o estado atual, mas também o contexto das decisões, suas justificativas e as condições em que podem ser reconsideradas.

**A documentação é viva. Nenhum documento é imutável por princípio.** Documento Mestre, especificações, arquitetura, ADRs e roadmap podem ser revisados quando a realidade, as necessidades ou as evidências mudarem. A revisão deve preservar histórico e rastreabilidade, não congelar decisões antigas.

## 2. Princípios

1. Preservar o contexto sem perpetuar proibições por acidente.
2. Separar registro, pesquisa, análise, proposta, aprovação, planejamento e implementação.
3. Não apagar a história para atualizar o presente.
4. Reavaliar decisões diante de novas evidências, tecnologias, necessidades ou restrições.
5. Manter coerência entre código, documentos vigentes, ADRs e registros históricos.
6. Não inventar autorização. Na dúvida sobre escopo ou permissão para implementar, explicitar a lacuna antes de agir.
7. Fazer alterações proporcionais, somente nos documentos impactados.
8. Registrar mudanças relevantes no próprio método documental.

## 3. Ciclo de vida das ideias

| Estado | Significado |
| --- | --- |
| CAPTURADA | Ideia preservada, ainda sem investigação suficiente. |
| EM PESQUISA | Busca de referências, tecnologias, alternativas, riscos ou evidências. |
| EM ANÁLISE | Evidências comparadas com objetivos, arquitetura, custos e riscos. |
| PROPOSTA | Existe direção recomendada, ainda não aprovada. |
| APROVADA | Responsável autorizou a direção; isso não significa implementação iniciada. |
| PLANEJADA | Trabalho priorizado e registrado no roadmap/plano de execução. |
| EM IMPLEMENTAÇÃO | Alteração autorizada foi iniciada. |
| IMPLEMENTADA | Existe no código/configuração e foi confirmada por evidência adequada. |
| PAUSADA | Temporariamente suspensa, com motivo e condição de retomada registrados. |
| REJEITADA | Avaliada e não aceita naquele contexto. |
| DESCARTADA | Encerrada como iniciativa atual; o registro histórico permanece. |
| SUBSTITUÍDA | Superada por outra decisão ou proposta, que deve ser referenciada. |

Uma ideia rejeitada, descartada ou pausada pode voltar à pesquisa/análise quando houver justificativa nova. Isso não apaga a avaliação anterior: cria uma nova avaliação ligada ao histórico.

## 4. Registro proporcional

Para ideias relevantes, registrar título/identificador, problema, origem e contexto, estado, evidências, alternativas, riscos, dependências, relações com requisitos/ADRs/módulos, justificativa atual, condição de reavaliação e próximo passo. Ideias incipientes podem ter registro breve. Não criar requisitos, ADRs ou itens de roadmap prematuramente.

## 5. Decisões e ADRs

**Regra de não revogação implícita:** uma proposta, pesquisa, brainstorm, prompt ou relatório posterior não substitui nem suspende uma decisão com status Aceita e Ativa. A decisão continua orientando o trabalho até que o responsável aprove formalmente sua revisão e os documentos afetados sejam atualizados. A data mais recente, isoladamente, não concede autoridade.

Quando uma proposta conflitar com uma decisão vigente, deve declarar qual decisão pretende revisar, por que o contexto mudou, quais alternativas e impactos existem e o que permanece vigente enquanto a revisão não for aprovada.

ADRs registram decisões arquiteturais e consequências; não são proibições eternas.

- Aceita e Ativa: decisão atualmente adotada.
- Substituída: superada por outra, preservando o registro original.
- Depreciada: deixou de ser recomendada, mas pode permanecer por compatibilidade.
- Rejeitada: alternativa não escolhida naquele contexto.

Para revisar uma decisão: localizar ADR e documentos afetados; recuperar contexto e consequências; descrever o que mudou; avaliar impactos técnicos, de produto, privacidade, segurança, custo e manutenção; registrar nova decisão quando relevante; atualizar documentos e referências; preservar o registro anterior.

## 6. Interpretação de escopo

Distinguir explicitamente:
- Restrição estrutural vigente: limite decorrente de princípio/decisão ativa, cuja mudança exige revisão.
- Não planejado: sem compromisso atual de execução.
- Adiado/pausado: reconhecido, mas sem prioridade ou condição atual.
- Rejeitado no contexto atual: não escolhido com as evidências daquela ocasião.
- Descartado: encerrado como iniciativa atual, sem impedir nova avaliação fundamentada.
- Proibição permanente: usar somente quando o motivo for realmente permanente, explícito e justificado.

A ausência atual de IA não significa que toda IA futura seja proibida. A decisão de não depender de backend proprietário não proíbe automaticamente processamento local no dispositivo. Cada proposta deve ser comparada com princípios e decisões vigentes, distinguindo conflito real de interpretação excessivamente ampla.

## 7. Autoridade e autorização

- O responsável pelo projeto decide prioridades, aprova mudanças de produto e autoriza implementação.
- O assistente pode auditar, identificar contradições, pesquisar e propor alterações documentais.
- Brainstorming não vira automaticamente requisito, ADR aceito ou item planejado.
- Autorização para pesquisa ou atualização documental não autoriza alteração de código.
- Se a documentação não permitir determinar autorização, o assistente deve apontar a lacuna e pedir esclarecimento antes de ação irreversível ou implementação.
- Quando houver autorização explícita para uma atualização documental delimitada, os documentos impactados podem ser atualizados dentro do escopo concedido, sem pedir confirmação arquivo a arquivo.

## 8. Revisão documental

Documentos podem mudar quando código, comportamento, requisitos, visão, decisões ou evidências mudarem, ou quando forem encontradas contradições e lacunas.

Procedimento: identificar a regra problemática; consultar fontes e evidências; separar conteúdo vigente de histórico; atualizar somente os arquivos impactados; verificar links, referências e coerência; registrar mudanças metodológicas relevantes em HISTORICO_DOCUMENTACAO.md; atualizar README quando necessário.

Não preservar uma contradição apenas porque aparece em documento mestre. Hierarquia resolve conflitos factuais provisoriamente; contradições recorrentes entre documentos vigentes são defeitos a corrigir.

## 9. Continuidade entre conversas

No início de nova sessão: consultar README e esta governança; verificar estado atual, roadmap e ADRs relevantes; localizar iniciativas abertas; conferir fontes atuais; retomar o próximo passo sem reiniciar discussões concluídas.

No Google AI Studio, manter artefatos operacionais existentes coerentes com /docs conforme o mapeamento estabelecido. Não criar duplicatas nem declarar sincronização sem verificação.

## 10. Relação com documentos

A orientação obrigatória de leitura e autoridade está em `00_ORIENTACAO_E_AUTORIDADE.md`; agentes e colaboradores devem consultá-la antes de qualquer outro material.

README: índice e hierarquia. 01: visão e princípios. 02: requisitos aprovados. 03: arquitetura. 07: estado real. 09: fluxo operacional. 10: roadmap. 11: ADRs. 12: rastreabilidade. HISTORICO_DOCUMENTACAO.md: mudanças no método documental.

## Regra final

**A documentação deve preservar a memória do projeto sem aprisionar o projeto ao seu passado.** Toda decisão tem contexto; todo contexto pode ser reavaliado diante de novas evidências; toda mudança relevante deve ser rastreável.