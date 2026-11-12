package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePickupUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

public final class RemotePickupUpgradeTab extends LinkedStorageUpgradeTab<RemotePickupUpgradeContainer> {
    private final ContentsFilterControl filterControl;

    public RemotePickupUpgradeTab(RemotePickupUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_pickup_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_pickup_upgrade.tooltip"));
        filterControl = addHideableChild(new ContentsFilterControl.Advanced(screen,
                new Position(x + 3, y + 24), container.getFilterLogicContainer(), 3,
                RemoteContentsFilterType.DEFINITION));
        ToggleButton<SourcePriority> priority = RemoteSourcePriorityButton.create(new Position(x + 4, y + 104),
                container::getSourcePriority, container::toggleSourcePriority);
        addHideableChild(priority);
        addConnectionStatus(25, 109);
    }

    @Override
    protected void moveSlotsToTab() {
        filterControl.moveSlotsToView();
    }
}
