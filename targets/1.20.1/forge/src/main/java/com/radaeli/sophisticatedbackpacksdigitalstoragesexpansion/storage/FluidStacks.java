package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraftforge.fluids.FluidStack;

/** Small Forge 1.20 helper matching the amount-copy operation used by newer Minecraft versions. */
public final class FluidStacks {
    private FluidStacks() { }

    public static FluidStack copyWithAmount(FluidStack source, int amount) {
        if (source.isEmpty() || amount <= 0) return FluidStack.EMPTY;
        FluidStack copy = source.copy();
        copy.setAmount(amount);
        return copy;
    }
}
