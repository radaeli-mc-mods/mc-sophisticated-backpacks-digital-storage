package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** One optional backend. Instances are created only when their mod is installed. */
public interface StorageAdapter {
    String backendId();

    Optional<StorageEndpoint> identify(Level level, BlockPos pos);

    /** Binding interaction, including the exact multipart hit and the player's origin. */
    default Optional<StorageEndpoint> identify(Level level, BlockPos pos,
            net.minecraft.world.phys.BlockHitResult hit, SourceLocation source) {
        return identify(level, pos);
    }

    Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint);

    default Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        return resolve(level, endpoint);
    }
}
