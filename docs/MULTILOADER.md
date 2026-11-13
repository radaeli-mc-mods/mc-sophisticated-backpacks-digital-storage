# Base multiloader

## Estrutura e versões

A raiz coordena builds independentes por Minecraft, seguindo vertical-trapdoors. `common` usa Java 17 sem Minecraft; suas fontes são incorporadas em cada target. As APIs de Minecraft e dos backends são compiladas separadamente.

| Target | Loader | Java | Gradle |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.10 | 17 | 8.11.1 |
| 1.21.1 | NeoForge 21.1.236 | 21 | 8.11.1 |
| 26.1.2 | NeoForge 26.1.2.124 | 25 | 9.4.0 |

Forge contém os nove upgrades e AE2/RS/Tom's. NeoForge inclui também SCC. Tom's não oferece fluidos; SCC limita fluidos às identidades preservadas por sua API. O [porte 26.1.2](PORT_26_1_2.md) segue P01–P12 e os planos individuais.

## Comandos

Na raiz, com JDKs 17, 21 e 25:

```powershell
.\gradlew.bat checkAll
.\gradlew.bat buildAll
.\gradlew.bat verify-forge-1.20.1
.\gradlew.bat verify-neoforge-1.21.1
.\gradlew.bat verify-neoforge-26.1.2
```

Cada versão também aceita tarefas locais do loader. A partir de `targets/1.20.1`:

```powershell
.\gradlew.bat :forge:verifyForgeProductionJar
.\gradlew.bat :forge:testForgeProductionJarVerification
```

O override `-PforgeProductionJar=C:/path/addon.jar` continua disponível; caminhos relativos agora são relativos ao build `targets/1.20.1`.

Artefatos: `targets/<minecraft>/<loader>/build/libs/<mod_id>-<mod_version>+<minecraft>-<loader>.jar`. A identidade e a versão vêm das propriedades da raiz, incluindo override de CI. Os verificadores coordenados rejeitam metadados, bytecode, recursos e empacotamento incompatíveis.

## Gates e evidência

Os gates Forge continuam em `:forge:check`: JAR reobfuscado, refmap de `pickupDelay`, configuração Mixin, MixinExtras Jar-in-Jar e sete fixtures negativas. O JAR em `build/devlibs` não é a distribuição. A suíte 1.21.1 e seu perfil SCC foram preservados. `check-neoforge-1.21.1-scc` executa os cinco testes legados desse perfil e pertence aos gates de check/build 1.21.1, com relatórios separados.

Para 26.1.2, cada perfil executa JUnit com o servidor efêmero oficial e GameTests com redes nativas. Use `-PbackendProfile=none|ae2|rs|toms|scc|all` a partir do target. O CI executa somente o perfil `all` (como nos demais loaders e versões) e publica relatórios/logs mesmo em falha. Os perfis isolados continuam disponíveis localmente, e a raiz oferece `check-neoforge-26.1.2` para a matriz completa, em sequência para não compartilhar outputs entre processos simultâneos.

A reorganização passou nas suítes Forge 1.20.1 e NeoForge 1.21.1. Registros finais e limites: [26.1.2](PORT_26_1_2.md#evidências-e-aceitação). GUI, sprites/animações, single player e multiplayer exigem validação adicional P12. Migração de mundos entre versões não integra esses testes.

## Registros anteriores à reorganização

Os comandos, caminhos e versões abaixo registram verificações históricas; os caminhos atuais são os descritos acima.
## Resultado da correção de empacotamento — 2026-11-01

- `gradlew.bat :forge:build`: aprovado, incluindo 1 teste JUnit, 10 GameTests Forge e os gates do JAR final. O verificador aceitou o corrigido e rejeitou as sete fixtures danificadas. O JAR original 0.4.0 também foi rejeitado pela ausência da referência ao refmap.
- O JAR original, SHA-1 `8164bb1241df110ca31bc1bdbd050026cd1d66c2`, reproduziu `InvalidAccessorException` em cliente de produção Forge 47.4.26/Java 17.0.8 com apenas as dependências Sophisticated fixadas no projeto. Isso dispensa outros mods para reproduzir a primeira falha.
- A correção somente do refmap revelou uma segunda falha na instalação mínima: `ClassMetadataNotFoundException` para `com.llamalad7.mixinextras.injector.wrapoperation.Operation`. MixinExtras existia no classpath de desenvolvimento, mas não no artefato distribuído. O build agora incorpora a mesma versão 0.4.1 por Jar-in-Jar.
- O artefato final inicializou MixinExtras 0.4.1 e completou o carregamento dos atlas em uma nova execução mínima Forge 47.4.26/Java 17.0.8, sem as duas exceções anteriores no log observado. Menu principal e entrada em mundo não foram confirmados visualmente. Não interpretar esse resultado parcial como aprovação de gameplay.
- A cópia isolada dos 15 JARs externos da modlist foi preparada e conferida por SHA-1. O perfil completo não foi iniciado. Forge 47.4.10 foi exercitado pelos GameTests de desenvolvimento; cliente/servidor de produção nessa versão continuam sem validação nesta tarefa.
- O usuário assumiu os testes manuais. Permanecem com ele a inicialização com a modlist completa, entrada em mundo, Magnet/Pickup, transferências parciais e servidor dedicado. Nenhum mundo ou arquivo da instância original foi alterado.
- Documentação técnica do portal sincronizada em inglês e português; `pnpm run check` e `pnpm run build` aprovados. A etapa inicial da investigação preservou a versão 0.4.0; a preparação posterior do patch está registrada abaixo.

Logs e arquivos temporários da investigação ficam em `forge/build/verification/`, incluindo o build, a rejeição do original e os perfis isolados. Esse diretório é ignorado pelo Git.

### Preparação do patch 0.4.1

A versão canônica foi atualizada para 0.4.1 e o changelog registra a correção de inicialização no Forge 1.20.1. `gradlew.bat build` passou nos dois loaders, incluindo os testes automatizados existentes, 10 GameTests Forge e as sete regressões do verificador de produção. Os JARs finais declaram `0.4.1+1.20.1` no Forge e `0.4.1+1.21.1` no NeoForge. A documentação 0.4.1 foi sincronizada em inglês e português e passou em `pnpm run check` e `pnpm run build`. Os testes manuais continuam com o usuário; a preparação não declara os gates de gameplay concluídos.
