package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;
import java.util.Optional;

/** Explicit result of validating a fluid-capable linked endpoint for one operation. */
public record FluidStorageConnection(Status status, Optional<FluidStorageTarget> target) {
    public FluidStorageConnection {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(target, "target");
        if ((status == Status.CONNECTED) != target.isPresent()) {
            throw new IllegalArgumentException("Only a connected fluid result may contain a target");
        }
    }

    public static FluidStorageConnection connected(FluidStorageTarget target) {
        return new FluidStorageConnection(Status.CONNECTED, Optional.of(target));
    }

    public static FluidStorageConnection unavailable(Status status) {
        return new FluidStorageConnection(status, Optional.empty());
    }

    public enum Status {
        CONNECTED,
        NOT_LINKED,
        BACKEND_MISSING,
        UNSUPPORTED_FLUIDS,
        DIMENSION_UNAVAILABLE,
        ENDPOINT_UNLOADED,
        ENDPOINT_MISSING,
        ACCESS_DENIED,
        UNAVAILABLE
    }
}
