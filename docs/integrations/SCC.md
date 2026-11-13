# SCC 1.5.3 integration

## Artifact and scope

Historical baseline below: Minecraft 1.21.1 / NeoForge 21.1.236 / Java 21. The additional 26.1.2 target is documented in its section at the end. Backend ID: `scc`.
Audited input: `scc-1.5.3.jar`, SHA-256
`e81ed8d3fdab7ce9bc35efc58036b63dcb6baefd07613156599dd638f8e84d3c`.
Discovery used JAR metadata, javap bytecode and Vineflower output. Decompiled dependency code is not distributed.
The ignored `libs/scc-1.5.3.jar` is now required to compile NeoForge; `verifySccJar` checks its hash.
The standard NeoForge client/server runs include SCC automatically; no `-PwithScc` flag is required.
Use `-PsccTests` for native contract tests.
The default tests retain the profile without optional storage backends. Forge does not load SCC.

## Shared architecture — phase 0

`SccStorageAdapter` and `SccFluidStorageAdapter` are registered lazily only when SCC is installed.
`SccAccessPolicy` resolves the current loaded Network Antenna and calls `SccNetwork.at` on every access.
Targets retain endpoint and SourceLocation, never a cached NetworkView or player.
Power/controller availability comes from `NetworkView.online`; connected alone is insufficient.
SccSecurity.may checks PUT for insertion and TAKE for extraction. Binding accepts either right through
connectForBinding/canBind; other backends retain their existing insertion-based binding policy.
There is no additional terminal item requirement and no additional energy charge.

SCC identification precedes AE2: the audited `Ae2AntennaBlockEntity` extends the SCC antenna
and implements AE2's `IWirelessAccessPoint`. Matching AE2 first saves the wrong backend.
New links to this block therefore save `scc`, displayed as `SCC` in both languages.
Previously saved `ae2` links must be manually rebound to the antenna; tooltips do not inspect
the world or silently rewrite existing links, and pending reservations still block rebinding (P02/P09).

Coverage projects `WirelessAccess.inRange` onto the backpack origin: dimensional first, then same-dimension,
then infinite, then squared distance to antenna center <= range squared. Read range from the antenna;
the audited values are 32 + 32 per expansion, at most eight expansions (288). Placed contexts retain P04.
Never call WirelessAccess.antenna: its default overload may load a chunk.

Item discovery uses contents, active AutoCompress.syncText/parse/expand and native find for real variants.
Directed queries accept predicate, optional item and needed amount. Counts are operation-local estimates.
Exact variants use extractExact capped by free(StackKey). Default identities may use SccNetwork.extract,
which calls extractFree and performs native decompression. Simulate first and reject aggregate identity
mismatches in favor of exact physical extraction. Actual extracted identities are checked again by consumers;
recover mismatches without consuming them. No automatic crafting orders are submitted.
Native internal decompression can modify several stores and return/drop excess; never refund a whole
compressed input after a partial operation. Runtime conservation tests must include these side effects.
Insertion returns native remainder, including configured SCC void behavior. Accepted does not mean retained.

Fluids use insertFluid/drainFluid; the latter selects by Fluid, not components. Modified component identities
are rejected remotely. fluidContents discovers types only; simulated plain drain confirms up to 1000 mB.
Existing Pump reservations, routing and guards retain authority. A component fluid may still use the local tank.

## Per-upgrade phases and acceptance

All paths below are wired through existing universal upgrades; none has a temporary feature flag.
Implementation is present; runtime acceptance remains pending for every row.

