package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Checks the guard on use, including references cached by pipes and storage buses. */
public final class GuardedFluidHandler implements IFluidHandler {
    private final IFluidHandler delegate;
    private final Supplier<UUID> contentsId;

    public GuardedFluidHandler(IFluidHandler delegate, Supplier<UUID> contentsId) {
        this.delegate = delegate;
        this.contentsId = contentsId;
    }

    private boolean blocked() { return FluidTransferGuard.isBlocked(contentsId.get()); }

    @Override public int getTanks() { return blocked() ? 0 : delegate.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) {
        return blocked() ? FluidStack.EMPTY : delegate.getFluidInTank(tank);
    }
    @Override public int getTankCapacity(int tank) { return blocked() ? 0 : delegate.getTankCapacity(tank); }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return !blocked() && delegate.isFluidValid(tank, stack);
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        return blocked() ? 0 : delegate.fill(resource, action);
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        return blocked() ? FluidStack.EMPTY : delegate.drain(resource, action);
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        return blocked() ? FluidStack.EMPTY : delegate.drain(maxDrain, action);
    }
}
