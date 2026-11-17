# Integrated Dynamics — fase 0: dependências e baseline

Registro de 2026-11-16, conforme a fase 0 do [plano aprovado](INTEGRATED_DYNAMICS.md). Premissas aplicáveis: [P01](../UPGRADES.md#P01), [P11](../UPGRADES.md#P11) e [P12](../UPGRADES.md#P12).

**Fase 0 concluída.** A pilha do piloto é reproduzível e a baseline existente passou. O backend Integrated Dynamics ainda não está implementado; nenhuma operação ou aceitação em jogo dessa integração foi demonstrada.

## Checkout e escopo

- Base: commit `331026d`, versão do addon `0.6.0`.
- Piloto: Minecraft 1.21.1, NeoForge 21.1.238, Java 21. `common` permanece Java 17 sem Minecraft/Cyclops.
- O plano, a entrada no índice e `libs/to-support` já eram alterações locais no início da tarefa. Os doze JARs fornecidos foram auditados e, antes do commit, removidos a pedido do usuário. Os seis artefatos reproduzíveis do piloto ficam no diretório ignorado `libs/1.21.1/integrated`.
- Alterações desta fase: manifesto, tarefa explícita de download/verificação, regras de exclusão dos downloads e documentação interna. Nenhuma mudança nos upgrades, registros, persistência, assets, perfis, CI ou dependências de runtime.

## Artefatos reproduzíveis do piloto

O [manifesto](../../targets/1.21.1/integrated-dependencies.lock.json) é a referência de nomes de arquivo, URLs imutáveis, SHA-256, versões nativas, licenças e dependências obrigatórias declaradas nos JARs. Todas as seis licenças principais são MIT; os arquivos `LICENSE.txt` foram conferidos nos tags correspondentes. Dynamics preserva também os avisos de terceiros `LICENSE-re2j.txt` e `LICENSE-netty.txt` dentro de seu JAR.

| Artefato | Versão nativa | Origem fixada |
| --- | --- | --- |
| Cyclops Core | 1.30.0, arquivo build 1205 | [Modrinth MHVHsQ3p](https://modrinth.com/mod/cyclops-core/version/MHVHsQ3p) |
| Common Capabilities | 2.11.6 | [Modrinth R9PufPlP](https://modrinth.com/mod/common-capabilities/version/R9PufPlP) |
| Integrated Dynamics | 1.38.2 | [CurseForge 9385193](https://www.curseforge.com/minecraft/mc-mods/integrated-dynamics/files/9385193) |
| Integrated Tunnels | 1.13.0 | [CurseForge 9046753](https://www.curseforge.com/minecraft/mc-mods/integrated-tunnels/files/9046753) |
| Integrated Terminals | 1.11.1 | [CurseForge 9220317](https://www.curseforge.com/minecraft/mc-mods/integrated-terminals/files/9220317) |
| Integrated Crafting | 1.8.1 | [CurseForge 9383656](https://www.curseforge.com/minecraft/mc-mods/integrated-crafting/files/9383656) |

Os quatro arquivos baixados do CurseForge têm SHA-256 idêntico aos quatro JARs 1.21.1 fornecidos. Os arquivos do Modrinth consultados para esses quatro mods têm hashes diferentes; o nome/número de versão sozinho não identifica o binário auditado. O manifesto conserva os releases fornecidos, sem atualizar silenciosamente versões.

Os metadados confirmam Cyclops Core >=1.30.0 para os quatro mods. Terminals exige Common Capabilities >=2.11.6; Crafting exige >=2.11.5. O módulo `integrateddynamicscompat` embutido exige NeoForge >=21.1.238, embora o módulo principal declare >=21.1.228. Tunnels e Terminals também incluem módulos próprios de compatibilidade. Essas declarações não provam inicialização do conjunto.

### Reprodução

A partir de `targets/1.21.1`, com Java 21 disponível ao Gradle:

```powershell
.\gradlew.bat :neoforge:verifyIntegratedDependencies --console=plain
.\gradlew.bat :neoforge:verifyIntegratedDependencies --offline --console=plain
```

`gradle/verify-integrated-dependencies.gradle` registra a tarefa no build piloto. Ela confere a plataforma do manifesto, baixa arquivos ausentes em `libs/1.21.1/integrated`, valida o SHA-256 antes de promover cada `.part` e verifica novamente cada arquivo existente. Arquivos divergentes são recusados; não são substituídos automaticamente. Em modo offline, arquivos ausentes causam erro. JARs e `.part` são ignorados pelo Git. Nenhum JAR Integrated ou Cyclops é versionado ou embutido por esta fase.

Verificações executadas:

- Primeiro download dos seis artefatos pela nova tarefa: aprovado, seis SHA-256 corretos.
- Segunda execução com `--offline`: aprovada, seis arquivos conferidos sem download.
- Caso negativo: uma cópia baixada de Cyclops Core foi alterada temporariamente; a tarefa falhou com `SHA-256 mismatch for cyclopscore`. O conteúdo original foi restaurado em `finally` e seu hash conferido.

Logs locais ignorados: `build/integrated-audit/verify-dependencies.log`, `verify-offline.log` e `verify-mismatch.log`.

## Reconciliação com as APIs e documentos atuais

| Referência | Resultado da conferência |
| --- | --- |
| Plataforma | `targets/1.21.1/gradle.properties` fixa NeoForge 21.1.238; corrigidas as referências atuais a 21.1.236 em arquitetura e multiloader. |
| Builds | A raiz coordena três builds independentes; corrigida a descrição da verificação da arquitetura. O GameTest harness de redes reais está em 26.1.2, não em 1.21.1. |
| Contratos existentes | `StorageAdapter`, `StorageTarget`, `FluidStorageAdapter`, `StorageTarget.canBind` e `StorageAdapterRegistry.connectForBinding` já existem no piloto. Não são contratos a criar do zero. |
| Descoberta | `StorageAdapter.identify(Level, BlockPos)` não recebe face. O host multipart precisa ser inspecionado pelo futuro adaptador. `EndpointLinkHandler` só persiste após validar o target; a representação do vínculo permanece a atual. |
| Backend opcional | Os registros constroem adaptadores nos branches dos mods carregados. Integrated ainda não está registrado. A condição conjunta Dynamics + Tunnels será implementada na fase 1. |
| Operações nativas | `javap` confirmou `IItemNetwork`/`IFluidNetwork`, `IPositionedAddonsNetworkIngredients.getChannel(int)`, `INetworkIngredientsChannel` e `WILDCARD_CHANNEL = -1` nos JARs fixados. |
| Common Capabilities | `IIngredientComponentStorage` expõe iteração direcionada, `insert(T, boolean)`, `extract(T, M, boolean)` e quantidades `long` no matcher. Essas APIs existem; sua aplicação segura nos adaptadores depende das fases 1/2. |
| Escopo da documentação | O plano passa a registrar a fase 0 e o manifesto. Arquitetura e P01 distinguem preparação de dependências de backend implementado. Registros históricos de testes não foram convertidos em resultados atuais. |

A documentação pública EN/PT-BR permanece para a fase de comportamento verificado definida no plano. Este relatório é evidência interna; não anuncia suporte Integrated aos jogadores.

## Baseline anterior ao backend

Comandos executados na raiz, usando os JDKs resolvidos pelos builds:

```powershell
.\gradlew.bat :common:check :check-neoforge-1.21.1 :verify-neoforge-1.21.1 --console=plain
.\gradlew.bat :common:test --rerun-tasks --console=plain
.\gradlew.bat :check-forge-1.20.1 :verify-forge-1.20.1 --console=plain
.\gradlew.bat :check-neoforge-26.1.2 --console=plain
```

| Camada | Resultado observado |
| --- | --- |
| `common` | 26 JUnit aprovados; reexecutados com `--rerun-tasks` após o primeiro check reutilizar resultados. |
| NeoForge 1.21.1 padrão | 72 JUnit aprovados; nenhum erro, falha ou skip. |
| NeoForge 1.21.1 `-PsccTests` | 5 JUnit aprovados em `test-results/scc-contract`; nenhum erro, falha ou skip. |
| Artefato 1.21.1 | Build e verificador de produção aprovados, incluindo nove upgrades, metadados, recursos, bytecode e empacotamento. |
| Forge 1.20.1 | 16 GameTests aprovados; build, recursos, JAR reobfuscado, refmap/MixinExtras e rejeição das sete fixtures inválidas aprovados. O comando coordenado passou por `check` e `build`, repetindo os 16 cenários. |
| NeoForge 26.1.2 | Seis perfis existentes aprovados conforme a tabela abaixo; reinício `prepare`/`verify` aprovado, cinco cenários por etapa. |

| Perfil 26.1.2 | JUnit | GameTests do addon com rede nativa |
| --- | ---: | ---: |
| `none` | 84 | 1 |
| `ae2` | 84 | 21 |
| `rs` | 84 | 21 |
| `toms` | 84 | 21 |
| `scc` | 86 | 21 |
| `all` | 86 | 81 |

Os relatórios JUnit dos seis perfis não apresentam erros, falhas ou skips. O gate existente validou contagens e unicidade dos cenários GameTest, ausência de falhas/skips e gerou novos `summary.json` por perfil. As datas dos relatórios e dos dois XMLs de reinício correspondem a esta execução. A contagem se refere aos cenários do addon; testes dos mods de terceiros não são contabilizados como seus cenários.

Evidências locais:

- `baseline.log`: piloto e verificação do seu JAR.
- `build/integrated-audit/baseline-common.log`, `baseline-forge.log`, `baseline-26.log`.
- `common/build/test-results/test/`.
- `targets/1.21.1/neoforge/build/test-results/{test,scc-contract}/`.
- `targets/26.1.2/neoforge/build/test-results/<perfil>/` e `build/reports/integration/<perfil>/{gametest.xml,summary.json}`.
- `targets/26.1.2/neoforge/build/reports/restart/{prepare,verify}/gametest.xml`.

Não foram identificadas falhas preexistentes nas suítes executadas. Houve avisos preexistentes de depreciação do Gradle, recursos de terminal/contadores do Windows e refmap de desenvolvimento Forge; o verificador do refmap de produção passou. A primeira tentativa foi impedida pelo sandbox ao abrir o lock do cache global Gradle; a execução com acesso autorizado concluiu. Nenhuma dessas ocorrências foi tratada como falha de gameplay.

## Inventário dos JARs fornecidos para os portes

Os quatro hashes 1.21.1 estão no manifesto reproduzível. Os oito abaixo completam o inventário dos doze JARs originalmente fornecidos em `libs/to-support`, removidos após a auditoria a pedido do usuário. São hashes dos arquivos fornecidos, sem alegação de runtime validado ou de origens de download conferidas para os portes nesta fase.

| Arquivo | SHA-256 |
| --- | --- |
| `IntegratedDynamics-1.20.1-1.31.4.jar` | `11de72c8d7efdeb191198610404ba940e01d5a047de3433f5940946274866ba7` |
| `IntegratedTunnels-1.20.1-1.9.0.jar` | `2b9f53a962e4902caf392b6d80f05502eb8813cf4da7ebd5c435389e08e78d76` |
| `IntegratedTerminals-1.20.1-1.7.0.jar` | `9fec43958c4b9fbf9598946d5d6871cf55eb7b00e6639f00b15dcca0bd4b738f` |
| `IntegratedCrafting-1.20.1-1.4.10.jar` | `13f971438a149d3fa4644adb4b8f3d46156bfc64d4ef2e8fd103a2f449eae307` |
| `integrateddynamics-26.1.2-neoforge-1.36.3.jar` | `0a94f0fed16183932f26b01c3fec62b7c6b6efb0be7fb3dbf64cdbd3f4f31f38` |
| `integratedtunnels-26.1.2-neoforge-1.13.2.jar` | `c216a5f05ef805e47c589bbfbb5ec04e7eba411bcdc47136ada7739be572e908` |
| `integratedterminals-26.1.2-neoforge-1.11.1.jar` | `54d770821bcc477646c4d964855fa8e6ec3924b7a35a0a681a0836ab27d3a632` |
| `integratedcrafting-26.1.2-neoforge-1.8.1.jar` | `d174a9ad58bedeb0ccacefdc30881f557ddb03c541f7985393f4f1a975ec663c` |

## Gate e continuidade

- [x] Seis dependências do piloto fixadas com origem, metadados, licença e SHA-256.
- [x] Download reproduzido pela tarefa, revalidação offline e rejeição de divergência demonstrados.
- [x] Baseline existente registrada, sem falhas nas suítes executadas.
- [x] Referências atuais de plataforma, build e contratos reconciliadas.
- [ ] Fase 1: perfis 1.21.1, harness nativo, isolamento opcional, acesso, descoberta multipart, vínculo e persistência Integrated.

Aceitação visual, de gameplay, de multiplayer e de servidor de produção da pilha Integrated continua pendente (P12). Esta fase não executou cliente Integrated, não instalou JARs em instância existente e não mudou versões. Após sua conclusão, o usuário solicitou um commit local e a remoção dos JARs de referência; tag, push e publicação permanecem fora do escopo.
