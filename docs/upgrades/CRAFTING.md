# Crafting remoto — plano de implementação

## Objetivo e premissas

Implementar a bancada remota nos loaders em que o Sophisticated registra a bancada nativa, Forge 1.20.1 e NeoForge 1.21.1. Aplicar [P01–P08 e P10–P12](../UPGRADES.md#P01). A operação é síncrona e usa recuperação imediata conforme P08.

## Comportamento e implementação

O plano aprovado de experiência, seleção de receitas, preenchimento parcial, persistência e recuperação está em [Remote Crafting Upgrade](../remote-crafting-plan.md). A grade e os controles de crafting usam as classes nativas do Sophisticated. Cada pedido prepara uma execução e preserva ingredientes compatíveis já presentes. A fonte secundária cobre o déficit. Uma grade incompatível bloqueia a busca. O resultado nativo continua visível quando uma grade parcial corresponde a outra receita.

Registrar um item universal com conflito nas duas ordens de instalação, vínculo manual, prioridade persistida e desvínculo compartilhado. A conversão usa a receita P07 e preserva os nove slots, controles nativos e nome. O seletor apresenta receitas shaped e shapeless sincronizadas, permite busca e envia o ID escolhido pelo container Sophisticated.

## Estado implementado

Forge e NeoForge registram `remote_crafting_upgrade`, seu container e aba. O item estende a bancada nativa e participa dos fluxos comuns de vínculo e desvínculo por `ManuallyLinkedUpgrade`. A conversão usa a receita nativa de próximo tier com o padrão P07 e preserva a grade, os controles e o nome da bancada.

O seletor usa as receitas sincronizadas, com busca por nome do resultado ou ID. O servidor valida menu, aba, wrapper e regras da receita, e limita o preenchimento a um pedido por tick. A atribuição preserva itens na grade e usa as quantidades disponíveis na mochila e na rede para maximizar o preenchimento antes de aplicar a prioridade. O preenchimento usa a posição derivada do menu e o guard da própria mochila. Se a extração real falhar, o cálculo tenta outras fontes para os slots restantes. A autorização de devolução à rede é separada da autorização de extração.

## Validação

Em 2026-11-08, o build passou nos dois loaders. A suíte comum verificou a atribuição por capacidade, ingredientes sobrepostos, prioridade e preservação da grade. O NeoForge decodificou a receita empacotada e confirmou a preservação dos componentes nativos na conversão. O servidor GameTest do Forge carregou a receita e verificou a preservação do NBT, o preenchimento parcial e a conclusão de uma receita com formato, além de alternativas sobrepostas e rejeição de uma grade incompatível numa receita sem formato.

Verificar conversão e persistência, posições e espelhamentos, alternativas sobrepostas, prioridades, extrações parciais, recuperação, resultados concorrentes, recarga de datapacks e acesso inválido. A validação manual em cliente e servidor dedicado com AE2, Refined Storage, Tom’s e SCC onde suportado permanece aberta.

## POC opcional com JEI — NeoForge 1.21.1

JEI `19.64.0.455` é compilado como API opcional e adicionado apenas ao runtime de desenvolvimento do target NeoForge 1.21.1. O JAR do addon não empacota JEI; Forge 1.20.1 e NeoForge 26.1.2 não recebem esta integração.

O plugin JEI registra um handler para o menu nativo `BackpackContainer` e `RecipeTypes.CRAFTING`. Primeiro, ele chama o handler nativo do Sophisticated. Se ele retornar um erro apresentado ao jogador e existir um endpoint salvo no Upgrade de Crafting Remoto, o POC libera o botão “+”; ao clicar, envia a receita pelo container do upgrade. O servidor revalida menu, upgrade, receita, regra de crafting, acesso e filtros como no botão Buscar ingredientes. Esse caminho inclui também o inventário do jogador para completar receitas com fontes mistas. Sem Shift, o handler nativo executa a transferência normal e o addon só assume quando faltarem ingredientes. Com Shift, ele usa o handler nativo apenas para validar a receita e envia a operação ao servidor, que planeja quantas execuções cabem no estoque combinado e nos limites dos slots e transfere quantidades por ingrediente. Se não houver estoque para uma execução completa, mantém o fallback de preenchimento parcial. Sem vínculo, o fluxo permanece nativo.

No NeoForge 1.21.1, o callback nativo de `Refill crafting grid` tenta primeiro o armazenamento da mochila e o inventário do jogador. Se faltar a unidade consumida, o wrapper remoto consulta e extrai uma unidade compatível da rede vinculada, usando o contexto da aba aberta e os adaptadores de armazenamento existentes. Esta extensão sempre prioriza as fontes locais nesse callback, independentemente da prioridade usada pelo botão Buscar ingredientes, e não muda o preenchimento do JEI.

O POC segue o registro de transfer handlers usado por `UseCraftingRecipeTransfer` do AE2 JEI Integration e pelos handlers de crafting grid da Refined Storage JEI Integration. No JEI fixado, o registro associa um handler a uma classe de container e tipo de receita; a entrada da mochila do Sophisticated usa `BackpackContainer` e `RecipeTypes.CRAFTING`. A integração depende da ordem de plugins para substituir essa entrada. Em 2026-11-12, o usuário confirmou em jogo o funcionamento das integrações implementadas, incluindo o botão “+”, o Shift para crafting em lote e o reabastecimento remoto após craft. A validação multiplayer permanece aberta (P12).

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

A exceção de networking autorizada pelo usuário limita o payload próprio S2C ao catálogo paginado; consulta e ações continuam no container Sophisticated. O servidor valida sessão/menu/aba, receita e limite por tick (P06). GameTests verificam preenchimento com as duas prioridades, resultado shaped, preenchimento parcial shapeless e devolução dos três baldes de uma receita de bolo; JUnit cobre conversão e codecs (P08/P09/P12). A evidência automatizada não encerra a aceitação manual P12.
