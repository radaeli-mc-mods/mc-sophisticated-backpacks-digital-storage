package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

/** Shared packet handling for the network/backpack source-order setting. */
public abstract class PriorityLinkedStorageUpgradeContainer<W extends PriorityLinkedStorageUpgradeWrapper<W, ?>,
        C extends PriorityLinkedStorageUpgradeContainer<W, C>> extends LinkedStorageUpgradeContainer<W, C> {
    private static final String DATA_SOURCE_PRIORITY = "sourcePriority";

    protected PriorityLinkedStorageUpgradeContainer(Player player, int containerId, W wrapper,
            UpgradeContainerType<W, C> type) {
        super(player, containerId, wrapper, type);
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        if (data.contains(DATA_SOURCE_PRIORITY)) {
            upgradeWrapper.setSourcePriority(data.getBoolean(DATA_SOURCE_PRIORITY)
                    ? SourcePriority.BACKPACK_FIRST : SourcePriority.NETWORK_FIRST);
        }
    }

    public final SourcePriority getSourcePriority() {
        return upgradeWrapper.getSourcePriority();
    }

    public final void toggleSourcePriority() {
        SourcePriority priority = getSourcePriority().next();
        // Follow native controls: update the client wrapper before sending the selected value.
        upgradeWrapper.setSourcePriority(priority);
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putBoolean(DATA_SOURCE_PRIORITY, priority == SourcePriority.BACKPACK_FIRST);
            return data;
        });
    }
}
