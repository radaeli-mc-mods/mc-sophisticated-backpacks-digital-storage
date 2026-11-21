# Integrated Dynamics: integração faseada dos nove upgrades

Plano aprovado em 2026-11-16. Fase 0 implementada em 2026-11-16; evidências e limites no [registro da fase 0](INTEGRATED_DYNAMICS_PHASE_0.md). A fase 1 implementa vínculo e acesso; a [fase 2](INTEGRATED_DYNAMICS_PHASE_2.md) acrescenta as operações básicas e apresentação no piloto, com validação e limites registrados separadamente. Ver [evidências e limites da fase 1](INTEGRATED_DYNAMICS_PHASE_1.md). As evidências de auditoria abaixo se referem ao checkout e aos JARs inspecionados nessa data.

## 1. Objetivo e decisões aprovadas

Implementar primeiro no **Minecraft 1.21.1 / NeoForge 21.1.238 / Java 21**. Após os testes automatizados e o aceite manual do usuário no piloto, portar para 26.1.2 e Forge 1.20.1.

- Backend persistido: `integrateddynamics`; nome apresentado: **Integrated Dynamics**.
- Ativação com **Integrated Dynamics + Integrated Tunnels**, acompanhados de suas bibliotecas obrigatórias. Integrated Terminals e Integrated Crafting permanecem opcionais, com testes de coexistência.
- Vínculo por Omni-Directional Connector e por outras partes funcionais conectadas, que descobrem um Omni elegível da mesma rede. Cabos puros são recusados.
- Acesso sem exigir Portable Storage Terminal no inventário.
- Uso da visão nativa de **todos os canais**, `WILDCARD_CHANNEL = -1`, sem seletor adicional.
- Os nove upgrades conservam suas funções atuais. Remote Crafting busca ingredientes existentes e usa a bancada Sophisticated; pedidos de autocrafting ficam fora desta entrega.
- Quinta identidade visual nos upgrades: quatro cantos atuais preservados, ícone em ciano Menril e pequena marca superior somente no estado Integrated Dynamics.

O Omni é a infraestrutura usada pelo terminal portátil nativo, conforme o [manual oficial](https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/portable.html).

## 2. Resultado da revisão e arquitetura proposta

### O padrão existente que será preservado

O backend nasce implementando `StorageAdapter`/`StorageTarget` e o contrato separado de fluidos. Os registros constroem adaptadores apenas quando as dependências estão carregadas. Os upgrades continuam usando os serviços existentes de transferência, prioridade, filtros, reservas e recuperação.

`common` permanece Java 17 sem Minecraft ou tipos Cyclops. Código com APIs de Minecraft e Integrated fica no target correspondente, dentro do pacote atual do projeto: `com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion`.

As mudanças de interface ficam limitadas a:

- Novo backend reconhecido pelos registros de itens e fluidos.
- Implementações isoladas: `IntegratedDynamicsStorageAdapter`, `IntegratedDynamicsFluidStorageAdapter`, `IntegratedDynamicsAccessPolicy` e `IntegratedDynamicsLinkDiscovery`.
- Nome, estado visual e perfis de desenvolvimento/verificação adicionais.
- Preservação do formato `backend + dimensão + posição`, dos IDs dos nove upgrades e do protocolo Sophisticated.

### O que os JARs já confirmam

| Target | Dynamics | Tunnels | Terminals | Crafting |
|---|---:|---:|---:|---:|
| 1.21.1 — piloto | 1.38.2 | 1.13.0 | 1.11.1 | 1.8.1 |
| 26.1.2 | 1.36.3 | 1.13.2 | 1.11.1 | 1.8.1 |
| Forge 1.20.1 | 1.31.4 | 1.9.0 | 1.7.0 | 1.4.10 |

A inspeção confirmou redes nativas de itens/fluidos, canais, filtros por interface, simulação e índices com quantidades `long`. Os hashes dos doze JARs foram levantados.

Existem diferenças reais entre versões: Forge possui `getChannelInternal`, enquanto 26.1.2 usa `TransactionContext`. O porte será uma adaptação das APIs, sem cópia mecânica do piloto.

