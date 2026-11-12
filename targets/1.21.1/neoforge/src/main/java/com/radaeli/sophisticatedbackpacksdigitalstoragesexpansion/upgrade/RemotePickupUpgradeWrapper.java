package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;

/** Universal pickup filter and routing settings; Sophisticated retains control of the pickup event. */
public final class RemotePickupUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemotePickupUpgradeWrapper,
        RemotePickupUpgradeItem> implements IContentsFilteredUpgrade, IPickupResponseUpgrade {
    public static final int FILTER_SLOT_COUNT = 9;

    private final ContentsFilterLogic filterLogic;

    public RemotePickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new ContentsFilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT,
                storageWrapper::getInventoryHandler,
                (MemorySettingsCategory) storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
                ModCoreDataComponents.FILTER_ATTRIBUTES);
    }

    @Override
    public ContentsFilterLogic getFilterLogic() {
        return filterLogic;
    }

    @Override
    public ItemStack pickup(Level level, ItemStack offered, boolean simulate) {
        if (level.isClientSide || offered.isEmpty() || !isEnabled()) {
            return offered;
        }
        return RemotePickupContext.current()
                .filter(RemotePickupContext.Context::isValid)
                .map(context -> PickupService.pickup(this, context.player(), offered, simulate))
                .orElse(offered);
    }
}
