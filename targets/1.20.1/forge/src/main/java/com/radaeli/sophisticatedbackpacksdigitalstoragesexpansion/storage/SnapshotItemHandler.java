package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Read-only item identities for Sophisticated's inventory filter, never a transfer destination. */
public final class SnapshotItemHandler implements IItemHandler {
    private final List<ItemStack> stacks;

    public SnapshotItemHandler(Collection<ItemStack> stacks) {
        this.stacks = stacks.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList();
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot).copy();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }
}
