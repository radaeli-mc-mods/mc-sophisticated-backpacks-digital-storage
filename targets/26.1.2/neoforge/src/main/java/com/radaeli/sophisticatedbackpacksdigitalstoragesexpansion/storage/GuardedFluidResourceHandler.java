package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.UUID;
import java.util.function.Supplier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Consults the live guard for every operation, including cached native capabilities. */
public record GuardedFluidResourceHandler(ResourceHandler<FluidResource> delegate,
        Supplier<UUID> contentsId) implements ResourceHandler<FluidResource> {
    private boolean blocked() { return FluidTransferGuard.isBlocked(contentsId.get()); }
    @Override public int size() { return blocked() ? 0 : delegate.size(); }
    @Override public FluidResource getResource(int index) { return blocked() ? FluidResource.EMPTY : delegate.getResource(index); }
    @Override public long getAmountAsLong(int index) { return blocked() ? 0 : delegate.getAmountAsLong(index); }
    @Override public long getCapacityAsLong(int index, FluidResource resource) { return blocked() ? 0 : delegate.getCapacityAsLong(index, resource); }
    @Override public boolean isValid(int index, FluidResource resource) { return !blocked() && delegate.isValid(index, resource); }
    @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) {
        return blocked() ? 0 : delegate.insert(index, resource, amount, tx);
    }
    @Override public int extract(int index, FluidResource resource, int amount, TransactionContext tx) {
        return blocked() ? 0 : delegate.extract(index, resource, amount, tx);
    }
}
