# Integrated Dynamics — porte Forge 1.20.1 (fase 10)

Registro do porte solicitado em 2026-11-21, antes do aceite manual do piloto. O escopo é Forge 47.4.10 / Java 17; o porte 26.1.2 foi implementado posteriormente na mesma data, conforme [registro próprio](INTEGRATED_DYNAMICS_26_1_2.md). Este registro complementa o [plano canônico](INTEGRATED_DYNAMICS.md), as premissas [P01–P12](../UPGRADES.md) e os planos individuais dos nove upgrades, indexados em [UPGRADES.md](../UPGRADES.md).

## Implementação

- Criados adaptadores Forge de itens e fluidos, política de acesso e descoberta de Omni no pacote do target 1.20.1.
- Registros constroem os adaptadores somente quando Integrated Dynamics **e** Integrated Tunnels estão carregados. A ausência de qualquer um mantém todas as classes Cyclops isoladas.
- O vínculo usa `connectForBinding`/`canBind` no target Forge: exige Omni válido, mas não exige que uma interface de itens já esteja conectada. As operações continuam verificando sua capacidade nativa antes de acessar o canal.
- Descoberta preserva o Omni clicado, o desempate determinístico entre partes conectadas e a recusa de cabo puro. Antes de autorizar, relê o host multipart, o endpoint carregado, a rede atual, o estado configurado do Omni, os listeners nativos e a permissão do jogador (P02/P03/P04/P10/P11).
- Consultas, presença, inserção e extração usam o canal wildcard posicionado do Integrated Dynamics; não leem inventários físicos. Itens preservam componentes e quantidades `long`; fluidos preservam identidade e mB. Filtros e prioridades continuam no caminho nativo; não há fallback para outro Omni ou cache de rede persistido (P05/P08/P09/P11).
- Os nove upgrades recebem o quinto estado visual e os recursos de transição usados pelo piloto. Os estados e índices anteriores permanecem iguais. A receita cosmética da mochila Integrated continua excluída do target (P07).
- Os contratos comuns e o formato do endpoint (`backend + dimensão + posição`) não mudaram. Não foram criados itens, receitas ou dados de vínculo adicionais.

## Adaptações de API Forge

Os adaptadores usam `LazyOptional`, `IItemHandler` e `net.minecraftforge.fluids.FluidStack` das versões fixadas em 1.20.1. No contrato Forge de `StorageTarget`, a origem é verificada na abertura da conexão; os métodos do alvo não recebem `SourceLocation`. Cada chamada posterior ainda resolve o jogador, o mundo e a rede atual e usa o canal nativo com seus filtros.

O `INetwork` dos JARs Forge 1.20.1 não expõe `isValid(element)`. A descoberta revalida a identidade da rede obtida no endpoint, a associação atual da peça ao host e a presença do elemento na rede viva, além do estado Omni e dos listeners nativos de energia. O porte não importa a chamada disponível em versões Cyclops mais novas.

## Dependências e perfis

O manifesto [integrated-dependencies.lock.json](../../targets/1.20.1/integrated-dependencies.lock.json) fixa Forge 47.4.10 / Java 17, Cyclops Core 1.22.5, Common Capabilities 2.9.11, Integrated Dynamics 1.31.4, Integrated Tunnels 1.9.0, Integrated Terminals 1.7.0 e Integrated Crafting 1.4.10, com origem, licença e SHA-256. `verifyIntegratedDependencies` verifica os seis JARs antes da compilação. Os JARs locais auditados fornecem as APIs para `compileOnly`; no client, `modRuntimeOnly` resolve os mesmos arquivos CurseForge e o ModDevGradle os remapeia de SRG para nomes oficiais. Nenhum deles entra no JAR do addon.

O `:forge:runClient` usa por padrão `legacy-integrated`: mantém AE2/RS/Tom's do perfil anterior e carrega os seis módulos Integrated, incluindo Integrated Terminals e Integrated Crafting. Use `-PbackendProfile=legacy` para iniciar sem Integrated, `integrated` para a pilha mínima, `terminals` para acrescentar Integrated Terminals ou `full` para carregar também Integrated Crafting. Os perfis não selecionados não entram no runtime.

## Evidências e limites

- `:forge:compileJava --offline` — passou no target `targets/1.20.1`.
- `:forge:verifyForgeResources` — passou; os modelos e animações Integrated chegaram aos recursos preparados, e a receita cosmética da mochila segue ausente.
- `verifyIntegratedDependencies` — validou os seis hashes do manifesto.
- `:neoforge:compileJava --offline` — passou no target piloto após a generalização do verificador compartilhado, com os seis hashes 1.21.1 válidos.
- `:forge:runClient -PbackendProfile=legacy-integrated` — iniciou com Forge 47.4.10 e os seis módulos Integrated; o log registrou a inicialização de Integrated Dynamics e as configurações de Integrated Terminals/Crafting, sem o crash de símbolos SRG do boot anterior. A execução fechou normalmente após a saída do mundo de teste; o perfil padrão contém agora os seis módulos.
- Nenhuma suíte ou teste foi adicionado ou executado. Não houve execução de `build`, `check`, GameTests ou JAR de produção.
- O boot do client comprova carregamento dos módulos, não os nove fluxos de upgrade, filtros nativos de item/fluidos, recuperação, multiplayer, animações ou gates do JAR Forge. A aceitação manual por plataforma e por upgrade permanece pendente conforme P12.

