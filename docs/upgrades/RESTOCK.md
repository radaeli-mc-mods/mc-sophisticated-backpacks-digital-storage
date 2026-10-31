# Restock — plano de implementação

## 1. Objetivo e comportamento

Adicionar `remote_restock_upgrade`. O botão **Abastecer** busca itens filtrados na rede vinculada e preenche a mochila até sua capacidade. Não modifica diretamente inventário do jogador e não define metas por item. Não oferece prioridade nem transferência por clique em containers externos.

Usar filtros avançados de permissão, bloqueio e conteúdo da mochila. O modo por conteúdo considera identidades presentes no início do pedido; não expande a seleção com itens recém-inseridos.

## 2. Premissas aplicáveis

[P02 — vínculo](../UPGRADES.md#P02), [P04 — GUI](../UPGRADES.md#P04), [P06 — instalação](../UPGRADES.md#P06), [P08 — recuperação](../UPGRADES.md#P08), [P10 — guard de saída](../UPGRADES.md#P10) e [P11 — desempenho](../UPGRADES.md#P11). Aplicar [P03](../UPGRADES.md#P03) e [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-12):** serviço de extração, limites de capacidade, recuperação, prioridade fixa rede→mochila, aba manual e receita foram registrados. A UI/serviço reaproveitam `DepositFilterLogic` em modo `INVENTORY` para snapshot do conteúdo, em vez de `ContentsFilterLogic` da especificação inicial. Sua textura 16×16 parte do ícone avançado original, incluindo o padrão de cantos e pontos de borda, e usa a paleta remota do Magnet; o modelo aponta para o asset local. Build e início do cliente passaram; aparência em jogo, filtros e transferências continuam sem prova manual.

**Linha visual (2026-10-31, P07):** no NeoForge 1.21.1, a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. O Forge mantém a textura anterior. Compila; a aparência e a animação em jogo ainda não foram conferidas.

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
