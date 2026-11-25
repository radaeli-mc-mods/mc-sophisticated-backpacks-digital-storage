package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeWrapper;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.UpgradeOperationContext;

/** Manual network-to-backpack transfer with real extraction counts and native filter rules. */
public final class RestockService {
    private RestockService() {}

    public static void restock(UpgradeOperationContext context, RemoteRestockUpgradeWrapper wrapper) {
        if (wrapper.hasPendingDelivery()) return;
        StorageConnection connection = StorageAdapterRegistry.connect(context.player(), wrapper.getEndpoint(),
                context.source(), StorageOperation.EXTRACT);
        if (connection.target().isEmpty()) {
            context.player().displayClientMessage(Component.translatable(connection.status().messageKey()), true);
            return;
        }
        StorageTarget target = connection.target().orElseThrow();
        IItemHandler backpack = context.storage().getInventoryForUpgradeProcessing();
        wrapper.getFilterLogic().setInventory(backpack);
        long moved = 0;
        boolean limited = false;
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalExtraction(context.storage())) {
            // Freeze candidate identities before the first insertion changes the contents-filter view.
            List<StoredItemAmount> candidates = target.getAvailableItemAmounts(context.player()).stream()
                    .filter(candidate -> candidate.amount() > 0 && wrapper.getFilterLogic().matchesFilter(candidate.exemplar()))
                    .toList();
            var result = RestockTransferService.transfer(backpack, target, context.player(), candidates,
                    wrapper.getFilterLogic()::matchesFilter,
                    () -> wrapper.isEnabled() && context.storage().getUpgradeHandler().getSlotWrappers().containsValue(wrapper)
                            && target.canAccess(context.player(), context.source(), StorageOperation.EXTRACT),
                    remainder -> recover(context, target, remainder), wrapper.deliveryJournal(context.player().level()));
            moved = result.moved();
            limited = result.limited();
        }
        context.player().displayClientMessage(Component.translatable(limited ? "message.sophisticated_backpacks_digital_storages_expansion.restock_limit" : moved > 0
                ? "message.sophisticated_backpacks_digital_storages_expansion.restocked"
                : "message.sophisticated_backpacks_digital_storages_expansion.nothing_to_restock", moved), true);
    }

    private static void recover(UpgradeOperationContext context, StorageTarget source, ItemStack remainder) {
        StorageConnection insertConnection = StorageAdapterRegistry.connect(context.player(), source.endpoint(),
                context.source(), StorageOperation.INSERT);
        if (insertConnection.target().isPresent()) {
            remainder = insertConnection.target().get().insert(remainder, context.player());
        }
        if (!remainder.isEmpty()) context.player().getInventory().placeItemBackInInventory(remainder);
    }
}
