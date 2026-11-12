package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public final class RemoteRefillUpgradeItem extends LinkedStorageUpgradeItem<RemoteRefillUpgradeWrapper> {
    private static final UpgradeType<RemoteRefillUpgradeWrapper> TYPE = new UpgradeType<>(RemoteRefillUpgradeWrapper::new);

    public RemoteRefillUpgradeItem() {
        super(1);
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.refill_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof RefillUpgradeItem,
                0, message, message));
    }

    @Override
    public UpgradeType<RemoteRefillUpgradeWrapper> getType() {
        return TYPE;
    }
}
