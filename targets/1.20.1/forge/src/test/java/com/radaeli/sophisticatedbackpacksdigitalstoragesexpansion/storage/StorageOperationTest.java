package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageOperationTest {
    @Test
    void exposesDistinctInsertAndExtractPermissionsForBackendAdapters() {
        assertEquals(2, StorageOperation.values().length);
        assertEquals(StorageOperation.INSERT, StorageOperation.valueOf("INSERT"));
        assertEquals(StorageOperation.EXTRACT, StorageOperation.valueOf("EXTRACT"));
    }
}
