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
