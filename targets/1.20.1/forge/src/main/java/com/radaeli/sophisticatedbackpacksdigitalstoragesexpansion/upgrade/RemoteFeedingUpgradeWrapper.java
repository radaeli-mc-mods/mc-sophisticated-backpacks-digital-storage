package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;

/** Universal Feeding configuration backed by native data components and filter rules. */
public final class RemoteFeedingUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemoteFeedingUpgradeWrapper,
        RemoteFeedingUpgradeItem> implements IFilteredUpgrade, ITickableUpgrade {
    public static final int FILTER_SLOT_COUNT = 9;
    private final FilterLogic filterLogic;

    public RemoteFeedingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new FilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT,
                stack -> !stack.isEmpty() && stack.getItem().isEdible(),
                "");
    }

    @Override
    public FilterLogic getFilterLogic() {
        return filterLogic;
    }

    public HungerLevel getFeedAtHungerLevel() {
        HungerLevel level = HungerLevel.fromName(upgrade.getOrCreateTag().getString("feedAtHungerLevel"));
        return level == null ? HungerLevel.HALF : level;
    }

    public void setFeedAtHungerLevel(HungerLevel hungerLevel) {
        upgrade.getOrCreateTag().putString("feedAtHungerLevel", hungerLevel.getSerializedName());
        save();
    }

    public boolean shouldFeedImmediatelyWhenHurt() {
        return !upgrade.hasTag() || !upgrade.getTag().contains("feedImmediatelyWhenHurt")
                || upgrade.getTag().getBoolean("feedImmediatelyWhenHurt");
    }

    public void setFeedImmediatelyWhenHurt(boolean feedImmediately) {
        upgrade.getOrCreateTag().putBoolean("feedImmediatelyWhenHurt", feedImmediately);
        save();
    }

    @Override
    public void tick(Entity entity, Level level, net.minecraft.core.BlockPos pos) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            FeedingService.tick(this, player);
        }
    }

    public void setServiceCooldown(Level level, int ticks) {
        setCooldown(level, ticks);
    }

    public boolean hasPendingReservation() {
        return FeedingReservation.hasPending(upgrade);
    }

    public void saveReservationChange() {
        save();
    }
}
