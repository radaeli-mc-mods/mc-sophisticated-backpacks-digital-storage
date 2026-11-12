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