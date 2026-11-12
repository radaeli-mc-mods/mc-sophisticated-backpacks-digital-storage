package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

interface LinkedUpgradeInventory {
    IStorageWrapper getStorageWrapper();

    Optional<StorageEndpoint> getEndpoint();
}
