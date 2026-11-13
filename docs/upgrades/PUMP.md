# Pump — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_pump_upgrade`. Transferir fluidos entre armazenamento selecionado e recipientes nas mãos, fluid handlers adjacentes ou mundo, com filtros e direção de entrada/saída nativos.

O armazenamento pode ser rede ou tanque local, conforme prioridade; padrão Rede primeiro. Rede funcional dispensa tanque na mochila. O tanque é alternativa apenas quando instalado. Limitar cada oferta/reserva a um balde (1.000 mB). O Pump nativo garante um **mínimo** de um balde no cálculo de vazão; manter esse mínimo antes de aplicar o limite de reserva do addon. Preservar cooldowns do Pump avançado.

Funciona com a mochila equipada, carregada/na mão ou colocada. Para a mochila colocada, aplicar o contexto de jogador próximo definido em [P04](../UPGRADES.md#P04); não fabricar autorização remota com jogador falso. Fontes de mundo ficam no raio nativo (`distância² < 16`), em busca por largura a partir da mochila. Tanks/tubos usam as seis posições adjacentes e a face voltada para a mochila; saída para o mundo usa todas as direções exceto para cima.

Tom’s desta versão não oferece a capacidade: vínculo é permitido, mostra **“Esta rede não suporta fluidos”** e pausa a função inteira, inclusive com tanque local. Não transformar baldes em substituto de storage de fluidos.

## 2. Premissas aplicáveis

[P02 — vínculo incompatível](../UPGRADES.md#P02), [P03 — proteção](../UPGRADES.md#P03), [P04 — jogador](../UPGRADES.md#P04), [P05 — prioridade](../UPGRADES.md#P05), [P08 — simulação](../UPGRADES.md#P08), [P09 — reserva](../UPGRADES.md#P09), [P10 — ciclos de fluidos](../UPGRADES.md#P10) e [P11 — limites](../UPGRADES.md#P11). Seguir [P06](../UPGRADES.md#P06) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-15):** `PumpService` resolve contexto, modos e alvos. `PumpStorageRouter` aplica prioridade dentro de cada interação e oferece ao segundo storage somente o restante: entrada pode dividir um balde entre rede e tanque; saída pode somar a mesma identidade/componente das duas fontes. `PumpTransferService` simula, persiste a extração real e recupera sobras. Coleta do mundo exige capacidade para o balde inteiro. Saída aceita ar e fluido corrente, preservando fontes existentes. Recipientes usam cópia de uma unidade; a própria mochila é excluída por identidade/UUID. `GuardedFluidHandler` protege referências externas já em cache; o guard engloba resolução, consulta, execução e recuperação. Tom’s pausa por incompatibilidade. Uma reserva pendente continua sendo recuperada mesmo quando o upgrade está desativado; sem reserva, o estado desativado impede novas transferências. Textura e modelo continuam no padrão aprovado. A regressão operacional no cliente ainda precisa ser executada.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Causas corrigidas nesta revisão:** limite abaixo de 1.000 mB bloqueava baldes e mundo em qualquer contexto; tick descartava mochilas colocadas; seleção parava no primeiro fluido mesmo quando outro correspondia ao filtro/destino; colocação confundia fonte com fluido corrente; prioridade não complementava operações parciais; recipiente na mão podia ser a própria mochila ou uma stack incompatível com a capability. Em Forge 1.20.1, a validação da reserva agora lê o identificador `FluidName` produzido pelo `FluidStack.writeToNBT`; a chave `Fluid` usada na implementação NeoForge não existe na serialização Forge e fazia todos os candidatos serem rejeitados.

**GUI refinada:** os quatro toggles de Pump usam os mesmos sprites 16×16, estados e tooltips da aba Pump avançada do Sophisticated Core (Pump input/output, recipiente na mão, mundo e tanques/tubos). A prioridade usa o botão compartilhado do Magnet/Pickup conforme [P05](../UPGRADES.md#P05). Os controles ficam 6 px abaixo dos filtros; o resumo de vínculo ocupa uma linha própria, 8 px abaixo do botão de prioridade. O tamanho da aba acompanha os controles nativos, conforme P02/P06, sem o espaço vazio do posicionamento anterior.

**Verificado:** Core oferece `FluidFilterLogic`, `PumpUpgradeConfig`, controles de direção e interação com mãos/mundo/handlers. O wrapper usa callbacks nativos e handler de fluidos; várias rotinas são privadas. AE2 tem `AEFluidKey`; RS tem `FluidResource`. Tom’s auditado expõe inventários de itens, sem contrato de fluidos identificado.

**APIs auditadas nos JARs fixados:** `PumpUpgradeWrapper.getMaxInOut()` usa `Math.max(1000, configuração × linhas × multiplicador)`. Mãos têm cooldown de três ticks; handlers e colocação no mundo, vinte; coleta usa `20 × max(1, distância)`. `fillFromBlockInRange` usa busca por largura com distância² menor que dezesseis. Backpacks chama `tick(null, level, pos)` para a mochila colocada. NeoForge exige stack de uma unidade para `IFluidHandlerItem`; `BucketPickupHandlerWrapper` representa coleta, e `FluidUtil.tryPlaceFluid` aplica colocação/vaporização. Esses métodos privados do Pump foram auditados como referência, não invocados por Mixin.

**Contratos internos implementados:** capacidade opcional de fluidos no adaptador, normalização em mB, fachada limitada, roteamento, transferência com reserva e guard de handler externo. São contratos do addon, não APIs novas do Sophisticated. O guard de itens existente não protege fluidos. Pump depende da fundação e do mecanismo de reservas consolidado no [Alchemy](ALCHEMY.md).

## 4. Implementação

- Item/wrapper: filtros/direção/alternâncias e prioridade persistidos; limite/conflitos Pump básico e avançado.
- Adaptadores: converter identidade/componentes e quantidades para as unidades nativas de AE2/RS; consulta, simulação e execução específicas. Não usar conversão presumida por nome de constante.
- Serviço: oferecer fachada interna limitada de `IFluidHandler` para a operação validada, sem publicá-la como tanque ilimitado da mochila. Usar capacidades NeoForge e rotinas públicas nativas; orquestrar o mínimo necessário quando a rotina nativa for privada.
- Interações: preservar alcance local e seleção de alvos do Pump avançado. Ao habilitar múltiplos modos, manter mãos → mundo → handlers, independentemente da prioridade de storage, e no máximo uma transferência/reserva ativa por execução. Considerar outros fluidos elegíveis se o primeiro não passar no filtro ou no destino.
- Reserva: persistir identidade/componentes e quantidade real, até 1.000 mB, antes de concluir etapa que possa deixar sobra. Drenar reservas pendentes antes de novos recursos.
- Guard: proteger entrada/saída externas do tanque da própria mochila durante operações remotas, mantendo funcionamento normal fora do escopo. O Mixin localizado em `BackpackBlockEntity.getExternalFluidHandler` envolve o retorno em `GuardedFluidHandler`; cada consulta, simulação e mutação confere o UUID atual, inclusive referências já obtidas por pipes/storage buses. Acesso interno ao tanque continua permitido.
- GUI: controles nativos, prioridade, estado/capacidade e pendências; sem novas artes ou botão que contorne autorização.

## 5. Fluxo operacional

1. Validar instalação, cooldown e contexto de jogador (P04). Abrir o guard antes de resolver/consultar a rede. Usar a posição real da mochila para alcance remoto.
2. Recuperar primeiro qualquer reserva pendente, mesmo com o upgrade desativado. Se não houver reserva e o upgrade estiver desativado, encerrar o tick sem iniciar transferência. Com o upgrade habilitado, selecionar interação local e armazenamento pela prioridade.
3. Ler identidade e simular origem/destino com componentes preservados e quantidade limitada.
4. Em handlers/recipientes, drenar somente quantidade planejada, persistir reserva real e preencher destino; sobra tenta a fonte externa imediatamente quando reversível, depois segue recuperação. Ao somar extrações remota/local, persistir cada parcela antes de chamar a próxima fonte.
5. Em mundo, exigir aceitação de unidade completa antes de alterar bloco. Usar APIs que respeitem proteção, coleta/colocação e comportamento da dimensão.
6. Atualizar recipiente na mão pelo retorno da capability, sem duplicar stack original; concluir quantidade real e limpar reserva correspondente.
7. Liberar guard e aplicar cooldown do tipo de interação.

Para coleta do mundo, se o destino mudar após simulação, manter fluido efetivamente coletado na reserva. Para colocação, se a ação for recusada, conservar/devolver fluido reservado; se a dimensão consumiu o fluido pela regra nativa, não devolver a mesma unidade como sobra.

## 6. Falhas e ciclo de vida

Não drenar unidade parcial de bloco do mundo. Rede/tanque cheios podem deixar reserva pendente, nunca descartar fluido. Fallback não repete quantidade já aceita. Permissões de drenar origem e preencher destino são independentes.

Remoção ou reinício mantém reserva no upgrade. Ao reinstalar, tentar primeiro devolver à origem válida/autorizada, depois tanque local compatível; se não houver destino seguro, conservar pendência e pausar. Fluido não usa fallback de drop de item nem desaparece para desbloquear o upgrade.

Desativar o Pump não descarta nem congela uma reserva já extraída: cada tick elegível continua a recuperação e não inicia outra transferência enquanto desativado.

Alterar direção/filtros não transmuta reserva; ela mantém identidade original para recuperação. Troca/desvínculo bloqueados durante pendência. Sem jogador no contexto de P04, nenhuma interação de mundo/mãos/handlers é iniciada. Mochila colocada não conserva acesso quando esse jogador sai do alcance ou desconecta.

## 7. Etapas de entrega

- [ ] Implementar capacidades/unidades AE2 e RS; gate: um balde equivale exatamente a 1.000 mB ida/volta.
- [ ] Implementar roteamento, reserva e guard de fluidos; gate: sobras e própria mochila/tanque conservam quantidades.
- [ ] Integrar mãos/handlers; gate: capabilities retornam recipientes corretos e aceitação parcial segura.
- [ ] Integrar mundo/proteção; gate: unidade completa, recusa e evaporação sem restituição indevida.
- [ ] Registrar GUI/incompatibilidade; gate: Tom’s não transfere e estado pendente sobrevive ao reinício.

## 8. Validação e aceitação

**Verificação executada nesta revisão (2026-10-12):** `build` com Java 21 passou; 51 testes passaram, incluindo 21 regressões de Pump/guard. Cobertura específica: capabilities reais de baldes NeoForge nos dois sentidos sem tanque local, mínimo de 1.000 mB nos seis tiers nativos, prioridades e complemento parcial, seleção de outros fluidos, componentes, simulação sem mutação, exigência de balde inteiro, destino que muda após simular, persistência antes da segunda fonte, reserva ao remover/copiar o item, geometria/colocação e guard com capability em cache. A classe nativa de mochila colocada carregou com o Mixin. Os testes usam os APIs reais de Core/Backpacks/NeoForge, com destinos controlados; não montam redes AE2/RS nem substituem prova de interação em mundo.

**Reteste manual obrigatório após reiniciar o cliente:** em AE2 e RS, repetir entrada e saída com mochila equipada, na mão e colocada; testar individualmente mãos, mundo e tanks/tubos adjacentes, ambas as prioridades, sem tanque local e com tanque. Na mochila colocada, permanecer no alcance de contexto de P04; afastar/desconectar o jogador deve pausar a operação. Confirmar água/lava, fluido filtrado que não é o primeiro da rede, falta de um balde completo, capacidade parcial, perda de alcance/energia e recipiente empilhado. Nenhum gate manual foi marcado como concluído.

Testes: conversão de unidades/extremos, componentes, ambas as direções/prioridades, capacidade parcial, nenhum tanque local, reserva cheia, recuperação impossível ou com upgrade desativado, sem iniciar nova transferência desativado, handler muda após simular e incompatibilidade Tom’s.

Em jogo: água/lava e fluido modded, recipientes empilhados e com capability, tanques/pipe adjacentes, mundo protegido, água em dimensão que evapora, rede sem energia, tanque exposto à própria rede, remoção/reinício com fluido pendente. **Aceito quando** AE2/RS transferem quantidades corretas, nenhuma sobra some e Tom’s comunica a limitação sem executar a função.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target usa `ResourceHandler<FluidResource>` na capability externa e uma fachada local transacional. O guard consulta o UUID em cada acesso e protege referências em cache. GameTests verificam um balde nos dois sentidos com as duas prioridades, recusa Tom’s e identidades com componentes em AE2/RS; SCC mantém a recusa de componentes adicionais (P03/P08/P09/P10). A evidência automatizada não encerra a aceitação manual P12.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.

## Vínculo RS/SCC por dispositivos da rede

A descoberta compartilhada de [P02](../UPGRADES.md#P02) também aceita dispositivos ativos do RS nos três targets e dispositivos da rede SCC online nos dois targets NeoForge. Cabos são recusados, inclusive o cabo central de hosts SCC multipart. O endpoint salvo continua sendo controller no RS ou antena elegível no SCC; esta função mantém suas regras operacionais, filtros e reservas. Aplicar P03/P04/P09/P11 e validar a interação em jogo conforme P12.
