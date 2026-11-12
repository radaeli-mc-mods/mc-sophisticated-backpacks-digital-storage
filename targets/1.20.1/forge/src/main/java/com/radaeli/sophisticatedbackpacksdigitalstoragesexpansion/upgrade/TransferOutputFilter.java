package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;

/** Prevents the backpack's own contents from being extracted back through its network. */
final class TransferOutputFilter extends FilterLogic {
    private final IStorageWrapper storage;

    TransferOutputFilter(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(upgrade, saveHandler, 0);
        this.storage = storage;
    }

    @Override
    public boolean matchesFilter(ItemStack stack) {
        return !StorageTransferGuard.isExternalExtractionBlocked(storage);
    }
}
