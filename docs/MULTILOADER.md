# Base multiloader

## Implementação

O projeto entrega módulos Gradle `common`, `neoforge` e `forge`, com um JAR independente por loader. `common` compila em Java 17 e contém apenas código sem dependências de Minecraft. As APIs e classes Minecraft são compiladas por plataforma; não há um binário Minecraft compartilhado entre as duas versões.

O módulo `forge` contém os oito upgrades remotos: Deposit, Restock, Pickup, Magnet, Refill, Feeding, Alchemy e Pump. O código inclui registros, abas/configuração Sophisticated, persistência NBT, desvínculo, recursos 1.20.1, adaptadores de itens para AE2 15.x/Refined Storage 1.x/Tom's Storage 1.7.x e adaptadores de fluidos para AE2/Refined Storage. Tom's informa incompatibilidade de fluidos. Os adaptadores opcionais são carregados apenas quando o respectivo mod está instalado.

Regras e wrappers Minecraft foram portados para fontes Forge e NeoForge separadas. No estado atual, a única classe Java compartilhada em `common` é `TransferQuantities`; ampliar a extração de regras puras continua sendo uma melhoria arquitetural possível, sem mudar os contratos funcionais.

## Versões e build

| Módulo | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `forge` | 1.20.1 | Forge 47.4.10 | 17 |
| `neoforge` | 1.21.1 | NeoForge 21.1.236 | 21 |
| `common` | independente | — | 17 |

As dependências de desenvolvimento estão fixadas por artefatos Maven do Modrinth em `forge/build.gradle` e `neoforge/build.gradle`. O build Forge usa ModDevGradle Legacy Forge, gera refmap, inclui `MixinConfigs` e reobfusca o JAR com `reobfJar`. Os mods de storage não são incorporados ao artefato.

## Validação feita

### Gate do artefato Forge de produção — P01/P12

Com Java 21 para executar Gradle e toolchain Java 17 disponível para Forge:

```powershell
.\gradlew.bat :forge:build
.\gradlew.bat :forge:verifyForgeProductionJar
.\gradlew.bat :forge:testForgeProductionJarVerification
```

Os dois novos gates integram `:forge:check` e dependem de `reobfJar`. Inspecionam o JAR em `forge/build/libs`, não o JAR de desenvolvimento em `forge/build/devlibs` nem somente os recursos processados. O verificador exige `MixinConfigs` no manifesto, a configuração com referência explícita ao refmap, o recurso referenciado, a classe do accessor e o mapeamento `pickupDelay → f_31986_:I` nos contextos padrão e `searge`. Também exige a declaração Jar-in-Jar de MixinExtras e seu JAR incorporado, necessário ao `PickupContextMixin` em instalações mínimas. A versão incorporada é 0.4.1; o intervalo `[0.4.1,)` permite a seleção compartilhada por Jar-in-Jar.

O teste usa o mesmo verificador contra sete cópias temporárias: referência ausente, refmap ausente, mapeamento do accessor ausente, contexto `searge` incorreto, manifesto ausente, metadados Jar-in-Jar ausentes e declaração MixinExtras ausente. Cada caso deve falhar pelo motivo esperado. Os arquivos ficam apenas em `build/tmp`.

Para auditar um artefato anterior, sem substituí-lo:

```powershell
.\gradlew.bat :forge:verifyForgeProductionJar -PforgeProductionJar=C:/path/to/addon.jar
```

O caminho relativo é resolvido a partir da raiz do repositório. Essa opção altera somente o arquivo inspecionado; `reobfJar` continua como pré-requisito do gate. Falha de validação encerra a tarefa com erro.

O JSON Forge declara `sophisticated_backpacks_digital_storages_expansion.refmap.json`; gerar e empacotar o arquivo sozinho não basta para selecioná-lo no Mixin. O accessor permanece remapeável, e os Mixins das APIs Sophisticated mantêm `remap = false`. Não há alteração de IDs, NBT ou regras do Magnet.

### Validação de runtime

O build inclui os GameTests do ambiente de desenvolvimento. Para conferir a distribuição, usar Java 17 e carregar o JAR reobfuscado em instâncias isoladas de Minecraft 1.20.1/Forge 47.4.10 e 47.4.26, fora de `runClient` e sem flags de remapeamento de desenvolvimento. Começar apenas com addon/Core/Backpacks; depois conferir Core 1.5.21.2444, Backpacks 3.26.19.2239 e o conjunto de mods do caso investigado.

Registrar separadamente inicialização até o menu, entrada em mundo novo, servidor dedicado e cenários do [Magnet](upgrades/MAGNET.md): delay infinito/finito, sobras parciais e coexistência com Pickup. Uma falha posterior deve ser investigada a partir do primeiro erro causal do novo log. Não considerar aprovação de build ou inspeção de ZIP como aprovação desses cenários.

Os resultados atuais de execução estão registrados ao final deste documento; as tabelas abaixo preservam o levantamento inicial da base multiloader.

| Verificação | Resultado | Limite da evidência |
| --- | --- | --- |
| `:common:test` | 3 testes aprovados | Contrato aritmético independente de Minecraft. |
| `:neoforge:test --rerun-tasks` | 51 testes aprovados | Suíte existente da linha 1.21.1. |
| `:forge:test` | 1 teste aprovado | Contrato de permissão INSERT/EXTRACT; não inicializa o jogo. |
| `:forge:build` | Aprovado | Compilação, JAR Forge reobfuscado, recursos, tag `tags/items`, oito receitas e configuração Mixin. |

O artefato gerado é `forge/build/libs/sophisticated_backpacks_digital_storages_expansion-forge-0.1.0.jar`. A inspeção do JAR confirmou as oito classes de item, receitas, tag `sophisticatedbackpacks:upgrade` e configuração Mixin. O ambiente de testes JUnit Forge não inicializa registries de Minecraft; por isso, o teste Forge atual permanece restrito a lógica pura.

## Validação ainda necessária

Não foram executados GameTests Forge, boot de cliente/servidor com o JAR final, nem cenários com redes reais AE2, Refined Storage e Tom's Storage. Permanecem pendentes a matriz de cinco perfis de backends, persistência em mundo, permissões/alcance, GUI sincronizada, troca de topologia e validação manual em cliente e servidor dedicado. Compilar e empacotar não comprova esses cenários.

As receitas e assets partem dos recursos 1.21.1. O build converte o campo `result.id` para `result.item`, move tags de `tags/item` para `tags/items`, e usa `pack_format: 15` no Forge. As diferenças de API de filtros, hooks, capabilities e NBT ficam no código específico de cada módulo.

Os contratos funcionais e limites de cada upgrade permanecem em [UPGRADES.md](UPGRADES.md) e nos [planos individuais](upgrades/).

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
