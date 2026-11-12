package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.UpgradeOperationContext;

/** Applies the Sophisticated deposit filter to a validated upgrade operation. */
public final class DepositService {
    private DepositService() {
    }

    public static void deposit(UpgradeOperationContext context, Optional<StorageEndpoint> endpoint, DepositFilterLogic filters) {
        StorageConnection connection = StorageAdapterRegistry.connect(context.player(), endpoint, context.source());
        if (connection.target().isEmpty()) {
            context.player().displayClientMessage(Component.translatable(connection.status().messageKey()), true);
            return;
        }

        StorageTarget target = connection.target().get();
        ItemTransferService.TransferResult result;
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalInsertion(context.storage())) {
            if (filters.getDepositFilterType() == DepositFilterType.INVENTORY) {
                // A potentially large network snapshot is needed only for the native inventory matching mode.
                filters.setInventory(target.getAvailableItems(context.player()));
            }

            result = ItemTransferService.deposit(context.storage().getInventoryForUpgradeProcessing(),
                    target.insertionFor(context.player()), filters::matchesFilter,
                    context.player().getInventory()::placeItemBackInInventory);
        }
        context.player().displayClientMessage(Component.translatable(result.movedAnything()
                ? "message.sophisticated_backpacks_digital_storages_expansion.deposited"
                : "message.sophisticated_backpacks_digital_storages_expansion.nothing_to_deposit", result.movedStacks()), true);
    }
}
