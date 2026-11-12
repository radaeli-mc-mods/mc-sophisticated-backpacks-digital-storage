package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.List;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageOperation;

public interface StorageTarget {
    StorageEndpoint endpoint();

    /** Presence does not require extraction rights or an extraction simulation. */
    default boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
            java.util.Optional<net.minecraft.world.item.Item> item) {
        IItemHandler contents = getAvailableItems(player);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack stack = contents.getStackInSlot(slot);
            if (!stack.isEmpty() && item.map(stack::is).orElse(true) && matches.test(stack)) return true;
        }
        return false;
    }

    default boolean canBind(ServerPlayer player, SourceLocation source) {
        return canAccess(player, source, StorageOperation.INSERT);
    }

    default List<StoredItemAmount> queryItems(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
            java.util.Optional<net.minecraft.world.item.Item> item, int needed) {
        if (needed <= 0) return List.of();
        return getAvailableItemAmounts(player).stream()
                .filter(value -> item.map(type -> value.exemplar().is(type)).orElse(true))
                .filter(value -> matches.test(value.exemplar())).toList();
    }

    boolean isReachable(ServerPlayer player, SourceLocation source);

    IItemHandler getAvailableItems(ServerPlayer player);

    int simulateInsert(ItemStack stack, ServerPlayer player);

    ItemStack insert(ItemStack stack, ServerPlayer player);

    default boolean canAccess(ServerPlayer player, SourceLocation source, StorageOperation operation) {
        return isReachable(player, source);
    }

    default List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
        var items = new java.util.ArrayList<StoredItemAmount>();
        IItemHandler snapshot = getAvailableItems(player);
        for (int slot = 0; slot < snapshot.getSlots(); slot++) {
            ItemStack stack = snapshot.getStackInSlot(slot);
            if (!stack.isEmpty()) items.add(new StoredItemAmount(stack.copyWithCount(1), stack.getCount()));
        }
        return List.copyOf(items);
    }

    default int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
        return 0;
    }

    default ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
        return ItemStack.EMPTY;
    }

    default ItemInsertion insertionFor(ServerPlayer player) {
        return new ItemInsertion() {
            @Override
            public int simulateInsert(ItemStack stack) {
                return StorageTarget.this.simulateInsert(stack, player);
            }

            @Override
            public ItemStack insert(ItemStack stack) {
                return StorageTarget.this.insert(stack, player);
            }
        };
    }
}
