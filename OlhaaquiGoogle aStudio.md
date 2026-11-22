NEXUS — Implementação da evolução visual e análise de escopo

Você está trabalhando no repositório ativo do aplicativo Android NEXUS. Esta tarefa possui duas partes distintas:

1. Implementar as mudanças visuais e adaptativas explicitamente aprovadas neste documento.
2. Analisar as demais ideias presentes na documentação de design e produto, sem implementá-las.

Não trate a segunda parte como autorização para ampliar o escopo da implementação.

---

PARTE 1 — IMPLEMENTAÇÕES AUTORIZADAS

1. Inspeção inicial obrigatória

Antes de modificar qualquer arquivo:

- Inspecione a estrutura atual do projeto.
- Identifique como os temas claro e escuro estão organizados.
- Localize os componentes da Home e da Agenda semanal.
- Identifique os recursos atuais do ícone Android.
- Consulte o Design System e os documentos técnicos disponíveis.
- Verifique quais das mudanças solicitadas já existem parcialmente.
- Reaproveite estruturas e componentes existentes sempre que possível.

Não crie implementações duplicadas nem substitua estruturas existentes sem necessidade.

A implementação deve acontecer no aplicativo Android real, e não apenas nos protótipos em "design-explorations/".

Os protótipos anteriores devem permanecer preservados.

2. Tema claro Grafite Suave

Implementar o tema claro Grafite Suave como tema real do aplicativo.

A direção visual desejada é uma interface em tons de cinza, confortável e equilibrada, com menos luminosidade do que a versão excessivamente branca apresentada anteriormente.

Utilize esta paleta como referência inicial:

Elemento| Cor de referência
Fundo| "#D4D7DC"
Superfície| "#E3E5E8"
Superfície elevada| "#C9CED4"
Bordas| "#B0B6BF"
Texto principal| "#121417"
Atrasos| "#AD1E1E"
Conflitos| "#874E00"
Positivo| "#136B31"
Informação| "#1353A6"

Essas cores são referências, não uma exigência de reprodução cega.

Avalie os tons em conjunto e ajuste-os quando necessário para obter:

- Boa legibilidade.
- Contraste adequado.
- Hierarquia clara entre superfícies.
- Separação visual entre cartões e fundo.
- Conforto em uso prolongado.
- Consistência entre telas e componentes.

Preserve a identidade semântica dos estados:

- Vermelho para atrasos.
- Amarelo/âmbar para conflitos.
- Verde para estados positivos.
- Azul para informações.

A cor não deve ser o único meio de comunicar um estado.

Preserve o tema escuro e suas características. As mudanças de bordas coloridas descritas na próxima seção também devem contemplar o tema escuro.

3. Revisão das bordas coloridas em todo o aplicativo

A alteração de bordas deve se concentrar exclusivamente nos contornos coloridos utilizados para representar estados, alertas, seleções e destaques.

Quero uma revisão desses contornos em todo o aplicativo, tanto no tema claro quanto no tema escuro.

A principal referência visual deve ser o contorno verde utilizado atualmente para indicar o dia selecionado na visão semanal da Agenda.

O que deve mudar

- Revisar os contornos coloridos existentes no aplicativo.
- Torná-los mais perceptíveis quando isso melhorar a hierarquia visual.
- Utilizar espessuras proporcionais ao tamanho e à função de cada componente.
- Preservar as cores e seus significados.
- Ajustar contraste e tonalidade conforme o tema utilizado.

Proporcionalidade e bom senso visual

Não aplique uma espessura única a todos os componentes.

Avalie individualmente:

- Dimensões do componente.
- Importância do destaque.
- Função da borda na compreensão da informação.
- Espaço disponível.
- Legibilidade.
- Hierarquia visual.
- Conforto durante o uso.

Por exemplo:

- Um cartão grande de alerta pode comportar um contorno mais evidente.
- Um cartão de dia da Agenda pode utilizar uma espessura intermediária.
- Pequenos cartões de tarefas dentro da visão semanal devem manter contornos discretos, pois possuem pouco espaço visual.
- Ícones pequenos não devem receber contornos exagerados.
- Elementos selecionados devem ser perceptíveis sem competir com informações mais importantes.

Esses exemplos orientam o raciocínio, mas a decisão deve considerar os componentes reais do aplicativo.

Aplicação nos dois temas

A revisão deve funcionar nos temas claro e escuro.

- Garantir visibilidade adequada em ambos.
- Ajustar tonalidades quando necessário.
- Evitar cores agressivas ou excessivamente luminosas no tema escuro.
- Evitar contornos apagados no tema claro.
- Preservar a semântica das cores.

Manter os significados:

