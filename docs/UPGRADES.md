# Plano de implementação dos upgrades universais

## Estado e referências

Este documento consolida as decisões aprovadas e os critérios dos upgrades universais. Deposit, Restock, Pickup, Magnet, Refill, Feeding, Alchemy, Pump e Crafting têm implementação nos dois loaders. Consulte a arquitetura e cada plano individual para o estado da validação por função e backend.

O módulo Forge 1.20.1 agora também contém os oito upgrades e os adaptadores específicos dos JARs auditados. `:forge:build` passa e produz JAR reobfuscado. A suíte Forge ainda não cobre redes reais nem inicia registries do jogo em GameTests; veja a evidência e os limites atualizados em [MULTILOADER.md](MULTILOADER.md).

A [arquitetura atual](ARCHITECTURE.md) descreve a base existente. O [PRD](PRD.md) permanece como registro do MVP original. Em caso de diferença de escopo, este documento e os planos individuais definem a evolução posterior ao MVP.

## Catálogo e planos individuais

Todos os IDs de itens e receitas usam o namespace `sophisticated_backpacks_digital_storages_expansion`.

| Função | ID | AE2 | Refined Storage | Tom’s | Plano |
| --- | --- | --- | --- | --- | --- |
| Deposit | `remote_deposit_upgrade` | Sim | Sim | Sim | [Deposit](upgrades/DEPOSIT.md) |
| Restock | `remote_restock_upgrade` | Sim | Sim | Sim | [Restock](upgrades/RESTOCK.md) |
| Pickup | `remote_pickup_upgrade` | Sim | Sim | Sim | [Pickup](upgrades/PICKUP.md) |
| Magnet | `remote_magnet_upgrade` | Sim | Sim | Sim | [Magnet](upgrades/MAGNET.md) |
| Refill | `remote_refill_upgrade` | Sim | Sim | Sim | [Refill](upgrades/REFILL.md) |
| Feeding | `remote_feeding_upgrade` | Sim | Sim | Sim | [Feeding](upgrades/FEEDING.md) |
| Alchemy | `remote_alchemy_upgrade` | Sim | Sim | Sim | [Alchemy](upgrades/ALCHEMY.md) |
| Pump | `remote_pump_upgrade` | Sim | Sim | Incompatível com fluidos | [Pump](upgrades/PUMP.md) |
| Crafting | `remote_crafting_upgrade` | Sim | Sim | Sim | [Crafting](upgrades/CRAFTING.md) |

“Sim” expressa o suporte planejado, não aprovação antecipada dos testes. Tom’s pode ser vinculado ao Pump, mas o item informa incompatibilidade e não executa a função.

## Premissas compartilhadas

As âncoras explícitas `P01` a `P12` são referências estáveis. Os planos individuais apontam para elas; alterações futuras devem manter os identificadores e atualizar os documentos afetados.

<a id="P01"></a>
### P01 — Plataforma e dependências

NeoForge: Minecraft 1.21.1, NeoForge 21.1.236 e Java 21. Forge: Minecraft 1.20.1, Forge 47.4.10 e Java 17. O pacote base é `com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion`. Sophisticated Core e Backpacks são obrigatórios; os backends são opcionais e suas classes permanecem isoladas nos adaptadores. O ID do mod é `sophisticated_backpacks_digital_storages_expansion`; os IDs persistidos de itens e receitas mantêm o namespace anterior para compatibilidade dentro de cada versão de Minecraft. As implementações e os gates de runtime por plataforma estão em [MULTILOADER.md](MULTILOADER.md); build aprovado não declara os cenários de jogo concluídos.

JARs auditados no workspace: Core `1.5.1.2341`, Backpacks `3.26.3.2158`, AE2 `19.2.18`, RS `2.0.9`, Tom’s `2.4.2`. Mudanças de versão exigem nova conferência das assinaturas e dos pontos de Mixin.

<a id="P02"></a>
### P02 — Item universal e vínculo

