package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Backend- and menu-independent transfer with recovery based on the actual insertion result. */
public final class ItemTransferService {
    private ItemTransferService() {
    }

    public static TransferResult deposit(ItemDeliveryJournal journal, IItemHandler source, ItemInsertion destination,
            Predicate<ItemStack> filter, Consumer<ItemStack> recoverRemainder) {
        if (journal.hasPending()) return new TransferResult(0, 0);
        int movedStacks = 0;
        long movedItems = 0;
        List<ItemStack> initialContents = new ArrayList<>(source.getSlots());
        for (int slot = 0; slot < source.getSlots(); slot++) {
            initialContents.add(source.getStackInSlot(slot).copy());
        }
        for (int slot = 0; slot < initialContents.size(); slot++) {
            ItemStack initial = initialContents.get(slot);
            ItemStack current = source.getStackInSlot(slot);
            // Do not process items restored or replenished into a later slot during this transfer.
            if (initial.isEmpty() || !ItemStack.isSameItemSameComponents(initial, current)) {
                continue;
            }
            // Handlers can expose a live stack. Keep our offered count independent of their mutations.
            ItemStack offered = current.copyWithCount(Math.min(initial.getCount(), current.getCount()));
            if (offered.isEmpty() || !filter.test(offered)) {
                continue;
            }

            int planned = ItemInsertion.acceptedCount(offered, destination.simulateInsert(offered.copy()));
            if (planned == 0) {
                continue;
            }
            ItemStack extracted = source.extractItem(slot, planned, false);
            if (extracted.isEmpty()) {
                continue;
            }

            var delivery = ItemDelivery.deliver(extracted,
                    List.of(ItemDelivery.step("deposit.remote", destination::insert)), journal);
            if (delivery.uncertain()) break;
            ItemStack remainder = delivery.remainder();
            int inserted = extracted.getCount() - remainder.getCount();
            if (inserted > 0) {
                movedStacks++;
                movedItems += inserted;
            }
            var recovered = restoreRemainder(journal, source, slot, remainder, recoverRemainder);
            if (recovered.uncertain()) break;
            journal.clear();
            if (delivery.failure() != null || recovered.failure() != null) break;
        }
        return new TransferResult(movedStacks, movedItems);
    }

    private static com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery.Result<ItemStack> restoreRemainder(
            ItemDeliveryJournal journal, IItemHandler source, int originalSlot, ItemStack remainder,
            Consumer<ItemStack> recoverRemainder) {
        List<com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery.Step<ItemStack>> steps = new ArrayList<>();
        steps.add(ItemDelivery.step("deposit.original_slot", stack -> source.insertItem(originalSlot, stack, false)));
        for (int slot = 0; slot < source.getSlots(); slot++) {
            if (slot == originalSlot) continue;
            int currentSlot = slot;
            steps.add(ItemDelivery.step("deposit.source_slot_" + slot, stack -> source.insertItem(currentSlot, stack, false)));
        }
        steps.add(ItemDelivery.step("deposit.player", stack -> { recoverRemainder.accept(stack); return ItemStack.EMPTY; }));
        return ItemDelivery.deliver(remainder, steps, journal);
    }

    public record TransferResult(int movedStacks, long movedItems) {
        public boolean movedAnything() {
            return movedItems > 0;
        }
    }
}
