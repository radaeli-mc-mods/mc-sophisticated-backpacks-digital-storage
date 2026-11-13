# Pickup — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_pickup_upgrade`. Ao caminhar sobre um drop, encaminhar itens filtrados para rede/mochila na prioridade configurada. Padrão Rede primeiro; a sobra final retorna ao fluxo original de coleta. Não coletar XP.

O upgrade responde à coleta normal, sem procurar entidades próximas. Respeitar quais mochilas o Sophisticated considera elegíveis e sua ordem de processamento.

Mochila primeiro mantém o filtro de cada destino, conforme [P05](../UPGRADES.md#P05): em `Stored content`, a mochila precisa já conter o item ou aprová-lo pela memória nativa. Item existente somente na rede segue para ela, mesmo com espaço livre local. Regra reconfirmada em 2026-10-30. O controle compartilhado atualiza o estado local antes de enviar a prioridade selecionada ao servidor, seguindo o padrão nativo (P06/P11).

## 2. Premissas aplicáveis

[P03 — infraestrutura](../UPGRADES.md#P03), [P04 — contexto](../UPGRADES.md#P04), [P05 — prioridade](../UPGRADES.md#P05), [P06 — instalação](../UPGRADES.md#P06), [P08 — simulação](../UPGRADES.md#P08) e [P10 — ciclos](../UPGRADES.md#P10). Seguir [P02](../UPGRADES.md#P02), [P07](../UPGRADES.md#P07) e [P11](../UPGRADES.md#P11).

## 3. Estado atual e dependências

**Implementação atual (2026-10-12):** wrapper, filtros, prioridade e serviço de roteamento estão registrados. `PickupContextMixin` usa MixinExtras para delimitar o jogador/evento durante o handler original do Sophisticated; inicialização do cliente passou. Sua textura 16×16 parte do ícone avançado original, incluindo o padrão de cantos e pontos de borda, e usa a paleta remota do Magnet; o modelo aponta para o asset local. Aparência em jogo, coleta, simulação, múltiplas mochilas e sobras ainda precisam de validação manual.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Verificado:** `IPickupResponseUpgrade.pickup(Level, ItemStack, boolean)` não recebe jogador. `CommonEventHandler.onItemPickup(ItemEntityPickupEvent.Pre)` conhece o jogador e usa simulação antes da execução. `InventoryHelper` tem overloads com/sem jogador, mas o callback do upgrade permanece sem esse argumento.

**Proposto:** contexto transitório de pickup e roteamento compartilhado. Não obter autoridade de um tick anterior ou de GUI previamente aberta.

## 4. Implementação

- Wrapper: implementar `IPickupResponseUpgrade`, filtros avançados `ContentsFilterLogic` e prioridade persistida; negar atuação remota sem contexto válido.
- Mixin localizado: envolver o corpo original de `CommonEventHandler.onItemPickup` com escopo do jogador/evento, restaurando contexto anterior em `finally`. O escopo cobre simulação e execução e suporta aninhamento.
- Não substituir o handler original nem duplicar a iteração de mochilas. Conferir descritor exato no JAR; o Mixin é comum, sem referência a classes opcionais.
- Serviço: rotear uma cópia da oferta para destinos local/remoto, devolvendo o resto. Sobras do primeiro destino são a única oferta do segundo.
- Container/aba: filtros, prioridade, estado do vínculo; desvínculo somente pela receita de [P02](../UPGRADES.md#P02); sem botão de transferência.
- Item: conflito apenas com Pickup básico/avançado, mantendo convivência com Magnet.

## 5. Fluxo operacional

1. Entrar no escopo nativo de coleta com jogador e mundo válidos.
2. Resolver wrapper instalado/habilitado e origem carregada pelo jogador.
3. Verificar filtro; itens recusados retornam intactos.
4. Em simulação, estimar aceitação dos dois destinos sem mutação. Tom’s pode superestimar; isso não autoriza confirmar coleta antecipadamente.
5. Em execução, conectar novamente e inserir na ordem configurada, protegendo entrada externa na própria mochila durante acesso à rede.
6. Retornar sobra real ao Sophisticated, que conserva autoridade sobre atualização da entidade e conclusão da coleta.
7. Liberar escopo mesmo em exceção.

## 6. Falhas e ciclo de vida

Rede indisponível permite tentativa local; ambos cheios devolvem a oferta intacta. Falta de vínculo não concede acesso remoto. Não cancelar o evento adicionalmente nem remover a entidade por conta própria.

Simulação que estimou sucesso pode ser seguida de execução parcial: a sobra real precisa sobreviver. Troca de jogador, remoção do upgrade ou exceção nunca deixa contexto global acessível à próxima coleta. Configuração sobre apenas mochilas vestidas continua sendo a nativa.

## 7. Etapas de entrega

- [ ] Criar escopo e Mixin; gate: contexto correto em ambas as fases e limpeza garantida.
- [ ] Implementar roteamento/simulação; gate: zero mutações em simulação e conservação da oferta real.
- [ ] Registrar item e configuração; gate: conflitos corretos e persistência da prioridade/filtros.
- [ ] Validar fluxo nativo; gate: múltiplas mochilas e destinos parciais sem coleta duplicada.

## 8. Validação e aceitação

Testes: ambas as prioridades, filtro recusado, nenhuma conexão, destinos parciais/cheios, superestimativa do Tom’s, guard de entrada, contextos aninhados, limpeza em exceção e jogador diferente na coleta seguinte.

Em jogo: caminhar sobre drops nos três backends, slots nativos de mochila suportados, configurações de mochila vestida, múltiplas mochilas e Magnet junto. **Aceito quando** só a quantidade efetivamente aceita é encaminhada e o restante continua disponível ao fluxo original, sem depender da GUI.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Stored content — correção de 2026-10-30

P03/P05/P06/P08/P10/P11: Pickup e Magnet avaliam conteúdo separadamente por destino: a rede usa identidades presentes no backend; a mochila conserva o filtro nativo, incluindo memória. Prioridade oferece somente a sobra ao destino seguinte, que precisa aprovar seu próprio filtro. A consulta de presença em StorageTarget não exige extração nem usa simulação de extração; comparações reutilizam stackMatchesFilter e só restringem por tipo no modo ITEM. As consultas são transitórias, sem cache entre operações.

Restock inicializa INVENTORY apenas quando não há configuração salva; reconstrução não sobrescreve ALLOW/BLOCK. Containers de Pickup/Magnet consultam o wrapper atual após sincronização. Deposit continua comparando com conteúdo da rede; Restock usa o snapshot inicial da mochila. No Forge, a consulta do Deposit passa a ocorrer dentro do guard.

A ausência de transferência manual relatada em Tom’s/SCC ainda exige reprodução com rede real. Testes isolados de filtro não comprovam descoberta da topologia, sincronização da GUI ou permissões em mundo. Validar os três modos, reabertura/reinstalação/reinício, conteúdo somente em cada destino, memória, componentes, ambas as prioridades e redes cheias/offline. Gates de jogo de P12 permanecem abertos; publicação no portal continua adiada conforme o plano SCC.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

A coleta recebe `ItemResource`, quantidade e `TransactionContext`. A reserva usa journal; abort raiz/aninhado não grava na rede. Commit persiste antes da entrega; a sobra real permanece salva, bloqueia vínculo/desvínculo e é recuperada antes de nova oferta. Exceção externa com resultado incerto coloca a reserva em quarentena, sem reenvio/refund automático (P04/P08/P09/P10). O GameTest publica o evento nativo de pickup; JUnit cobre transações e persistência. A evidência automatizada não encerra a aceitação manual P12.

P04/P08/P10 em 26.1.2: o contexto Pickup consulta capacidade antes de o handler nativo abrir a transação raiz, usando o mesmo provedor de mochilas, opção de mochila vestida e resolvedor de hosts vinculados do Sophisticated. O callback revalida acesso/filtro e limita a reserva à capacidade estimada e à oferta; a entrega continua após commit. No Tom’s, a estimativa usa a visão transacional nativa com abort e a execução continua pelo terminal. Redes e mochilas cheias preservam a entidade original nos cenários nativos de capacidade finita.

A entrega Pickup grava o marcador de operação em andamento antes de cada destino imediato e grava a sobra conhecida após o retorno. Se a sessão termina com esse marcador salvo, a reconstrução não reenvia automaticamente a reserva. Exceções de recuperação mantêm o último checkpoint em quarentena e são registradas sem interromper o callback que atualiza a entidade original. Isso não promete atomicidade de persistência entre storages distintos (P08/P09).

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.
