# Magnet — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_magnet_upgrade`. Procurar drops próximos à origem e encaminhá-los conforme prioridade Rede primeiro/Mochila primeiro. O padrão é Rede primeiro. Coletar somente itens; XP está fora desta versão.

Alcance de atração e conexão sem fio são independentes: um drop dentro da atração não concede acesso remoto. Usar alcance, limites e cooldowns do Magnet avançado nativo.

Mochila primeiro mantém o filtro de cada destino, conforme [P05](../UPGRADES.md#P05): em `Stored content`, a mochila precisa já conter o item ou aprová-lo pela memória nativa. Item existente somente na rede segue para ela, mesmo com espaço livre local. Regra reconfirmada em 2026-10-30. O controle compartilhado atualiza o estado local antes de enviar a prioridade selecionada ao servidor, seguindo o padrão nativo (P06/P11).

## 2. Premissas aplicáveis

[P03 — alcance](../UPGRADES.md#P03), [P04 — automação](../UPGRADES.md#P04), [P05 — prioridade](../UPGRADES.md#P05), [P06 — reaproveitamento](../UPGRADES.md#P06), [P08 — quantidades reais](../UPGRADES.md#P08), [P10 — ciclos](../UPGRADES.md#P10) e [P11 — desempenho](../UPGRADES.md#P11). Registro, vínculo e receita seguem [P02](../UPGRADES.md#P02) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-15):** Forge e NeoForge Magnet reutilizam a faixa do item avançado nativo, bloqueadores do Sophisticated via accessor e transferência parcial com atualização do drop. No callback carregado usam o portador; para mochila colocada procuram o jogador real elegível mais próximo dentro de três blocos, validam interação com o bloco e usam a posição da mochila como origem de alcance remoto. A elegibilidade segue o nativo: impede somente o pickup delay infinito (`32767`) e os bloqueadores registrados. `PreventRemoteMovement` bloqueia o Magnet carregado; para mochila colocada, o bloqueio é suspenso quando o drop também tem `AllowMachineRemoteMovement`. Delay finito e dono diferente não bloqueiam a coleta. Os três estados do filtro usam ícones de lista permitida, bloqueada e mochila do atlas Sophisticated. A aparência do item e a coexistência com Pickup ainda aguardam validação em mundo.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Verificado:** Core fornece `ITickableUpgrade`, `ContentsFilterLogic`, `MagnetUpgradeConfig.magnetRange` e `IMagnetPreventionChecker.isBlocked(Entity)`. O wrapper nativo registra checkers por `addMagnetPreventionChecker`; seu conjunto e rotinas de seleção são privados.

**Proposto:** serviço de varredura usando o roteamento do [Pickup](PICKUP.md), consulta ao registro real de bloqueadores por accessor localizado e contexto de tick validado. Não copiar esse registro em cache permanente.

## 4. Implementação

**Gate Forge de empacotamento (P01/P12):** preservar `@Accessor("pickupDelay")` remapeável e declarar o refmap no JSON Mixin. O JAR final precisa conter `pickupDelay → f_31986_:I` nos contextos padrão e `searge`. `verifyForgeProductionJar` confere essa relação após `reobfJar`, e fixtures negativas impedem regressões. Isso não altera elegibilidade: `32767` bloqueia a coleta; delay finito mantém a regra nativa. Ver [validação multiloader](../MULTILOADER.md).

- Item/wrapper: implementar `ITickableUpgrade`, filtros avançados, prioridade e estado de cooldown por wrapper. Não implementar um segundo caminho de coleta normal via `IPickupResponseUpgrade`.
- Serviço: buscar `ItemEntity` na região nativa de atração; ordenar candidatos por distância e ID de entidade para processamento estável.
- Elegibilidade: respeitar entidade viva, stack presente, delay infinito (`32767`) e impedimentos de magnetismo nativos. Aplicar `PreventRemoteMovement` no contexto carregado; no contexto colocado, respeitar a exceção Sophisticated `AllowMachineRemoteMovement`. Igualar o comportamento Sophisticated para drops com delay finito ou dono diferente; não contornar proteção de outro mod.
- Accessor Mixins localizados: leitura do conjunto nativo de `IMagnetPreventionChecker` e do pickup delay privado na versão fixada; aplicar checkers e a exceção para delay infinito conforme a semântica nativa. Não refletir campos por nome em runtime.
- Roteamento: oferecer cópia do stack atual, processar destinos na ordem configurada e atualizar entidade somente pela diferença aceita.
- Container/aba: controles de filtros e prioridade, estado; desvínculo somente pela receita de [P02](../UPGRADES.md#P02). Não mostrar opção de XP sem implementação.

## 5. Fluxo operacional

1. Receber callback server-side e validar wrapper/cooldown. Para mochila carregada usar o portador; para mochila colocada resolver jogador real elegível até três blocos, validar interação com o bloco e pausar sem esse contexto. A elegibilidade dos drops usa o tipo de contexto do upgrade, não a identidade do jogador próximo.
2. Resolver contexto e elegibilidade de atração; rede indisponível deixa somente o caminho local disponível.
3. Buscar candidatos dentro dos limites nativos e aplicar bloqueadores/filtro.
4. Para cada candidato ainda válido, simular e executar roteamento protegido.
5. Reduzir stack pela quantidade real aceita; remover entidade apenas se a sobra for vazia.
6. Reproduzir feedback nativo de coleta somente quando houver movimento e aplicar cooldown correspondente.

## 6. Falhas e ciclo de vida

Ambos os destinos cheios deixam o drop intacto. Não reservar entidades entre ticks. Outra mochila pode coletar primeiro; reler existência/stack imediatamente antes da transferência evita usar oferta antiga.

Mochila colocada sem jogador elegível próximo não opera. Sair de alcance da rede não altera o raio de atração e permite fallback local. Remover/desabilitar o upgrade interrompe buscas; guard e contexto não sobrevivem ao tick.

## 7. Etapas de entrega

- [ ] Integrar contexto e cooldown nativos; gate: mochila colocada usa somente jogador real próximo e respeita a cadência do callback.
- [ ] Integrar bloqueadores e elegibilidade; gate: drops protegidos não são modificados.
- [ ] Conectar roteamento e atualizar entidades; gate: transferências parciais conservam o resto.
- [ ] Registrar GUI/receita e regressão; gate: convivência com Pickup e múltiplas mochilas aprovada.

## 8. Validação e aceitação

Testes: fronteira de atração, ambas as prioridades, filtros, bloqueadores registrados, delay infinito/finito, dono diferente, `PreventRemoteMovement` carregado, `PreventRemoteMovement` com/sem `AllowMachineRemoteMovement` colocado, entidade removida, mudança do stack, destinos parciais/cheios e cooldown.

Em jogo: drops dentro/fora da atração e cobertura sem fio, backends isolados/juntos, Magnet com Pickup, duas mochilas próximas, mochila colocada com/sem jogador elegível, combinação dos bloqueadores por contexto e XP no chão. **Aceito quando** somente drops elegíveis são coletados, XP não é processado e a quantidade remanescente nunca desaparece.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Stored content — correção de 2026-10-30

P03/P05/P06/P08/P10/P11: Pickup e Magnet avaliam conteúdo separadamente por destino: a rede usa identidades presentes no backend; a mochila conserva o filtro nativo, incluindo memória. Prioridade oferece somente a sobra ao destino seguinte, que precisa aprovar seu próprio filtro. A consulta de presença em StorageTarget não exige extração nem usa simulação de extração; comparações reutilizam stackMatchesFilter e só restringem por tipo no modo ITEM. As consultas são transitórias, sem cache entre operações.

Restock inicializa INVENTORY apenas quando não há configuração salva; reconstrução não sobrescreve ALLOW/BLOCK. Containers de Pickup/Magnet consultam o wrapper atual após sincronização. Deposit continua comparando com conteúdo da rede; Restock usa o snapshot inicial da mochila. No Forge, a consulta do Deposit passa a ocorrer dentro do guard.

A ausência de transferência manual relatada em Tom’s/SCC ainda exige reprodução com rede real. Testes isolados de filtro não comprovam descoberta da topologia, sincronização da GUI ou permissões em mundo. Validar os três modos, reabertura/reinstalação/reinício, conteúdo somente em cada destino, memória, componentes, ambas as prioridades e redes cheias/offline. Gates de jogo de P12 permanecem abertos; publicação no portal continua adiada conforme o plano SCC.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target mantém tick, elegibilidade e coleta nativos, adapta filtros e destinos ao inventário transacional e preserva as duas prioridades (P04/P05/P08/P10). Os GameTests acionam o wrapper instalado e verificam drop, mochila e rede reais. A evidência automatizada não encerra a aceitação manual P12.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.
