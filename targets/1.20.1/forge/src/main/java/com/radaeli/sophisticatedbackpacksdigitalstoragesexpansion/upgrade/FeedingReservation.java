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

    enum State { RESERVED, USING, RECOVERING, IN_FLIGHT }

    static com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal<ItemStack> journal(ItemStack upgrade,
            net.minecraft.core.HolderLookup.Provider registries, Runnable save) {
        return new com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal<>() {
            private String deliveryStage = "recovery";
            @Override public String description() { return com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.DeliveryDiagnostics.describe(upgrade, deliveryStage); }
            @Override public void checkpoint(ItemStack remainder) {
                updateStack(upgrade, remainder, registries);
                setState(upgrade, State.RECOVERING);
                save.run();
            }
            @Override public void beginDelivery(String stage) {
                deliveryStage = stage;
                setState(upgrade, State.IN_FLIGHT);
                save.run();
            }
        };
    }

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
        if (data == null || !data.contains(STACK)) return ItemStack.EMPTY;
        ItemStack identity = ItemStack.of(data.getCompound(STACK));
        int amount = data.contains("amount") ? data.getInt("amount") : identity.getCount();
        if (!identity.isEmpty() && amount <= 0) throw new IllegalStateException("Invalid persisted consumption quantity");
        return identity.isEmpty() ? ItemStack.EMPTY : identity.copyWithCount(amount);
    }

    static boolean remote(ItemStack upgrade) {
        CompoundTag data = getRoot(upgrade);
        return data != null && data.getBoolean(REMOTE);
    }

    static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, net.minecraft.core.HolderLookup.Provider registries) {
        if (stack.isEmpty()) return false;
        CompoundTag reservation = new CompoundTag();
        reservation.putString(STATE, State.RESERVED.name());
            reservation.putInt("amount", stack.getCount());
        reservation.putBoolean(REMOTE, remote);
        reservation.put(STACK, stack.copyWithCount(1).save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
        return true;
    }

    static void updateStack(ItemStack upgrade, ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag reservation = getRoot(upgrade);
        if (reservation == null) return;
        if (stack.isEmpty()) { reservation.remove(STACK); reservation.remove("amount"); }
        else reservation.put(STACK, stack.copyWithCount(1).save(new CompoundTag()));
        if (!stack.isEmpty()) reservation.putInt("amount", stack.getCount());
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
