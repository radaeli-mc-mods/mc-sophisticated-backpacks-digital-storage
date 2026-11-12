package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

final class NativeTestInventory extends ItemStacksResourceHandler {
    NativeTestInventory(int slots) { super(slots); }
    void setStackInSlot(int slot, ItemStack stack) { set(slot, ItemResource.of(stack), stack.getCount()); }
    ItemStack getStackInSlot(int slot) { return getResource(slot).toStack(getAmountAsInt(slot)); }
}
