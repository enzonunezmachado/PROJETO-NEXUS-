# 00 — Orientação obrigatória de leitura e autoridade documental

> Leia este arquivo antes de consultar qualquer outro documento ou executar trabalho no NEXUS.

## Regra principal
A documentação preserva a evolução do NEXUS, mas nem todo conteúdo tem autoridade para orientar o estado atual. Um texto encontrado não é automaticamente decisão, requisito, autorização ou instrução de implementação.

Na dúvida, não combine orientações incompatíveis. Identifique o estado de cada uma, aplique a hierarquia abaixo e registre a lacuna se ela não puder ser resolvida.

## Hierarquia de autoridade

**Para fatos sobre o que está implementado:**
1. Código e configuração atuais do repositório ativo `enzonunezmachado/PROJETO-NEXUS-`.
2. Documentação técnica vigente em `docs/`, especialmente estado atual, especificações, arquitetura e ADRs.
3. Espelho em `NEXUS-DOCUMENTACAO/GOOGLE_IA/docs/`, que acompanha a origem, mas não prevalece sobre ela.
4. Artefatos internos do Google AI Studio, como memória operacional complementar.
5. Relatórios, snapshots e documentos arquivados, apenas como evidência histórica.

**Para visão e intenção de produto:**
1. Decisões explicitamente aprovadas pelo responsável e registradas em `NEXUS-DOCUMENTACAO/CHATGPT/`.
2. Diretrizes estratégicas vigentes, respeitando seu escopo e estado.
3. Propostas, pesquisas, brainstorms e protótipos, sem autoridade de decisão.

A visão estratégica não altera por si só o comportamento implementado. A implementação existente também não torna uma escolha técnica passada um princípio imutável de produto.

## Estados e efeitos

| Classificação | Interpretação |
|---|---|
| VIGENTE / ACEITA E ATIVA | Orientação atualmente adotada; seguir no escopo indicado. |
| APROVADA | Direção autorizada, mas pode ainda não estar planejada ou implementada. |
| PLANEJADA | Priorizada; conferir escopo e autorização do lote. |
| EM PESQUISA / EM ANÁLISE / PROPOSTA | Material para avaliação; não altera decisões nem autoriza implementação. |
| CAPTURADA / BRAINSTORM | Ideia preservada, sem autoridade normativa. |
| IMPLEMENTADA | Existe no código; verificar separadamente se foi validada. |
| PAUSADA / REJEITADA / DESCARTADA / SUBSTITUÍDA | Não executar como orientação atual; consultar motivo e eventual substituta. |
| HISTÓRICA / ARQUIVADA | Preservada para entender o passado; nunca usar isoladamente como especificação atual. |

A existência de proposta posterior não revoga decisão vigente. “Aprovada” não significa “implementada”; “implementada” não significa “validada”.

## Resolução de divergências
1. Identifique documentos, trechos, datas e estados em conflito.
2. Determine se a divergência é factual, estratégica, técnica ou exploratória.
3. Aplique a hierarquia e confira o estado formal da decisão.
4. Uma proposta só substitui decisão anterior quando houver aprovação explícita e registro da revisão/substituição.
5. Sem evidência suficiente, mantenha a decisão vigente em seu escopo e sinalize a questão como aberta.
6. Corrija a contradição nos documentos de entrada usados pelos agentes; não basta acrescentar outra nota contraditória.

## Exemplo — conectividade e armazenamento
**Direção técnica vigente:** arquitetura local-first/offline-first, sem conta, backend proprietário ou sincronização em nuvem obrigatórios. O núcleo deve continuar útil sem conexão. O módulo Mercado utiliza rede para obter informações externas e mantém cache local.

Não descrever o aplicativo como “100% sem internet” em sentido literal, pois isso ignoraria integrações de rede existentes. Também não transformar essas APIs em dependência de conectividade para o núcleo pessoal.

Open Finance, sincronização, serviços externos, IA local/remota e outras integrações permanecem em pesquisa/análise enquanto não forem aprovadas. Sua presença em brainstorm, roadmap exploratório ou relatório não revoga a direção atual.

**Para agentes:** seguir a arquitetura vigente em qualquer implementação autorizada hoje. Se uma iniciativa exigir mudá-la, apresentar revisão, impactos e alternativas e aguardar aprovação. Não implementar nova arquitetura silenciosamente nem tratar a decisão atual como proibição eterna.

## Documentos e responsabilidades
- `README.md`: índice técnico e mapa de leitura.
- `01_DOCUMENTO_MESTRE.md`: visão executiva técnica resumida.
- `02_ESPECIFICACAO_PRODUTO.md`: requisitos e regras funcionais.
- `03_ARQUITETURA_ENGENHARIA.md`: arquitetura técnica.
- `07_ESTADO_ATUAL.md`: o que existe, falta ou não foi verificado.
- `10_ROADMAP.md`: planejamento e pendências; não é autorização irrestrita.
- `11_DECISOES_ADRS.md`: decisões arquiteturais e seus estados.
- `13_GOVERNANCA_DOCUMENTAL.md`: processo normativo.
- `HISTORICO_DOCUMENTACAO.md`: alterações documentais relevantes.
- `15_REGISTRO_LACUNAS_E_FALHAS.md`: inventário consolidado de lacunas, defeitos, inconsistências documentais e validações pendentes.
- `NEXUS-DOCUMENTACAO/CHATGPT/`: visão, decisões, ideias, pesquisas e propostas; não substitui especificação técnica nem autoriza implementação.
- `NEXUS-DOCUMENTACAO/GOOGLE_IA/`: espelho técnico; a origem é `PROJETO-NEXUS-/docs/`.

## Instrução final aos agentes
Antes de planejar ou editar: leia este arquivo e o README; consulte os documentos vigentes do assunto; verifique ADRs e estados; use propostas e histórico apenas como contexto; não trate prompts antigos, protótipos ou relatórios de IA como decisões; não altere código sem autorização explícita.

**Preservar ideias não significa deixá-las competir com as regras vigentes. Preservar decisões não significa torná-las eternas.**
