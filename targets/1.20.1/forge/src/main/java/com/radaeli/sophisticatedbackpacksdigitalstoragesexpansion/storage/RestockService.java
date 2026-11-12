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
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalExtraction(context.storage())) {
            // Freeze candidate identities before the first insertion changes the contents-filter view.
            List<StoredItemAmount> candidates = target.getAvailableItemAmounts(context.player()).stream()
                    .filter(candidate -> candidate.amount() > 0 && wrapper.getFilterLogic().matchesFilter(candidate.exemplar()))
                    .toList();
            for (StoredItemAmount candidate : candidates) {
                ItemStack exemplar = candidate.exemplar();
                int capacity = capacity(backpack, exemplar, (int) Math.min(candidate.amount(), Integer.MAX_VALUE));
                if (capacity <= 0) continue;
                int estimate = Math.min(capacity, target.simulateExtract(exemplar, capacity, context.player()));
                if (estimate <= 0) continue;
                ItemStack extracted = target.extract(exemplar, estimate, context.player());
                if (extracted.isEmpty()) continue;
                ItemStack remainder = insert(backpack, extracted.copy());
                moved += extracted.getCount() - remainder.getCount();
                if (!remainder.isEmpty()) recover(context, target, remainder);
            }
        }
        context.player().displayClientMessage(Component.translatable(moved > 0
                ? "message.sophisticated_backpacks_digital_storages_expansion.restocked"
                : "message.sophisticated_backpacks_digital_storages_expansion.nothing_to_restock", moved), true);
    }

    private static int capacity(IItemHandler destination, ItemStack exemplar, int maximum) {
        ItemStack remainder = exemplar.copyWithCount(maximum);
        int accepted = 0;
        for (int slot = 0; slot < destination.getSlots() && !remainder.isEmpty(); slot++) {
            ItemStack left = destination.insertItem(slot, remainder, true);
            accepted += remainder.getCount() - left.getCount();
            remainder = left;
        }
        return accepted;
    }

    private static ItemStack insert(IItemHandler destination, ItemStack offered) {
        ItemStack remainder = offered;
        for (int slot = 0; slot < destination.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = destination.insertItem(slot, remainder, false);
        }
        return remainder;
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
