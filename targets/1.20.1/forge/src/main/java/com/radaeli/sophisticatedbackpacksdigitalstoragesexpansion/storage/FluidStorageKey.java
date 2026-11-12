package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Immutable fluid identity for Pump queries. Amounts are always expressed in mB. */
public record FluidStorageKey(Fluid fluid, CompoundTag tag) {
    public FluidStorageKey {
        Objects.requireNonNull(fluid, "fluid");
        tag = tag == null ? new CompoundTag() : tag.copy();
    }

    public static FluidStorageKey of(FluidStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("An empty fluid stack has no storage identity");
        }
        return new FluidStorageKey(stack.getFluid(), stack.hasTag() ? stack.getTag() : new CompoundTag());
    }

    public FluidStack toStack(int amountMb) {
        if (amountMb <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amountMb, tag.copy());
    }
}
