package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Set;

import net.neoforged.fml.ModList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StorageAdapterRegistryTest extends MinecraftStorageTest {
    @Test
    void onlyTheRequestedOptionalAdaptersAreLoaded() {
        assertTrue(ModList.get().isLoaded("sophisticatedcore"));
        assertTrue(ModList.get().isLoaded("sophisticatedbackpacks"));
        Set<String> expected = switch (System.getProperty("digitalstorage.backendProfile", "none")) {
            case "none" -> Set.of(); case "ae2" -> Set.of("ae2"); case "rs" -> Set.of("refinedstorage");
            case "toms" -> Set.of("toms_storage"); case "scc" -> Set.of("scc");
            case "all" -> Set.of("ae2", "refinedstorage", "toms_storage", "scc");
            default -> throw new IllegalArgumentException("Unknown backend profile");
        };
        for (String backend : Set.of("ae2", "refinedstorage", "toms_storage", "scc")) {
            assertEquals(expected.contains(backend), ModList.get().isLoaded(backend), backend);
        }
        assertEquals(expected.contains("ae2"), ModList.get().isLoaded("guideme"), "GuideME must follow the AE2 profile");
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
