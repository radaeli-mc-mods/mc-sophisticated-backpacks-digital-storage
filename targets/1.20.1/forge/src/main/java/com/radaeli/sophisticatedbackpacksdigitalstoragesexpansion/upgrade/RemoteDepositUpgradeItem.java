package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class RemoteDepositUpgradeItem extends LinkedStorageUpgradeItem<RemoteDepositUpgradeWrapper> {
    public static final int FILTER_SLOT_COUNT = 9;
    private static final UpgradeType<RemoteDepositUpgradeWrapper> TYPE = new UpgradeType<>(RemoteDepositUpgradeWrapper::new);

    public RemoteDepositUpgradeItem() {
        super(1);
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.deposit_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof DepositUpgradeItem,
                0, message, message));
    }

    @Override
    public UpgradeType<RemoteDepositUpgradeWrapper> getType() {
        return TYPE;
    }
}
