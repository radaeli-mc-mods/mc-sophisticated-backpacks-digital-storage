# Refill — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_refill_upgrade`. Repor automaticamente itens configurados nos slots do portador, extraindo da rede/mochila conforme prioridade. Padrão Rede primeiro. Preencher somente o déficit permitido pelo stack e slot, sem metas numéricas próprias.

Expor a seleção de destinos do Refill avançado nativo, incluindo os slots de mão suportados. Destino ocupado por identidade/componentes incompatíveis não é sobrescrito. Não atuar sobre jogadores próximos ou mochila colocada.

## 2. Premissas aplicáveis

[P04 — portador](../UPGRADES.md#P04), [P05 — fontes](../UPGRADES.md#P05), [P06 — interfaces nativas](../UPGRADES.md#P06), [P08 — extração](../UPGRADES.md#P08), [P10 — guard](../UPGRADES.md#P10) e [P11 — cooldown](../UPGRADES.md#P11). Aplicar [P02](../UPGRADES.md#P02), [P03](../UPGRADES.md#P03) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-15):** serviço por slot-alvo, filtro nativo, prioridade e callback de seleção de bloco estão registrados. A extração usa a camada compartilhada e tenta devolver itens não aceitos. Forge 1.20.1 e NeoForge 1.21.1 usam o controle visual do Refill original: grade nativa de filtros, sigla verde no canto de cada filtro ocupado e seleção de destino pelo scroll. A tooltip mantém o nome do item e acrescenta o modo atual e a instrução de scroll com as traduções nativas do Sophisticated Backpacks. Remover um filtro volta seu destino para `ANY`. A geometria da grade continua na posição nativa (`x + 3`, `y + 24`) e o rodapé é posicionado a partir da altura real do controle. Compilação dos dois loaders passou; validação visual e funcional em jogo continua necessária.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Verificado:** Backpacks fornece `RefillUpgradeWrapper.TargetSlot`, seus codecs e controles de seleção. Refill nativo implementa `ITickableUpgrade`, `IFilteredUpgrade` e `IBlockPickResponseUpgrade`. Parte das rotinas de preenchimento é privada.

**GUI nativa (P02/P06):** reutilizar o `FilterLogicControl` e seus slots. Não criar botões adicionais. Para cada filtro ocupado, desenhar o acrônimo de `TargetSlot` em verde na posição nativa dentro do slot. O scroll avança/retrocede entre `ANY`, mãos e posições da hotbar; cada passo atualiza o wrapper local e envia a configuração pelo container Sophisticated. Prioridade e vínculo continuam abaixo da grade.

**Correção da GUI (P06/P11/P12):** os relatos em jogo demonstraram que usar `Slot.x/y` para desenhar e detectar hover misturava coordenadas relativas ao menu com coordenadas da tela. Indicador e hit test agora compartilham `FilterSlotGrid`, construído a partir da origem absoluta do controle e de seu `slotsTopYOffset`. A tooltip usa `ItemTooltipEvent`, como o Refill original: acrescenta destino e instrução de scroll ao tooltip normal do item, restrita à tela, à aba aberta e ao filtro sob o cursor. A referência à aba é fraca e é limpa no fechamento; nenhuma tooltip adicional é desenhada sobre a vanilla. Os testes comuns reproduzem o menu deslocado, os nove slots, bordas e áreas externas. Testes comuns e compilação dos dois loaders passaram; comparação visual e interação em jogo após esta correção permanecem pendentes.

**Proposto:** serviço de obtenção por identidade/quantidade sobre `ItemExtraction` e fonte local, reutilizando contratos e recuperação do [Restock](RESTOCK.md). `TargetSlot` é um tipo existente; o serviço remoto é novo. Preservar semântica pública das seleções, sem assumir acesso a métodos privados de preenchimento.

## 4. Implementação

- Wrapper: filtros avançados, mapa filtro → `TargetSlot`, prioridade e cooldown; salvar alterações via componentes/codificação nativa quando compatível.
- Serviço: calcular déficit pela identidade configurada e estado real do alvo; consultar/extrair somente o necessário. Processar regras em ordem de slot de filtro. Para `ANY`, completar primeiro uma pilha existente que corresponda em identidade/componentes e só usar um slot vazio quando não houver pilha parcial compatível.
- Destino: usar API do inventário do jogador, validar aceitação e sincronizar alterações pelo caminho nativo. Não modificar equipamento fora dos destinos nativos expostos.
- Seleção de bloco: implementar `IBlockPickResponseUpgrade` usando o mesmo serviço; preservar regras nativas de slot selecionado e realocação da seleção anterior.
- Container/aba: adaptar controles nativos ao wrapper universal; expor seleção por filtro, prioridade e vínculo; desvínculo somente pela receita de [P02](../UPGRADES.md#P02).
- Item: limite um e conflito com Refill básico/avançado. Nenhum Mixin específico planejado.

## 5. Fluxo operacional

1. Receber tick do portador válido ou callback de seleção de bloco; conferir habilitação/cooldown aplicável.
2. Ler regras e destino atual; calcular déficit sem substituir item incompatível. Para `ANY`, priorizar pilhas parciais compatíveis antes de slots vazios.
3. Planejar espaço e abrir guard de saída ao acessar rede que possa conter a própria mochila.
4. Extrair da fonte prioritária e da secundária apenas o déficit restante.
5. Revalidar destino e inserir o resultado real; recuperar qualquer excedente.
6. Atualizar inventário e cooldown. Na seleção de bloco, reportar sucesso somente quando o item estiver efetivamente disponível no destino nativo.

## 6. Falhas e ciclo de vida

Rede sem extração autorizada permite fonte local. Um slot modificado entre planejamento e commit não é sobrescrito; recuperar a oferta. Item não empilhável tem déficit máximo compatível com seu limite, não 64 fixo.

Não consumir reservas de Feeding/Alchemy e não retirar recursos de slots de upgrades. Remover a mochila ou trocar portador invalida a operação. Não manter rede ou inventário do jogador em campos persistidos.

## 7. Etapas de entrega

- [ ] Implementar obtenção por identidade e déficit; gate: quantidades extraídas nunca excedem espaço planejado.
- [ ] Adaptar configurações/controles de `TargetSlot`; implementação, regressão de coordenadas e compilação presentes. Gate: confirmar indicador, tooltip, scroll nas duas direções e reabertura em Forge/NeoForge dentro do jogo.
- [ ] Implementar seleção de bloco; gate: inventário cheio e seleção anterior preservados conforme regras nativas.
- [ ] Registrar item e regressão; gate: nenhum abastecimento de jogador próximo ou mochila colocada.

## 8. Validação e aceitação

Testes: slots vazios/parciais/cheios, `ANY` escolhendo pilha parcial compatível antes de slot vazio, componentes distintos, stack máximo variável, mão principal/secundária, regra sobreposta, destino alterado, fonte parcial, recuperação e seleção de bloco sem espaço.

Em jogo: consumir blocos, tochas e foguetes, trocar o item do slot, selecionar bloco, perder cobertura e alternar prioridade nos três backends. **Aceito quando** somente o déficit chega ao portador, sem sobrescrita e com comportamento nativo de seleção de destino.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target adapta os acessos a inventário/hotbar e os recursos nativos, mantendo o cálculo de déficit e a validação de slots (P04/P05/P08). Os GameTests verificam o preenchimento do portador com as duas prioridades e recursos locais/remotos. A evidência automatizada não encerra a aceitação manual P12.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.

## Vínculo RS/SCC por dispositivos da rede

A descoberta compartilhada de [P02](../UPGRADES.md#P02) também aceita dispositivos ativos do RS nos três targets e dispositivos da rede SCC online nos dois targets NeoForge. Cabos são recusados, inclusive o cabo central de hosts SCC multipart. O endpoint salvo continua sendo controller no RS ou antena elegível no SCC; esta função mantém suas regras operacionais, filtros e reservas. Aplicar P03/P04/P09/P11 e validar a interação em jogo conforme P12.