Um item por função, sem variantes por rede ou tiers Wireless/Dimensional. Shift + clique direito com o item vincula manualmente um endpoint; nenhum failover procura outra rede. Preservar backend, dimensão e posição no formato atual de vínculo.

Desvincular colocando somente o upgrade vinculado na grade de criação (2×2 ou 3×3). A receita compartilhada preserva filtros e demais componentes, remove somente o endpoint e recusa itens com reserva pendente (P09). Não há botão nem ação de desvínculo na GUI. O resumo do vínculo é curto; dimensão e coordenadas ficam no tooltip da GUI e na tooltip avançada do item.

Hierarquia das tooltips dos oito itens: nome com estilo nativo; descrição da função em cinza (`GRAY`); status de vínculo em cinza escuro (`DARK_GRAY`), com somente o nome da rede após os dois-pontos em verde (`GREEN`); instrução de desvínculo em cinza escuro, somente quando vinculado. Itens sem vínculo exibem apenas “Sem vínculo” após a descrição. Avisos de recuperação integram o status antes da instrução; dimensão e coordenadas continuam restritas ao modo avançado, em cinza escuro. O destaque verde identifica o vínculo salvo, sem afirmar disponibilidade, alcance ou permissão atuais da rede.

Novo vínculo válido substitui o anterior; tentativa inválida preserva a configuração. Dados de backend ausente continuam legíveis. Um endpoint válido sem capacidade para a função pode ser vinculado, mas a GUI deve indicar a incompatibilidade. A operação fica pausada.

<a id="P03"></a>
### P03 — Infraestrutura nativa

Respeitar energia, cobertura, dimensão, disponibilidade e permissões nativas em cada operação. Não criar alcance sem fio próprio, carregar chunks ou movimentar o jogador para contornar validadores.

AE2 usa o access point vinculado; RS usa o controller e seus validadores; Tom’s usa terminal e `canInteractWith(player, false)`. A API do Tom’s é centrada no jogador: não prometer validação de uma posição arbitrária da mochila colocada. Ver [limitações atuais](ARCHITECTURE.md).

<a id="P04"></a>
### P04 — Contexto e autoridade

Toda mutação ocorre no servidor. Contexto de GUI valida menu, aba e wrapper instalado; contexto de tick vem do callback nativo; contexto de coleta vem do processamento original de pickup.

Revalidar instalação e habilitação antes de executar. Automação remota pausa sem `ServerPlayer` válido no contexto. Não persistir dono para acesso offline nem reutilizar o último jogador que abriu a mochila. Feeding, Refill e Alchemy atuam somente no portador. Para mochilas colocadas, Deposit e Restock funcionam pela GUI. Pump e Magnet resolvem o contexto no tick: usam o portador ou, quando o callback não fornece jogador, o jogador real elegível mais próximo dentro da caixa da mochila expandida em três blocos. Excluem jogadores falsos, mortos e espectadores e validam interação com o bloco. Sem esse contexto, a operação remota pausa; a posição usada para alcance da rede continua sendo a da mochila colocada. Pickup continua exigindo jogador no callback.

<a id="P05"></a>
### P05 — Prioridade e fallback

Pickup e Magnet selecionam destino; Refill, Feeding e Alchemy selecionam fonte. Pump seleciona o armazenamento da operação, dependendo de sua direção. Oferecer **Rede primeiro / Mochila primeiro**, padrão **Rede primeiro**. Tentar o secundário somente para o déficit ou sobra real, sem repetir o que já foi transferido.

Sem conexão ou permissão para a rede, o caminho local pode funcionar nas funções compatíveis. Falta de vínculo também não deve ampliar acesso remoto. Pump vinculado a backend sem fluidos pausa toda a função e mostra incompatibilidade, mesmo com tanque local.

Prioridade não substitui o filtro de cada destino. Em Pickup/Magnet com `Stored content`, Mochila primeiro aceita localmente somente itens presentes na mochila ou aprovados pela memória nativa; espaço livre sozinho não aprova um item. Se apenas a rede contiver o item, ele segue para a rede. Essa regra foi reconfirmada pelo usuário em 2026-10-30. O botão compartilhado atualiza o wrapper local imediatamente, como os controles Sophisticated, e envia o valor escolhido ao servidor; a operação continua sujeita à validação server-side de P04.

