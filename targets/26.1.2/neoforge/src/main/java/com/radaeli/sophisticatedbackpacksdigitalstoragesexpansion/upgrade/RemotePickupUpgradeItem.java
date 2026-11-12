package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeItem;

public final class RemotePickupUpgradeItem extends LinkedStorageUpgradeItem<RemotePickupUpgradeWrapper> {
    private static final UpgradeType<RemotePickupUpgradeWrapper> TYPE = new UpgradeType<>(RemotePickupUpgradeWrapper::new);

    public RemotePickupUpgradeItem(net.minecraft.world.item.Item.Properties properties) {
        super(1, properties);
    }

    @Override
    public UpgradeType<RemotePickupUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override public boolean canChangeBinding(net.minecraft.world.item.ItemStack stack) {
        return !PickupReservation.hasPending(stack);
    }

    @Override protected Component getLinkStatus(net.minecraft.world.item.ItemStack stack) {
        Component status = super.getLinkStatus(stack);
        if (!PickupReservation.hasPending(stack)) return status;
        return status.copy().append(Component.translatable(PickupReservation.uncertain(stack)
                ? "message.sophisticated_backpacks_digital_storages_expansion.pickup_uncertain"
                : "message.sophisticated_backpacks_digital_storages_expansion.pickup_pending"));
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.pickup_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof PickupUpgradeItem,
                0, message, message));
    }
}
