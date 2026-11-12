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
import net.neoforged.neoforge.fluids.FluidStack;

/** A fluid already removed from an origin and not yet safely delivered or recovered. */
public final class PumpReservation {
    private static final String ROOT = "remote_pump_reservation";
    private static final String STATE = "state";
    private static final String FLUID = "fluid";
    private static final String REMOTE_ORIGIN = "remote_origin";

    public enum State {
        RESERVED,
        RECOVERING
    }

    private PumpReservation() {
    }

    public static boolean hasPending(ItemStack upgrade) {
        CompoundTag root = readRoot(upgrade);
        return root != null && root.contains(STATE, Tag.TAG_STRING);
    }

    public static Optional<State> state(ItemStack upgrade) {
        CompoundTag root = readRoot(upgrade);
        if (root == null || !root.contains(STATE, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        try {
            return Optional.of(State.valueOf(root.getString(STATE)));
        } catch (IllegalArgumentException ignored) {
            return Optional.of(State.RECOVERING);
        }
    }

    public static FluidStack fluid(ItemStack upgrade, HolderLookup.Provider registries) {
        CompoundTag root = readRoot(upgrade);
        if (root == null || !root.contains(FLUID, Tag.TAG_COMPOUND)) {
            return FluidStack.EMPTY;
        }
        return FluidStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), root.get(FLUID))
                .result().orElse(FluidStack.EMPTY);
    }

    public static boolean canEncode(FluidStack fluid, HolderLookup.Provider registries) {
        return !fluid.isEmpty() && fluid.getAmount() <= 1_000
                && FluidStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), fluid.copy())
                        .result().isPresent();
    }

    public static boolean hasRemoteOrigin(ItemStack upgrade) {
        CompoundTag root = readRoot(upgrade);
        return root != null && root.getBoolean(REMOTE_ORIGIN);
    }

    public static void reserve(ItemStack upgrade, FluidStack fluid, boolean remoteOrigin,
            HolderLookup.Provider registries) {
        if (fluid.isEmpty() || fluid.getAmount() > 1_000) {
            throw new IllegalArgumentException("A Pump reservation must contain between 1 and 1,000 mB");
        }
        Tag encodedFluid = FluidStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), fluid.copy())
                .result().orElseThrow(() -> new IllegalStateException("Could not persist Pump fluid reservation"));
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = new CompoundTag();
            reservation.putString(STATE, State.RESERVED.name());
            reservation.putBoolean(REMOTE_ORIGIN, remoteOrigin);
            reservation.put(FLUID, encodedFluid.copy());
            data.put(ROOT, reservation);
        });
    }

    public static void updateFluid(ItemStack upgrade, FluidStack fluid, HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (reservation.isEmpty()) {
                return;
            }
            if (fluid.isEmpty()) {
                reservation.remove(FLUID);
            } else {
                Tag encodedFluid = FluidStack.CODEC
                        .encodeStart(RegistryOps.create(NbtOps.INSTANCE, registries), fluid.copy())
                        .result().orElseThrow(() -> new IllegalStateException("Could not update Pump fluid reservation"));
                reservation.put(FLUID, encodedFluid);
            }
            data.put(ROOT, reservation);
        });
    }

    public static void setState(ItemStack upgrade, State state) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> {
            CompoundTag reservation = data.getCompound(ROOT);
            if (!reservation.isEmpty()) {
                reservation.putString(STATE, state.name());
                data.put(ROOT, reservation);
            }
        });
    }

    public static void clear(ItemStack upgrade) {
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, data -> data.remove(ROOT));
    }

    private static CompoundTag readRoot(ItemStack upgrade) {
        CustomData customData = upgrade.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(ROOT, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT) : null;
    }
}
