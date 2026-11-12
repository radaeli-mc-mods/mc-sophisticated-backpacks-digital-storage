package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Backend- and menu-independent transfer with recovery based on the actual insertion result. */
public final class ItemTransferService {
    private ItemTransferService() {
    }

    public static TransferResult deposit(IItemHandler source, ItemInsertion destination,
            Predicate<ItemStack> filter, Consumer<ItemStack> recoverRemainder) {
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
            if (initial.isEmpty() || !ItemStack.isSameItemSameTags(initial, current)) {
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

            ItemStack remainder = destination.insert(extracted.copy());
            int inserted = extracted.getCount() - remainder.getCount();
            if (inserted > 0) {
                movedStacks++;
                movedItems += inserted;
            }
            restoreRemainder(source, slot, remainder, recoverRemainder);
        }
        return new TransferResult(movedStacks, movedItems);
    }

    private static void restoreRemainder(IItemHandler source, int originalSlot, ItemStack remainder,
            Consumer<ItemStack> recoverRemainder) {
        if (remainder.isEmpty()) {
            return;
        }
        ItemStack left = source.insertItem(originalSlot, remainder, false);
        // An extraction hook may have changed the original slot. Try the remaining source slots first.
        for (int slot = 0; slot < source.getSlots() && !left.isEmpty(); slot++) {
            if (slot != originalSlot) {
                left = source.insertItem(slot, left, false);
            }
        }
        if (!left.isEmpty()) {
            recoverRemainder.accept(left);
        }
    }

    public record TransferResult(int movedStacks, long movedItems) {
        public boolean movedAnything() {
            return movedItems > 0;
        }
    }
}
