# Histórico de Evolução da Documentação do NEXUS

Este é o registro central de alterações documentais do NEXUS. **Toda atualização relevante realizada em qualquer documento de /docs/ deve gerar uma entrada aqui**, inclusive correções de estado, inclusão de pesquisas, mudanças de especificação, rastreabilidade, roadmap, governança e registros históricos. Não é changelog do aplicativo nem substitui o histórico Git.

Cada entrada deve explicar motivação, contexto/evidência, documentos afetados, mudanças realizadas, limites e o que não foi alterado. Quando uma atualização ocorrer apenas em memória estratégica ou em um repositório espelho, registrar isso no histórico correspondente daquele repositório e apontar a relação, sem declarar que /docs/ foi atualizado.

---

## 2026-11-21 — Governança Documental e Ciclo de Vida de Ideias

**Tipo:** atualização metodológica.

### Motivação
A documentação descrevia produto, arquitetura, estado e execução, mas não definia suficientemente como ideias novas, decisões antigas, propostas rejeitadas e possibilidades futuras deveriam ser preservadas e reavaliadas. A seção “Fora de Escopo Permanente / Descartado” do roadmap misturava situações distintas e podia transformar uma decisão contextual em aparente proibição definitiva.

### Problemas identificados
- Ausência de ciclo de vida explícito entre conversa, pesquisa, análise, proposta, aprovação, planejamento e implementação.
- Falta de clareza de que documentos vigentes, inclusive o Documento Mestre, podem evoluir.
- Risco de decisões antigas bloquearem reavaliações sem considerar novas evidências.
- Distinção insuficiente entre autorização para pesquisa, aprovação de produto, atualização documental e implementação.
- Continuidade entre conversas dependente de inferências sobre o que foi aprovado.

### Mudanças introduzidas
1. Criado 13_GOVERNANCA_DOCUMENTAL.md como referência normativa.
2. Definidos estados para ideias e iniciativas, da captura à implementação, incluindo pesquisa, análise, proposta, pausa, rejeição, descarte e substituição.
3. Estabelecido que rejeição ou descarte contextual não significa proibição eterna.
4. Reforçada a distinção entre discussão, proposta, aprovação, planejamento e implementação.
5. Esclarecido que documentos mestres, especificações, arquitetura, ADRs e roadmap são revisáveis, preservando histórico.
6. Reorganizada a leitura de escopo no roadmap.
7. Complementado o guia de desenvolvimento com o fluxo de governança.
8. Atualizado o índice documental.

### Limites
- Nenhuma funcionalidade do aplicativo foi aprovada ou implementada.
- Nenhum requisito de IA local, reconhecimento de voz, integração bancária ou importação inteligente foi criado.
- Nenhum código Android, banco Room, backup do aplicativo ou ADR técnico foi alterado.
- Artefatos internos do Google AI Studio não foram modificados nesta operação; deverão ser alinhados quando estiverem acessíveis.
- Não foram executados testes de build ou Android, pois não houve alteração de código.

### Arquivos afetados
- docs/README.md
- docs/01_DOCUMENTO_MESTRE.md
- docs/09_GUIA_DESENVOLVIMENTO.md
- docs/10_ROADMAP.md
- docs/13_GOVERNANCA_DOCUMENTAL.md (novo)
- docs/HISTORICO_DOCUMENTACAO.md (novo)

### Resultado esperado
Uma sessão futura deverá distinguir ideia, pesquisa, análise, proposta, aprovação, planejamento e implementação. Decisões antigas continuarão consultáveis sem bloquear automaticamente novas soluções quando o contexto mudar.

---

## 2026-11-22 — Reforço da rastreabilidade documental e reabertura factual do BUG-001

**Tipo:** atualização de estado técnico documentado e regra de manutenção do histórico.

### Motivação e evidência
Após instalar e testar a versão que continha o lote visual/adaptativo, o usuário relatou novamente que girar o celular ou tablet durante o uso de diferentes abas pode retornar à tela inicial. Exemplo explícito: editar uma tarefa já existente e perder a tela de edição ao girar o dispositivo. O relato confirma que BUG-001 continua perceptível no uso real; não identifica por si só a causa completa nem prova quais telas são afetadas.

### Mudanças introduzidas
1. Reforçado que BUG-001 permanece EM ABERTO, com novo relato posterior à versão visual/adaptativa.
2. Explicitada a separação entre adaptação visual da interface e preservação funcional da rota/edição durante recriação da Activity.
3. Acrescentada rastreabilidade do BUG-001 na matriz, vinculando shell/navegação, editores, estado, investigação e validação necessária.
4. Tornado este arquivo o registro obrigatório de todos os lotes relevantes de atualização documental em /docs/, não apenas mudanças metodológicas.

### Arquivos afetados
- docs/07_ESTADO_ATUAL.md
- docs/12_RASTREABILIDADE.md
- docs/HISTORICO_DOCUMENTACAO.md