Deposit e Restock têm direção fixa e não exibem prioridade: mochila → rede e rede → mochila, respectivamente. Não há interação de transferência por Shift + clique em containers externos nesta etapa.

<a id="P06"></a>
### P06 — Reaproveitamento e instalação

Usar o ciclo de vida de upgrades, componentes, filtros, controles e callbacks do Sophisticated. Um upgrade por função, incompatível com todas as versões básicas/avançadas nativas correspondentes. A validação precisa funcionar nas duas ordens de instalação; não bloquear funções diferentes, como Pickup e Magnet.

Registrar item, `UpgradeType`, wrapper, container e aba; incluir o item na tag `sophisticatedbackpacks:upgrade`. Não introduzir um networking paralelo ao fluxo do Sophisticated.

<a id="P07"></a>
### P07 — Recursos e receitas

Padrão de receita 3×3 compartilhado pelos upgrades, com resultado de uma unidade. A célula central usa o upgrade Sophisticated avançado da função ou a bancada nativa no caso de Crafting. A célula inferior central fica vazia.

| Pérola do Ender | Diamante | Pérola do Ender |
| --- | --- | --- |
| Diamante | Upgrade Sophisticated da função | Diamante |
| Pó de redstone | Vazio | Pó de redstone |

Nos dois loaders, os oito upgrades seguem a [linha visual dos upgrades remotos](design/UPGRADE_VISUALS.md), aprovada em 2026-10-31: o ícone original de cada upgrade é preservado; moldura de ardósia; um canto por rede (AE2, SCC, TSS, RS); tela verde sem vínculo; tela escura e ícone na cor da rede quando vinculado; animação de vínculo e de troca de rede no slot, na mão e no mundo. O estado visual vem do vínculo, então isso não cria itens nem tiers por rede. O Forge 1.20.1 usa os mesmos sprites, modelos e animação. Traduções PT-BR e EN-US devem cobrir nomes, configurações, estados e erros.

<a id="P08"></a>
### P08 — Transferência e recuperação

Simulação não altera itens, entidades, fluidos ou reservas. Quantidades simuladas são estimativas, especialmente no Tom’s. A verdade é a quantidade aceita/extraída na execução. Preservar componentes e limites reais de slots e stacks.

Planejar capacidade antes da extração. Recuperar sobras na origem quando possível, depois na mochila e, com jogador válido, pelo mecanismo nativo de recuperação do inventário do jogador. Não refazer uma oferta que já foi aceita. Uma exceção após commit parcial não autoriza rollback cego da oferta inteira.

<a id="P09"></a>
### P09 — Reservas persistidas

Operações prolongadas e de fluidos mantêm somente recursos realmente extraídos em uma reserva persistida no item do upgrade. Reserva não é inventário de rede, slot de filtro nem recurso disponível para outra função.

Resolver pendências antes de iniciar outra operação. Remoção, desconexão e reinício não apagam recursos. Exibir pendência em tooltip/GUI e retomar recuperação ao reinstalar. Bloquear troca de vínculo e desvínculo enquanto houver reserva pendente; recuperar não exige acesso indevido à rede antiga. Remover o item continua permitido e transporta a reserva junto.

Para consumo, persistir a unidade reservada e seu estado antes de iniciar o uso. Após reinício, recuperar uma aplicação interrompida em vez de reaplicar efeitos automaticamente. O mecanismo protege interrupções normais de ciclo de vida; não promete transação atômica contra encerramento abrupto do processo entre a modificação de dois storages.

<a id="P10"></a>
### P10 — Proteção contra ciclos

Preservar o guard de entrada existente e acrescentar proteção de saída para extração remota. Identificar a própria mochila pelo UUID de conteúdo nativo. Guards têm escopo de operação, suportam aninhamento e são liberados em `finally`/`AutoCloseable`.

