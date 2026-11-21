# NEXUS — Documentação Oficial de Produto e Engenharia

> **Entrada principal da documentação do repositório ativo `PROJECT_NEXUS`.**  
> **Última revisão documental:** 2026-11-20 · **Baseline de código auditada:** `com.nexus.app` (`versionCode = 7`, `versionName = "5.2.0"`, Room `DATABASE_VERSION = 6`, Backup `CURRENT_FORMAT_VERSION = 7`, Módulo Mercado Lotes `01–09` concluídos no código).

---

## 1. Propósito Desta Documentação

Esta suíte documental em `/docs/` constitui a **planta completa de produto, arquitetura, dados, comportamentos, design, decisões e estado atual** do aplicativo Android **NEXUS**. Ela foi estruturada para que qualquer desenvolvedor ou inteligência artificial sem acesso a chats anteriores compreenda o projeto integralmente, continue a evolução sem regressões ou reconstrua o sistema com fidelidade.

---

## 2. Regra de Resolução de Conflitos (Hierarquia da Verdade)

Em caso de dúvida entre documentos ou registros históricos, siga rigorosamente esta ordem de precedência:

1. **Código-fonte real em `app/src/` e configuração Gradle/Manifest (`build.gradle.kts`, `AndroidManifest.xml`)** — fonte máxima do que existe hoje em execução/estrutura.
2. **Documentação técnica vigente em `/docs/*.md` (documentos `01` a `12`)** — retrato consolidado e auditado do sistema atual.
3. **Documento de continuidade nos artefatos do AI Studio (`NEXUS_CONTINUIDADE.md`)** — sincronizado com `/docs/`.
4. **Documentos históricos em `/docs/archive/`** — preservam contexto de origem, especificações anteriores (`v4`/`v5`) e diagnósticos pontuais; **não devem ser tratados como retrato do estado atual** sem conferência no código.

---

## 3. Mapa Completo e Ordem Recomendada de Leitura

### Documentos Vigentes (`/docs/`)

| Ordem | Arquivo | Finalidade Principal | Quando Consultar |
| --- | --- | --- | --- |
| **0** | [`README.md`](./README.md) | Índice documental, mapa de leitura, hierarquia da verdade e matriz vigente vs. histórico | Sempre como ponto de partida |
| **1** | [`01_DOCUMENTO_MESTRE.md`](./01_DOCUMENTO_MESTRE.md) | Visão do produto, problema resolvido, pilares, limites e resumo executivo | Entender o todo em 5 minutos |
| **2** | [`02_ESPECIFICACAO_PRODUTO.md`](./02_ESPECIFICACAO_PRODUTO.md) | Requisitos (`REQ-*`), regras de negócio e especificação completa dos 13 módulos | Definir comportamento funcional e regras de domínio |
| **3** | [`03_ARQUITETURA_ENGENHARIA.md`](./03_ARQUITETURA_ENGENHARIA.md) | Arquitetura em camadas, DI (`AppContainer`), navegação por estado, diagramas C4, rede e workers | Planejar mudanças estruturais ou novos componentes |
| **4** | [`04_MODELO_DADOS.md`](./04_MODELO_DADOS.md) | Dicionário completo das 27 tabelas Room, enums, stores não-Room (Mercado/Widgets), migrações `v1→v6` e Backup `v1→v7` | Qualquer leitura/escrita persistente ou migração |
| **5** | [`05_FLUXOS_COMPORTAMENTOS.md`](./05_FLUXOS_COMPORTAMENTOS.md) | Fluxos ponta a ponta (Agenda, Tarefas/Recorrência, Cartão/Fatura, Eventos, Mercado/Alertas, Rotina, Widgets, Erros) | Implementar ou depurar interações entre módulos |
| **6** | [`06_DESIGN_SYSTEM.md`](./06_DESIGN_SYSTEM.md) | Tokens reais (`NexusPalette`), tipografia, formas, gráficos Canvas, estados padrão e widgets Glance dual-theme | Criar ou ajustar UI, gráficos e widgets |
| **7** | [`07_ESTADO_ATUAL.md`](./07_ESTADO_ATUAL.md) | Auditoria factual por módulo (`IMPLEMENTADA`, `PARCIAL`, `NÃO IMPLEMENTADA`, `INCONSISTENTE`, `NÃO VERIFICADO`) e dívidas abertas | Saber exatamente o que existe vs. o que falta |
| **8** | [`08_QUALIDADE_SEGURANCA.md`](./08_QUALIDADE_SEGURANCA.md) | Offline-first, privacidade, integridade transacional Room ↔ Mercado, permissões, tratamento de erros, testes JUnit e CI | Avaliar riscos, testes, segurança e pipeline |
| **9** | [`09_GUIA_DESENVOLVIMENTO.md`](./09_GUIA_DESENVOLVIMENTO.md) | Convenções Kotlin/Compose, prevenção de OOM no compilador K2, checklist por lote e manutenção documental | Antes e ao final de qualquer alteração de código |
| **10** | [`10_ROADMAP.md`](./10_ROADMAP.md) | Separação clara de entregas concluídas, planejadas/pendentes, dívidas técnicas e itens fora de escopo | Priorizar próximos lotes |
| **11** | [`11_DECISOES_ADRS.md`](./11_DECISOES_ADRS.md) | Architecture Decision Records (`ADR-001`..`ADR-014`) com status, alternativas e consequências | Entender o porquê das escolhas arquiteturais |
| **12** | [`12_RASTREABILIDADE.md`](./12_RASTREABILIDADE.md) | Matriz `Requisito → Funcionalidade → Módulo → Componente → Dados → Fluxo → Teste/ADR` | Rastrear impacto completo de qualquer mudança |

### Documentos Históricos e Arquivados (`/docs/archive/`)

| Arquivo Arquivado | Natureza Histórica | Substituído por |
| --- | --- | --- |
| [`archive/NEXUS_ARCHITECTURE_AND_PROGRESS_V4.md`](./archive/NEXUS_ARCHITECTURE_AND_PROGRESS_V4.md) | Snapshot técnico da fase Room `v4` / `5.0.0` | `03_ARQUITETURA_ENGENHARIA.md`, `04_MODELO_DADOS.md`, `07_ESTADO_ATUAL.md` |
| [`archive/NEXUS_SPEC_HISTORICO.md`](./archive/NEXUS_SPEC_HISTORICO.md) | Plano de evolução de Etapas 1–6 sobre a base `v4` | `02_ESPECIFICACAO_PRODUTO.md`, `10_ROADMAP.md`, `11_DECISOES_ADRS.md` |
| [`archive/DIAGNOSTICO_COMPILACAO_2026-11-17.md`](./archive/DIAGNOSTICO_COMPILACAO_2026-11-17.md) | Diagnóstico do OOM do compilador K2 e correções das Builds #3 e #4 do CI | `09_GUIA_DESENVOLVIMENTO.md` e `08_QUALIDADE_SEGURANCA.md` |

---

## 4. Sincronização com Artefatos do Google AI Studio

Quando o projeto for continuado dentro do Google AI Studio, os artefatos da sessão (`NEXUS_CONTINUIDADE.md`, `NEXUS_PRODUCT_SPEC.md`, `NEXUS_DESIGN_SYSTEM.md`, `NEXUS_INTEGRATION_MAP.md`, `NEXUS_IMPLEMENTATION_ROADMAP.md`, `NEXUS_CURRENT_STATE.md`) devem permanecer alinhados aos documentos `/docs/` do repositório ativo — sendo os arquivos em `/docs/` a fonte portátil que acompanha exportações e pacotes `.zip` do código-fonte.
