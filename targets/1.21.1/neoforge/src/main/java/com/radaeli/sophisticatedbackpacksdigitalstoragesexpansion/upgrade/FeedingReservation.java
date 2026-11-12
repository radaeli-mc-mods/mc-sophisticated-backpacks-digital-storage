package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

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

    static ItemStack stack(ItemStack upgrade, HolderLookup.Provider registries) {
        CompoundTag data = getRoot(upgrade);
        if (data == null || !data.contains(STACK, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), data.get(STACK))
                .result().orElse(ItemStack.EMPTY);
    }

    static boolean remote(ItemStack upgrade) {
        CompoundTag data = getRoot(upgrade);
        return data != null && data.getBoolean(REMOTE);
    }

    static boolean reserve(ItemStack upgrade, ItemStack stack, boolean remote, HolderLookup.Provider registries) {
        var encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copy()).result();
        if (encoded.isEmpty()) return false;
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = new CompoundTag();
            reservation.putString(STATE, State.RESERVED.name());
            reservation.putBoolean(REMOTE, remote);
            reservation.put(STACK, (Tag) encoded.get());
            data.put(ROOT, reservation);
        });
        return true;
    }

    static void updateStack(ItemStack upgrade, ItemStack stack, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (reservation.isEmpty()) return;
            if (stack.isEmpty()) {
                reservation.remove(STACK);
            } else {
                ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), stack.copy())
                        .result().ifPresent(encoded -> reservation.put(STACK, (Tag) encoded));
            }
            data.put(ROOT, reservation);
        });
    }

    static void setState(ItemStack upgrade, State state) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (!reservation.isEmpty()) {
                reservation.putString(STATE, state.name());
                data.put(ROOT, reservation);
            }
        });
    }

    static void clear(ItemStack upgrade) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> data.remove(ROOT));
    }

    private static CompoundTag getRoot(ItemStack upgrade) {
        CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        return tag.contains(ROOT, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT) : null;
    }
}
