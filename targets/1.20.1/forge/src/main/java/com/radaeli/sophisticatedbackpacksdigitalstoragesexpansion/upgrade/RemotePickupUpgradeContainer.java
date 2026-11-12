package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogicContainer;

public final class RemotePickupUpgradeContainer
        extends PriorityLinkedStorageUpgradeContainer<RemotePickupUpgradeWrapper, RemotePickupUpgradeContainer> {
    private final ContentsFilterLogicContainer filterLogicContainer;

    public RemotePickupUpgradeContainer(Player player, int containerId, RemotePickupUpgradeWrapper wrapper,
            UpgradeContainerType<RemotePickupUpgradeWrapper, RemotePickupUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        filterLogicContainer = new ContentsFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
    }

    public ContentsFilterLogicContainer getFilterLogicContainer() {
        return filterLogicContainer;
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        super.handleSettingsPacket(data);
        filterLogicContainer.handleMessage(data);
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        // Pickup is invoked only by Sophisticated's native item pickup flow.
    }
}
