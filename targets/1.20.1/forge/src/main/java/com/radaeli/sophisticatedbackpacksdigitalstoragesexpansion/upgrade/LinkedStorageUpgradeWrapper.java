package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

public abstract class LinkedStorageUpgradeWrapper<W extends IUpgradeWrapper, T extends LinkedStorageUpgradeItem<W>>
        extends UpgradeWrapperBase<W, T> implements IIOFilterUpgrade, LinkedUpgradeInventory {
    private final FilterLogic externalInputFilter;
    private final FilterLogic externalOutputFilter;

    protected LinkedStorageUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        // This filter is transient and has no configurable slots or serialized settings.
        externalInputFilter = new TransferInputFilter(storageWrapper, upgrade, saveHandler);
        externalOutputFilter = new TransferOutputFilter(storageWrapper, upgrade, saveHandler);
    }

    @Override
    public final Optional<FilterLogic> getInputFilter() {
        return Optional.of(externalInputFilter);
    }

    @Override
    public final Optional<FilterLogic> getOutputFilter() {
        return Optional.of(externalOutputFilter);
    }

    public final Optional<StorageEndpoint> getEndpoint() {
        return EndpointBinding.read(upgrade);
    }

    public final void setEndpoint(StorageEndpoint endpoint) {
        EndpointBinding.write(upgrade, endpoint);
        save();
    }

    public final void clearEndpoint() {
        EndpointBinding.clear(upgrade);
        save();
    }

    public final IStorageWrapper getStorageWrapper() {
        return storageWrapper;
    }
}
