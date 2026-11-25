package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

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
            @Override public void checkpoint(ItemStack remainder) {
                updateStack(upgrade, remainder, registries);
                setState(upgrade, State.RECOVERING);
                save.run();
            }
            @Override public void beginDelivery(String stage) {
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
        ItemStack identity = ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), data.get(STACK)).result().orElse(ItemStack.EMPTY);
        int amount = data.contains("amount") ? data.getInt("amount") : identity.getCount();
        if (!identity.isEmpty() && amount <= 0) throw new IllegalStateException("Invalid persisted consumption quantity");
        return identity.isEmpty() ? ItemStack.EMPTY : identity.copyWithCount(amount);
    }

    public static boolean isRemote(ItemStack upgrade) {
        CompoundTag root = root(upgrade);
        return root != null && root.getBoolean(REMOTE);
    }

    public static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, HolderLookup.Provider registries) {
        var encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copyWithCount(1)).result();
        if (encoded.isEmpty()) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = new CompoundTag();
            reservation.putString(STATE, State.RESERVED.name());
            reservation.putInt("amount", stack.getCount());
            reservation.putBoolean(REMOTE, remote);
            reservation.put(STACK, (Tag) encoded.get());
            data.put(ROOT, reservation);
        });
        return true;
    }

    public static void updateStack(ItemStack upgrade, ItemStack stack, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (reservation.isEmpty()) {
                return;
            }
            if (stack.isEmpty()) {
                reservation.remove(STACK);
                reservation.remove("amount");
            } else {
                ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copyWithCount(1))
                        .result().ifPresent(encoded -> reservation.put(STACK, (Tag) encoded));
            }
            if (!stack.isEmpty()) reservation.putInt("amount", stack.getCount());
            data.put(ROOT, reservation);
        });
    }

    public static void setState(ItemStack upgrade, State state) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (reservation.isEmpty()) {
                return;
            }
            reservation.putString(STATE, state.name());
            data.put(ROOT, reservation);
        });
    }

    public static void clear(ItemStack upgrade) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> data.remove(ROOT));
    }

    private static CompoundTag root(ItemStack upgrade) {
        CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(ROOT, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT) : null;
    }
}
