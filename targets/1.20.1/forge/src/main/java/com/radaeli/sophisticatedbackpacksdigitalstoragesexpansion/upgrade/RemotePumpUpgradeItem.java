package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeItem;

/** Universal linked version of Sophisticated Core's advanced Pump. */
public final class RemotePumpUpgradeItem extends LinkedStorageUpgradeItem<RemotePumpUpgradeWrapper> {
    private static final UpgradeType<RemotePumpUpgradeWrapper> TYPE = new UpgradeType<>(RemotePumpUpgradeWrapper::new);

    public RemotePumpUpgradeItem() {
        super(1);
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.pump_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof PumpUpgradeItem,
                0, message, message));
    }

    @Override
    public UpgradeType<RemotePumpUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public boolean canChangeBinding(ItemStack stack) {
        return !PumpReservation.hasPending(stack);
    }

    @Override
    protected Component getLinkStatus(ItemStack stack) {
        var status = super.getLinkStatus(stack).copy();
        if (PumpReservation.hasPending(stack)) {
            status.append(" · ").append(Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.pump.pending"));
        }
        return status;
    }
}
