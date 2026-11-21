# Integrated Dynamics — porte NeoForge 26.1.2

Pedido explícito de 2026-11-21: antecipar o porte da fase 10 e entregar implementação, testes automatizados, verify, client iniciado e task VS Code com os módulos Integrated. O usuário dispensou testes de interação com Java nesta rodada. Não declara aceite visual ou multiplayer (P12).

## Contratos implementados

- Minecraft 26.1.2, NeoForge 26.1.2.124 e Java 25; seis dependências oficiais fixadas em [integrated-dependencies.lock.json](../../targets/26.1.2/integrated-dependencies.lock.json).
- Adaptadores isolados de itens/fluidos, descoberta multipart e acesso por Omni, sem terminal portátil ou raio adicional; construção somente com Dynamics + Tunnels. Revalidação por chamada, sem carregar chunks ou trocar endpoint (P02/P03/P04/P11).
- Wildcard nativo -1, filtros, prioridades, interceptadores, componentes e quantidades long. Simulação abre transação abortável; execução confirma antes de devolver o resultado. Não há transferência remota durante a transação local abortável do Pickup (P05/P08/P09/P10).
- Durante a revalidação do Pickup com transação aberta, a energia usa o mesmo canal nativo em uma transação filha abortável. O helper booleano de EnergyNetwork.canUpdate abre outra raiz e falha nessa API; os demais listeners permanecem nativos. Sem transação externa, o listener original é usado.
- Os nove serviços existentes recebem o backend pelos registros; não foram alterados contratos comuns, IDs, dados persistidos, prioridade ou recuperação. Crafting usa estoque existente, sem autocrafting.
- Índices visuais 1–4 preservados, Integrated = 5 e canvas = 6. Sprites ciano Menril, marca superior, nove modelos e todas as transições importados do piloto; exportador e verificadores aceitam 26.1.2 (P07).
- Perfil all inclui os seis módulos Integrated e os quatro backends anteriores. Perfis: none, dynamics (sem Tunnels), integrated, terminals, full, ae2, rs, toms, scc, all.

## Harness e verificações

GameTests usam cabos, Omni, bateria energizada e interfaces Tunnels de verdade. Exercitam os nove upgrades por menu/callback/evento, ambas as prioridades, mochilas colocadas nos fluxos manuais, contratos de acesso, filtros, canais, componentes, quantidades acima de int, simulação, sobras, interceptadores e capabilities em cache. As baterias recebem energia pela API nativa; consumo é habilitado no harness, sem alterar configuração de produção.

O source set junitSupport cria mundos para o servidor efêmero oficial antes de ServerStarted; Cyclops usa SavedData do Overworld nesse evento. Esse suporte só carrega no JUnit e fica fora do JAR de produção. O encerramento solicita halt e aguarda a thread do servidor antes de o FML fechar o classloader, evitando corrida com ticks e exclusão duplicada do mundo efêmero. GameTests e client usam o ciclo normal do jogo.

Cada execução remove relatórios anteriores e exige os nomes dos cenários, unicidade, ausência de falhas/errors/skips e JUnit não vazio. Perfil Integrated exige 30 GameTests do addon; all exige 110, além de seis cenários em cada processo de reinício. Os verificadores rejeitam dependências, GameTests e suporte JUnit empacotados.

## Comandos reproduzíveis

Na raiz:

```powershell
.\gradlew.bat :common:check check-neoforge-26.1.2 verify-neoforge-26.1.2
```

Em targets/26.1.2:

```powershell
.\gradlew.bat :neoforge:check -PbackendProfile=integrated
.\gradlew.bat :neoforge:check -PbackendProfile=all
.\gradlew.bat :neoforge:runClient -PbackendProfile=all
```

Task VS Code: **Digital Storages Expansion: Run NeoForge 26.1.2 Client (Integrated)**. Usa all, incluindo Dynamics, Tunnels, Terminals, Crafting, Cyclops Core e Common Capabilities. O seletor geral também oferece os perfis Integrated. Clientes isolados usam diretórios por perfil; all conserva o diretório de desenvolvimento existente.

Relatórios e logs consolidados: build/integrated26; JUnit: targets/26.1.2/neoforge/build/test-results/<perfil>; GameTests: build/reports/integration/<perfil>; reinício: build/reports/restart/<fase> no módulo.