A fase 0 obteve e auditou **Cyclops Core 1.30.0 (build 1205) e Common Capabilities 2.11.6** para o piloto. Os seis artefatos exatos, URLs, licenças, metadados e SHA-256 estão no [manifesto do piloto](../../targets/1.21.1/integrated-dependencies.lock.json), verificável por `:neoforge:verifyIntegratedDependencies` a partir de `targets/1.21.1`. A compatibilidade embutida de Dynamics exige NeoForge 21.1.238. O conjunto está reproduzível em disco; carregamento integrado e perfis serão exercitados nas fases seguintes. As bibliotecas dos portes ainda serão fixadas na fase 10.

### Contrato de acesso e vínculo — P02/P03/P04/P11

- Descobrir membros pela topologia nativa; proximidade física não estabelece pertencimento.
- Preferir o Omni diretamente clicado. Para outra parte funcional, selecionar deterministicamente um Omni carregado da mesma rede e salvar sua posição como endpoint operacional.
- Como o contrato atual recebe a posição do bloco, identificar as partes funcionais presentes no host multipart; blocos contendo somente cabo não qualificam.
- Em múltiplos candidatos, ordenar por dimensão, coordenadas e face. Após salvar o vínculo, a operação permanece ancorada naquele bloco, sem procurar outro Omni quando ele ficar indisponível.
- Revalidar jogador, proteção do endpoint, chunk, rede atual, estado do Omni, energia e capacidade solicitada em cada chamada.
- Usar a infraestrutura remota nativa sem inventar raio próprio. Consultas a outros mundos ou posições exigem verificação prévia de disponibilidade e chunk carregado.
- Preservar os contextos atuais de mochila carregada/colocada e a pausa quando não houver jogador elegível.
- Respeitar os filtros nativos independentemente para consulta, inserção e extração; não criar permissões administrativas próprias.

### Contrato de recursos — P05/P08/P09/P10/P11

- Consultas retornam estimativas transitórias; execução e sobras reais determinam o resultado.
- Usar os canais nativos, preservando filtros, prioridades, limites e interceptadores de inserção. Não acessar inventários físicos diretamente para contornar essas regras.
- Consulta de presença para `Stored content` não exige nem simula extração.
- Empregar consultas direcionadas quando a identidade for conhecida; snapshots amplos ficam restritos aos consumidores que precisam deles.
- Tratar índices desatualizados como estimativas, incluindo mudanças no mesmo tick e a própria mochila exposta à rede.
- Preservar componentes/NBT, contagens `long` e conversões limitadas para stacks e operações reais.
- Fluidos permanecem em mB, com identidade completa e prova de ida/volta de 1.000 mB.
- Guards de itens e fluidos continuam protegendo referências externas já obtidas pela rede.
- Falha após resultado remoto incerto interrompe a oferta; não autoriza restituição integral nem repetição automática.

## 3. Fases e gates de entrega

