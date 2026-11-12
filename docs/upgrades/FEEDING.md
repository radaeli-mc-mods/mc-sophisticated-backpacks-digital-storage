# Feeding — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_feeding_upgrade`. Alimentar automaticamente quem carrega a mochila com comida elegível da rede/mochila na prioridade configurada. Padrão Rede primeiro. Consumir uma unidade por alimentação.

Expor filtros avançados, seleção nativa de `HungerLevel` e opção de alimentar imediatamente quando ferido. Inicializar configurações com os padrões do Feeding avançado nativo da versão fixada. Não alimentar jogadores próximos nem mochila colocada.

## 2. Premissas aplicáveis

Aplicar [P02 — vínculo e receita compartilhada de desvínculo](../UPGRADES.md#P02); não oferecer botão de desvínculo na aba.

[P04 — portador](../UPGRADES.md#P04), [P05 — prioridade](../UPGRADES.md#P05), [P08 — recuperação](../UPGRADES.md#P08), [P09 — recursos pendentes](../UPGRADES.md#P09), [P10 — ciclos](../UPGRADES.md#P10) e [P11 — cadência](../UPGRADES.md#P11). Instalação e recursos seguem [P06](../UPGRADES.md#P06) e [P07](../UPGRADES.md#P07); acesso segue [P03](../UPGRADES.md#P03).

## 3. Estado atual e dependências

**Implementação atual (2026-10-12):** wrapper e aba reutilizam filtros e nível de fome Sophisticated; serviço consome uma unidade, executa os hooks do item e persiste reservas/recipientes. Sua textura 16×16 parte do ícone avançado original e usa a paleta remota do Magnet; o modelo aponta para o asset local. O código compila e o cliente inicia; aparência, semântica de alimentação e recuperação em jogo ainda não foram confirmadas.

**Verificado:** Core fornece `FeedingUpgradeWrapper`, `FilterLogic`, `HungerLevel`, getters/setters de limiar e alimentação quando ferido. Rotinas internas de escolha/consumo são privadas. A base atual não extrai alimentos nem devolve recipientes após consumo.

**Proposto:** serviço de seleção/consumo reutilizando obtenção de itens do [Refill](REFILL.md), extração do [Restock](RESTOCK.md) e recuperação compartilhada. Não presumir que herdar wrapper nativo seja compatível com o item universal.

## 4. Implementação

- Wrapper: implementar `ITickableUpgrade` e `IFilteredUpgrade`; salvar filtros, limiar, opção de ferimento e prioridade pelo ciclo nativo.
- Seleção: restringir a comida comestível para aquele portador e aprovada pelo filtro; reproduzir os critérios nativos de fome/saturação aplicáveis. Considerar candidatos em ordem estável de identidade dentro de cada fonte.
- Consumo: extrair uma unidade, revalidar elegibilidade e executar consumo pelo mecanismo nativo usado pelo Feeding, preservando efeitos e hooks. Não aplicar apenas aumento manual da barra de fome.
- Recuperação: guardar recurso ainda não consumido ou recipiente pendente no item antes de abandonar a operação. Reservas não são filtros nem ofertas reutilizáveis.
- Recipientes: tentar rede com permissão de inserção própria, depois mochila e recuperação nativa do jogador. Nunca desfazer alimentação já concluída por falha de retorno.
- Container/aba: controles nativos de fome/filtros/ferimento, prioridade e vínculo. Sem botão de consumir ou Mixin específico.

**Seletor de filtros (2026-10-30, P05/P06/P11):** Forge e NeoForge expõem `MatchButton.ALLOW_LIST` no próprio `FilterLogicControl`, com sprites, estados, tooltips e pacote nativos de lista permitida/bloqueada. Lista permitida aceita somente alimentos que correspondam aos filtros; lista bloqueada exclui os correspondentes. O modo salvo é preservado, inclusive ao copiar/reinstalar o upgrade. O container consulta o wrapper atual após sincronização, e fome/ferimento/prioridade/vínculo ficam abaixo da altura real do controle, acomodando a nova linha sem sobreposição. O botão compartilhado de prioridade atualiza o estado local antes de enviar o valor capturado ao servidor, conforme [P05](../UPGRADES.md#P05). Validação visual e alimentação em jogo continuam pendentes (P12).

**Linha visual (2026-10-31, P07):** o Feeding foi o piloto. A textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

## 5. Fluxo operacional

1. No tick elegível, validar portador, habilitação e cooldown; resolver pendências anteriores.
2. Avaliar fome/ferimento e selecionar alimento pela primeira fonte que tenha candidato elegível.
3. Revalidar acesso e extrair uma unidade real sob guard de saída quando remoto.
4. Revalidar fome/alimento imediatamente antes de consumir; se a necessidade desapareceu, recuperar a unidade.
5. Concluir consumo uma vez; tratar o stack/recipiente retornado e limpar reserva correspondente.
6. Aplicar cooldown nativo e sincronizar somente o estado alterado.

## 6. Falhas e ciclo de vida

Sem alimento remoto, tentar local; sem candidato, pausar até a próxima cadência. Uma fonte negada não autoriza extração administrativa. Componentes diferentes não podem substituir um candidato filtrado.

Se o jogador morrer, sair ou retirar o upgrade antes de concluir, preservar recurso não consumido e recuperar ao reinstalar. Reinício não reaplica automaticamente um consumo interrompido. Estado pendente bloqueia novas extrações e mudança de vínculo conforme P09.

Feeding e Alchemy não usam snapshots antigos de fome/efeitos: a segunda função executada reavalia o resultado real da primeira.

## 7. Etapas de entrega

- [ ] Adaptar critérios e configurações nativas; gate: fome/ferimento correspondem ao Feeding avançado.
- [ ] Implementar consumo e recipientes; gate: efeitos/hooks nativos ocorrem uma vez.
- [ ] Implementar recuperação de pendências; gate: remoção e reinício conservam recursos.
- [ ] Registrar interface/receita e regressão; gate: prioridades e três backends aprovados.

## 8. Validação e aceitação

Testes: limiares, ferimento, filtros, comidas com componentes/efeitos, item não comestível, extração vazia, necessidade alterada, retorno de recipiente negado e pendência persistida.

Em jogo: alimentos comuns e com recipientes, rede somente com extração permitida, perda de alcance, inventário cheio e Feeding junto com Alchemy. **Aceito quando** o portador é alimentado sob condições configuradas, efeitos são preservados e não há consumo/restituição dupla.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target usa `DataComponents.FOOD` e `ItemStack.finishUsingItem` para conservar o consumo e os efeitos nativos, mantendo a reserva e recuperação (P04/P08/P09). Os GameTests usam jogador em sobrevivência e verificam aumento de alimentação e contagem por origem nas duas prioridades. A evidência automatizada não encerra a aceitação manual P12.
