package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** NBT persistence bridge for Forge 1.20.1 upgrade item stacks. */
final class ForgeUpgradeNbt {
    private ForgeUpgradeNbt() {
    }

    static CompoundTag read(ItemStack stack, String key) {
        if (!stack.hasTag() || !stack.getTag().contains(key, CompoundTag.TAG_COMPOUND)) return null;
        return stack.getTag().getCompound(key).copy();
    }

    static void write(ItemStack stack, String key, CompoundTag value) {
        stack.getOrCreateTag().put(key, value.copy());
    }

    static void clear(ItemStack stack, String key) {
        if (stack.hasTag()) stack.getTag().remove(key);
    }
}
