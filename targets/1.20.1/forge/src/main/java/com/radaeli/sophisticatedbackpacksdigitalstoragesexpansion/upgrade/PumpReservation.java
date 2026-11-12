package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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
        return FluidStack.loadFluidStackFromNBT(root.getCompound(FLUID));
    }

    public static boolean canEncode(FluidStack fluid, HolderLookup.Provider registries) {
        if (fluid.isEmpty() || fluid.getAmount() > 1_000) {
            return false;
        }
        CompoundTag encoded = fluid.writeToNBT(new CompoundTag());
        // Forge 1.20.1 writes the fluid registry ID under "FluidName".
        // NeoForge uses its own version-specific representation.
        FluidStack decoded = FluidStack.loadFluidStackFromNBT(encoded);
        return encoded.contains("FluidName", Tag.TAG_STRING) && !decoded.isEmpty()
                && decoded.isFluidEqual(fluid) && decoded.getAmount() == fluid.getAmount();
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
        CompoundTag reservation = new CompoundTag();
        reservation.putString(STATE, State.RESERVED.name());
        reservation.putBoolean(REMOTE_ORIGIN, remoteOrigin);
        reservation.put(FLUID, fluid.writeToNBT(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
    }

    public static void updateFluid(ItemStack upgrade, FluidStack fluid, HolderLookup.Provider registries) {
        CompoundTag reservation = readRoot(upgrade);
        if (reservation == null) return;
        if (fluid.isEmpty()) reservation.remove(FLUID);
        else reservation.put(FLUID, fluid.writeToNBT(new CompoundTag()));
        ForgeUpgradeNbt.write(upgrade, ROOT, reservation);
    }

    public static void setState(ItemStack upgrade, State state) {
        CompoundTag reservation = readRoot(upgrade);
        if (reservation != null) { reservation.putString(STATE, state.name()); ForgeUpgradeNbt.write(upgrade, ROOT, reservation); }
    }

    public static void clear(ItemStack upgrade) {
        ForgeUpgradeNbt.clear(upgrade, ROOT);
    }

    private static CompoundTag readRoot(ItemStack upgrade) {
        return ForgeUpgradeNbt.read(upgrade, ROOT);
    }
}