- Vermelho: atrasos.
- Amarelo/âmbar: conflitos.
- Verde: estados positivos e seleções pertinentes.
- Azul: informações.

O que NÃO deve mudar

Não modificar as bordas neutras do aplicativo.

As bordas comuns, cinzas ou utilizadas apenas para separar superfícies e componentes já estão satisfatórias. Preserve seu aspecto atual.

Não aumente a espessura delas, não altere suas tonalidades e não transforme elementos neutros em elementos coloridos.

Também remover as bordas dos widgets.

Os widgets devem apresentar uma composição mais limpa, sem contornos ao redor deles. Preserve o conteúdo, a organização e a funcionalidade; a mudança aqui é especificamente a remoção das bordas.

Não adicionar novas bordas a componentes que atualmente não precisam delas.

Diretriz de implementação

Inspecione onde os contornos coloridos são definidos e utilizados.

Se fizer sentido, organize os padrões em tokens, estilos ou componentes reutilizáveis do Design System, evitando inconsistências.

O objetivo é melhorar a linguagem visual dos destaques coloridos, sem alterar desnecessariamente o restante da interface.

Pense como um designer de produto: cada alteração precisa fazer sentido para o tamanho do componente, sua função e a experiência do usuário.

4. Agenda semanal — altura adaptável dos cards

Os cartões que representam os dias da semana devem aproveitar melhor a altura disponível na tela.

A intenção é evitar que os cartões tenham uma altura fixa que desperdice espaço ou fique desproporcional em diferentes dispositivos.

Implementar uma composição que:

- Considere a altura útil real da janela.
- Respeite barras do sistema e demais componentes da interface.
- Distribua o espaço vertical de maneira proporcional.
- Preserve a legibilidade de datas, indicadores e informações.
- Mantenha alvos de toque confortáveis.

Não basta aumentar todos os cartões indiscriminadamente. A altura deve acompanhar o espaço realmente disponível.

5. Agenda semanal — exibição automática dos sete dias

A Agenda semanal deve identificar automaticamente se existe espaço horizontal suficiente para exibir todos os sete cartões de dias simultaneamente.

Implementar dois comportamentos:

Modo expandido

Quando houver largura suficiente para apresentar os sete cartões com boa qualidade visual:

- Exibir todos os sete dias simultaneamente.
- Distribuir os cartões de maneira equilibrada.
- Utilizar espaçamentos proporcionais.
- Manter textos, indicadores e elementos visuais legíveis.
- Preservar áreas de toque confortáveis.
- Evitar espaços vazios desnecessários.

Modo deslizável

Quando não houver espaço suficiente:

- Manter o comportamento atual de rolagem horizontal.
- Permitir que o usuário deslize para os lados e consulte os dias desejados.
- Não comprimir excessivamente os cartões.
- Não sacrificar legibilidade para tentar exibir os sete dias.

Adaptação automática

A decisão entre os dois modos deve considerar as constraints reais de largura da área disponível.

Não utilizar apenas o modelo do dispositivo ou sua orientação como critério.

Avaliar a largura mínima confortável dos cartões, espaçamentos, margens e demais elementos.

A implementação pode utilizar WindowSizeClass ou outra abordagem compatível com a arquitetura atual, desde que a decisão seja realmente baseada no espaço disponível.

Preservação de contexto

Ao alternar entre os modos, redimensionar a janela ou mudar a orientação:

- Preservar o dia selecionado.
- Preservar a semana consultada.
- Manter a seleção coerente.
- Evitar perda de conteúdo ou mudanças inesperadas de contexto.

Esse comportamento deve funcionar em conjunto com a adaptação vertical e com os layouts para tablets.

6. Agenda — layout adaptativo para tablets

Em tablets na orientação paisagem, avaliar e implementar uma composição com dois painéis:

- Um lado dedicado aos dias/calendário.
- Outro lado dedicado aos compromissos e itens do dia selecionado.

A seleção do dia deve atualizar o conteúdo do segundo painel.

Não é obrigatório dividir a tela em duas metades iguais. Utilize proporções adequadas ao conteúdo e à experiência de uso.

Preserve a experiência atual em celulares.

7. Adaptação para telas maiores

Avaliar como aproveitar melhor telas largas.

Quando houver espaço suficiente, reorganizar informações em painéis ou colunas quando isso realmente melhorar a experiência.

Evitar simplesmente esticar componentes originalmente projetados para celulares.

A intenção é tornar o aplicativo mais confortável em tablets e telas maiores, sem presumir que isso significa desenvolver uma versão desktop completa.

8. Identidade tipográfica NEXUS

Implementar a direção visual escolhida para a identidade tipográfica.

Símbolo N

Utilizar o N-1 — Monólito Chanfrado.

