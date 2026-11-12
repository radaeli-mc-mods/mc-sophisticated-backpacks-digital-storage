package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A transient, operation-scoped view of a backend fluid store. Implementations must
 * resolve the live network on every call and must not be persisted by an upgrade.
 */
public interface FluidStorageTarget {
    StorageEndpoint endpoint();

    boolean isReachable(ServerPlayer player, SourceLocation source, FluidStorageAccess access);

    List<FluidStorageAmount> getAvailableFluids(ServerPlayer player);

    /** Returns the amount that would be accepted, in mB, without mutating storage. */
    long simulateInsert(FluidStack stack, ServerPlayer player);

    /** Inserts the real stack and returns the unaccepted remainder. */
    FluidStack insert(FluidStack stack, ServerPlayer player);

    /** Returns the amount that would be extracted, in mB, without mutating storage. */
    long simulateExtract(FluidStorageKey key, long amountMb, ServerPlayer player);

    /** Extracts the real amount and returns the actual fluid stack. */
    FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player);
}