### Limites e itens não alterados
- Nenhum código Android foi alterado.
- Nenhum diagnóstico técnico novo foi executado nesta atualização.
- Nenhum build, teste automatizado ou teste de dispositivo foi executado pelo assistente nesta operação.
- BUG-001 não foi corrigido nem validado.
- Nenhum ADR foi alterado.
- Os artefatos internos do Google AI Studio não foram atualizados; o ambiente conectado e seu conteúdo não foram acessados nesta operação.

### Próximo passo
Realizar investigação técnica abrangente do estado de navegação e edição, propor uma correção transversal e validar em dispositivo em diferentes telas/abas e etapas de edição. Não encerrar BUG-001 com base apenas em compilação bem-sucedida ou layout adaptativo.

---

## 2026-11-22 — Registro da pesquisa e direção adaptativa da Agenda

**Tipo:** consolidação documental de feedback pós-implementação visual.

### Motivação
O usuário aprovou a aparência dos cartões semanais no celular, mas considerou compactas demais as informações da Agenda semanal e insatisfatória a composição em tablet na orientação horizontal. Solicitou painel semanal ocupando a altura útil, rolagem interna independente por dia, coerência visual com Kanban e pesquisa de padrões adaptativos.

### Arquivos afetados no repositório estratégico NEXUS-DOCUMENTACAO
Esta entrada documenta a relação com a memória estratégica; não significa que os arquivos abaixo estejam em /docs/:
- CHATGPT/09_METODOLOGIA_DE_PESQUISA_E_DESENVOLVIMENTO.md (novo)
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/PESQUISA_LAYOUTS_ADAPTATIVOS_E_ROLAGEM.md (novo)
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/DIRECAO_LAYOUTS_ADAPTATIVOS.md
- CHATGPT/03_IDEIAS_E_BRAINSTORMS.md
- CHATGPT/06_PENDENCIAS_E_DIRECAO_FUTURA.md
- CHATGPT/07_HISTORICO_DE_REGISTROS.md
- CHATGPT/README.md
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/README.md

### Limites
- A IDEA-006 segue em pesquisa/análise.
- Não houve implementação adicional nem alteração de código por essa atualização.
- O registro detalhado e as fontes estão no repositório estratégico indicado acima.

---

## Modelo para próximos registros

Registrar data, tipo, motivação, evidência/origem, documentos afetados (com caminhos e repositórios), mudanças introduzidas, estado resultante, limites, itens não alterados e próximos passos. Toda atualização documental relevante em /docs/ deve ser registrada neste arquivo.


## 2026-11-23 — Hierarquia de autoridade e prevenção de contradições

**Tipo:** revisão estrutural de governança e clareza documental.

### Motivação
O responsável identificou risco de agentes encontrarem decisões técnicas vigentes e propostas posteriores de revisão sem indicação explícita de que a proposta não revoga a decisão. O exemplo principal era a arquitetura local/offline-first e a possibilidade futura de serviços online, sincronização e integrações.

### Diagnóstico
- O índice técnico possuía hierarquia de fontes, mas não definia suficientemente o efeito normativo de propostas e brainstorms.
- O ADR-003 usava “Permanente”, embora a governança permita revisar decisões.
- Memória estratégica e documentação técnica convivem em repositórios distintos.
- “100% sem internet” é impreciso em sentido literal, pois o Mercado utiliza provedores externos.

### Mudanças
1. Criado `docs/00_ORIENTACAO_E_AUTORIDADE.md`, leitura obrigatória antes dos demais documentos.
2. Reforçada no README a precedência entre código, documentação técnica, espelho, propostas e histórico.
3. Ajustado o Documento Mestre para descrever arquitetura local-first/offline-first, sem dependência obrigatória de nuvem e sem sugerir ausência absoluta de rede.
4. Revisado o status do ADR-003 de “Aceita e Permanente” para “Aceita e Ativa — vigente até eventual revisão formal”.
5. Acrescentada regra explícita de que propostas, brainstorms e relatórios não revogam decisões vigentes por data ou presença documental.

### Limites
- Nenhuma decisão sobre sincronização, backend, login, Open Finance ou IA foi aprovada.
- Arquitetura implementada e código Android não foram alterados.
- Nenhum teste ou build foi executado.
- Espelho e memória estratégica ainda precisam ser sincronizados; artefatos internos do AI Studio não foram acessados.

### Arquivos afetados
- `docs/00_ORIENTACAO_E_AUTORIDADE.md` (novo)
- `docs/README.md`
- `docs/01_DOCUMENTO_MESTRE.md`
- `docs/11_DECISOES_ADRS.md`
- `docs/13_GOVERNANCA_DOCUMENTAL.md`
- `docs/HISTORICO_DOCUMENTACAO.md`

### Resultado esperado
Agentes distinguem arquitetura vigente de propostas de evolução. Uma proposta não produz comportamento contraditório antes de ser aprovada e refletida nos documentos normativos.
