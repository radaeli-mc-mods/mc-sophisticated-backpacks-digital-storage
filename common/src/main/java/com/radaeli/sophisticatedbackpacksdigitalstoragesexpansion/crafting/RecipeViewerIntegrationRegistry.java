package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Optional-free registry for client recipe-viewer adapters. This is an internal addon contract. */
public final class RecipeViewerIntegrationRegistry {
    private static final Map<String, RecipeViewerAdapter> ADAPTERS = new LinkedHashMap<>();

    private RecipeViewerIntegrationRegistry() {
    }

    public static synchronized void register(RecipeViewerAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ADAPTERS.put(adapter.id(), adapter);
    }

    public static synchronized void unregister(String adapterId) {
        ADAPTERS.remove(adapterId);
    }

    public static boolean isReadyFor(Object menu) {
        if (menu == null) {
            return false;
        }
        RecipeViewerAdapter[] adapters;
        synchronized (RecipeViewerIntegrationRegistry.class) {
            adapters = ADAPTERS.values().toArray(RecipeViewerAdapter[]::new);
        }
        for (RecipeViewerAdapter adapter : adapters) {
            if (adapter.isReadyFor(menu)) {
                return true;
            }
        }
        return false;
    }

    static synchronized void clearForTests() {
        ADAPTERS.clear();
    }
}
