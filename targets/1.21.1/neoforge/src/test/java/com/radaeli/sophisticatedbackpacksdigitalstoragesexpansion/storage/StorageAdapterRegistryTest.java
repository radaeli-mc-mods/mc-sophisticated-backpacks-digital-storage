package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Set;

import net.neoforged.fml.ModList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageAdapterRegistryTest extends MinecraftStorageTest {
    @Test
    void optionalAdaptersRemainUnloadedWhenOnlySophisticatedIsInstalled() {
        assertTrue(ModList.get().isLoaded("sophisticatedcore"));
        assertTrue(ModList.get().isLoaded("sophisticatedbackpacks"));
        String profile = System.getProperty("digitalstorage.backendProfile", "legacy");
        var expected = new java.util.HashSet<String>();
        for (String id : java.util.List.of("ae2", "refinedstorage", "toms_storage", "scc")) {
            if (ModList.get().isLoaded(id)) expected.add(id);
        }
        if (ModList.get().isLoaded("integrateddynamics") && ModList.get().isLoaded("integratedtunnels")) expected.add("integrateddynamics");
        if (profile.equals("legacy") || profile.equals("none") || profile.equals("dynamics")) assertEquals(Set.of(), expected);
        assertEquals(expected, StorageAdapterRegistry.installedBackendIds());
        assertThrows(UnsupportedOperationException.class, () -> StorageAdapterRegistry.installedBackendIds().add("ae2"));
    }

    @Test
    void connectionResultsCannotClaimConnectivityWithoutATarget() {
        assertThrows(IllegalArgumentException.class,
                () -> StorageConnection.unavailable(StorageConnection.Status.CONNECTED));
        StorageConnection missing = StorageConnection.unavailable(StorageConnection.Status.BACKEND_MISSING);
        assertTrue(missing.target().isEmpty());
        assertEquals(StorageConnection.Status.BACKEND_MISSING, missing.status());
    }
}
