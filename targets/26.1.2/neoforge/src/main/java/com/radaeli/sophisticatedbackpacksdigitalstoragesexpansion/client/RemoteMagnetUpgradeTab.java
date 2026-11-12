package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterControl;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteMagnetUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

public final class RemoteMagnetUpgradeTab extends LinkedStorageUpgradeTab<RemoteMagnetUpgradeContainer> {
    private final ContentsFilterControl filterControl;

    public RemoteMagnetUpgradeTab(RemoteMagnetUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_magnet_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_magnet_upgrade.tooltip"));
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
