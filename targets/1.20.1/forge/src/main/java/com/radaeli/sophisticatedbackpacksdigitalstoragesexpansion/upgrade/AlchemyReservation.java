package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

/** Persisted one-item reservation used to survive normal Alchemy operation interruptions. */
public final class AlchemyReservation {
    private static final String ROOT = "remote_alchemy_reservation";
    private static final String STATE = "state";
    private static final String STACK = "stack";
    private static final String REMOTE = "remote";

    public enum State {
        RESERVED,
        USING,
        RECOVERING
    }

    private AlchemyReservation() {
    }

    public static boolean hasPending(ItemStack upgrade) {
        CompoundTag root = root(upgrade);
        return root != null && root.contains(STATE);
    }

    public static Optional<State> state(ItemStack upgrade) {
        CompoundTag root = root(upgrade);
        if (root == null || !root.contains(STATE)) {
            return Optional.empty();
        }
        try {
            return Optional.of(State.valueOf(root.getString(STATE)));
        } catch (IllegalArgumentException ignored) {
            return Optional.of(State.RECOVERING);
        }
    }

    public static ItemStack getStack(ItemStack upgrade, HolderLookup.Provider registries) {
        CompoundTag root = root(upgrade);
        if (root == null || !root.contains(STACK, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(root.getCompound(STACK));
    }

    public static boolean isRemote(ItemStack upgrade) {
        CompoundTag root = root(upgrade);
        return root != null && root.getBoolean(REMOTE);
    }

    public static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return false;
        }
        CompoundTag reservation = new CompoundTag();
        reservation.putString(STATE, State.RESERVED.name());
        reservation.putBoolean(REMOTE, remote);
        reservation.put(STACK, stack.save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
        return true;
    }

    public static void updateStack(ItemStack upgrade, ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag reservation = root(upgrade);
        if (reservation == null) return;
        if (stack.isEmpty()) reservation.remove(STACK);
        else reservation.put(STACK, stack.save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
    }

    public static void setState(ItemStack upgrade, State state) {
        CompoundTag reservation = root(upgrade);
        if (reservation != null) { reservation.putString(STATE, state.name()); ForgeUpgradeNbt.write(upgrade, ROOT, reservation); }
    }

    public static void clear(ItemStack upgrade) {
        ForgeUpgradeNbt.clear(upgrade, ROOT);
    }

    private static CompoundTag root(ItemStack upgrade) {
        return ForgeUpgradeNbt.read(upgrade, ROOT);
    }
}
