package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Set;

import net.minecraftforge.fml.ModList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StorageAdapterRegistryTest {
    @Test
    public void optionalAdaptersStayUnavailableInTheBaseGameTestProfile() {
        assertTrue(ModList.get().isLoaded("sophisticatedcore"));
        assertTrue(ModList.get().isLoaded("sophisticatedbackpacks"));
        var expected = new java.util.HashSet<>(java.util.List.of(System.getProperty("digitalstorage.expectedBackends", "").split(",")));
        expected.remove("");
        assertEquals(expected, StorageAdapterRegistry.installedBackendIds());
        assertThrows(UnsupportedOperationException.class,
                () -> StorageAdapterRegistry.installedBackendIds().add("ae2"));
    }

    @Test
    public void connectionResultsCannotClaimConnectivityWithoutATarget() {
        assertThrows(IllegalArgumentException.class,
                () -> StorageConnection.unavailable(StorageConnection.Status.CONNECTED));
        StorageConnection missing = StorageConnection.unavailable(StorageConnection.Status.BACKEND_MISSING);
        assertTrue(missing.target().isEmpty());
        assertEquals(StorageConnection.Status.BACKEND_MISSING, missing.status());
    }
}
