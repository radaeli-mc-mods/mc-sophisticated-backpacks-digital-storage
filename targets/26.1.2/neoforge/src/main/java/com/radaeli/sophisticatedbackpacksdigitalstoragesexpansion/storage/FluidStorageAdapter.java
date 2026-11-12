package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;

/** Optional backend implementation. Keep imports of backend APIs inside implementations. */
public interface FluidStorageAdapter {
    String backendId();

    Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint);

    default Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        return resolve(level, endpoint);
    }
}
