package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;

/** A transient IO veto, neutral with respect to Sophisticated's normal allow/block filter composition. */
final class TransferInputFilter extends FilterLogic {
    private final IStorageWrapper storage;

    TransferInputFilter(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(upgrade, saveHandler, 0);
        this.storage = storage;
    }

    @Override
    public boolean isAllowList() {
        // Sophisticated combines all input filters with AND when at least one is a block list.
        return !StorageTransferGuard.isExternalInsertionBlocked(storage);
    }

    @Override
    public boolean matchesFilter(ItemStack stack) {
        if (StorageTransferGuard.isExternalInsertionBlocked(storage)) {
            return false;
        }
        boolean hasOtherFilter = false;
        for (IIOFilterUpgrade upgrade : storage.getUpgradeHandler().getWrappersThatImplement(IIOFilterUpgrade.class)) {
            Optional<FilterLogic> input = upgrade.getInputFilter();
            if (input.isPresent() && !(input.get() instanceof TransferInputFilter)) {
                hasOtherFilter = true;
                if (!input.get().isAllowList()) {
                    return true; // Neutral value when Sophisticated combines filters with AND.
                }
            }
        }
        // Neutral value for an OR of real allow lists; pass everything if this is the only kind of filter.
        return !hasOtherFilter;
    }
}
