package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleExtractor;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageConnection;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageOperation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTarget;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StoredItemAmount;

/** Per-operation source lookup and exact extraction used by the three consumable upgrades. */
final class UpgradeItemSources {
    private UpgradeItemSources() {
    }

    static List<SourceItem> find(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            SourcePriority priority, Predicate<ItemStack> eligible) {
        return find(wrapper, player, priority, eligible, Optional.empty(), Integer.MAX_VALUE);
    }

    static List<SourceItem> find(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            SourcePriority priority, Predicate<ItemStack> eligible, Optional<net.minecraft.world.item.Item> knownItem, int needed) {
        return find(wrapper, player, SourceLocation.carriedBy(player), priority, eligible, knownItem, needed);
    }

    static List<SourceItem> find(LinkedUpgradeInventory wrapper, ServerPlayer player, SourceLocation location,
            SourcePriority priority, Predicate<ItemStack> eligible, Optional<net.minecraft.world.item.Item> knownItem, int needed) {
        List<SourceItem> network = new ArrayList<>();
        try (StorageTransferGuard guard = StorageTransferGuard.blockExternalExtraction(wrapper.getStorageWrapper())) {
            resolveExtractTarget(wrapper, player, location).ifPresent(target -> {
                for (StoredItemAmount amount : target.queryItems(player, eligible, knownItem, needed)) {
                    ItemStack exemplar = amount.exemplar();
                    if (!exemplar.isEmpty() && amount.amount() > 0 && eligible.test(exemplar)) {
                        network.add(new SourceItem(exemplar.copyWithCount(1), amount.amount(), target));
                    }
                }
            });
        }
        List<SourceItem> backpack = new ArrayList<>();
        ITrackedContentsItemHandler inventory = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && eligible.test(stack)) {
                backpack.add(new SourceItem(stack.copyWithCount(1), stack.getCount(), null));
            }
        }
        Comparator<SourceItem> stable = Comparator.comparing((SourceItem item) -> BuiltInRegistries.ITEM.getKey(item.stack().getItem()).toString())
                .thenComparingInt(item -> item.stack().hashCode());
        network.sort(stable);
        backpack.sort(stable);
        List<SourceItem> result = new ArrayList<>(network.size() + backpack.size());
        if (priority == SourcePriority.NETWORK_FIRST) {
            result.addAll(network);
            result.addAll(backpack);
        } else {
            result.addAll(backpack);
            result.addAll(network);
        }
        return result;
    }

    static ItemStack extract(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            SourceItem source, int amount) {
        return extract(wrapper, player, SourceLocation.carriedBy(player), source, amount);
    }

    static ItemStack extract(LinkedUpgradeInventory wrapper, ServerPlayer player, SourceLocation location,
            SourceItem source, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        int requested = (int) Math.min(amount, Math.min(source.amount(), Integer.MAX_VALUE));
        if (requested <= 0) {
            return ItemStack.EMPTY;
        }
        if (source.target() == null) {
            ITrackedContentsItemHandler inventory = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing();
            if (!(inventory instanceof IItemHandlerSimpleExtractor extractor)) {
                return ItemStack.EMPTY;
            }
            return extractor.extractItem(source.stack().copyWithCount(requested), false);
        }
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalExtraction(wrapper.getStorageWrapper())) {
            int available = source.target().simulateExtract(source.stack(), requested, player);
            if (available <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = source.target().extract(source.stack(), Math.min(available, requested), player);
            if (!extracted.isEmpty() && !ItemStack.isSameItemSameComponents(source.stack(), extracted)) {
                ItemStack remainder = returnToSource(wrapper, player, location, source, extracted);
                if (!remainder.isEmpty()) recover(wrapper, player, remainder);
                return ItemStack.EMPTY;
            }
            return extracted;
        }
    }

    static ItemStack returnToSource(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            SourceItem source, ItemStack stack) {
        return returnToSource(wrapper, player, SourceLocation.carriedBy(player), source, stack);
    }

    static ItemStack returnToSource(LinkedUpgradeInventory wrapper, ServerPlayer player, SourceLocation location,
            SourceItem source, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (source.target() == null) {
            return wrapper.getStorageWrapper().getInventoryForUpgradeProcessing().insertItem(stack, false);
        }
        return returnToLinkedNetwork(wrapper, player, location, stack);
    }

    static ItemStack recover(LinkedUpgradeInventory wrapper, ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing().insertItem(stack, false);
        if (!remaining.isEmpty()) {
            player.getInventory().placeItemBackInInventory(remaining);
        }
        return ItemStack.EMPTY;
    }

    static ItemStack returnToLinkedNetwork(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            ItemStack stack) {
        return returnToLinkedNetwork(wrapper, player, SourceLocation.carriedBy(player), stack);
    }

    private static ItemStack returnToLinkedNetwork(LinkedUpgradeInventory wrapper, ServerPlayer player,
            SourceLocation location, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalInsertion(wrapper.getStorageWrapper())) {
            return resolveInsertTarget(wrapper, player, location).map(target -> target.insert(stack, player)).orElse(stack);
        }
    }

    private static Optional<StorageTarget> resolveExtractTarget(LinkedUpgradeInventory wrapper,
            ServerPlayer player, SourceLocation location) {
        return StorageAdapterRegistry.connect(player, wrapper.getEndpoint(), location,
                StorageOperation.EXTRACT).target();
    }

    private static Optional<StorageTarget> resolveInsertTarget(LinkedUpgradeInventory wrapper,
            ServerPlayer player, SourceLocation location) {
        return StorageAdapterRegistry.connect(player, wrapper.getEndpoint(), location,
                StorageOperation.INSERT).target();
    }

    record SourceItem(ItemStack stack, long amount, StorageTarget target) {
        SourceItem {
            stack = stack.copyWithCount(1);
        }

        boolean remote() {
            return target != null;
        }
    }
}
