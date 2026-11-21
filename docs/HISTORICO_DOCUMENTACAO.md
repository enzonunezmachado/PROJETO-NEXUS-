# Histórico de Evolução do Modelo Documental

Este arquivo registra mudanças relevantes no próprio sistema de documentação do NEXUS. Não é changelog do aplicativo nem substitui o histórico Git.

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

## Modelo para próximos registros

Registrar data, tipo, motivação, problema, documentos e regras afetados, mudanças introduzidas, limites, itens não alterados e resultado esperado.
