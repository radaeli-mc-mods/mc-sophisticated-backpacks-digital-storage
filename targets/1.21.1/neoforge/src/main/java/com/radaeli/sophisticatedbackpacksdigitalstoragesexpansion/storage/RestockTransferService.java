package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** One synchronous request, bounded stack offers, fresh capacity/access per batch. */
public final class RestockTransferService {
    public static final int MAX_BATCHES = 1_024;
    private RestockTransferService() {}
    public record Result(long moved, boolean limited) {}

    public static Result transfer(IItemHandler backpack, StorageTarget target, ServerPlayer player,
            List<StoredItemAmount> candidates, Predicate<ItemStack> filter, BooleanSupplier active,
            Consumer<ItemStack> recovery, ItemDeliveryJournal journal) {
        long moved = 0;
        int batches = 0;
        if (journal.hasPending()) return new Result(0, false);
        for (StoredItemAmount candidate : candidates) {
            ItemStack exemplar = candidate.exemplar();
            long remaining = candidate.amount();
            while (remaining > 0 && active.getAsBoolean()) {
                int request = (int) Math.min(remaining, exemplar.getMaxStackSize());
                int capacity = capacity(backpack, exemplar, request);
                if (capacity <= 0) break;
                if (batches == MAX_BATCHES) return new Result(moved, true);
                batches++;
                int estimate = Math.max(0, Math.min(capacity, target.simulateExtract(exemplar, capacity, player)));
                if (estimate == 0) break;
                ItemStack extracted = target.extract(exemplar, estimate, player);
                if (extracted.isEmpty()) break;
                remaining -= extracted.getCount();
                var steps = new ArrayList<com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery.Step<ItemStack>>();
                if (ItemStack.isSameItemSameComponents(exemplar, extracted) && filter.test(extracted)) {
                    for (int slot = 0; slot < backpack.getSlots(); slot++) {
                        int destinationSlot = slot;
                        steps.add(ItemDelivery.step("restock.slot_" + slot, offer -> backpack.insertItem(destinationSlot, offer, false)));
                    }
                }
                var result = ItemDelivery.deliver(extracted, steps, journal);
                if (result.uncertain()) return new Result(moved, false);
                moved += extracted.getCount() - result.remainder().getCount();
                if (!result.remainder().isEmpty()) {
                    var restored = ItemDelivery.deliver(result.remainder(), List.of(ItemDelivery.step("restock.recovery", offer -> {
                        recovery.accept(offer); return ItemStack.EMPTY;
                    })), journal);
                    if (restored.uncertain()) return new Result(moved, false);
                    journal.clear();
                    break;
                }
                journal.clear();
            }
        }
        return new Result(moved, false);
    }
    private static int capacity(IItemHandler destination, ItemStack exemplar, int maximum) {
        ItemStack remaining = exemplar.copyWithCount(maximum);
        for (int slot = 0; slot < destination.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = destination.insertItem(slot, remaining, true);
        }
        return maximum - remaining.getCount();
    }
}
