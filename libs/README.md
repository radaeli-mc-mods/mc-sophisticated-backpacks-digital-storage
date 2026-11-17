# Dependências auditadas

Cada versão tem seu próprio build e dependências fixadas. Nenhum mod de storage, Sophisticated Core/Backpacks ou GuideME é incorporado ao JAR do addon.

- Forge 1.20.1: coordenadas imutáveis em `targets/1.20.1/forge/build.gradle`.
- NeoForge 1.21.1: coordenadas em `targets/1.21.1/neoforge/build.gradle`, mais SCC 1.5.3 versionado e validado por SHA-256.
- NeoForge 26.1.2: arquivos, URLs imutáveis e SHA-256 em `targets/26.1.2/dependencies.lock.json`. Core 1.5.34.2469, Backpacks 3.26.20.2250, AE2 26.1.13-beta, RS 3.2.1, Tom's 2.10.3, SCC 1.6.5 e GuideME 26.1.14-beta.

## Target 26.1.2

Os arquivos ficam em `libs/26.1.2`. O build verifica hashes antes de compilar e baixa arquivos ausentes das origens fixadas. Também é possível copiar os JARs auditados para esse diretório; um arquivo diferente será rejeitado. Downloads não é um diretório de dependências do projeto.

SCC 1.6.5 é versionado com sua licença MIT em `26.1.2/SCC-LICENSE`, conforme o modelo de SCC 1.5.3. Os demais JARs são ignorados pelo Git e obtidos das origens do manifesto.

Perfis de runtime/testes: `none` (padrão), `ae2`, `rs`, `toms`, `scc`, `all`. Core/Backpacks são obrigatórios; GuideME é instalado com AE2. Todos os backends estão disponíveis para compilar, mas somente os selecionados entram no runtime.

A auditoria usa JARs exatos, `javap` e fontes geradas localmente. Código de terceiros decompilado permanece em diretórios ignorados de build e não é distribuído.

Veja [multiloader](../docs/MULTILOADER.md) e [porte 26.1.2](../docs/PORT_26_1_2.md).

## Piloto Integrated Dynamics — fase 0 (1.21.1)

O manifesto [integrated-dependencies.lock.json](../targets/1.21.1/integrated-dependencies.lock.json) fixa os seis artefatos: Cyclops Core 1.30.0 (build 1205), Common Capabilities 2.11.6, Integrated Dynamics 1.38.2, Tunnels 1.13.0, Terminals 1.11.1 e Crafting 1.8.1. Registra URLs imutáveis, SHA-256, licença MIT e sua fonte no tag correspondente, dependências obrigatórias dos metadados e módulos de compatibilidade embutidos. O módulo embutido de Dynamics exige NeoForge 21.1.238, a versão do piloto.

A partir de `targets/1.21.1`:

```powershell
.\gradlew.bat :neoforge:verifyIntegratedDependencies --console=plain
.\gradlew.bat :neoforge:verifyIntegratedDependencies --offline --console=plain
```

A tarefa baixa arquivos ausentes para `libs/1.21.1/integrated`, verifica o SHA-256 antes de promover o download e rejeita arquivos existentes divergentes. Em modo offline, um arquivo ausente causa falha. Os JARs e downloads parciais desse diretório são ignorados pelo Git. Também é possível copiar os arquivos exatos para ele antes da verificação.

Esta tarefa é explícita: a fase 0 prepara dependências e não as adiciona à compilação, aos testes, ao runtime ou ao JAR do addon. Perfis e ativação opcional pertencem à fase 1 do [plano](../docs/integrations/INTEGRATED_DYNAMICS.md). Os JARs de referência fornecidos em `libs/to-support` foram removidos após a auditoria, a pedido do usuário; seus hashes permanecem registrados no relatório. O build não depende desse diretório.

Os quatro releases Integrated foram baixados do CurseForge e comparados aos JARs fornecidos. O Modrinth oferece arquivos diferentes, inclusive com o mesmo número de versão; não substituir a origem mantendo o hash anterior. As bibliotecas faltantes foram obtidas por IDs de versão fixos do Modrinth. Evidência e baseline: [fase 0](../docs/integrations/INTEGRATED_DYNAMICS_PHASE_0.md).
