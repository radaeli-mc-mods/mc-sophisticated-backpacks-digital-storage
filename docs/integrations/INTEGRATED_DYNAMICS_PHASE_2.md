# Integrated Dynamics — fase 2: operações básicas e apresentação

Implementação do [plano aprovado](INTEGRATED_DYNAMICS.md) em 2026-11-20, sobre `c2f5c1c`, versão de desenvolvimento `0.6.0`. Piloto: Minecraft 1.21.1, NeoForge 21.1.238, Java 21. Aplicam-se P01–P04, P07/P08/P10/P11 e os gates P12. Implementação, automação e aceite manual são estados distintos.

## Contratos implementados

- `IntegratedDynamicsStorageAdapter`: consulta por identidade/tipo, presença sem simular extração, snapshots defensivos somente de leitura, inserção e extração simulada/real. Contagens agregadas são `long`; a extração real respeita o máximo da stack.
- `IntegratedDynamicsFluidStorageAdapter`: registro opcional junto com Dynamics + Tunnels, consulta com quantidades `long`, componentes completos e operações em mB. O `FluidStack` real limita cada pedido a `int`; não há conversão por baldes nem perda de componentes.
- Toda chamada revalida jogador, proteção, chunk, rede atual, Omni habilitado e validadores nativos, inclusive energia. Um target obtido antes da remoção do endpoint não concede acesso depois. Ausência de capacidade e recusa por identidade não alteram o vínculo.
- Inserção usa o canal agregado nativo `getChannel(WILDCARD_CHANNEL)`, incluindo filtros, prioridades, limites e interceptadores. Somente a sobra retornada é considerada não aceita. Os argumentos são copiados; uma falha com resultado remoto incerto propaga sem restituição integral ou repetição no adaptador.
- Consulta e extração dirigida usam o canal nativo `IngredientChannelPositioned` com `WILDCARD_CHANNEL = -1`. A auditoria e as primeiras execuções demonstraram que o índice assíncrono do canal agregado pode estar vazio no primeiro acesso sem terminal ou após observar um guard. O canal posicionado nativo consulta a identidade pedida nos storages das interfaces, conservando seus filtros, prioridades, limites e proteção de reentrância. Não há acesso direto a inventários físicos pelo addon.
- Snapshots usam `IngredientCollectionPrototypeMap.prototypeIterator`, preservando a identidade e o agregado `long` sem duplicar canais nem converter o total para o tamanho de uma stack. Consultas por tipo usam o matcher nativo `ItemMatch.ITEM`; presença interrompe na primeira identidade elegível e não solicita extração.
- Os guards existentes continuam protegendo inventários e tanques externos em cache. O primeiro acesso e a liberação do guard não dependem de abrir um terminal ou aguardar um índice remoto. Não são persistidos objetos de rede/jogador, snapshots ou canais.

O registro opcional continua exigindo Dynamics **e** Tunnels. `common` não recebe tipos Minecraft/Cyclops. Terminals e Crafting continuam opcionais; o addon não solicita autocrafting.

## Apresentação

O piloto oferece o quinto estado dos nove upgrades: ciano Menril (`#B0F2E7` / `#36B9B2` / `#1E757B`) e marca superior de dois pixels, preservando as quatro quinas e o ícone original. Os índices 1–4 não mudam; Integrated ocupa 5. A animação usa a marca superior como origem, com 25 quadros e duração de 1,08 s. Existem todas as transições, incluindo revínculo à mesma rede.

As traduções EN-US/PT-BR mostram exatamente **Integrated Dynamics**, tanto no nome completo quanto no resumo do vínculo. Não foram criados itens, receitas ou variantes cosméticas de mochila.

O gerador usa os caminhos atuais por target, reutiliza imagens idênticas de `shared` e não apaga faixas existentes. A entrada de arte usa os originais versionados, sem depender de imagens locais ignoradas. As 234 imagens/strips legadas compartilhadas continuam byte a byte intactas. O preparo de recursos Forge exclui o quinto estado, seus modelos/faixas/traduções e o override correspondente; isso preserva o porte para a fase 10, sem implementar o backend Forge. Os recursos do 26.1.2 permanecem independentes.

