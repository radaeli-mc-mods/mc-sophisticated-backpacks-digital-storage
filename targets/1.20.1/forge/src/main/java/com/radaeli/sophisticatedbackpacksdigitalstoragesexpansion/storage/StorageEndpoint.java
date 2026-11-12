package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record StorageEndpoint(String backendId, ResourceKey<Level> dimension, BlockPos pos) {
    public StorageEndpoint {
        if (Objects.requireNonNull(backendId).isBlank()) {
            throw new IllegalArgumentException("A storage endpoint needs a backend id");
        }
        Objects.requireNonNull(dimension);
        pos = Objects.requireNonNull(pos).immutable();
    }
}
