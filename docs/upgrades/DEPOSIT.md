# Deposit — plano de implementação

## 1. Objetivo e comportamento

Consolidar a POC de `remote_deposit_upgrade`. O botão **Depositar** envia itens filtrados da mochila aberta à rede vinculada. A ação não inclui inventário do jogador, equipamento ou slots de upgrades. Não há prioridade de storage nem interação de transferência com containers externos.

Manter os nove slots de filtro existentes e os modos nativos de permissão, bloqueio e conteúdo da rede. Quantidade não aceita permanece na origem ou segue recuperação compartilhada.

## 2. Premissas aplicáveis

[P02 — vínculo](../UPGRADES.md#P02), [P03 — infraestrutura](../UPGRADES.md#P03), [P04 — contexto](../UPGRADES.md#P04), [P06 — instalação](../UPGRADES.md#P06), [P08 — transferência](../UPGRADES.md#P08), [P10 — ciclos](../UPGRADES.md#P10) e [P11 — compatibilidade](../UPGRADES.md#P11). Receita e recursos seguem [P07](../UPGRADES.md#P07).

## 3. Estado atual e dependências

**Implementação atual (2026-10-12):** Deposit permanece o upgrade final da POC: filtros/GUI Sophisticated, vínculo universal, conflitos nos dois sentidos e desvínculo pela receita compartilhada. Sua textura 16×16 parte do ícone avançado original, incluindo o padrão de cantos e pontos de borda, e usa a paleta remota do Magnet; o modelo aponta para o asset local. Compilação e inicialização do cliente passaram; operação, aparência em jogo e regressão por backend ainda não foram validadas após a consolidação. Ver [arquitetura atual](../ARCHITECTURE.md).

**Linha visual (2026-10-31, P07):** a textura segue a [linha visual dos upgrades remotos](../design/UPGRADE_VISUALS.md): ícone original preservado, um canto por rede, sprite da rede vinculada e animações de vínculo e de troca de rede. Vale para NeoForge 1.21.1 e Forge 1.20.1. Compila nos dois loaders; a aparência e a animação em jogo no Forge ainda não foram conferidas.

**Existente:** item, wrapper, container, aba, vínculo, `DepositService`, `ItemTransferService`, três adaptadores e guard de entrada. `RemoteDepositUpgradeWrapper` usa `DepositFilterLogic` e `ModCoreDataComponents.FILTER_ATTRIBUTES`. O snapshot do destino é consultado apenas no modo `INVENTORY`.

**Implementado nesta consolidação:** conflito nativo bidirecional com Deposit básico/avançado, resumo compacto de vínculo e receita compartilhada de desvínculo que preserva os filtros. A conexão atual exige inserção no RS e não exige extração; separar requisitos de acesso continua previsto na fundação para os próximos upgrades. Não substituir o motor seguro por `InventoryHelper.transfer`: a auditoria atual identificou descarte de sobra real nessa rotina. Ver [arquitetura](../ARCHITECTURE.md).

## 4. Implementação

- Item: preservar ID, `UpgradeType`, limite de um e componentes existentes; aplicar conflito bidirecional com equivalentes nativos.
- Wrapper: manter filtros e salvamento atual; não migrar vínculo ou atributos sem necessidade.
- Serviço: solicitar consulta quando necessária e inserção; não exigir extração da rede. Usar snapshot de identidades para comparação com destino.
- Container: preservar validação por identidade do container/wrapper e `getOpenContainer()`, incluindo instalação com GUI aberta.
- Aba: manter Depositar e filtros; resumo compacto ao lado do botão, endereço no tooltip. Remover Desvincular e sua ação no servidor; usar a receita de P02. Feedback de vínculo: somente “Vinculado”. Apresentar falhas de acesso específicas.
- Adaptadores: adaptar à nova autorização sem alterar regras de alcance. Não há Mixin específico desta função.

## 5. Fluxo operacional

1. Validar menu, aba aberta, wrapper instalado e habilitado.
2. Ler endpoint no servidor e conectar com requisitos de inserção.
3. Capturar conteúdo do destino somente quando o filtro exige isso.
4. Abrir guard de entrada da mochila, capturar origem inicial e processar slots elegíveis.
5. Simular, extrair quantidade limitada e inserir; contabilizar pelo resto real.
6. Recuperar sobra e liberar guard; informar stacks efetivamente movidas ou ausência de itens elegíveis.

## 6. Falhas e ciclo de vida

Rede cheia não é perda de conexão; produzir resultado sem movimento ou parcial. Endpoint removido, falta de energia, alcance e acesso negado preservam o vínculo. Pedidos antigos não operam em wrappers substituídos. Uma exceção após inserção parcial não autoriza devolver cegamente a oferta inteira.

Não criar reserva duradoura na operação normal de Deposit: recuperação é síncrona. Reabrir a mochila não deve ser pré-requisito para o primeiro depósito após equipar.

## 7. Etapas de entrega

- [ ] Integrar autorização específica de inserção; gate: jogador sem extração ainda consegue depositar.
- [ ] Aplicar conflitos e conferir tag; gate: instalação rejeitada nas duas ordens com equivalentes nativos.
- [ ] Preservar dados e GUI; gate: item antigo mantém vínculo/filtros e funciona imediatamente após equipar.
- [ ] Executar regressão de transferência e redes; gate: todos os cenários abaixo aprovados.

## 8. Validação e aceitação

Cenários adicionais: desvincular nas grades 2×2/3×3, Shift + clique no resultado, filtros e outros componentes preservados, item sem vínculo não gera resultado, ingrediente extra recusa receita e conflitos nas duas ordens. Ainda pendentes de validação em jogo.

Testes: permissão/bloqueio/conteúdo, componentes, simulação zero, capacidade parcial, recuperação, UUID compartilhado, pedidos repetidos/desatualizados e instalação incompatível.

Em jogo: mochila carregada e colocada, GUI já aberta, cada backend, fronteiras sem fio, permissões, rede cheia/offline, reinício com item antigo e mochila exposta à própria rede. **Aceito quando** o comportamento da POC é preservado e a fundação nova não introduz dependência de extração, duplicação ou reabertura da GUI.

[Critérios comuns](../UPGRADES.md#P12) · [Voltar ao catálogo](../UPGRADES.md)

## SCC integration

The NeoForge path includes SCC 1.5.3 through the shared adapters. Follow the function-specific phase and acceptance criteria in [SCC integration](../integrations/SCC.md), together with P02–P12. Implementation is present; network/GUI/runtime acceptance remains pending. Pump remote support is limited to fluids without added components.

## Stored content — correção de 2026-10-30

P03/P05/P06/P08/P10/P11: Pickup e Magnet avaliam conteúdo separadamente por destino: a rede usa identidades presentes no backend; a mochila conserva o filtro nativo, incluindo memória. Prioridade oferece somente a sobra ao destino seguinte, que precisa aprovar seu próprio filtro. A consulta de presença em StorageTarget não exige extração nem usa simulação de extração; comparações reutilizam stackMatchesFilter e só restringem por tipo no modo ITEM. As consultas são transitórias, sem cache entre operações.

Restock inicializa INVENTORY apenas quando não há configuração salva; reconstrução não sobrescreve ALLOW/BLOCK. Containers de Pickup/Magnet consultam o wrapper atual após sincronização. Deposit continua comparando com conteúdo da rede; Restock usa o snapshot inicial da mochila. No Forge, a consulta do Deposit passa a ocorrer dentro do guard.

A ausência de transferência manual relatada em Tom’s/SCC ainda exige reprodução com rede real. Testes isolados de filtro não comprovam descoberta da topologia, sincronização da GUI ou permissões em mundo. Validar os três modos, reabertura/reinstalação/reinício, conteúdo somente em cada destino, memória, componentes, ambas as prioridades e redes cheias/offline. Gates de jogo de P12 permanecem abertos; publicação no portal continua adiada conforme o plano SCC.