Essa é a alternativa escolhida. Não reabrir a exploração de N-2 ou N-3, nem inventar outro desenho para substituí-la.

Características que devem ser preservadas:

- Silhueta única e contínua.
- Chanfros externos de 45 graus.
- Pontas internas alinhadas à linguagem de cortes do restante do wordmark.
- Construção que preserve a leitura do N em tamanhos reduzidos.
- Ausência de uma fenda central que desconecte visualmente os segmentos.

Demais letras

Preservar E, X, U e S da identidade Kinetik Cut já explorada.

O wordmark NEXUS deve manter coerência entre todas as letras.

A identidade tipográfica é destinada ao wordmark e a elementos específicos de destaque. Não substituir a tipografia global do aplicativo.

Se os arquivos atuais já contiverem os desenhos aprovados, reutilize-os. Não redesenhe ou simplifique a identidade sem necessidade.

9. Home — composição da saudação

Implementar a composição visual aprovada para a saudação da Home.

Exemplo de referência:

Boa tarde, Gabriel Ferreira

Requisitos:

- Saudação e nome na mesma linha.
- Nome ao lado da saudação, não em uma linha inferior.
- Capitalização convencional.
- Composição compacta.
- Evitar espaçamento excessivo.
- Não utilizar títulos inteiramente em caixa alta.

Preservar a organização geral da Home que já foi considerada satisfatória.

10. Remoção do N verde redundante

Existe uma correção visual identificada como COR-001.

Remover o N verde redundante que aparece antes do nome NEXUS na interface.

O objetivo é evitar a repetição desnecessária do símbolo na composição da marca.

Preservar a identidade do wordmark e a coerência com o novo símbolo N-1.

11. Substituição do ícone Android

Está autorizada a substituição do ícone Android de produção.

Utilizar o símbolo N-1 — Monólito Chanfrado, escolhido para representar a identidade NEXUS.

Direção visual:

- N-1 branco.
- Fundo preto.
- Composição centralizada.
- Boa leitura em tamanhos pequenos.
- Aparência limpa e consistente com o desenho aprovado.

Inspecionar a estrutura atual dos recursos Android e substituir corretamente os arquivos de launcher icon necessários.

Considerar:

- Ícones adaptativos.
- Foreground e background.
- Diferentes densidades e tamanhos.
- Recursos utilizados pelo launcher.
- Compatibilidade com as versões Android suportadas pelo projeto.

Não utilizar uma imagem genérica ou reconstruir o N com uma fonte textual.

O desenho deve seguir a versão N-1 aprovada. Se o arquivo-fonte necessário não estiver disponível, identifique essa limitação e não invente uma versão diferente.

Verifique se todas as referências ao ícone antigo foram atualizadas.

---

PARTE 2 — ANÁLISE DAS DEMAIS IDEIAS DOCUMENTADAS

Esta parte é exclusivamente analítica.

Não implementar nenhuma das ideias encontradas.

12. Consultar a documentação disponível

Inspecione os documentos de design, produto e planejamento presentes no repositório conectado.

Priorize, caso existam:

- "docs/06_DESIGN_SYSTEM.md"
- "docs/02_ESPECIFICACAO_PRODUTO.md"
- "docs/05_FLUXOS_COMPORTAMENTOS.md"
- "docs/07_ESTADO_ATUAL.md"
- "docs/10_ROADMAP.md"

Consulte também outros documentos relevantes que encontrar.

Identifique as ideias de evolução visual, experiência de uso, organização de telas, componentes e funcionalidades que estejam registradas.

Atenção à origem documental: o ambiente conectado está associado ao repositório ativo "PROJETO-NEXUS-". Não presuma que você tem acesso ao repositório separado "NEXUS-DOCUMENTACAO" ou aos arquivos estratégicos existentes nele.

Se não conseguir consultar documentos de outro repositório, informe isso explicitamente. Não declare ter lido arquivos aos quais não teve acesso.

13. Relatório de análise das ideias

Para cada ideia encontrada, apresentar:

1. Nome e descrição.
2. Problema que pretende resolver.
3. Benefício esperado para o usuário.
4. Telas e módulos afetados.
5. Dependências com funcionalidades existentes.
6. Impacto no Design System.
7. Impacto técnico e arquitetural.
8. Complexidade estimada.
9. Riscos e possíveis conflitos.
10. Critérios de aceitação que ainda precisam ser definidos.
11. Possibilidade de agrupamento com outras ideias.
12. Prioridade sugerida.

Ao final, organizar as ideias em:

- Melhorias independentes.
- Melhorias que podem ser agrupadas.
- Melhorias que dependem de outras implementações.
- Melhorias que exigem pesquisa ou decisão adicional.
- Melhorias que podem causar impactos arquiteturais maiores.

