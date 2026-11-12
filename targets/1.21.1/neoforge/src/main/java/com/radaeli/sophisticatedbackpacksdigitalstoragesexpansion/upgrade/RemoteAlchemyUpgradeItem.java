package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeItem;

public final class RemoteAlchemyUpgradeItem extends LinkedStorageUpgradeItem<RemoteAlchemyUpgradeWrapper> {
    private static final UpgradeType<RemoteAlchemyUpgradeWrapper> TYPE = new UpgradeType<>(RemoteAlchemyUpgradeWrapper::new);

    public RemoteAlchemyUpgradeItem() {
        super(1);
    }

    @Override
    public boolean canChangeBinding(net.minecraft.world.item.ItemStack stack) {
        return super.canChangeBinding(stack) && !AlchemyReservation.hasPending(stack);
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.alchemy_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof AlchemyUpgradeItem,
                0, message, message));
    }

    @Override
    public UpgradeType<RemoteAlchemyUpgradeWrapper> getType() {
        return TYPE;
    }
}
