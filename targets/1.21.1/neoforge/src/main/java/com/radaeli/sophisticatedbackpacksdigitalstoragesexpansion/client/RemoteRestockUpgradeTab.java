package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeContainer;

public final class RemoteRestockUpgradeTab extends LinkedStorageUpgradeTab<RemoteRestockUpgradeContainer> {
    private final DepositFilterLogicControl filterControl;

    public RemoteRestockUpgradeTab(RemoteRestockUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_restock_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_restock_upgrade.tooltip"));
        filterControl = addHideableChild(new DepositFilterLogicControl.Advanced(screen,
                new Position(x + 3, y + 24), container.getFilterContainer(), 3));
        Button restock = new Button(new Position(x + 4, y + 104), ButtonDefinitions.TRANSFER_TO_INVENTORY,
                ignored -> container.requestRestock());
        restock.setTooltip(List.of(Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.restock")));
        addHideableChild(restock);
        addConnectionStatus(25, 109);
    }

    @Override protected void moveSlotsToTab() { filterControl.moveSlotsToView(); }
}
