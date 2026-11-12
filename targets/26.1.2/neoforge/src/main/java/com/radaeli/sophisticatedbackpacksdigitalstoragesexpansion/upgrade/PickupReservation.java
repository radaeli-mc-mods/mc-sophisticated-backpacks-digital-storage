package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class PickupReservation {
    private static final String KEY = "remote_pickup_reservation";
    private static final String UNCERTAIN = "remote_pickup_uncertain";
    static boolean uncertain(ItemStack upgrade) {
        return upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(UNCERTAIN, false);
    }
    static void markUncertain(ItemStack upgrade) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> data.putBoolean(UNCERTAIN, true));
    }
    static boolean hasPending(ItemStack upgrade) {
        return upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains(KEY);
    }
    static ItemStack read(ItemStack upgrade, HolderLookup.Provider registries) {
        var tag = upgrade.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(KEY)) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), tag.get(KEY))
                .getOrThrow(message -> new IllegalStateException("Invalid persisted pickup reservation: " + message));
    }
    static void write(ItemStack upgrade, ItemStack pending, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            // A settled remainder is known. An interrupted in-flight delivery keeps its marker
            // through serialization until a known result is explicitly recorded.
            data.remove(UNCERTAIN);
            if (pending.isEmpty()) { data.remove(KEY); }
            else data.put(KEY, ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), pending)
                    .getOrThrow(message -> new IllegalStateException("Could not persist pickup reservation: " + message)));
        });
    }
}
