package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;

/** A query result that preserves the exact fluid components and reports mB. */
public record FluidStorageAmount(FluidStorageKey key, long amountMb) {
    public FluidStorageAmount {
        Objects.requireNonNull(key, "key");
        if (amountMb < 0) {
            throw new IllegalArgumentException("Fluid amount cannot be negative");
        }
    }
}
