package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogicContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.RestockService;

public final class RemoteRestockUpgradeContainer extends LinkedStorageUpgradeContainer<RemoteRestockUpgradeWrapper,
        RemoteRestockUpgradeContainer> {
    private static final String RESTOCK = "restock";
    private final DepositFilterLogicContainer filterContainer;
    public RemoteRestockUpgradeContainer(Player player, int id, RemoteRestockUpgradeWrapper wrapper,
            UpgradeContainerType<RemoteRestockUpgradeWrapper, RemoteRestockUpgradeContainer> type) {
        super(player, id, wrapper, type);
        filterContainer = new DepositFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
    }
    public DepositFilterLogicContainer getFilterContainer() { return filterContainer; }
    @Override protected void handleSettingsPacket(CompoundTag data) { filterContainer.handlePacket(data); }
    @Override protected void handleAction(String action, UpgradeOperationContext context) {
        if (RESTOCK.equals(action)) RestockService.restock(context, upgradeWrapper);
    }
    public void requestRestock() { sendAction(RESTOCK); }
}
