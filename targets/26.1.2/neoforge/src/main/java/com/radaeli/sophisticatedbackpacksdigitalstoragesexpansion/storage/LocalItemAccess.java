package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Immediate local operations. Each native transaction closes before a backend is called. */
public record LocalItemAccess(ResourceHandler<ItemResource> handler) implements IItemHandler {
    public static LocalItemAccess of(ResourceHandler<ItemResource> handler) { return new LocalItemAccess(handler); }
    @Override public int getSlots() { return handler.size(); }
    @Override public ItemStack getStackInSlot(int slot) { return handler.getResource(slot).toStack(handler.getAmountAsInt(slot)); }
    @Override public int getSlotLimit(int slot) { return handler.getCapacityAsInt(slot, handler.getResource(slot)); }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return handler.isValid(slot, ItemResource.of(stack)); }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - accepted);
        }
    }
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - accepted);
        }
    }
    public ItemStack extractItem(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(extracted);
        }
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, tx);
            if (!simulate) tx.commit();
            return resource.toStack(extracted);
        }
    }
}