Bloquear a interface externa da própria mochila sem impedir acesso interno autorizado à função. Manter a combinação OR/AND dos filtros nativos fora do escopo. Fluidos exigem proteção equivalente no handler externo; o guard de itens não protege tanques.

<a id="P11"></a>
### P11 — Persistência e desempenho

Preservar o ID, vínculo e filtros do Deposit. Persistir apenas configurações e recursos/estado necessários à recuperação, nunca objetos de jogador, rede, mundo ou terminal.

Usar cooldowns e limites do upgrade avançado nativo correspondente. Limitar trabalho ao pedido e usar consulta direcionada quando a identidade for conhecida. Snapshots são locais à operação; não enumerar toda a rede a cada tick nem usar objetos de rede em cache como autoridade de acesso. Estado automático indisponível não gera mensagem de chat em cada tentativa.

<a id="P12"></a>
### P12 — Critério de conclusão

Cada função exige testes de lógica e prova em jogo nos backends compatíveis. Build não equivale a validação da GUI, alcance ou comportamento real. Confirmar single player, servidor dedicado, ausência de dependências opcionais e coexistência dos três backends.

## Contratos compartilhados

Os nomes abaixo descrevem contratos internos do addon. Eles isolam serviços comuns das APIs específicas de cada backend e não pretendem substituir APIs públicas do Sophisticated ou dos mods de storage.

| Contrato | Entrada/saída e responsabilidade |
| --- | --- |
| Contexto de operação | Jogador, wrapper e origem validados; fábricas distintas para GUI, tick e pickup. Preservar as verificações atuais da GUI. |
| Requisitos de acesso | Operações de consulta/inserção/extração de itens ou fluidos; conexão valida somente as permissões solicitadas. |
| Consulta de itens | Identidades com componentes e contagens `long`, sem expor inventário gravável. Consulta por identidade quando possível. |
| `ItemExtraction` | Simular/estimar quantidade por identidade e executar extração, devolvendo o `ItemStack` real. Não confiar em snapshot como reserva. |
| Roteamento | Aplicar prioridade entre storage local e remoto, oferecendo ao secundário apenas o restante. Deposit/Restock não usam prioridade. |
| Reserva de recurso | Persistir recurso e estado pendente no upgrade; concluir ou recuperar sem duplicação. |
| Operações de fluidos | Capacidade opcional, consulta por identidade, simulação/execução e quantidades normalizadas em mB. Ausência explícita de suporte. |

`StorageTarget` oferece conectividade, snapshot de identidades, consulta de quantidades, inserção e extração autorizadas por operação. `SnapshotItemHandler` permanece uma visão de leitura; `StoredItemAmount` carrega contagem sem expor inventário gravável.

### Adaptadores

- **AE2:** usar chaves nativas, `MEStorage` e fonte de ação com jogador; `AEFluidKey` existe no JAR auditado. Manter verificação de energia e alcance.
- **RS:** usar `ItemResource`/`FluidResource`, ações simuladas/reais e permissões correspondentes. O adaptador distingue `INSERT` e `EXTRACT` com os validadores nativos.
- **Tom’s:** manter consulta pelo tracker e usar `StorageTerminalBlockEntity.pullStack(StoredItemStack, long)` para extração real. Não há simulação pública nesse terminal; tratar consulta como estimativa. Não há contrato de fluidos identificado no JAR auditado.

Retorno de recipientes à rede exige autorização de inserção independente da extração do consumível. Falha nesse retorno usa recuperação local e não invalida um consumo já concluído.

## APIs verificadas e acessos localizados

Assinaturas confirmadas por inspeção dos JARs locais durante o planejamento:

- Core: `ITickableUpgrade`, `IPickupResponseUpgrade`, `IIOFilterUpgrade`, `ContentsFilterLogic`, `FilterLogic`, `FluidFilterLogic`, `HungerLevel`, `AlchemyCondition` e definições de alquimia.
- Backpacks: `RefillUpgradeWrapper.TargetSlot`, `IBlockPickResponseUpgrade`, lógica de Restock e Deposit.
- `CommonEventHandler.onItemPickup` chama o fluxo de simulação e execução do Sophisticated; o callback de upgrade não recebe jogador.
- Wrappers nativos contêm rotinas privadas e tipos concretos. Presença da classe não significa possibilidade de herdar toda sua implementação na base universal.

