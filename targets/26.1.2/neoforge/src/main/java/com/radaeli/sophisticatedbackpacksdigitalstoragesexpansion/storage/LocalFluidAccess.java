package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Synchronous boundary for a native fluid capability; no transaction encloses backend calls. */
public record LocalFluidAccess(ResourceHandler<FluidResource> handler) implements IFluidHandler {
    public static LocalFluidAccess of(ResourceHandler<FluidResource> handler) { return handler == null ? null : new LocalFluidAccess(handler); }
    @Override public int getTanks() { return handler.size(); }
    @Override public FluidStack getFluidInTank(int index) { return handler.getResource(index).toStack(handler.getAmountAsInt(index)); }
    @Override public int getTankCapacity(int index) { return handler.getCapacityAsInt(index, handler.getResource(index)); }
    @Override public boolean isFluidValid(int index, FluidStack stack) { return handler.isValid(index, FluidResource.of(stack)); }
    @Override public int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = handler.insert(FluidResource.of(stack), stack.getAmount(), tx);
            if (action.execute()) tx.commit();
            return accepted;
        }
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(stack), stack.getAmount(), tx);
            if (action.execute()) tx.commit();
            return stack.copyWithAmount(extracted);
        }
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        for (int index = 0; index < handler.size(); index++) {
            FluidStack stack = getFluidInTank(index);
            if (!stack.isEmpty()) {
                FluidStack drained = drain(stack.copyWithAmount(amount), action);
                if (!drained.isEmpty()) return drained;
            }
        }
        return FluidStack.EMPTY;
    }
}
