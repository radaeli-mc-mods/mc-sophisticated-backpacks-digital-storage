package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;

/** Item-only remote magnet settings with native carrier and placed-backpack tick contexts. */
public final class RemoteMagnetUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemoteMagnetUpgradeWrapper,
        RemoteMagnetUpgradeItem> implements IContentsFilteredUpgrade, ITickableUpgrade {
    private static final int ACTIVE_COOLDOWN_TICKS = 10;
    private static final int IDLE_COOLDOWN_TICKS = 40;

    private final ContentsFilterLogic filterLogic;

    public RemoteMagnetUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new ContentsFilterLogic(upgrade, saveHandler, RemoteMagnetUpgradeItem.FILTER_SLOT_COUNT,
                storageWrapper::getInventoryHandler,
                (MemorySettingsCategory) storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
                "");
    }

    @Override
    public ContentsFilterLogic getFilterLogic() {
        return filterLogic;
    }

    public int getRange() {
        return upgradeItem.getRange();
    }

    @Override
    public void tick(Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide || (entity != null && !entity.isAlive()) || !isEnabled()) {
            return;
        }
        // Sophisticated's native Magnet uses a global cadence for a player
        // carrier and the wrapper cooldown for a placed backpack (null entity).
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            if (RemoteMagnetEligibility.isInNativeCooldown(level)) return;
        } else if (isInCooldown(level)) {
            return;
        }
        boolean movedItems = RemoteMagnetService.tick(this, entity, level, pos);
        setCooldown(level, movedItems ? ACTIVE_COOLDOWN_TICKS : IDLE_COOLDOWN_TICKS);
    }
}