Apresentar uma sequência recomendada de evolução.

Não transformar sugestões em decisões aprovadas. Não alterar o estado documental das ideias sem autorização.

---

PARTE 3 — RESTRIÇÕES E PRESERVAÇÃO DO PROJETO

14. BUG-001 está fora deste lote

A perda de estado durante a rotação do dispositivo foi registrada como BUG-001.

Esse problema funcional não faz parte desta implementação.

É permitido implementar layouts que se adaptem visualmente à rotação e ao redimensionamento, mas não se deve tentar corrigir incidentalmente a persistência de estado.

Não declarar BUG-001 resolvido.

Não misturar a adaptação visual com uma refatoração de navegação ou persistência.

15. O que não deve ser alterado

Não modificar sem necessidade ou autorização específica:

- Banco de dados Room.
- Entidades e DAOs.
- Backup e restauração.
- Regras de domínio.
- Módulos Financeiro e Mercado.
- Navegação global.
- Arquitetura geral do aplicativo.
- Login, backend ou serviços externos.
- ADRs existentes.
- Protótipos anteriores em "design-explorations/".

Não migrar a navegação para NavHost.

Não realizar refatorações amplas apenas para facilitar a implementação visual.

Preservar o tema escuro, exceto pelas mudanças de contornos coloridos autorizadas.

Preservar as bordas neutras existentes.

Preservar funcionalidades e comportamentos já existentes.

16. Documentação

Atualizar a documentação técnica em "docs/" do repositório ativo de acordo com as mudanças realmente implementadas.

Avaliar especialmente:

- Design System.
- Estado atual.
- Fluxos e comportamentos.
- Roadmap, somente quando houver justificativa documental.

Atualizar os artefatos internos relevantes do Google AI Studio, incluindo os documentos correspondentes ao estado atual, Design System, integração e continuidade.

A documentação deve refletir o código efetivamente alterado.

Não registrar como implementado algo que ficou apenas parcialmente concluído.

Não presumir acesso ao repositório documental central "NEXUS-DOCUMENTACAO". Se não houver conexão disponível, informar isso no relatório, sem simular sincronização.

---

PARTE 4 — VERIFICAÇÃO E RELATÓRIO FINAL

17. Verificações

Depois da implementação:

- Executar os testes que o ambiente suportar.
- Executar compilação/build quando possível.
- Verificar referências aos recursos de ícone.
- Verificar a integração dos temas claro e escuro.
- Verificar os contornos coloridos em componentes de diferentes dimensões.
- Confirmar que as bordas neutras permaneceram inalteradas.
- Confirmar que os widgets estão sem bordas.
- Verificar a lógica adaptativa dos cards semanais.
- Verificar a preservação da seleção do dia.
- Verificar a composição da Agenda em diferentes larguras.
- Verificar o comportamento em celulares e tablets, quando houver ambiente adequado.

Não declarar testes ou builds aprovados sem executá-los.

Se não houver emulador, SDK, ferramentas ou ambiente necessário, informar exatamente o que não pôde ser verificado.

18. Relatório obrigatório

Ao terminar, apresentar:

A. Implementações realizadas

- Resumo das mudanças.
- Telas e componentes afetados.
- Comportamentos novos.

B. Arquivos alterados

Listar os principais arquivos modificados e explicar sua finalidade.

C. Decisões visuais e técnicas

Explicar brevemente as escolhas realizadas para tema, bordas coloridas, widgets, Agenda, layouts adaptativos, wordmark e launcher icon.

D. Revisão de bordas

Explicar como os contornos coloridos foram tratados nos dois temas, como a espessura foi adaptada aos diferentes tamanhos e funções dos componentes, como as bordas neutras foram preservadas e como os widgets ficaram sem bordas.

E. Comportamento adaptativo

Explicar qual critério determina a exibição dos sete cards ou a utilização de rolagem horizontal.

F. Verificações

Informar testes, builds e inspeções executados, com seus resultados reais.

G. Documentação

Listar os documentos técnicos e artefatos internos atualizados, além de eventuais limitações de sincronização.

H. Análise das demais ideias

Entregar o relatório solicitado na Parte 2, sem implementação.

I. Pendências

Listar tudo que não foi concluído, limitações encontradas e decisões que ainda dependem do usuário.

---

ORIENTAÇÃO FINAL

Este lote tem um escopo visual e adaptativo definido.

Implemente o que está explicitamente autorizado, preserve o restante do aplicativo e não aproveite a tarefa para introduzir funcionalidades adicionais.

A análise das demais ideias deve servir para organizar os próximos passos do NEXUS, não para ampliar esta implementação.

Ao concluir, apresente o relatório e aguarde minha avaliação antes de iniciar qualquer outro lote.