Cada fase acrescenta sua evidência à documentação interna. As referências normativas continuam sendo [P01–P12](../UPGRADES.md#P01), a [arquitetura](../ARCHITECTURE.md) e o plano individual de cada upgrade.

| Fase | Entrega | Gate para avançar |
|---|---|---|
| **0 — Baseline e dependências** | Fixar bibliotecas faltantes; registrar metadados, licenças, URLs e SHA-256; executar a baseline existente; reconciliar referências documentais antigas com o checkout. | Dependências reproduzíveis e baseline registrada, com falhas preexistentes identificadas. |
| **1 — Fundação de testes e acesso** | Criar perfis explícitos no 1.21.1 e harness de GameTests; implementar isolamento opcional, política de acesso, descoberta multipart, vínculo e persistência. | Ausência do backend inicia normalmente; partes válidas encontram somente sua rede; cabo puro, Omni removido/descarregado e acesso recusado preservam o vínculo anterior. |
| **2 — Operações básicas e apresentação** | Implementar consulta, presença, inserção e extração de itens/fluidos; snapshots de leitura; traduções; quinto estado visual. | Contratos passam em rede nativa antes da validação dos upgrades; simulação conserva recursos; quantidades/componentes/sobras corretos; visual piloto aprovado. |
| **3 — Deposit e Restock** | Validar os dois fluxos manuais pelos containers existentes, com checkpoint independente por função. | Filtros permitidos/bloqueados/conteúdo, mochila carregada/colocada, capacidade parcial, recuperação e bloqueio de autoalimentação. |
| **4 — Pickup e Magnet** | Validar coleta nativa e tick, mantendo prioridades, bloqueadores e atualização real dos drops. | Simulação sem mutação; sobra preservada; múltiplas mochilas; rede cheia/offline; Magnet colocado somente com contexto elegível; coexistência entre ambos. |
| **5 — Refill** | Validar déficit, `ANY`, mãos, hotbar e seleção de bloco pela fonte compartilhada. | Pilhas parciais antes de slots vazios; identidade exata; destino alterado não sobrescrito; somente o portador recebe itens. |
| **6 — Feeding e Alchemy** | Validar seleção, consumo nativo, duração, reservas e retorno de recipientes, separadamente por função. | Uma unidade/aplicação por vez; inserção de recipientes independente da extração; remoção, morte, desativação e reinício conservam pendências e não repetem efeitos. |
| **7 — Pump** | Integrar o adaptador de fluidos ao roteamento e às reservas existentes. | Entrada/saída, ambas as prioridades, mãos/mundo/handlers, componentes, evaporação, capacidade parcial, recuperação desativado e tanque exposto à própria rede. |
| **8 — Remote Crafting** | Validar seletor, preenchimento, JEI simples/lote e reabastecimento nativo da grade com a nova fonte. | Shaped/shapeless, alternativas, preenchimento parcial, componentes, recipientes e pedidos obsoletos; nenhum pedido de autocrafting criado. |
| **9 — Aceite do piloto** | Consolidar regressão, artefato de produção, cliente, servidor dedicado, documentação e roteiro manual. | Automação verde e aceite manual do usuário nos nove upgrades, visual e multiplayer. |
| **10 — Portes** | Adaptar primeiro para 26.1.2 e depois Forge 1.20.1, repetindo os gates por target. | Paridade funcional demonstrada, suites preservadas e aceitação manual registrada por plataforma. |

A fase 1 adapta a infraestrutura de perfis e harness de redes reais do 26.1.2 ao 1.21.1, usando a API de GameTests desta versão. A fixture Integrated fica isolada e só é registrada com Dynamics + Tunnels. Os demais perfis validam carregamento e regressão JUnit; redes reais dos demais backends continuam cobertas no target 26.1.2.

Planos individuais obrigatórios: [Deposit](../upgrades/DEPOSIT.md), [Restock](../upgrades/RESTOCK.md), [Pickup](../upgrades/PICKUP.md), [Magnet](../upgrades/MAGNET.md), [Refill](../upgrades/REFILL.md), [Feeding](../upgrades/FEEDING.md), [Alchemy](../upgrades/ALCHEMY.md), [Pump](../upgrades/PUMP.md) e [Crafting](../upgrades/CRAFTING.md). Crafting segue também o [plano de experiência e preenchimento](../remote-crafting-plan.md).

### Entrega visual da fase 2 — P07

- Preservar os sprites estáticos atuais das quatro redes e os ícones centrais originais.
- Acrescentar o estado Integrated Dynamics e transições nos dois sentidos, incluindo novo vínculo e revínculo.
- Usar a marca superior como origem da animação, mantendo os tempos atuais.
- Tornar o gerador compatível com os caminhos atuais e com exportação por target.
- Manter os índices visuais existentes. No porte 26.1.2, atribuir o quinto estado à rede e mover o canvas de animação para um índice próprio, atualizando os verificadores.
- A identidade solicitada abrange os upgrades; uma variante cosmética de mochila não integra esta entrega.

A [linha visual vigente](../design/UPGRADE_VISUALS.md) deverá incorporar essa extensão aprovada de P07 durante a implementação.

## 4. Cobertura de testes e proteção das integrações existentes

### Matriz de execução

No piloto, oferecer perfis para:

- Sophisticated sem backends.
- Dynamics com suas bibliotecas, sem Tunnels: adaptador permanece ausente.
- Dynamics + Tunnels.
- Dynamics + Tunnels + Terminals.
- Pilha completa com Integrated Crafting.
- AE2, RS, Tom’s e SCC isolados.
- Todos os backends coexistindo.

Preservar as invocações existentes, incluindo `-PsccTests`. O CI deve executar o perfil mínimo, Integrated mínimo, pilha Integrated completa e coexistência; a matriz isolada completa integra o gate local de consolidação.

### Camadas de prova

| Camada | Cobertura exigida |
|---|---|
| **Lógica e contratos** | Identidades, extremos, filtros por destino, prioridades/sobras, simulação, mudança entre estimativa/execução, exceções e recuperação. |
| **GameTests com rede real** | Cabos e partes nativas, interfaces filtradas, Omni, itens e tanques reais; ações pelo container, callback ou evento efetivamente usado pelo upgrade. |
| **Ciclo de vida** | Primeiro acesso sem abrir terminal, alteração de topologia/filtro, perda de energia, chunk descarregado, reinstalação e reinício com reserva. |
| **Artefato** | Metadados opcionais, bytecode, recursos, traduções, adaptadores presentes e dependências/fixtures ausentes do JAR. Preservar os gates Forge de reobfuscation/refmap/MixinExtras. |
| **Aceite manual** | GUI, sprites/animações, mãos/mundo, jogador comum, dois jogadores, servidor dedicado e coexistência com a modlist. |

Casos específicos obrigatórios:

- Duas redes próximas e vários Omni: descoberta não cruza topologias nem muda de endpoint durante a operação.
- Mesma identidade em canais diferentes: agregação nativa sem duplicar contagem.
- Inserção permitida e extração recusada, e o inverso, por filtros nativos.
- Mochila e tanque conectados à própria rede, inclusive com capabilities em cache.
- Quantidades acima de uma stack e acima de `int` na consulta, sem overflow ou violação dos limites nativos.
- Integrated Crafting com trabalhos preexistentes: preservar seus interceptadores e contabilizar o resultado real; receitas e saídas futuras não viram estoque disponível.
- Simulação não cria reservas, consome recursos, avança trabalhos ou altera entidades.

Relatórios devem conferir a presença dos cenários obrigatórios, rejeitar falhas e skips inesperados e descartar resultados antigos antes de cada execução. Novos cenários serão adicionados aos existentes, sem reduzir sua cobertura.

Mudanças em serviços compartilhados exigem regressão dos consumidores e backends afetados. Alterações específicas deste backend ficam nos adaptadores.

## 5. Portes, documentação e limites

**26.1.2:** adaptar simulação por abort e execução por commit às APIs transacionais auditadas, preservando o journal e a entrega após commit do Pickup. Não executar transferência remota durante uma transação local abortável. Auditar também os metadados das integrações embutidas: o Tunnels fornecido contém uma declaração de compatibilidade referindo 26.1.1, que precisa ser resolvida antes de homologar o conjunto. Seguir também o [plano do target](../PORT_26_1_2.md).

**Forge 1.20.1:** adaptar capabilities, canais, NBT e unidades pelas assinaturas dessa versão; repetir testes de preservação de dados e os verificadores do JAR reobfuscado. Preservar os gates da [base multiloader](../MULTILOADER.md).

Este documento é o plano canônico da integração, indexado no catálogo. Arquitetura, premissas e planos individuais serão atualizados conforme cada comportamento for implementado, distinguindo implementação, automação e aceite manual.

Guias públicos EN/PT-BR serão sincronizados após haver comportamento verificado, seguindo `mod-docs-sync`. Planejamento e progresso permanecem internos.

A rodada de planejamento realizou inspeção de documentos, código, metadados, assinaturas, bytecode e hashes. **Não executou suítes, alterou código ou validou gameplay.** O salvamento deste plano e sua inclusão no índice são alterações documentais posteriores à auditoria. O plano não inclui commit, versão, publicação ou instalação em instância existente.
