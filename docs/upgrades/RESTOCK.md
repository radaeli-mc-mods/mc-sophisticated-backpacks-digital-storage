# Restock — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_restock_upgrade`. O botão **Abastecer** busca itens filtrados na rede vinculada e preenche a mochila até sua capacidade. Não modifica diretamente inventário do jogador e não define metas por item. Não oferece prioridade nem transferência por clique em containers externos.

Usar filtros avançados de permissão, bloqueio e conteúdo da mochila. O modo por conteúdo considera identidades presentes no início do pedido; não expande a seleção com itens recém-inseridos.

## 2. Premissas aplicáveis

[P02 — vínculo](../UPGRADES.md#P02), [P04 — GUI](../UPGRADES.md#P04), [P06 — instalação](../UPGRADES.md#P06), [P08 — recuperação](../UPGRADES.md#P08), [P10 — guard de saída](../UPGRADES.md#P10) e [P11 — desempenho](../UPGRADES.md#P11). Aplicar [P03](../UPGRADES.md#P03) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-12):** serviço de extração, limites de capacidade, recuperação, prioridade fixa rede→mochila, aba manual e receita foram registrados. A UI/serviço reaproveitam `DepositFilterLogic` em modo `INVENTORY` para snapshot do conteúdo, em vez de `ContentsFilterLogic` da especificação inicial. Sua textura 16×16 parte do ícone avançado original, incluindo o padrão de cantos e pontos de borda, e usa a paleta remota do Magnet; o modelo aponta para o asset local. Build e início do cliente passaram; aparência em jogo, filtros e transferências continuam sem prova manual.

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e 26.1.2, e Forge 1.20.1. No target 26.1.2, quadros avaliados durante a extração da hotbar são enviados ao atlas em `RenderFrameEvent.Pre`, antes do desenho; a compilação não encerra a validação visual em jogo de P12.

**Verificado:** Restock nativo usa `ContentsFilterLogic`; seu wrapper expõe `restockFromHandler(IItemHandler)` e `onHandlerInteract`. Essas APIs recebem inventário real, não um snapshot de rede. A base atual só insere na rede e o snapshot existente guarda identidades, não quantidades.

**Proposto:** consulta com contagens, `ItemExtraction`, autorização de extração, motor de rede → inventário e guard externo de saída. Restock é a primeira função consumidora desses contratos, definidos no [plano principal](../UPGRADES.md).

## 4. Implementação

- Item/wrapper: usar base universal, limite/conflitos Restock e `ContentsFilterLogic` com configuração avançada nativa.
- Container/aba: botão Abastecer, controles nativos, vínculo e feedback de quantidade realmente recebida.
- Serviço: consultar candidatos filtrados em ordem estável de identidade, planejar capacidade por candidato e processar uma oferta de cada vez.
- Destino: usar o caminho nativo de inserção da mochila, respeitando limites, memória e filtros; não escrever diretamente em slots.
- Adaptadores: AE2/RS simulam extração nativa; Tom’s estima pela consulta e executa `pullStack`. Normalizar resultados reais e preservar componentes.
- Persistência: somente vínculo/configuração; recuperação normal é síncrona. Não representar `SnapshotItemHandler` como fonte gravável.

## 5. Fluxo operacional

1. Validar ação pela GUI e conectar com consulta/extração.
2. Abrir guard de saída da própria mochila antes de consultar recursos usados pela transferência.
3. Capturar referências de conteúdo inicial e lista de candidatos; ignorar quantidade não positiva.
4. Calcular espaço aceito pelo destino e pedir somente essa quantidade à rede.
5. Inserir o stack realmente extraído, sem presumir o valor simulado.
6. Devolver excedente conforme recuperação compartilhada; se necessário, usar inserção na rede com autorização independente.
7. Contabilizar recebimento real, liberar guard e atualizar feedback.

## 6. Falhas e ciclo de vida

Mochila cheia não extrai. Mudança de disponibilidade pode produzir extração menor ou vazia. Negação de inserção na rede não deve impedir Restock se a extração for permitida; a recuperação usa caminhos locais quando não puder devolver à rede.

Guard impede que o conteúdo da mochila seja retirado pela própria rede e anunciado como abastecimento. Um handler que já tenha agregado caches deve continuar respeitando o bloqueio no commit. Não iniciar outra oferta após uma falha cuja quantidade comprometida seja desconhecida.

## 7. Etapas de entrega

- [ ] Implementar contratos de extração/consulta e permissões; gate: os três adaptadores retornam quantidades reais corretas.
- [ ] Implementar guard de saída e recuperação; gate: própria mochila exposta à rede não gera movimento fictício.
- [ ] Registrar item, filtros e GUI; gate: botão funciona imediatamente após instalar.
- [ ] Completar regressão; gate: capacidade, componentes e dados persistidos aprovados.

## 8. Validação e aceitação

Testes: modos de filtro, conteúdo inicial, memória, stacks não empilháveis, componentes, destino cheio, simulação superestimada, extração parcial, mudança do destino e fallback de recuperação.

Em jogo: rede → mochila nos três sistemas, permissão de extração sem inserção, instalação/remoção na GUI, mochila colocada, rede compartilhando a própria mochila e reinício. **Aceito quando** somente itens elegíveis entram, a extração é limitada à capacidade e nenhuma sobra é descartada.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Stored content — correção de 2026-10-30

P03/P05/P06/P08/P10/P11: Pickup e Magnet avaliam conteúdo separadamente por destino: a rede usa identidades presentes no backend; a mochila conserva o filtro nativo, incluindo memória. Prioridade oferece somente a sobra ao destino seguinte, que precisa aprovar seu próprio filtro. A consulta de presença em StorageTarget não exige extração nem usa simulação de extração; comparações reutilizam stackMatchesFilter e só restringem por tipo no modo ITEM. As consultas são transitórias, sem cache entre operações.

Restock inicializa INVENTORY apenas quando não há configuração salva; reconstrução não sobrescreve ALLOW/BLOCK. Containers de Pickup/Magnet consultam o wrapper atual após sincronização. Deposit continua comparando com conteúdo da rede; Restock usa o snapshot inicial da mochila. No Forge, a consulta do Deposit passa a ocorrer dentro do guard.

A ausência de transferência manual relatada em Tom’s/SCC ainda exige reprodução com rede real. Testes isolados de filtro não comprovam descoberta da topologia, sincronização da GUI ou permissões em mundo. Validar os três modos, reabertura/reinstalação/reinício, conteúdo somente em cada destino, memória, componentes, ambas as prioridades e redes cheias/offline. Gates de jogo de P12 permanecem abertos; publicação no portal continua adiada conforme o plano SCC.

## Target NeoForge 26.1.2

O porte desta função preserva seu comportamento e as premissas P01–P12 aplicáveis. A implementação específica e os testes ficam no target 26.1.2; versões antigas conservam suas próprias fontes. APIs fixadas, transferência transacional, matriz por backend e evidências são registradas no [plano de porte](../PORT_26_1_2.md). Implementação, testes automatizados e aceitação manual são estados distintos.

O target usa inventário Sophisticated transacional, snapshot de identidades e extração real, preservando a recuperação e os filtros (P03/P05/P08/P10). O GameTest aciona o container nativo e verifica a conservação após extração da rede. A evidência automatizada não encerra a aceitação manual P12.

Correção da hotbar no NeoForge 26.1.2 (P07/P12): atualizar o atlas de texturas não invalida a imagem pronta mantida por GuiItemAtlas. LinkedUpgradeGuiAnimationMixin marca ItemStackRenderState como animado somente durante a transição e somente para LinkedModelProperty. Isso faz o cache da GUI redesenhar o canvas a cada frame; ao terminar, o ícone estático volta a ser reutilizado. A correção anterior de upload, isoladamente, não resolveu a hotbar, conforme reprodução do usuário. Validação visual desta correção permanece pendente.

## Vínculo AE2 por dispositivos da rede

Esta função usa a descoberta compartilhada de [P02](../UPGRADES.md#P02) nos três targets: controllers, terminais e outros dispositivos online da rede energizada, exceto cabos. Em terminais multipart, o clique deve atingir a peça do terminal. O endpoint salvo continua sendo um Wireless Access Point elegível da mesma rede; alcance, transferências, configurações e reservas permanecem conforme P03/P08/P09/P11 aplicáveis. A aceitação manual dessa interação segue P12.

## Vínculo RS/SCC por dispositivos da rede

A descoberta compartilhada de [P02](../UPGRADES.md#P02) também aceita dispositivos ativos do RS nos três targets e dispositivos da rede SCC online nos dois targets NeoForge. Cabos são recusados, inclusive o cabo central de hosts SCC multipart. O endpoint salvo continua sendo controller no RS ou antena elegível no SCC; esta função mantém suas regras operacionais, filtros e reservas. Aplicar P03/P04/P09/P11 e validar a interação em jogo conforme P12.