| Phase | Upgrade / path | Authorization and acceptance |
| --- | --- | --- |
| 1 | DepositService -> insert | PUT only; native filters, inventory snapshot under guard, partial/full network recovery; placed and carried GUI. |
| 2 | RestockService -> query/extract | TAKE; capacity before extraction, exact result revalidation, free reservations and virtual decompression; return requires PUT. |
| 3 | PickupService -> insert | PUT in simulation and execution; original pickup scope, both priorities, real remainder; no entity mutation during simulation. |
| 4 | RemoteMagnetService -> insert | PUT; native attraction/blockers/cooldowns, eligible nearby player for placed backpack, coverage at backpack, partial drops. |
| 5 | RefillService -> UpgradeItemSources | TAKE; bounded directed requests, deficit/ANY/hands/toolbar/pick-block; component mismatch recovery. |
| 6 | FeedingService -> UpgradeItemSources | TAKE for one unit, actual eligibility check, persisted reservation, native use; independent PUT for returned containers. |
| 7 | AlchemyService -> UpgradeItemSources | TAKE for one unit per rule; actual condition/definition revalidation, duration, single active use, interruption recovery and PUT for bottles. |
| 8 | PumpService -> fluid adapter | PUT input / TAKE output; 1000 mB, plain fluids, both priorities, hands/world/adjacent capabilities, persistent recovery. |

## Required runtime matrix

- Finite 32/64/288 boundaries, vertical and diagonal distances; infinite same dimension and dimensional access.
- Backpack and authorizing player on opposite sides of coverage; placed contexts obey P04.
- Removed/replaced antenna, unloaded chunks, topology/power changes, controller/power config and permission revocation.
- PUT-only, TAKE-only, owner and non-operator players; SCC security disabled/enabled.
- Reserved items, ignored name/damage components, compressed chains, four/nine recipes, insufficient excess capacity.
- No SCC, SCC alone and coexistence with AE2/RS/Tom's; dedicated server and single player.
- Backpack and tank exposed to their own network, including already cached capabilities.
- All eight GUI/gameplay paths, partial results, removal/restart and no duplicate consumption.

Do not mark runtime acceptance or release readiness complete from compilation or unit tests.
Public bilingual mod-docs guides are deferred until functional validation, as approved in the integration plan;
this discovery/implementation plan must not be published in the portal.

## Verification recorded on 2026-10-30

- Full common checks, NeoForge build/tests and Forge build passed. Forge ran all nine existing required GameTests successfully.
- Default NeoForge suite passed with 54 tests, including three SCC coverage regressions; optional backends remain absent in that profile.
- `:neoforge:test :neoforge:build -PsccTests` passed with five tests and SCC 1.5.3 loaded by NeoForge. Tests cover finite/infinite/dimensional coverage and the native fluid store's component-sensitive drain and simulation. This profile requires a 2 GB test heap.
- JAR inspection confirms adapter classes are packaged and dependency SCC classes are not embedded. Translation JSON parses and git diff whitespace checks pass.
- These tests do not mount a real SCC network or exercise GUI/gameplay, crafting reservations, compressed conservation, permission revocation or multiplayer. All corresponding runtime acceptance gates above remain open. No release readiness is claimed; public portal documentation remains deferred.

## SCC 1.6.5 — target 26.1.2

O adaptador específico de 26.1.2 usa o JAR 1.6.5 fornecido, SHA-256 `6fb0be9547c9aa8ea01adaa91fa846a43ca14256a8a5716bec87cbc3a6b65eb0`. A licença acompanha o artefato em `libs/26.1.2`. Os nove upgrades entram na matriz SCC do [porte](../PORT_26_1_2.md), incluindo Crafting. Permanecem os contratos de segurança, alcance na origem, reservas/descompressão nativas e rejeição de fluidos com componentes adicionais. Os registros anteriores desta página descrevem especificamente a baseline 1.21.1/SCC 1.5.3.

In 26.1.2, native security is tested both with its default disabled setting and enabled PUT-only/TAKE-only policies. Fluid QUERY follows TAKE; INSERT follows PUT. Native compression is configured before the network tick cache is initialized. Real fixtures verify nine-unit and four-unit decompression, including returned surplus and simulation without writes. Test-only recipes stay outside the distribution JAR.

## Device-based linking

P02/P03/P04/P09/P11: on both NeoForge targets, online network devices can discover an eligible Network Antenna belonging to the same native network. Cables are excluded; multipart hosts require an actual device hit. The saved endpoint remains the antenna. Existing coverage, native permissions, recovery reservations and operation-time resolution remain unchanged. Forge has no SCC target. See the shared linking contract in [P02](../UPGRADES.md#P02).
