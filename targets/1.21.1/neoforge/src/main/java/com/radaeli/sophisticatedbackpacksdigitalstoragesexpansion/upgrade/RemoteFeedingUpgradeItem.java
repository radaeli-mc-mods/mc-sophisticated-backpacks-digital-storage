package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.feeding.FeedingUpgradeItem;

public final class RemoteFeedingUpgradeItem extends LinkedStorageUpgradeItem<RemoteFeedingUpgradeWrapper> {
    public static final int FILTER_SLOT_COUNT = 9;
    private static final UpgradeType<RemoteFeedingUpgradeWrapper> TYPE = new UpgradeType<>(RemoteFeedingUpgradeWrapper::new);

    public RemoteFeedingUpgradeItem() {
        super(1);
    }

    @Override
    public boolean canChangeBinding(net.minecraft.world.item.ItemStack stack) {
        return super.canChangeBinding(stack) && !FeedingReservation.hasPending(stack);
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.feeding_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof FeedingUpgradeItem,
                0, message, message));
    }

    @Override
    public UpgradeType<RemoteFeedingUpgradeWrapper> getType() {
        return TYPE;
    }
}
