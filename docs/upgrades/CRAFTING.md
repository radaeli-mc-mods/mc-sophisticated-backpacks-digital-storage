# Crafting remoto — plano de implementação

## Objetivo e premissas

Implementar a bancada remota nos loaders em que o Sophisticated registra a bancada nativa: Forge 1.20.1 e NeoForge 1.21.1/26.1.2. Aplicar [P01–P08 e P10–P12](../UPGRADES.md#P01). A operação é síncrona e usa recuperação imediata conforme P08.

## Comportamento e implementação

O plano aprovado de experiência, seleção de receitas, preenchimento parcial, persistência e recuperação está em [Remote Crafting Upgrade](../remote-crafting-plan.md). A grade e os controles de crafting usam as classes nativas do Sophisticated. Cada pedido prepara uma execução e preserva ingredientes compatíveis já presentes. A fonte secundária cobre o déficit. Uma grade incompatível bloqueia a busca. O resultado nativo continua visível quando uma grade parcial corresponde a outra receita.

Registrar um item universal com conflito nas duas ordens de instalação, vínculo manual, prioridade persistida e desvínculo compartilhado. A conversão usa a receita P07 e preserva os nove slots, controles nativos e nome. O seletor apresenta receitas shaped e shapeless sincronizadas, permite busca e envia o ID escolhido pelo container Sophisticated.

## Estado implementado

Forge e NeoForge registram `remote_crafting_upgrade`, seu container e aba. O item estende a bancada nativa e participa dos fluxos comuns de vínculo e desvínculo por `ManuallyLinkedUpgrade`. A conversão usa a receita nativa de próximo tier com o padrão P07 e preserva a grade, os controles e o nome da bancada.

O seletor usa as receitas sincronizadas, com busca por nome do resultado ou ID. O servidor valida menu, aba, wrapper e regras da receita, e limita o preenchimento a um pedido por tick. A atribuição preserva itens na grade e usa as quantidades disponíveis na mochila e na rede para maximizar o preenchimento antes de aplicar a prioridade. O preenchimento usa a posição derivada do menu e o guard da própria mochila. Se a extração real falhar, o cálculo tenta outras fontes para os slots restantes. A autorização de devolução à rede é separada da autorização de extração.

## Validação

Em 2026-11-08, o build passou nos dois loaders. A suíte comum verificou a atribuição por capacidade, ingredientes sobrepostos, prioridade e preservação da grade. O NeoForge decodificou a receita empacotada e confirmou a preservação dos componentes nativos na conversão. O servidor GameTest do Forge carregou a receita e verificou a preservação do NBT, o preenchimento parcial e a conclusão de uma receita com formato, além de alternativas sobrepostas e rejeição de uma grade incompatível numa receita sem formato.

Verificar conversão e persistência, posições e espelhamentos, alternativas sobrepostas, prioridades, extrações parciais, recuperação, resultados concorrentes, recarga de datapacks e acesso inválido. A validação manual em cliente e servidor dedicado com AE2, Refined Storage, Tom’s e SCC onde suportado permanece aberta.

## Consolidação do preenchimento e controles do visualizador

A atualização consolidada para 0.6.0 acrescenta a transferência de receitas do JEI nos três targets e controles para mostrar ou ocultar o seletor e o botão Buscar ingredientes. O botão “+” do upgrade usa as fontes locais do Sophisticated e consulta a rede vinculada para o que faltar. Com Shift, a transferência do visualizador solicita o maior lote possível. A preferência `manualControls` permite escolher `AUTO`, `SHOW` ou `HIDE`; esses controles mudam a apresentação, sem alterar receita, grade, resultado, prioridade ou reabastecimento.

`CraftingFillEngine` em `common` concentra o planejamento e a execução sem depender de Minecraft, loader, Sophisticated ou JEI. Ele modela receitas shaped e shapeless, offsets e espelhamento, atribuição de alternativas, prioridades, déficits, preenchimento simples e em lote, extração real, replanejamento após falta de estoque e recuperação de itens recusados. Cada target adapta receitas, stacks, matriz nativa e `UpgradeItemSources`; os adaptadores continuam responsáveis pelos guards, filtros, acesso e origem dos itens. As prioridades permanecem estáveis dentro da ordem de cada backend.

O contrato interno `RecipeTransferRequest` descreve somente ID de receita e modo `SINGLE`/`MAX`. `RecipeViewerAdapter` e `RecipeViewerIntegrationRegistry` não importam JEI. Os plugins finos dos três targets usam, respectivamente, JEI `15.56.0.205`, `19.64.0.455` e `29.47.0.110`. O handler nativo do Sophisticated continua validando e transferindo primeiro no modo simples; Shift valida sem mutar a grade e delega o lote ao servidor. O caminho remoto só é usado com vínculo. O inventário do jogador é fonte adicional apenas nesses pedidos do visualizador. Sem vínculo, o comportamento nativo continua disponível.

A prontidão do visualizador só fica ativa depois de `onRuntimeAvailable` e quando a consulta ao runtime confirma que o handler deste addon está selecionado para o `BackpackContainer` e a categoria de crafting. `onRuntimeUnavailable` limpa a referência ao runtime. A integração continua opcional e JEI não é empacotado.

A configuração client-side `sophisticated_backpacks_digital_storages_expansion-client.toml`, chave `[crafting] manualControls`, oferece `AUTO`, `SHOW` e `HIDE` e usa o mesmo nome e padrão nos três targets. `AUTO` esconde seletor e Buscar ingredientes somente quando o handler JEI está pronto para o menu atual. O botão de alternância grava explicitamente `SHOW` ou `HIDE`; a configuração pode voltar a `AUTO` editando o arquivo. Ocultar controles não muda receita selecionada, matriz, resultado, reabastecimento nem prioridade. A aba recalcula a altura ao ocultar ou mostrar os controles e reavalia a prontidão durante sua atualização.

O callback nativo de `Refill crafting grid` permanece separado: tenta primeiro as fontes locais do Sophisticated, incluindo mochila e inventário do jogador, e só então consulta a rede vinculada para a unidade consumida. A prioridade de origem local nesse callback não depende da prioridade do botão de busca.

Em 2026-11-12, o usuário confirmou em jogo o botão “+”, Shift para crafting em lote e reabastecimento remoto após craft no NeoForge 1.21.1 na implementação anterior à consolidação. Essa evidência não cobre a nova preferência visual nem substitui a revalidação manual após a refatoração. A compilação, os testes e a inspeção dos JARs para os três targets são gates automatizados; confirmação visual, ciclo de vida do JEI em jogo e multiplayer continuam gates manuais de P12.

## Verificação da consolidação — 2026-11-13

`:common:test` passou. Os builds Forge 1.20.1 e NeoForge 1.21.1 passaram, e seus JARs de produção foram verificados. O target NeoForge 26.1.2 compilou, gerou o JAR e passou pelo verificador de empacotamento. O `buildAll` com o perfil `all` em 26.1.2 terminou com duas falhas em `PrioritySettingsTest` (`pickupAndMagnetBackpackFirstUseNativeInventoryBeforeNetworkInBothPhases` e `storedContentStillRequiresLocalItemOrMemoryWithBackpackFirst`): ambas tentam ler a configuração Sophisticated antes de ela ser carregada; não passaram pelo código de Crafting. Portanto, esse perfil não está validado como build integralmente verde. O portal de documentação passou em `pnpm run check` e `pnpm run build`.

A validação visual da preferência `AUTO`/`SHOW`/`HIDE`, do ciclo de vida JEI após esta consolidação e do multiplayer segue pendente de aceite manual P12.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

A exceção de networking autorizada pelo usuário limita o payload próprio S2C ao catálogo paginado; consulta e ações continuam no container Sophisticated. O servidor valida sessão/menu/aba, receita e limite por tick (P06). GameTests verificam preenchimento com as duas prioridades, resultado shaped, preenchimento parcial shapeless e devolução dos três baldes de uma receita de bolo; JUnit cobre conversão e codecs (P08/P09/P12). A evidência automatizada não encerra a aceitação manual P12.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.

## Vínculo RS/SCC por dispositivos da rede

A descoberta compartilhada de [P02](../UPGRADES.md#P02) também aceita dispositivos ativos do RS nos três targets e dispositivos da rede SCC online nos dois targets NeoForge. Cabos são recusados, inclusive o cabo central de hosts SCC multipart. O endpoint salvo continua sendo controller no RS ou antena elegível no SCC; esta função mantém suas regras operacionais, filtros e reservas. Aplicar P03/P04/P09/P11 e validar a interação em jogo conforme P12.

## Vínculo Tom’s por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: dispositivos nativos conectados podem identificar um terminal disponível. Cabos normais e emoldurados são recusados; conectores funcionais permanecem elegíveis. O endpoint salvo e a validação nativa do terminal não mudam, assim como filtros, transferências e reservas. Aplicar P03/P04/P09/P11 e separar os testes automatizados do aceite manual P12.
