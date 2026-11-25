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
        RECOVERING,
        IN_FLIGHT
    }

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

    public static ItemStack getStack(ItemStack upgrade, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag data = root(upgrade);
        if (data == null || !data.contains(STACK)) return ItemStack.EMPTY;
        ItemStack identity = ItemStack.of(data.getCompound(STACK));
        int amount = data.contains("amount") ? data.getInt("amount") : identity.getCount();
        if (!identity.isEmpty() && amount <= 0) throw new IllegalStateException("Invalid persisted consumption quantity");
        return identity.isEmpty() ? ItemStack.EMPTY : identity.copyWithCount(amount);
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
            reservation.putInt("amount", stack.getCount());
        reservation.putBoolean(REMOTE, remote);
        reservation.put(STACK, stack.copyWithCount(1).save(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
        return true;
    }

    public static void updateStack(ItemStack upgrade, ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag reservation = root(upgrade);
        if (reservation == null) return;
        if (stack.isEmpty()) { reservation.remove(STACK); reservation.remove("amount"); }
        else reservation.put(STACK, stack.copyWithCount(1).save(new CompoundTag()));
        if (!stack.isEmpty()) reservation.putInt("amount", stack.getCount());
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
