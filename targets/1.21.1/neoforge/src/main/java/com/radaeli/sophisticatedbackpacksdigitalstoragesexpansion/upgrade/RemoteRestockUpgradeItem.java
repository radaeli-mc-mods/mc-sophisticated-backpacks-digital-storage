package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.restock.RestockUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class RemoteRestockUpgradeItem extends LinkedStorageUpgradeItem<RemoteRestockUpgradeWrapper> {
    public static final int FILTER_SLOT_COUNT = 9;
    private static final UpgradeType<RemoteRestockUpgradeWrapper> TYPE = new UpgradeType<>(RemoteRestockUpgradeWrapper::new);
    public RemoteRestockUpgradeItem() { super(1); }
    @Override public UpgradeType<RemoteRestockUpgradeWrapper> getType() { return TYPE; }
    @Override public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component error = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.restock_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(RestockUpgradeItem.class::isInstance, 0, error, error));
    }
}