Mixin aplicado para delimitar o contexto de pickup no handler original, liberando-o mesmo em exceção. Acessores localizados expõem bloqueadores nativos de magnetismo e definições de alquimia; a guarda de fluido protege a capability externa da mochila colocada. Não substituir handlers inteiros, usar reflexão generalizada ou criar uma cópia paralela dos registros nativos. Os Mixins carregaram no cliente de desenvolvimento; interações em jogo ainda precisam de validação.

Referências de código upstream para navegação: [Sophisticated Core](https://github.com/P3pp3rF1y/SophisticatedCore/tree/1.21.x), [Sophisticated Backpacks](https://github.com/P3pp3rF1y/SophisticatedBackpacks/tree/1.21.x) e [Tom’s](https://github.com/tom5454/Toms-Storage/tree/1.21.1). As branches podem evoluir; a autoridade de compatibilidade deste plano são os [JARs locais](../libs/README.md).

## Sequência e gates

| Etapa | Entrega | Gate |
| --- | --- | --- |
| 1 | Contextos, capacidades, autorização por operação, prioridade e conflitos | Regressão do Deposit; extração nos três adaptadores sem exigir inserção. |
| 2 | Restock | Rede → mochila, recuperação e guard de saída. |
| 3 | Pickup | Contexto nativo e simulação sem mutação. |
| 4 | Magnet | Bloqueadores, drops parciais e convivência com Pickup. |
| 5 | Refill | Déficits, slots e seleção de bloco. |
| 6 | Feeding | Consumo e recipientes seguros. |
| 7 | Alchemy | Reservas, duração e interrupções. |
| 8 | Pump | Fluidos AE2/RS, guard de fluidos e incompatibilidade Tom’s. |
| 9 | Crafting | Conversão com preservação da grade, preenchimento parcial e resultado nativo nos dois loaders. |
| 10 | Integração final | Catálogo, receitas, documentação e servidor dedicado. |

Etapas 2 e 3 dependem da fundação; Magnet reutiliza o roteamento do Pickup; Refill e Feeding reutilizam extração do Restock; Alchemy reutiliza consumo/recuperação do Feeding. Pump acrescenta contratos próprios de fluidos e reutiliza autorização, contexto e reservas.

## Validação compartilhada

- [ ] Instalação, tag, limites e conflitos nas duas direções; funções distintas coexistem.
- [ ] Nenhum backend, cada backend isolado e os três juntos; classes opcionais não vazam para código comum.
- [ ] Vínculo, troca, desvínculo e persistência; upgrade incompatível mostra motivo explícito.
- [ ] GUI já aberta ao equipar; wrapper sincronizado, remoção e pedido desatualizado.
- [ ] Fronteiras nativas de alcance, energia, dimensão, chunk e permissões de cada operação.
- [ ] Quantidades reais, componentes, sobras, contagens extremas e ausência de mutação em simulação.
- [ ] Prioridade/fallback, reservas e interrupções de ciclo de vida.
- [ ] Mochila/tanque expostos à mesma rede e composição normal dos filtros após o guard.
- [ ] Cooldowns, múltiplas mochilas, habilitação e ausência de spam automático.
- [ ] Single player e servidor dedicado, com prova manual registrada por função/backend.

Manter a arquitetura sincronizada com o código. A implementação foi compilada e carregada no cliente; os cenários deste documento continuam sendo critérios de validação, não resultados presumidos.

## SCC 1.5.3

NeoForge usa o adaptador SCC 1.5.3 nos nove upgrades. O Pump aceita fluidos sem componentes adicionais. Os cenários em jogo continuam descritos nas [fases do SCC](integrations/SCC.md) e no [plano de Crafting](upgrades/CRAFTING.md). No Forge, os backends são AE2, Refined Storage e Tom’s.
