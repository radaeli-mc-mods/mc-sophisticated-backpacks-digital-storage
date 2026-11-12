package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteDepositUpgradeContainer;

public final class RemoteDepositUpgradeTab extends LinkedStorageUpgradeTab<RemoteDepositUpgradeContainer> {
    private final DepositFilterLogicControl filterLogicControl;

    public RemoteDepositUpgradeTab(RemoteDepositUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_deposit_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_deposit_upgrade.tooltip"));

        filterLogicControl = addHideableChild(new DepositFilterLogicControl.Advanced(screen,
                new Position(x + 3, y + 24), container.getFilterLogicContainer(), 3));

        Button deposit = new Button(new Position(x + 4, y + 104), ButtonDefinitions.TRANSFER_TO_STORAGE,
                ignored -> container.requestDeposit());
        deposit.setTooltip(List.of(Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.deposit")));
        addHideableChild(deposit);

        addConnectionStatus(25, 109);
    }

    @Override
    protected void moveSlotsToTab() {
        filterLogicControl.moveSlotsToView();
    }
}
