package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;

public final class RemoteRestockUpgradeWrapper extends LinkedStorageUpgradeWrapper<RemoteRestockUpgradeWrapper,
        RemoteRestockUpgradeItem> implements IFilteredUpgrade {
    private final DepositFilterLogic filterLogic;
    public RemoteRestockUpgradeWrapper(IStorageWrapper storage, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storage, upgrade, saveHandler);
        filterLogic = new DepositFilterLogic(upgrade, saveHandler, RemoteRestockUpgradeItem.FILTER_SLOT_COUNT,
                ModCoreDataComponents.FILTER_ATTRIBUTES);
        if (!upgrade.has(net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents.FILTER_BY_INVENTORY)) { filterLogic.setDepositFilterType(DepositFilterType.INVENTORY); }
    }
    @Override public DepositFilterLogic getFilterLogic() { return filterLogic; }
}
