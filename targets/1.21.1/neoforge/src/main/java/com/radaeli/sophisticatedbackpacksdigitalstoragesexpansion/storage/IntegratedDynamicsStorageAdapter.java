package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Phase 1 binding foundation. Resource operations remain closed until phase 2. */
public final class IntegratedDynamicsStorageAdapter implements StorageAdapter {
    public String backendId() { return "integrateddynamics"; }
    public Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        return IntegratedDynamicsLinkDiscovery.identify(level, pos);
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        return resolve(level, endpoint, null);
    }
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        if (!backendId().equals(endpoint.backendId()) || !level.dimension().equals(endpoint.dimension())
                || !level.isLoaded(endpoint.pos())) return Optional.empty();
        return Optional.of(new BindingTarget(endpoint, source));
    }
    private record BindingTarget(StorageEndpoint endpoint, SourceLocation source) implements StorageTarget {
        public boolean canBind(ServerPlayer player, SourceLocation origin) {
            return IntegratedDynamicsAccessPolicy.resolve(player, endpoint, origin).isPresent();
        }
        public boolean isReachable(ServerPlayer player, SourceLocation origin) { return canBind(player, origin); }
        public boolean canAccess(ServerPlayer player, SourceLocation origin, StorageOperation operation) { return false; }
        public IItemHandler getAvailableItems(ServerPlayer player) { return new SnapshotItemHandler(List.of()); }
        public int simulateInsert(ItemStack stack, ServerPlayer player) { return 0; }
        public ItemStack insert(ItemStack stack, ServerPlayer player) { return stack.copy(); }
    }
}
