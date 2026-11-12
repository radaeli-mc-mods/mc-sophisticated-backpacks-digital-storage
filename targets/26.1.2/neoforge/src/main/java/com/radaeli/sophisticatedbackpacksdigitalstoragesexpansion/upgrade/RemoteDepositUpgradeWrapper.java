package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;

public final class RemoteDepositUpgradeWrapper
        extends LinkedStorageUpgradeWrapper<RemoteDepositUpgradeWrapper, RemoteDepositUpgradeItem> implements IFilteredUpgrade {
    private final DepositFilterLogic filterLogic;

    public RemoteDepositUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new DepositFilterLogic(upgrade, saveHandler, RemoteDepositUpgradeItem.FILTER_SLOT_COUNT,
                ModCoreDataComponents.FILTER_ATTRIBUTES);
    }

    @Override
    public DepositFilterLogic getFilterLogic() {
        return filterLogic;
    }
}
