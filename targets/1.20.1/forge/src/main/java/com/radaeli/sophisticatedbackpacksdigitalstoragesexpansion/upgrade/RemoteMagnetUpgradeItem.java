package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.magnet.MagnetUpgradeItem;

/** The range supplier must use the active advanced Magnet range from Sophisticated's configuration. */
public final class RemoteMagnetUpgradeItem extends LinkedStorageUpgradeItem<RemoteMagnetUpgradeWrapper> {
    public static final int FILTER_SLOT_COUNT = 9;
    private static final UpgradeType<RemoteMagnetUpgradeWrapper> TYPE = new UpgradeType<>(RemoteMagnetUpgradeWrapper::new);

    private final IntSupplier range;

    public RemoteMagnetUpgradeItem(IntSupplier range) {
        super(1);
        this.range = range;
    }

    public int getRange() {
        return range.getAsInt();
    }

    @Override
    public UpgradeType<RemoteMagnetUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        Component message = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.magnet_conflict");
        return List.of(new IUpgradeItem.UpgradeConflictDefinition(item -> item instanceof MagnetUpgradeItem,
                0, message, message));
    }
}
