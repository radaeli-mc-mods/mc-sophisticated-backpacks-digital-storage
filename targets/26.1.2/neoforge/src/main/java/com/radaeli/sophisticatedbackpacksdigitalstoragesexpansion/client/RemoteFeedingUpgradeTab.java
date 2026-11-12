package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControlBase.MatchButton;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.HungerLevel;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteFeedingUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

public final class RemoteFeedingUpgradeTab extends LinkedStorageUpgradeTab<RemoteFeedingUpgradeContainer> {
    private final FilterLogicControl<?, ?> filterControl;

    public RemoteFeedingUpgradeTab(RemoteFeedingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_feeding_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_feeding_upgrade.tooltip"));
        filterControl = addHideableChild(new FilterLogicControl<>(screen, new Position(x + 3, y + 24),
                container.getFilterLogicContainer(), 3, MatchButton.ALLOW_LIST));

        int controlsY = filterControl.getY() + filterControl.getHeight() + 6;
        ToggleButton<HungerLevel> hunger = new ToggleButton<>(new Position(x + 5, controlsY), FeedingUpgradeTab.HUNGER_LEVEL,
                ignored -> container.setFeedAtHungerLevel(container.getFeedAtHungerLevel().next()),
                container::getFeedAtHungerLevel);
        addHideableChild(hunger);
        ToggleButton<Boolean> hurt = new ToggleButton<>(new Position(x + 25, controlsY),
                FeedingUpgradeTab.FEED_IMMEDIATELY_WHEN_HURT,
                ignored -> container.setFeedImmediatelyWhenHurt(!container.shouldFeedImmediatelyWhenHurt()),
                container::shouldFeedImmediatelyWhenHurt);
        addHideableChild(hurt);

        addHideableChild(SourcePriorityControl.create(new Position(x + 5, controlsY + 20),
                () -> container.getSourcePriority() == SourcePriority.NETWORK_FIRST, container::toggleSourcePriority));
        addConnectionStatus(25, controlsY - y + 26);
    }

    @Override
    protected void moveSlotsToTab() {
        filterControl.moveSlotsToView();
    }
}
