package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Immutable fluid identity for Pump queries. Amounts are always expressed in mB. */
public record FluidStorageKey(Fluid fluid, DataComponentPatch components) {
    public FluidStorageKey {
        Objects.requireNonNull(fluid, "fluid");
        components = Objects.requireNonNull(components, "components");
    }

    public static FluidStorageKey of(FluidStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("An empty fluid stack has no storage identity");
        }
        return new FluidStorageKey(stack.getFluid(), stack.getComponentsPatch());
    }

    public FluidStack toStack(int amountMb) {
        if (amountMb <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack stack = new FluidStack(fluid, amountMb);
        stack.applyComponents(components);
        return stack;
    }
}
