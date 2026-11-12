package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Lazy optional fluid adapter registry. Common Pump code can link any existing
 * StorageEndpoint without loading optional API classes when a backend is absent.
 */
public final class FluidStorageAdapterRegistry {
    private FluidStorageAdapterRegistry() {
    }

    private static final class InstalledAdapters {
        private static final Map<String, FluidStorageAdapter> ALL = create();

        private static Map<String, FluidStorageAdapter> create() {
            Map<String, FluidStorageAdapter> adapters = new LinkedHashMap<>();
            if (ModList.get().isLoaded("ae2")) {
                register(adapters, new Ae2FluidStorageAdapter());
            }
            if (ModList.get().isLoaded("refinedstorage")) {
                register(adapters, new RefinedStorageFluidStorageAdapter());
            }
            if (ModList.get().isLoaded("scc")) register(adapters, new SccFluidStorageAdapter());
            return Collections.unmodifiableMap(adapters);
        }

        private static void register(Map<String, FluidStorageAdapter> adapters, FluidStorageAdapter adapter) {
            if (adapters.putIfAbsent(adapter.backendId(), adapter) != null) {
                throw new IllegalStateException("Duplicate fluid storage backend: " + adapter.backendId());
            }
        }
    }

    public static FluidStorageConnection connect(ServerPlayer player, Optional<StorageEndpoint> endpoint,
            SourceLocation source, FluidStorageAccess access) {
        return endpoint.map(value -> connect(player, value, source, access))
                .orElseGet(() -> FluidStorageConnection.unavailable(FluidStorageConnection.Status.NOT_LINKED));
    }

    public static FluidStorageConnection connect(ServerPlayer player, StorageEndpoint endpoint,
            SourceLocation source, FluidStorageAccess access) {
        FluidStorageAdapter adapter = InstalledAdapters.ALL.get(endpoint.backendId());
        if (adapter == null) {
            FluidStorageConnection.Status status = "toms_storage".equals(endpoint.backendId())
                    ? FluidStorageConnection.Status.UNSUPPORTED_FLUIDS
                    : FluidStorageConnection.Status.BACKEND_MISSING;
            return FluidStorageConnection.unavailable(status);
        }

        ServerLevel level = player.server.getLevel(endpoint.dimension());
        if (level == null) {
            return FluidStorageConnection.unavailable(FluidStorageConnection.Status.DIMENSION_UNAVAILABLE);
        }
        if (!level.isLoaded(endpoint.pos())) {
            return FluidStorageConnection.unavailable(FluidStorageConnection.Status.ENDPOINT_UNLOADED);
        }
        if (!level.mayInteract(player, endpoint.pos())) {
            return FluidStorageConnection.unavailable(FluidStorageConnection.Status.ACCESS_DENIED);
        }
        Optional<FluidStorageTarget> target = adapter.resolve(level, endpoint, source);
        if (target.isEmpty()) {
            return FluidStorageConnection.unavailable(FluidStorageConnection.Status.ENDPOINT_MISSING);
        }
        return target.filter(value -> value.isReachable(player, source, access))
                .map(FluidStorageConnection::connected)
                .orElseGet(() -> FluidStorageConnection.unavailable(FluidStorageConnection.Status.UNAVAILABLE));
    }
}
