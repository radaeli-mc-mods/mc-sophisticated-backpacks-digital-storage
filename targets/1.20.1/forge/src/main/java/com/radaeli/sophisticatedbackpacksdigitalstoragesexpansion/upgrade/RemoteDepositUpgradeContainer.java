package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.DepositService;

public final class RemoteDepositUpgradeContainer
        extends LinkedStorageUpgradeContainer<RemoteDepositUpgradeWrapper, RemoteDepositUpgradeContainer> {
    private static final String DEPOSIT = "deposit";
    private final DepositFilterLogicContainer filterLogicContainer;

    public RemoteDepositUpgradeContainer(Player player, int containerId, RemoteDepositUpgradeWrapper wrapper,
            UpgradeContainerType<RemoteDepositUpgradeWrapper, RemoteDepositUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        // Resolve through the current wrapper; Sophisticated replaces wrappers when syncing upgrade stacks.
        filterLogicContainer = new DepositFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
    }

    public DepositFilterLogicContainer getFilterLogicContainer() {
        return filterLogicContainer;
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        filterLogicContainer.handleMessage(data);
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        if (DEPOSIT.equals(action)) {
            DepositService.deposit(context, upgradeWrapper.getEndpoint(), upgradeWrapper.getFilterLogic());
        }
    }

    public void requestDeposit() {
        sendAction(DEPOSIT);
    }
}
