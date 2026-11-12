package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageConnection.Status;

public final class StorageAdapterRegistry {
    private StorageAdapterRegistry() {
    }

    private static final class InstalledAdapters {
        private static final Map<String, StorageAdapter> ALL = create();

        private static Map<String, StorageAdapter> create() {
            Map<String, StorageAdapter> adapters = new LinkedHashMap<>();
            // Keep construction inside each loaded-mod branch: optional classes must not load eagerly.
            // SCC's AE2-compatible antenna also implements IWirelessAccessPoint.
            // Identify its native backend before the generic AE2 interface matches it.
            if (ModList.get().isLoaded("scc")) {
                register(adapters, new SccStorageAdapter());
            }
            if (ModList.get().isLoaded("ae2")) {
                register(adapters, new Ae2StorageAdapter());
            }
            if (ModList.get().isLoaded("refinedstorage")) {
                register(adapters, new RefinedStorageAdapter());
            }
            if (ModList.get().isLoaded("toms_storage")) {
                register(adapters, new TomsStorageAdapter());
            }
            return Collections.unmodifiableMap(adapters);
        }

        private static void register(Map<String, StorageAdapter> adapters, StorageAdapter adapter) {
            if (adapters.putIfAbsent(adapter.backendId(), adapter) != null) {
                throw new IllegalStateException("Duplicate storage backend: " + adapter.backendId());
            }
        }
    }

    public static Set<String> installedBackendIds() {
        return InstalledAdapters.ALL.keySet();
    }

    public static Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        if (level.isClientSide || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        for (StorageAdapter adapter : InstalledAdapters.ALL.values()) {
            Optional<StorageEndpoint> endpoint = adapter.identify(level, pos);
            if (endpoint.isPresent()) {
                return endpoint;
            }
        }
        return Optional.empty();
    }

    public static StorageConnection connect(ServerPlayer player, Optional<StorageEndpoint> endpoint, SourceLocation source) {
        return endpoint.map(value -> connect(player, value, source))
                .orElseGet(() -> StorageConnection.unavailable(Status.NOT_LINKED));
    }

    public static StorageConnection connect(ServerPlayer player, Optional<StorageEndpoint> endpoint, SourceLocation source,
            StorageOperation operation) {
        return endpoint.map(value -> connect(player, value, source, operation))
                .orElseGet(() -> StorageConnection.unavailable(Status.NOT_LINKED));
    }

    public static StorageConnection connect(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source) {
        return connect(player, endpoint, source, StorageOperation.INSERT);
    }

    public static StorageConnection connect(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source,
            StorageOperation operation) {
        return connect(player, endpoint, source, operation, false);
    }

    public static StorageConnection connectForBinding(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source) {
        return connect(player, endpoint, source, StorageOperation.INSERT, true);
    }

    private static StorageConnection connect(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source, StorageOperation operation, boolean binding) {
        StorageAdapter adapter = InstalledAdapters.ALL.get(endpoint.backendId());
        if (adapter == null) {
            return StorageConnection.unavailable(Status.BACKEND_MISSING);
        }
        ServerLevel level = player.server.getLevel(endpoint.dimension());
        if (level == null) {
            return StorageConnection.unavailable(Status.DIMENSION_UNAVAILABLE);
        }
        if (!level.isLoaded(endpoint.pos())) {
            return StorageConnection.unavailable(Status.ENDPOINT_UNLOADED);
        }
        // Check protection in the endpoint's dimension, including native cross-dimension networks.
        if (!level.mayInteract(player, endpoint.pos())) {
            return StorageConnection.unavailable(Status.ACCESS_DENIED);
        }
        Optional<StorageTarget> target = adapter.resolve(level, endpoint, source);
        if (target.isEmpty()) {
            return StorageConnection.unavailable(Status.ENDPOINT_MISSING);
        }
        return target.filter(value -> (binding ? value.canBind(player, source) : value.canAccess(player, source, operation)))
                .map(StorageConnection::connected)
                .orElseGet(() -> StorageConnection.unavailable(Status.UNAVAILABLE));
    }
}