## Auditoria das dependências

As versões e URLs são do manifesto. Os JARs Modrinth fixados têm hashes próprios, distintos dos arquivos CurseForge auditados na rodada de planejamento; nenhum arquivo foi modificado. Cyclops Core 1.31.5 e Common Capabilities 2.11.8 atendem às dependências dos quatro módulos planejados. O bloco compat de Tunnels declara Minecraft [26.1.1,] e NeoForge [26.1.1.1-beta,), intervalos com limite inferior inclusivo que aceitam 26.1.2. Não é uma dependência exata de 26.1.1.

Referências: [plano canônico](INTEGRATED_DYNAMICS.md), [porte do target](../PORT_26_1_2.md), [P01–P12](../UPGRADES.md) e planos individuais dos nove upgrades.

## Incidente no mundo de desenvolvimento

Na primeira inicialização, o perfil full foi usado no diretório client existente. Esse mundo continha AE2/RS/Tom’s/SCC, ausentes em full; o jogo substituiu os blocos desconhecidos ao carregar os chunks (-1,0) e (-1,-1), seção Y=-4. Os arquivos de região foram gravados durante essa sessão. O client foi reiniciado em all e os perfis isolados passaram a usar diretórios próprios. Foram preservadas cópias das regiões em build/integrated26, mas não foi encontrado backup nem stream comprimido antigo recuperável nas regiões. Não foi feita reconstrução presumida dos blocos ou de seus inventários. A perda desses blocos de desenvolvimento permanece sem recuperação.

## Correção da baseline de Dynamics para reinício

O restart GameTest preservou Omni e energia, mas expôs um marcador addedToGroup transitório falso após desserializar a parte. A política agora exige que a posição e a face do Omni salvo pertençam ao registro nativo LOADED_GROUPS, autoridade usada pelo terminal portátil. Não aceita outro membro, nem escreve no registro nativo. A baseline também foi atualizada de 1.36.3 para 1.37.1, incorporando a correção upstream #1786/#1785 de reanexação de partes não-tickantes após recarregar chunks, publicada em 1.37.0. O manifesto final e o requisito mínimo usam 1.37.1: [release oficial](https://github.com/CyclopsMC/IntegratedDynamics/releases/tag/26.1.2-1.37.1). Nenhum validador de acesso foi relaxado, e os JARs permanecem oficiais e não modificados.

## Resultado automatizado final — 2026-11-21

- Dez perfis aprovados: none/dynamics/integrated/terminals/full/ae2/rs/toms/scc/all. 844 execuções JUnit (84 por perfil, 86 em scc/all), zero falhas, erros ou skips.
- 298 GameTests obrigatórios somados entre perfis e reinício: 30 em cada perfil Integrated mínimo/Terminals/full; 110 em all; seis em cada JVM de reinício, sem contar minecraft:always_pass.
- A rodada inicial cobriu os dez perfis; após fixar 1.37.1 e a consulta de membership, os cinco perfis com módulos Integrated foram repetidos e aprovados. O build final repetiu também none. Log final: build/integrated26/final-verify.log — BUILD SUCCESSFUL em 7m35s.
- :common:test foi renovado: 26 testes aprovados. verify-forge-1.20.1 e verify-neoforge-1.21.1 passaram com seus builds e regressões; verify-neoforge-26.1.2 passou após os perfis finais.
- Verificador visual e exportador 26.1.2 aprovados: nove upgrades, cinco backends e 270 faixas de 25 quadros; os cinco estados de vínculo e o canvas são conferidos separadamente.
- README e guias de instalação/vínculo/técnicos EN/PT-BR sincronizados; pnpm check e build do portal aprovados. Preview CurseForge não conferido.
- Client final iniciado com all, Dynamics 1.37.1 e os seis módulos Integrated; entrou em New World sem erros de inicialização no log final. Há avisos de texturas de terceiros (compatibilidade Integrated/RS e modelos Tom’s). Nenhuma interação manual com Java foi executada.
- Relatório gerado com contagens e SHA-256 dos três JARs: build/integrated26/validation.json. O incidente no mundo de desenvolvimento descrito acima permanece sem recuperação e não é omitido pelo resultado dos testes.
