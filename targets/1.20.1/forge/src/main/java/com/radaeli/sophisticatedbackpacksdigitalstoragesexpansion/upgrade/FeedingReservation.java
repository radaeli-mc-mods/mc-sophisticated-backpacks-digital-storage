package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class FeedingReservation {
    private static final String ROOT = "remote_feeding_reservation";
    private static final String STACK = "stack";
    private static final String STATE = "state";
    private static final String REMOTE = "remote";

    enum State { RESERVED, USING, RECOVERING }

    private FeedingReservation() {
    }

    static boolean hasPending(ItemStack upgrade) {
        CompoundTag data = getRoot(upgrade);
        return data != null && data.contains(STATE);
    }

    static State state(ItemStack upgrade) {
        CompoundTag data = getRoot(upgrade);
        if (data == null || !data.contains(STATE)) return null;
        try {
            return State.valueOf(data.getString(STATE));
        } catch (IllegalArgumentException exception) {
            return State.RECOVERING;
        }
    }

    static ItemStack stack(ItemStack upgrade, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag data = getRoot(upgrade);
        if (data == null || !data.contains(STACK, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.of(data.getCompound(STACK));
    }

    static boolean remote(ItemStack upgrade) {
        CompoundTag data = getRoot(upgrade);
        return data != null && data.getBoolean(REMOTE);
    }

    static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, net.minecraft.core.HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        CompoundTag reservation = new CompoundTag();
        reservation.putString(STATE, State.RESERVED.name());
        reservation.putBoolean(REMOTE, remote);
        reservation.put(STACK, stack.save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
        return true;
    }

    static void updateStack(ItemStack upgrade, ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag reservation = getRoot(upgrade);
        if (reservation == null) return;
        if (stack.isEmpty()) reservation.remove(STACK);
        else reservation.put(STACK, stack.save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
    }

    static void setState(ItemStack upgrade, State state) {
        CompoundTag reservation = getRoot(upgrade);
        if (reservation != null) { reservation.putString(STATE, state.name()); ForgeUpgradeNbt.write(upgrade, ROOT, reservation); }
    }

    static void clear(ItemStack upgrade) {
        ForgeUpgradeNbt.clear(upgrade, ROOT);
    }

    private static CompoundTag getRoot(ItemStack upgrade) {
        return ForgeUpgradeNbt.read(upgrade, ROOT);
    }
}
