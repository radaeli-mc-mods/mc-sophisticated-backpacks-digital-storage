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
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), root.get(STACK))
                .result().orElse(ItemStack.EMPTY);
    }

    public static boolean isRemote(ItemStack upgrade) {
        CompoundTag root = root(upgrade);
        return root != null && root.getBoolean(REMOTE);
    }

    public static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, HolderLookup.Provider registries) {
        var encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copy()).result();
        if (encoded.isEmpty()) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = new CompoundTag();
            reservation.putString(STATE, State.RESERVED.name());
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
            } else {
                ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copy())
                        .result().ifPresent(encoded -> reservation.put(STACK, (Tag) encoded));
            }
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
