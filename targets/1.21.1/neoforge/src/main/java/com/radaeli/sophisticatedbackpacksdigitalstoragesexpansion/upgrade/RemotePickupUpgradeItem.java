package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;

public final class RemotePickupUpgradeItem extends LinkedStorageUpgradeItem<RemotePickupUpgradeWrapper> {
    private static final UpgradeType<RemotePickupUpgradeWrapper> TYPE = new UpgradeType<>(RemotePickupUpgradeWrapper::new);

    public RemotePickupUpgradeItem() {
        super(1);
    }

    @Override
    public UpgradeType<RemotePickupUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.pickup_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof PickupUpgradeItem,
                0, message, message));
    }
}
