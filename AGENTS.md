# Instruções de desenvolvimento

## Planos obrigatórios dos upgrades

Sempre que uma tarefa envolver desenvolvimento, correção, revisão ou evolução de um upgrade, seguir seu plano individual e os planos base. Isso vale também quando o pedido mencionar somente o nome da função, sem citar documentos.

Antes de editar código:

1. Ler [docs/UPGRADES.md](docs/UPGRADES.md): premissas P01–P12, contratos compartilhados, dependências, sequência e gates.
2. Ler [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): implementação existente, responsabilidades, ciclo de vida e limitações auditadas.
3. Ler integralmente o plano individual da função na tabela abaixo, incluindo suas referências às premissas base.
4. Conferir no código e nos JARs fixados as APIs e o estado atual relevantes. Distinguir APIs existentes de contratos propostos nos documentos.

| Upgrade | Plano obrigatório |
| --- | --- |
| Deposit / Depósito | [DEPOSIT.md](docs/upgrades/DEPOSIT.md) |
| Restock / Abastecimento manual | [RESTOCK.md](docs/upgrades/RESTOCK.md) |
| Pickup / Coleta | [PICKUP.md](docs/upgrades/PICKUP.md) |
| Magnet / Ímã | [MAGNET.md](docs/upgrades/MAGNET.md) |
| Refill / Reposição automática | [REFILL.md](docs/upgrades/REFILL.md) |
| Feeding / Alimentação | [FEEDING.md](docs/upgrades/FEEDING.md) |
| Alchemy / Alquimia | [ALCHEMY.md](docs/upgrades/ALCHEMY.md) |
| Pump / Bomba | [PUMP.md](docs/upgrades/PUMP.md) |
| Crafting remoto | [CRAFTING.md](docs/upgrades/CRAFTING.md) |

Para mudanças compartilhadas, ler também os planos das funções afetadas. Para uma função nova, definir seu plano individual, referenciar as premissas base e incluí-lo no catálogo e no índice antes de implementar.

## Aplicação dos planos

- Conduzir a implementação pelas etapas e critérios do plano individual, respeitando os pré-requisitos e gates do documento principal. Não ampliar o escopo silenciosamente.
- Ao explicar a abordagem ou registrar uma alteração técnica, referenciar o plano da função e as premissas compartilhadas aplicáveis pelos identificadores P01–P12.
- Usar ao máximo APIs, filtros, controles e ciclo de vida do Sophisticated. Concentrar comportamento reutilizável na base comum; manter detalhes de backend isolados nos adaptadores.
- Manter um item universal por função, vínculo manual e regras nativas de acesso. Seguir os planos para prioridade, conflitos, reservas, recuperação e limitações por rede.
- Preservar dados existentes e não tratar uma compilação como prova de funcionamento em jogo. Não marcar checklists como concluídos sem evidência; distinguir implementação, verificações executadas e validação manual pendente.
- Os cenários de testes dos planos são critérios de entrega, não autorização automática para adicionar ou executar testes. Seguir as instruções vigentes da tarefa sobre testes e validação.

## Consistência da documentação

[docs/PRD.md](docs/PRD.md) registra o MVP original. [docs/UPGRADES.md](docs/UPGRADES.md) e os planos individuais definem a evolução aprovada; [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) descreve o estado implementado.

Se uma instrução explícita do usuário alterar uma decisão aprovada, incorporá-la e atualizar os documentos afetados. Se houver contradição relevante sem decisão do usuário, esclarecê-la antes de implementar a parte dependente; continuar trabalho independente quando possível.

Ao implementar contratos ou mudar comportamento, atualizar a arquitetura e o plano correspondente para refletir o resultado. Manter premissas compartilhadas no documento principal, com âncoras estáveis, e referenciá-las nos planos individuais. Atualizar [docs/README.md](docs/README.md) quando houver novos documentos.

## Convenções do projeto

- Java usa o pacote base `com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion`.
- O ID do mod e o namespace de recursos, itens e receitas é `sophisticated_backpacks_digital_storages_expansion`.
- Plataforma e versões seguem os arquivos de build e a premissa P01.
- Texturas e modelos próprios devem seguir o padrão visual definido em P07. Não criar variantes nem ampliar o escopo de assets sem instrução explícita do usuário.
- Commits, versões e releases seguem a política pessoal fornecida pelo usuário e a skill `release-versioning` quando aplicável.
