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
        assertFalse(ModList.get().isLoaded("ae2"));
        assertFalse(ModList.get().isLoaded("refinedstorage"));
        assertFalse(ModList.get().isLoaded("toms_storage"));
        assertEquals(Set.of(), StorageAdapterRegistry.installedBackendIds());
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
