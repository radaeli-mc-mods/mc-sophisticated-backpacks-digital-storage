# Integrated Dynamics — fase 1: testes, acesso e vínculo

Implementação no piloto Minecraft 1.21.1 / NeoForge 21.1.238 / Java 21, conforme o [plano aprovado](INTEGRATED_DYNAMICS.md). Premissas: [P01–P04](../UPGRADES.md#P01), [P11](../UPGRADES.md#P11), [P12](../UPGRADES.md#P12). A arquitetura descreve o [contrato implementado](../ARCHITECTURE.md#integrated-dynamics--fundação-do-piloto).

**Fase 1 concluída no piloto**, com gate automatizado aprovado. Operações de recursos e apresentação continuam para a fase 2; aceite manual permanece separado (P12).

## Escopo implementado

- Registro opcional condicionado a Dynamics **e** Tunnels; Dynamics sozinho mantém o adaptador ausente.
- Descoberta de hosts multipart com partes, sem busca por proximidade. Cabo puro não qualifica. Preferência pelo Omni do bloco clicado; candidatos da mesma rede ordenados por dimensão, X, Y, Z e face. Um Omni diretamente clicado mas inelegível não encaminha o vínculo para outro bloco.
- Revalidação de mundo/chunk carregado, rede atual inicializada e não encerrada/crashed, host atual, estado e grupo Omni, validadores nativos de energia, jogador elegível e proteção do endpoint. Não exige terminal portátil, não cria raio e não carrega chunks. Operação permanece ancorada na posição salva.
- Vínculo pelo evento compartilhado `EndpointLinkHandler`, com backend `integrateddynamics`, dimensão e posição no `EndpointBinding` existente. Tentativa inválida preserva o vínculo anterior; dados continuam legíveis sem dependências opcionais.
- Fundação de `StorageTarget` deliberadamente fechada para recursos: não consulta nem transfere itens/fluidos. Operações e apresentação pertencem à fase 2. Nenhuma migração de dados, novo item, textura ou tradução nesta fase.
- Classpaths de desenvolvimento e testes por perfil, harness nativo em source set separado, CI e tarefas VS Code para escolher perfil em check/cliente/servidor. JARs Cyclops permanecem externos e fixados pelo manifesto da fase 0; sua verificação precede a compilação.

## Perfis e reprodução

A partir de `targets/1.21.1`, com Java 21:

```powershell
.\gradlew.bat :neoforge:check -PbackendProfile=none --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=dynamics --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=integrated --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=terminals --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=full --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=ae2 --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=rs --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=toms --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=scc --console=plain
.\gradlew.bat :neoforge:check -PbackendProfile=all --console=plain
```

`none` carrega Sophisticated sem backends; `dynamics` inclui Cyclops Core, Common Capabilities e Dynamics sem Tunnels; `integrated` acrescenta Tunnels; `terminals` acrescenta Terminals; `full` inclui Crafting. Os quatro perfis existentes carregam cada backend isolado; `all` carrega as quatro redes anteriores e a pilha Integrated completa. JEI continua no runtime de desenvolvimento existente.

Sem `backendProfile`, o modo `legacy` preserva os runtimes anteriores: cliente/servidor com quatro backends, JUnit mínimo e sem iniciar o novo GameTest automaticamente. `-PsccTests` continua selecionando o contrato SCC antigo; usar essa propriedade junto com `backendProfile` é recusado para impedir matriz ambígua.

CI acrescenta `none`, `integrated`, `full`, `all`, sem remover as suítes anteriores. VS Code oferece `1.21.1 Check/Client/Server Profile`, com seleção explícita. Cliente/servidor normais não carregam o mod de fixtures.

## Harness e verificadores

O harness usa GameTests nativos 1.21.1, `JUnitLikeTestReporter`, estruturas NBT e um mod `digitalstorage_tests` separado. A fixture Integrated só é registrada quando Dynamics e Tunnels estão carregados. A adaptação não copia as APIs de registro de testes do 26.1.2.

O NeoForge 1.21.1 carrega holders anotados antes de aplicar o filtro de namespaces. O JAR Dynamics inclui um holder de compatibilidade RS que referencia tipos de RS mesmo no perfil mínimo. `GameTestNamespaceMixin`, exclusivo do source set de testes, restringe esses holders ao namespace do harness antes do carregamento. Não altera o mod de produção nem as suítes existentes de outros targets. JUnit carrega somente o mod principal, sem o mod de fixtures.

`runGameTestServer` descarta XML/summary anteriores e exige exatamente os nomes obrigatórios, sem duplicatas, falhas, erros ou skips. JUnit recusa execução vazia e skips. Relatórios ficam em `neoforge/build/reports/integration/<perfil>/` e `build/test-results/<perfil>/`. Logs desta implementação estão em `build/integrated-audit/phase1-<perfil>.log` na raiz, ignorados pelo Git.

`verifyIntegratedJar`, integrado a `check`, exige as três classes novas e rejeita APIs Cyclops e fixtures no artefato. O verificador geral de produção continua conferindo nove upgrades, recursos, bytecode e metadados.

As invocações legadas também foram reexecutadas: `:neoforge:check` sem perfil passou com 72 JUnit; `:neoforge:test -PsccTests` passou com 5 JUnit. Nenhuma falha, erro ou skip. Logs: `phase1-legacy.log` e `phase1-scc-contract.log` no mesmo diretório de auditoria.

## Cobertura dos cenários nativos

- `profile`: adaptadores esperados e composição Dynamics/Tunnels/Terminals/Crafting por perfil.
- `topology`: redes próximas distintas, leitor funcional que encontra apenas sua própria rede, vários Omni, ordem determinística, preferência pelo bloco clicado e recusa de cabo puro.
- `binding`: evento real de Shift + clique, descoberta pelo leitor, endpoint Omni salvo, serialização/deserialização do item, tentativa inválida preservando o endpoint, acesso sem terminal e vínculo dos nove itens universais. Operação de recursos permanece recusada nesta fase.
- `unavailable`: recusa de espectador real, rede sem energia com multiplicador nativo temporariamente habilitado e restaurado em `finally`, Omni desabilitado, Omni removido mesmo com outro Omni na rede, chunk indisponível e conservação do vínculo anterior. O jogador da fixture respeita os modos reais; o mock antigo do GameTest sobrescreve `isSpectator` e não serve para esse cenário.

## Resultados e limites

A matriz local de `:neoforge:check` passou nos dez perfis, sem falhas, erros ou skips:

| Perfil | JUnit | GameTests do harness |
| --- | ---: | ---: |
| `none` | 72 | 1 |
| `dynamics` | 72 | 1 |
| `integrated` | 72 | 4 |
| `terminals` | 72 | 4 |
| `full` | 72 | 4 |
| `ae2` | 72 | 1 |
| `rs` | 72 | 1 |
| `toms` | 72 | 1 |
| `scc` | 74 | 1 |
| `all` | 74 | 4 |

O verificador geral `node tools/verify-jar.mjs <jar> 1.21.1 neoforge 21 sophisticated_backpacks_digital_storages_expansion 0.6.0` aprovou o artefato. `verifyIntegratedJar` passou em cada check. JSON das tarefas VS Code, links relativos novos/afetados e `git diff --check` foram conferidos.

A execução final dos quatro perfis Integrated repetiu e aprovou o gate nativo e o gate do JAR após fortalecer os cenários de energia e cabo puro conectado. Logs finais: `build/integrated-audit/phase1-native-final-<perfil>.log`.

 Os perfis sem Integrated validam carregamento e regressão JUnit; esta fase não monta redes de transferência AE2/RS/Tom’s/SCC no piloto. Essas redes continuam cobertas pelas suítes preservadas do target 26.1.2.

Aceite manual de GUI, interação física, multiplayer e modlist permanece pendente. O teste de serialização não representa reinício completo com reserva; reservas e operações de recursos serão exercitadas nas fases correspondentes. A consulta aos validadores nativos foi conferida no bytecode fixado; não foi adicionado consumo de energia próprio. A documentação pública EN/PT-BR continua adiada conforme o plano, pois esta fase não oferece operações dos upgrades em Integrated. Nenhum porte, commit, versão, publicação ou instalação em instância existente integra esta entrega.
