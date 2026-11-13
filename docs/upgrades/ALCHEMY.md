# Alchemy — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_alchemy_upgrade`. Aplicar consumíveis condicionais somente ao portador, obtendo-os da rede/mochila conforme prioridade. Padrão Rede primeiro. Não incluir entidades próximas nem seleção de alvos nesta etapa.

Usar condições e definições do Alchemy avançado nativo. Processar regras em ordem de slot e executar uma aplicação por vez, preservando duração e efeitos de uso. Comparar efeitos, duração e amplificador pelos controles nativos disponíveis.

## 2. Premissas aplicáveis

Aplicar [P02 — vínculo e receita compartilhada de desvínculo](../UPGRADES.md#P02); não oferecer botão de desvínculo na aba.

[P04 — portador](../UPGRADES.md#P04), [P05 — fontes](../UPGRADES.md#P05), [P06 — definições nativas](../UPGRADES.md#P06), [P08 — recuperação](../UPGRADES.md#P08), [P09 — reserva](../UPGRADES.md#P09), [P10 — ciclos](../UPGRADES.md#P10) e [P11 — persistência](../UPGRADES.md#P11). Aplicar [P03](../UPGRADES.md#P03) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-15):** condição/atributos e definições nativas estão conectados por accessor Mixin localizado; o serviço mantém duração e reserva persistida. A textura 16×16 parte do ícone avançado original e usa a paleta remota do Magnet. Forge 1.20.1 e NeoForge 1.21.1 reproduzem a aba avançada nativa: oito slots de poção fantasma em duas colunas, nove estados nativos de condição, três controles nativos de comparação e sprites/tooltips do Sophisticated Core. Slots vazios mostram a condição desabilitada; inserir um item escolhe o padrão da primeira definição nativa compatível, substituir preserva a configuração e remover redefine para `NEVER`. `HURT` começa em 75% e seu limiar muda em passos de 5% por scroll. Configurações antigas descartam o nono filtro e seu atributo ao serem carregadas. A prioridade remota e o vínculo permanecem abaixo dos controles nativos. Testes e validação visual/operação em jogo ainda são necessários.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Verificado:** `AlchemyCondition.test(LivingEntity, float)` e condições `NEVER`, `ALWAYS`, `UNDER_WATER`, `ON_FIRE`, `FALLING`, `MINING`, `SPRINTING`, `HURT` e `NEGATIVE_EFFECT`. Há atributos, comparadores públicos e `AlchemyItemDefinition` com funções de início/fim de uso. `addItemDefinition` permite extensões; a lista de definições e parte da aplicação são privadas.

**Proposto:** serviço de aplicação sobre extração compartilhada e estado persistido de reserva. Reutilizar consumo/recipientes do [Feeding](FEEDING.md), mas manter duração própria do consumível. Accessor localizado consulta a lista nativa real, inclusive extensões, sem reconstruí-la manualmente.

## 4. Implementação

- Wrapper: implementar `ITickableUpgrade`, atributos/condições nativos e prioridade. Não expor `EntityMatch` nesta primeira versão.
- Definições: consultar registro nativo na operação; usar filtros, `canApply`, comparadores e rotinas de uso da definição. Não restringir a uma lista própria de poções/leite/maçãs.
- Estado proposto: ocioso, reservado, em uso e recuperando. Persistir a unidade real reservada e dados suficientes de recuperação; referências de entidade/definição são transitórias.
- Serviço: primeira regra elegível em ordem de slot inicia uma aplicação; nenhuma segunda extração enquanto houver recurso reservado.
- Container/aba: oito slots de poção fantasma e grade de condições em duas colunas, usando `AlchemyUpgradeTab.ALCHEMY_CONDITION`, `ANY_EFFECT_MISSING`, `MATCH_DURATION` e `MATCH_AMPLIFIER`. Reproduzir slots e fundos nativos; deixar condição, tooltip e scroll inativos quando o filtro está vazio. Prioridade e vínculo ficam abaixo das quatro linhas nativas. Validar alterações no servidor.
- Compatibilidade: normalizar filtros e atributos persistidos para oito posições, preservando os índices 0–7 e descartando o índice 8. A migração é idempotente e não altera vínculo, prioridade, comparação ou reserva.
- Accessor Mixin: expor registro privado de definições para leitura na versão fixada. Não usar reflexão nem substituir a implementação nativa global.

## 5. Fluxo operacional

1. Validar portador/habilitação e resolver recuperação pendente antes de buscar novos itens.
2. Sem aplicação ativa, avaliar regras no intervalo nativo e selecionar a primeira condição elegível com consumível disponível.
3. Extrair uma unidade real, persistir reserva e revalidar condição/alvo antes de iniciar o uso.
4. Iniciar uso nativo e avançar duração sem consultar/extrair novamente a oferta.
5. Se o alvo continuar válido, concluir uma vez e devolver recipiente; caso contrário, recuperar recurso ainda não consumido.
6. Limpar estado concluído e iniciar nova avaliação somente na próxima cadência elegível.

Perder conexão após reservar não apaga o item: a aplicação já iniciada pode concluir para o portador válido. Remoção/desabilitação, morte ou troca de portador interrompem uso ainda não concluído e encaminham recuperação.

## 6. Falhas e ciclo de vida

Reinício com aplicação ativa entra em recuperação; não reaplicar efeitos automaticamente nem persistir entidade para retomada cega. Remover o upgrade transporta o recurso pendente no item. Troca/desvínculo permanece bloqueado enquanto houver pendência.

Condição que deixou de existir antes do início não consome. Depois do início, seguir a semântica da definição nativa, sem prolongar uso indefinidamente. Resultado de consumo e recipiente são contabilizados separadamente; não restaurar unidade já consumida porque o retorno do frasco falhou.

Não prometer atomicidade entre efeitos e salvamento em encerramento abrupto do processo; a garantia cobre interrupções normais e recursos efetivamente persistidos, conforme P09.

## 7. Etapas de entrega

- [x] Adaptar atributos, definições nativas, layout e migração para oito filtros; gate funcional ainda depende dos cenários de integração abaixo.
- [ ] Implementar reserva/máquina de estados; gate: uma unidade e uma aplicação ativas no máximo.
- [ ] Integrar duração, consumo e recipientes; gate: efeitos/hooks executados uma vez.
- [ ] Implementar interrupções/GUI; gate: reinício e remoção não perdem a unidade não consumida.

## 8. Validação e aceitação

Testes: inserção/substituição/remoção/reabertura do filtro; cada condição e valor padrão; bloqueio de slot vazio; `HURT` em 0/75/100% e passos de 5%; migração com nona regra sem alteração das oito primeiras; comparação de efeitos/duração/amplificador; duas regras elegíveis, definição adicionada, simulação sem reserva, uso ativo sem nova extração, interrupção e retorno de recipiente.

Em jogo: fogo, submersão, ferimento e efeito negativo; poções e outros consumíveis definidos nativamente; perda de conexão após reserva; remoção, desabilitação, morte e reinício durante uso; Feeding junto. **Aceito quando** somente o portador é afetado, duração nativa é respeitada e nenhuma aplicação/reserva é duplicada.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target preserva definições, condições, duração de uso e a reserva, adaptando os acessos a componentes e os efeitos nativos (P04/P08/P09). Os GameTests verificam o efeito e o consumo por origem com as duas prioridades. A evidência automatizada não encerra a aceitação manual P12.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.
