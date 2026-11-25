# Review da integração com Integrated Dynamics

> **Atualização da implementação — 25/11/2026:** F01–F06 e R01–R03 foram tratados nos três targets. O texto da auditoria abaixo é histórico; a resolução, os commits e as verificações novas estão em [Correções implementadas](#correções-implementadas). A afirmação original de que não houve implementação ou testes se refere somente à rodada de auditoria.

Data: 25/11/2026. Checkout: cd27d4f1ae483262d06d8a514fbc3c53b637806f, versão de desenvolvimento 0.7.0-beta.2.

**Parecer:** a estrutura do backend é adequada para consolidação. O trabalho restante deve se concentrar na conservação de recursos diante de exceções, no abastecimento de quantidades maiores e na cobertura dos ports. Recomendo corrigir F01–F04 antes da release oficial. F05 é uma lacuna de regressão a fechar conforme os caminhos corrigidos; F06 é melhoria de desempenho a medir. R01–R03 são riscos separados, com condições ainda por confirmar.

A validação ingame de todos os loaders e a publicação das betas sem feedback negativo são informações fornecidas pelo usuário nesta revisão e consideradas como baseline. Os documentos antigos que ainda dizem “aceite pendente” não invalidam essa informação. Os achados de falha abaixo derivam do fluxo do código; não afirmo que esses cenários tenham ocorrido nas betas.

## Escopo e método

Foram revisados os quatro componentes Integrated de cada target — descoberta, autorização, itens e fluidos — e seus consumidores compartilhados: vínculo, registros opcionais, transferência, guards, recuperação, Pickup, Magnet, Refill, Feeding, Alchemy, Pump e Crafting. Também foram examinados build, metadados, manifestos, verificadores, fixtures e matriz de CI.

As referências normativas são [P01–P12](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/docs/UPGRADES.md), [arquitetura](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/docs/ARCHITECTURE.md), [plano Integrated](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/docs/integrations/INTEGRATED_DYNAMICS.md) e os nove planos individuais. Os JARs locais fixados foram usados para conferir as APIs e o comportamento nativo, inclusive por decompilação seletiva. Referências online complementam a inspeção; não substituem a baseline dos JARs.

Nenhum código de produção foi alterado. Não foram executados builds, JUnit, GameTests ou sessões de jogo nesta auditoria. As verificações efetivamente executadas estão registradas ao final.

| Target | Java / loader | Dynamics / Tunnels | Particularidade |
| --- | --- | --- | --- |
| Forge 1.20.1 | Java 17 / Forge 47.4.10 | 1.31.4 / 1.9.0 | LazyOptional; operações imediatas; origem validada na conexão. |
| NeoForge 1.21.1 | Java 21 / NeoForge 21.1.238 | 1.38.2 / 1.13.0 | Origem mantida no target; simulação booleana. |
| NeoForge 26.1.2 | Java 25 / NeoForge 26.1.2.124 | 1.37.1 / 1.13.2 | Abort/commit nativos; Pickup com journal e quarentena. |

## Pontos bem resolvidos

- **Isolamento opcional (P01):** registros instanciam os adaptadores somente com Dynamics e Tunnels. Terminals e Crafting não são exigidos para ativar o backend. As APIs Cyclops permanecem fora de common.
- **Autoridade e ciclo de vida (P02–P04/P11):** o vínculo conserva backend, dimensão e bloco; as chamadas resolvem a rede atual, verificam chunk, jogador, Omni e listeners. Não há rede persistida nem busca automática de outro bloco durante uma operação.
- **Regras nativas de recursos (P03/P08):** wildcard, filtros por interface, prioridades, interceptadores e identidade completa passam pelos canais Cyclops. O canal posicionado resolve a limitação do índice assíncrono nas consultas e extrações.
- **Proteção da própria mochila (P10):** guards por UUID, escopos aninhados e verificação no acesso à capability protegem também referências em cache. Não identifiquei nesta leitura um desvio novo desses guards específico do backend Integrated.
- **26.1.2:** as operações remotas usam transações próprias; o Pickup evita executar a entrega remota dentro da transação local abortável e persiste resultado incerto. Essa é uma referência útil para os demais fluxos.
- **Reprodutibilidade:** os 18 artefatos Integrated correspondem aos SHA-256 dos três manifestos. Os três JARs locais examinados passaram no verificador estrutural do projeto.

O Omni como infraestrutura de vínculo é coerente com o [manual oficial do Portable Terminal](https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/portable.html). A dispensa de carregar o terminal portátil é uma decisão aprovada do addon.

## Achados e correções propostas

### F01 — P1: a coleta pode deixar itens no destino e conservar o drop inteiro após exceção

**Afetados:** Magnet nos três targets; Pickup em Forge 1.20.1 e NeoForge 1.21.1. É uma fragilidade dos serviços compartilhados, alcançável pela integração Integrated.

**Evidência:** [DestinationItemFilter.route](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/DestinationItemFilter.java:16) efetiva o primeiro destino e só devolve um resultado após chamar o segundo. [RemoteMagnetService](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/RemoteMagnetService.java:77) atualiza a entidade apenas depois que o roteamento inteiro retorna. As versões antigas de [PickupService](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.21.1/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/PickupService.java:30) seguem o mesmo roteamento imediato.

**Cenário concreto:** um drop contém 64 pedras e a prioridade é Mochila primeiro. A mochila aceita 16; a rede recebe a oferta restante e lança exceção na execução, por exemplo em um insert pre-consumer. As 16 pedras já estão na mochila, mas o retorno não chega à atualização da entidade: o drop continua com 64. Não é necessário que a rede tenha aceitado algo para ocorrer esse excesso. A transação remota do 26.1.2 não abrange a inserção local já concluída pelo Magnet.

**Impacto:** duplicação por falta de contabilização da parcela conhecida; a exceção também pode escapar do callback. A ocorrência depende de uma falha excepcional do caminho externo, não de uma transferência parcial normal.

**Correção:** registrar cada parcela efetivamente aceita e preservar a sobra antes da próxima chamada falível. Tratar separadamente o resultado desconhecido da chamada que falhou; não devolver a oferta inteira nem tentar novamente sem informação. O fluxo precisa devolver controle ao processamento da entidade de forma consistente. Não basta adicionar catch e retornar a oferta original.

**Regressão indicada:** destino local aceita parte e remoto lança; testar também rede primeiro com aceitação conhecida seguida de falha local. Conferir a soma entre entidade, mochila, rede e eventual reserva, inclusive no tick seguinte. Referências: Pickup/Magnet e P05/P08/P09/P10.

### F02 — P1: o Pump não persiste os avanços de preenchimento entre destinos

**Afetados:** Pump nos três targets.

**Evidência:** [PumpStorageRouter.fill](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/PumpStorageRouter.java:68) acumula a quantidade aceita em uma variável local e chama o próximo handler. Ao contrário de drain, não informa um checkpoint por destino. [PumpTransferService.transfer](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/PumpTransferService.java:49) persiste a extração completa e só grava a sobra após fill retornar. [PumpService](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/PumpService.java:55) recupera automaticamente a reserva no tick seguinte.

**Cenário concreto:** uma operação reserva 1.000 mB vindos de um tanque adjacente. Com Mochila primeiro, o tanque local aceita 400 mB e a rede lança exceção ao tentar receber os 600 restantes. A reserva persistida ainda contém 1.000 mB, embora 400 já tenham sido entregues. Ao retomar, a recuperação pode oferecer os 1.000 novamente. O risco existe também no 26.1.2: a transação do destino remoto não reverte o tanque local já preenchido.

**Impacto:** duplicação de fluido na recuperação e possibilidade de falhas repetidas no tick.

**Correção:** acrescentar checkpoint por destino também no preenchimento, reduzindo a reserva imediatamente após cada resultado conhecido. Antes de entrar em uma chamada externa, marcar o trecho em andamento; em resultado incerto, pausar repetição automática e manter estado recuperável. Preservar a compatibilidade dos dados existentes e o limite de 1.000 mB.

**Regressão indicada:** 400 mB aceitos localmente + exceção remota; verificar a reserva após a falha, após reconstruir o wrapper e após reiniciar. Repetir a ordem inversa. Referências: Pump e P05/P08/P09/P10.

### F03 — P1: Deposit perde a custódia da extração quando a inserção lança exceção

**Afetados:** Deposit nos três targets.

**Evidência:** [ItemTransferService.deposit](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/ItemTransferService.java:41) retira a oferta da mochila e chama destination.insert. A restauração só existe após retorno normal; a oferta extraída não é registrada em reserva nem encaminhada para uma política de resultado incerto. O mesmo fluxo existe nos dois targets anteriores.

**Cenário concreto:** a simulação aceita 32 itens, o serviço os retira da origem e o insert pre-consumer lança antes de efetivar a inserção. Os 32 deixam de estar na origem e não chegam ao callback de recuperação. No 26.1.2, abortar a transação da rede não restaura a extração local, realizada antes dela.

**Impacto:** perda de itens em uma exceção da inserção. Se a falha ocorrer após aceitação parcial, uma restituição integral adicionada sem distinguir o resultado causaria duplicação.

**Correção:** estabelecer custódia explícita da oferta entre extração e entrega, com distinção entre falha comprovadamente sem commit e resultado desconhecido. Preservar/quarentenar o recurso e os dados necessários à resolução quando a quantidade aceita não puder ser determinada. Não resolver com rollback integral genérico.

**Regressão indicada:** simulação aceita e execução lança antes de mutar; depois, execução aceita parte e lança. Verificar conservação no estado conhecido e ausência de reenvio automático no estado incerto. Referências: Deposit e P08/P09/P10.

### F04 — P2: Restock processa no máximo uma stack de cada identidade por clique

**Afetados:** Integrated nos três targets.

**Evidência:** o [adaptador 26.1.2](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsStorageAdapter.java:98) limita a extração a getMaxStackSize; a mesma limitação existe no [Forge](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.20.1/forge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsStorageAdapter.java:158) e no NeoForge 1.21.1. Já [RestockService](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/RestockService.java:30) faz apenas uma simulação/extração por identidade agregada, sem continuar o déficit.

**Exemplo:** a rede contém 256 pedras e a mochila tem espaço para todas. Um clique transfere no máximo 64 dessa identidade, podendo transferir menos conforme os limites nativos. Para itens não empilháveis, o teto explícito é uma unidade por identidade. O [plano de Restock](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/docs/upgrades/RESTOCK.md:5) define preenchimento até a capacidade.

**Correção:** manter as ofertas individuais limitadas e continuar o déficit com trabalho limitado e revalidação, respeitando os limites nativos e o orçamento da operação. Se a política de vazão exigir continuação em ticks posteriores, ela deve ser explícita e cancelável. Não remover simplesmente o limite de stack do adaptador.

**Regressão indicada:** estoque de 256 pedras, capacidade de 256 e um pedido; incluir estoque parcial, itens não empilháveis e slots com upgrades de stack. O GameTest atual de Restock em 26.1.2 usa somente sete itens e não detecta essa limitação. Referências: Restock e P03/P08/P11.

### F05 — P2: a regressão automatizada não acompanha a implementação dos três ports

**Evidência verificável na configuração:**

- No [Forge build](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.20.1/forge/build.gradle:111), o runtime dos módulos Integrated está condicionado a runClientRequested. Selecionar um perfil Integrated ao executar check/GameTest não instala esses módulos. Os testes Forge preservados não exercitam uma rede Integrated real.
- O [CI](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/.github/workflows/ci.yml:10) preserva a suíte Forge, mas não possui uma matriz Integrated Forge equivalente à dos outros targets.
- No 1.21.1, o [gate de GameTests](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/gradle/integrated-gametest-gate.gradle:17) exige os cenários de topologia, vínculo e recursos. Não há nele os fluxos completos dos nove upgrades com rede Integrated que existem no 26.1.2.
- A cobertura nativa de 26.1.2 é mais ampla, incluindo upgrades, prioridades, mochila colocada e reinício. Entretanto, o teste nativeinterceptors chama diretamente o adaptador; não verifica a conservação de um serviço após uma exceção entre dois destinos.

**Impacto:** uma correção pode passar no target moderno e deixar o mesmo defeito nos ports. Um check verde do Forge não constitui evidência automatizada de funcionamento do backend Integrated nesse target.

**Correção:** permitir os perfis no runtime de teste/servidor Forge, isolar seus diretórios e acrescentar as regressões de F01–F04 por target. Portar cenários essenciais de serviço e recarga para 1.21.1. Não é necessário duplicar toda a matriz antes de corrigir os problemas: começar pelos fluxos que mudarem. Referências: P01/P08/P12.

### F06 — melhoria de desempenho: consultas seletivas ainda materializam a rede inteira

**Afetados:** todos os targets, com maior limitação no Forge.

**Evidência:** em [queryItems](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsStorageAdapter.java:52), todos os resultados do iterador são agregados antes de aplicar matches; needed só distingue pedidos positivos de zero. [UpgradeItemSources](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/UpgradeItemSources.java:44) volta a filtrar e ordena os candidatos. No Forge, [a consulta seletiva é privada](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.20.1/forge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsStorageAdapter.java:81) e os consumidores pedem o snapshot inteiro. Refill pode repetir essa busca para filtros diferentes no mesmo tick de avaliação.

**Impacto provável:** crescimento do custo no thread do servidor conforme número de interfaces, slots, variantes e mochilas. Não medi TPS ou latência nesta auditoria; não afirmo que haja lag nas betas.

**Correção:** separar snapshot completo, existência e seleção de candidatos; usar consultas por tipo/identidade quando a regra permitir e postergar a fonte secundária quando a prioritária já resolver a necessidade. Reutilizar consultas apenas dentro da operação e sob guards equivalentes, sem cache persistido de rede ou autoridade.

**Cuidado:** o teste channelsandlongcounts pede needed=1 e exige a contagem long completa. Truncar o resultado de queryItems alteraria o contrato observado. A otimização deve preservar contagens, alternativas, ordem estável e fallback quando um candidato visível não puder ser extraído. Referências: Refill/Feeding/Alchemy/Crafting e P11.

## Riscos a confirmar e manutenção

**R01 — membership do Omni diverge nos ports antigos.** [1.21.1](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.21.1/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsLinkDiscovery.java:56) e [Forge](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/1.20.1/forge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsLinkDiscovery.java:80) exigem isAddedToGroup. Nos JARs fixados, esse boolean é transitório e não é serializado; o callback de adição à rede é que o estabelece. O [26.1.2](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/storage/IntegratedDynamicsLinkDiscovery.java:56) usa a associação exata em LOADED_GROUPS porque seu teste de reinício já expôs uma divergência após desserialização, registrada no documento do port. Isso merece um teste específico de recarga/reinício nos dois targets antigos. Não é evidência de que todo reinício desses targets falhe: a ordem dos callbacks nativos pode restaurar o marcador. Se reproduzir, aplicar a verificação nativa equivalente da posição/face/grupo, mantendo as demais validações.

**R02 — exceção de um storage pode deixar a posição bloqueada na biblioteca nativa.** Nos três JARs fixados, IngredientChannelAdapter executa disablePosition, chama o storage e depois enablePosition sem finally nesse caminho. Uma exceção no storage pode interromper a limpeza. É uma vulnerabilidade upstream que o addon alcança; um catch no adaptador não desfaz esse estado. A consequência possível é uma interface deixar de participar de operações futuras. A persistência do bloqueio e o caminho de recuperação devem ser confirmados com falha injetada. Preferir correção upstream ou versão corrigida validada; não limpar indiscriminadamente os bloqueios nativos de outras operações. Evidência local: [classe decompilada do JAR 1.21.1](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/build/integrated-review/native-1.21.1/org/cyclops/integrateddynamics/core/network/IngredientChannelAdapter.java:154). Esse arquivo é temporário, em build.

**R03 — Feeding/Alchemy ainda não distinguem recuperação conhecida de entrega incerta.** Os caminhos de [Feeding](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/FeedingService.java:130) e [Alchemy](C:/Users/lucca/Documents/mc-mods/sophisticated-backpack-is-addon/targets/26.1.2/neoforge/src/main/java/com/radaeli/sophisticatedbackpacksdigitalstoragesexpansion/upgrade/AlchemyService.java:193) mantêm a reserva enquanto devolvem recursos/recipientes e só a limpam ao terminar o fluxo. Nos targets antigos, uma falha após inserção parcial pode deixar uma reserva que será oferecida novamente; no 26.1.2 a transação remota reduz esse risco quando todos os participantes obedecem ao journal. Hooks externos e fallbacks locais continuam exigindo análise de falha. Recomendo estender o contrato de resultado incerto usado no Pickup, com testes de retorno de recipiente. Não há demonstração nesta auditoria de duplicação de um recipiente comum pelo caminho transacional normal.

**Manutenibilidade:** a separação por target é necessária, mas os quatro arquivos Integrated repetem políticas que já divergiram: origem transitória no Forge, membership no 26.1.2 e seleção dos canais. Documentar essas diferenças junto de testes de contrato. Concentrar o cast para PositionedAddonsNetworkIngredients e as operações de canal em uma ponte pequena por target facilita futuras atualizações. Acrescentar Override e imports explícitos ajuda a detectar deriva, mas tem prioridade menor que a conservação de recursos. Não recomendo fundir tipos Minecraft/Cyclops no módulo common.

**Diagnóstico:** as recusas de energia, Omni e capacidade acabam em mensagens genéricas de indisponibilidade. Melhorar os motivos estruturados e logs limitados a mudanças de estado ajudaria suporte sem gerar mensagens a cada tick. Exceções devem registrar backend, endpoint e etapa, evitando despejo de NBT ou inventário do jogador.

**Documentação:** reconciliar as anotações antigas de “não validado”, “Forge sem variante Integrated” e “fase ainda pendente” com o estado atual. O checkout contém mudanças posteriores às notas de implementação. Registrar o aceite comunicado pelo usuário sem inventar uma matriz detalhada de cenários que não foi fornecida. O presente relatório é uma auditoria interna; não deve ser publicado como guia de jogadores.

## Leitura por função

| Função | Avaliação nesta revisão |
| --- | --- |
| Deposit | Caminho normal conserva sobras; falta custódia em exceção entre extração e inserção — F03. |
| Restock | Consulta long e guard adequados; uma extração por identidade limita o pedido — F04. |
| Pickup | 26.1.2 possui proteção de commit/resultado incerto; targets antigos compartilham a fragilidade de F01. |
| Magnet | Elegibilidade, contexto e guard mantidos; atualização da entidade precisa sobreviver a falhas entre destinos — F01. |
| Refill | Déficit e validação do destino presentes; consulta repetida e falha excepcional de recuperação merecem atenção — F06. |
| Feeding | Reserva e consumo nativo presentes; conferir devolução excepcional de recipiente — R03. |
| Alchemy | Definições e duração nativas preservadas; conferir recuperação após hooks/devolução falíveis — R03. |
| Pump | Reserva e checkpoint de extração presentes; falta checkpoint de preenchimento — F02. |
| Crafting | Autoridade server-side, grade nativa e fonte compartilhada preservadas; consultas e recuperações herdam os riscos comuns. Nenhum defeito adicional específico do backend foi estabelecido nesta leitura. |

## Ordem recomendada para consolidação

1. Definir um contrato comum de resultado conhecido, parcial e incerto e aplicá-lo a F01–F03. Usar o Pickup 26.1.2 como referência, adaptando o lifecycle de cada função.
2. Corrigir a continuação do Restock e testar quantidades acima de uma stack, sem contornar limites nativos.
3. Executar regressões de falha e recarga nos três targets, priorizando F01–F04 e R01. Fechar a lacuna de runtime de teste Forge.
4. Exercitar as operações corrigidas com Terminals/Crafting presentes, inclusive um trabalho real de Crafting, para validar interceptadores e reservas.
5. Medir consultas em uma rede grande antes de otimizar. Aplicar melhorias de manutenção e atualizar os registros de aceite.

Não vejo justificativa para reescrever o backend ou ampliar a entrega com autocrafting, seleção de canais ou novo formato de vínculo. O escopo de correção é tornar o comportamento excepcional tão cuidadoso quanto o caminho normal já validado.

## Verificações executadas e limites

- Estado Git inspecionado: checkout sem alterações versionadas antes e depois da auditoria.
- 18 SHA-256 verificados contra os três integrated-dependencies.lock.json: todos correspondem.
- APIs, serialização Omni, canais, interceptadores e tratamento de posição conferidos nos JARs fixados por decompilação seletiva.
- tools/verify-jar.mjs aprovado para Forge 1.20.1 beta.2 e NeoForge 26.1.2 beta.2.
- Para NeoForge 1.21.1, o JAR local disponível mais recente é beta.1; ele passou com sua própria versão. Não há beta.2 local nesse diretório. Isso não afirma nada sobre a publicação remota.
- Os verificadores confirmam estrutura, recursos, metadados e empacotamento dos artefatos existentes; não recompilam o checkout nem provam que esses binários representam o HEAD atual.
- CI e testes foram lidos, sem nova execução. Os cenários de exceção descritos em F01–F03 foram estabelecidos por análise do fluxo, sem reprodução ingame ou injeção de falha nesta rodada.
- Nenhum commit, bump, tag, publicação, alteração de mundo ou implementação de correção foi realizado.



## Correções implementadas

Implementação orientada por P01–P12, arquitetura e planos individuais, preservando versões, IDs, vínculo manual, filtros, limites nativos e dados anteriores. Cada achado recebeu commit próprio:

| Achado | Commit | Resultado e evidência |
| --- | --- | --- |
| F01 | `8f7b512` | Custódia/checkpoint entre destinos da coleta; resultado desconhecido fica em quarentena. Falhas nas duas prioridades não repetem parcelas aceitas. |
| F02 | `6d161f9` | Pump salva cada preenchimento e marca IN_FLIGHT antes da chamada. Regressão 400 mB entregues + 600 mB em custódia, reconstrução e ausência de reenvio. |
| F03 | `0d17415` | Deposit conserva a oferta extraída e os avanços da recuperação. Exceções antes/depois da mutação não autorizam restituição integral. |
| F04 | `89f94db` | Restock continua lotes por identidade até capacidade/estoque, com revalidação e orçamento de 1.024 lotes por clique. Casos: 256 itens, estoque parcial, não empilháveis, slots ampliados e extrações parciais. |
| F05 | `9b5f56d` | Runtime e CI Forge por perfil, fixtures isoladas e regressões nativas dos serviços nos três targets. O port 1.21.1 cobre os serviços alterados, sem alegar duplicação de toda a matriz de nove upgrades do 26.1.2. |
| F06 | `db3cd9b` | Consultas por tipo, filtro antes da agregação e fonte secundária adiada. Conserva contagens long completas com needed=1, alternativas e fallback. Cenário nativo com 4.096 variantes nos três targets. |
| R01 | `a752ae8` | Ports antigos verificam posição/face/grupo em LOADED_GROUPS, sem depender do boolean transitório. Desserialização nativa reproduzida antes da correção no 1.21.1; grupo errado e Omni desabilitado continuam recusados. |
| R02 | `8276ac7` | Escopo da ponte libera exclusivamente bloqueios nativos adquiridos pela própria operação. Falha real injetada, bloqueio alheio preservado e inserção posterior aprovada nos três targets. |
| R03 | `c718e90` | Checkpoints de retorno e quarentena de hooks Feeding/Alchemy. A regressão nativa de sopa com inventários cheios também reproduziu e corrigiu a restauração da mão que sobrescrevia o recipiente devolvido. |

Correções adicionais encontradas durante a implementação:

- `ef8bf43`: identidade unitária + quantidade int separada na persistência; compatibilidade com reservas antigas e teste de 16.384 itens/recipientes.
- `1ca2dd0`: falha no salvamento depois de uma entrega parcial mantém a última sobra em quarentena e devolve controle para atualizar a origem.
- `bcbd540`: especializações Pickup/Pump respeitam também a custódia genérica ao trocar/desfazer vínculo; regressão percorre os nove itens universais.
- `9c66ef8`: imports nativos explícitos e Override nos contratos; diferenças entre APIs continuam isoladas por target.
- `df70fd1`: logs de exceção incluem backend, endpoint e etapa; sem despejo de NBT ou inventário. Resultados incertos pausam a repetição automática.

Os contratos reutilizáveis ficam em common (DeliveryJournal, CheckpointedDelivery e LazySourceList); tipos Minecraft/Cyclops continuam nas pontes dos targets. A ponte nativa pequena de canais permanece em IntegratedDynamicsAccessPolicy, com escopo de posições separado. Mensagens de indisponibilidade da interface continuam genéricas; os logs de exceção agora fornecem o contexto de roteamento solicitado sem registrar mensagens por tick.

### Verificações desta implementação

Comandos executados com dependências locais, `--offline --console=plain`:

- Raiz: `./gradlew.bat :common:check` — 29 testes, sem falhas.
- Por target: `:forge:build -PbackendProfile=all` ou `:neoforge:build -PbackendProfile=all`, com os gates de check, recursos e empacotamento.
- Ausência opcional e conjunto mínimo: `:forge:check` / `:neoforge:check` com `-PbackendProfile=none` e `integrated`. Forge também repetiu `full`; os perfis full dos NeoForge foram exercitados durante as correções.

| Target / perfil final | JUnit | GameTests obrigatórios do addon |
| --- | ---: | ---: |
| Forge 1.20.1 / none | 1 | 17 |
| Forge 1.20.1 / integrated, full, all | 1 por execução | 32 por execução |
| NeoForge 1.21.1 / none | 86 | 1 |
| NeoForge 1.21.1 / integrated | 86 | 16 |
| NeoForge 1.21.1 / all | 88 | 16 |
| NeoForge 26.1.2 / none | 98 | 1 |
| NeoForge 26.1.2 / integrated | 98 | 35 |
| NeoForge 26.1.2 / all | 100 | 115 |

O XML do 26.1.2 contém ainda o cenário vanilla always_pass, excluído das contagens do addon. O perfil all executou também reinício em duas JVMs: seis cenários do addon em cada fase (profile + cinco backends), preservando recursos conhecidos e quarentena. Os gates rejeitam cenários obrigatórios ausentes, duplicados, falhos ou pulados. Relatórios ficam em `targets/<versão>/<loader>/build/reports/integration/<perfil>/`, JUnit em `build/test-results/`; reinício em `targets/26.1.2/neoforge/build/reports/restart/`.

Os três JARs 0.7.0-beta.2 recém-gerados passaram em `tools/verify-jar.mjs`: nove upgrades, recursos, metadados, bytecode e empacotamento. Forge também verifica reobf/refmap/MixinExtras e fixtures negativas do verificador. As fixtures de GameTest, o ajuste de Tom’s descrito abaixo e as dependências Cyclops não acompanham o artefato.

O perfil Forge all revelou uma falha do supplier de cliente do Tom’s 1.7.1 na validação de referências exclusiva do ambiente de desenvolvimento do Forge. Uma fixture exclusiva de teste (`ca12f4d`) evita avaliar esse ramo no servidor, como já ocorre em produção; execução no lado correspondente e JARs nativos são preservados. Uma execução sobreposta de Gradle também causou colisão de arquivos JUnit; o build final foi repetido sequencialmente. Essas execuções interrompidas não são contabilizadas como aprovação.

Guias locais e EN/PT-BR do mod-docs foram sincronizados; `pnpm run check` e `pnpm run build` passaram. Commit do portal: `57a3176`, preservando alterações preexistentes de outros trabalhos. Nenhum push, deploy, tag, publicação ou mudança de versão foi feito.

### Limites preservados

O aceite em jogo comunicado pelo usuário é a baseline dos três loaders; não houve nova sessão manual nesta implementação e nenhuma lista detalhada de cenários manuais foi presumida. O 1.21.1/Forge exercita desserialização nativa do Omni; a prova de reinício entre JVMs é do 26.1.2. A proteção de posições cobre chamadas pela ponte do addon, não operações independentes de outros mods.

Quando uma API externa lança sem recibo confiável, a última quantidade sob custódia permanece registrada e bloqueada; ela não comprova que essa quantidade ainda está fisicamente ausente do destino. Não há restituição integral, reenvio automático nem resolução automática inventada para esse estado. Checkpoints não constituem atomicidade entre arquivos de storage em um hard crash arbitrário.

As amostras locais das consultas seletivas estão na arquitetura; não são comparação controlada antes/depois nem medição de TPS. Snapshots amplos continuam necessários quando a regra precisa de alternativas sem tipo conhecido.