[Prévia dos nove upgrades](../design/upgrade_visuals_integrated_pilot.png) · [Especificação visual](../design/UPGRADE_VISUALS.md#extensão-integrated-dynamics--piloto-1211-p07)

## Harness e evidências

O gate preserva os quatro cenários da fase 1 e acrescenta seis cenários de recursos. As fixtures são carregadas somente com Dynamics + Tunnels. Cabos, Omni e interfaces Tunnels são nativos; os inventários/tanques de teste são capabilities reais em um bloco registrado exclusivamente pelo mod de testes. A fixture de mochila usa um `BackpackWrapper` real, com upgrade instalado e inventário IO do Sophisticated exposto à rede por essa capability. Os testes não substituem `INetwork` por mocks.

As fixtures aguardam a disponibilidade real da infraestrutura antes de executar ações, em vez de presumir um atraso fixo de 40 ticks. O consumo de energia nativo é habilitado no processo GameTest antes de construir as redes; não altera arquivos de configuração ou instâncias do usuário.

| Cenário novo | Evidência exigida |
| --- | --- |
| `items` | Primeiro acesso sem terminal, componentes, snapshots defensivos, simulações, conservação, capacidade alterada entre estimativa e execução, sobras e target retido após remoção do Omni. |
| `channelsandlongcounts` | Mesma identidade em canais 0 e 7, agregado único de 4.294.967.294 itens e extração limitada a uma stack real. |
| `nativefilters` | Filtro posicionado nativo: extração recusada com inserção permitida e o inverso, sem permissões acopladas. |
| `fluidsandcachedguard` | Componentes, ida/volta de 1.000 mB, simulações sem mutação, capacidade parcial, filtro nativo e guard sobre capability previamente armazenada pela rede. |
| `cachedbackpackguard` | Mochila real exposta à própria rede, guards de entrada/saída em referências nativas já obtidas e liberação imediata no mesmo tick. |
| `nativeinterceptors` | Pre-consumer nativo contabilizado pelo resultado real, simulação sem avanço e exceção remota incerta sem repetição. |

Os filtros de teste são instalados pelo contrato público `setPositionedStorageFilter`, na posição do storage atendido pela interface. Isso exercita os mesmos filtros do canal; configuração de variáveis pela GUI das filtering interfaces continua sujeita ao roteiro de integração posterior. O teste de pre-consumer comprova o hook usado por integrações, sem criar um pedido de autocrafting; não representa uma fila real preexistente do Integrated Crafting.

O verificador visual inspeciona os nove modelos, cinco estados vinculados, quinas, índices e os 270 strips de 25 quadros. A imagem gerada foi inspecionada fora do jogo e aprovada pelo usuário em 2026-11-20 (“Aprovo a prévia”). A conferência da animação em jogo continua separada. O verificador do JAR exige adaptadores, metadados opcionais, traduções, modelos/faixas e ausência de dependências Cyclops e fixtures embutidas. Os gates Forge de recursos/reobfuscation/refmap/MixinExtras permanecem preservados.

### Resultados executados

Matriz local do piloto, executada com `:neoforge:check -PbackendProfile=<perfil>`:

| Perfis | JUnit por perfil | GameTests por perfil | Resultado |
| --- | ---: | ---: | --- |
| `none`, `dynamics` | 72 | 1 | Passou |
| `integrated`, `terminals`, `full` | 72 | 10 | Passou |
| `ae2`, `rs`, `toms` | 72 | 1 | Passou |
| `scc` | 74 | 1 | Passou |
| `all` | 74 | 10 | Passou |

Total: 724 execuções JUnit e 46 execuções GameTest, sem falhas, erros ou skips. O cenário único dos perfis sem Tunnels verifica carregamento e registro opcional; não representa operações reais dos outros backends. Cada `check` também verificou o JAR do piloto e seus recursos.

A invocação legada `:neoforge:test -PsccTests` passou com cinco testes, sem falhas, erros ou skips.

`:verify-forge-1.20.1` passou: um teste JUnit, os 16 GameTests existentes, reobfuscation, refmap/acessor e MixinExtras, recursos e empacotamento do JAR de produção. O verificador rejeitou as sete fixtures deliberadamente inválidas e aprovou o artefato real. O JAR Forge não contém os modelos, faixas ou traduções Integrated do piloto. Essa regressão não implementa nem valida o backend Integrated no Forge.

O verificador visual passou para os nove upgrades e os 270 strips; uma nova exportação preservou os hashes de todos os recursos já existentes do piloto. `git diff --check` e a conferência dos links locais da documentação passaram.

Logs locais ignorados: `build/integrated-audit/phase2-*.log`; relatórios por perfil em `targets/1.21.1/neoforge/build/{reports/integration,test-results}/`.

## Gates e limites

- [x] Implementação de itens/fluidos, traduções e recursos visuais do piloto.
- [x] Verificação estrutural dos modelos e dos 270 strips; inspeção da prévia gerada.
- [x] Matriz automatizada final e verificação dos artefatos.
- [x] Prévia estática do piloto aprovada pelo usuário em 2026-11-20.
- [ ] Conferência e aceite da animação dentro do jogo.

Os gates específicos de Deposit/Restock, Pickup/Magnet, Refill, Feeding/Alchemy, Pump e Crafting continuam nas fases 3–8. O aceite em cliente, servidor dedicado com jogadores, multiplayer, modlist e trabalhos reais preexistentes do Integrated Crafting permanece separado. Não foram executados portes nem instalada uma instância existente. A documentação pública EN/PT-BR continua para o comportamento de jogador verificado e consolidado; este registro e os planos são internos e não devem ser publicados no portal.
