package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;

public final class RemoteFeedingUpgradeContainer
        extends PriorityLinkedStorageUpgradeContainer<RemoteFeedingUpgradeWrapper, RemoteFeedingUpgradeContainer> {
    private static final String DATA_HUNGER_LEVEL = "feedAtHungerLevel";
    private static final String DATA_FEED_WHEN_HURT = "feedImmediatelyWhenHurt";
    private final FilterLogicContainer<FilterLogic> filterLogicContainer;

    public RemoteFeedingUpgradeContainer(Player player, int containerId, RemoteFeedingUpgradeWrapper wrapper,
            UpgradeContainerType<RemoteFeedingUpgradeWrapper, RemoteFeedingUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        filterLogicContainer = new FilterLogicContainer<>(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
    }

    public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
        return filterLogicContainer;
    }

    public HungerLevel getFeedAtHungerLevel() {
        return upgradeWrapper.getFeedAtHungerLevel();
    }

    public boolean shouldFeedImmediatelyWhenHurt() {
        return upgradeWrapper.shouldFeedImmediatelyWhenHurt();
    }

    public void setFeedAtHungerLevel(HungerLevel level) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putString(DATA_HUNGER_LEVEL, level.getSerializedName());
            return data;
        });
    }

    public void setFeedImmediatelyWhenHurt(boolean value) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putBoolean(DATA_FEED_WHEN_HURT, value);
            return data;
        });
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        super.handleSettingsPacket(data);
        filterLogicContainer.handleMessage(data);
        if (data.contains(DATA_HUNGER_LEVEL)) {
            upgradeWrapper.setFeedAtHungerLevel(HungerLevel.fromName(data.getString(DATA_HUNGER_LEVEL)));
        }
        if (data.contains(DATA_FEED_WHEN_HURT)) {
            upgradeWrapper.setFeedImmediatelyWhenHurt(data.getBoolean(DATA_FEED_WHEN_HURT));
        }
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        // Feeding is tick driven and has no manual GUI action.
    }
}
