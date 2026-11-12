package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper.TargetSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

public final class RemoteRefillUpgradeContainer
        extends PriorityLinkedStorageUpgradeContainer<RemoteRefillUpgradeWrapper, RemoteRefillUpgradeContainer> {
    public static final int FILTER_COUNT = RemoteRefillUpgradeWrapper.FILTER_SLOT_COUNT;
    private static final String DATA_TARGET_SLOT_INDEX = "targetSlotIndex";
    private static final String DATA_TARGET_SLOT = "targetSlot";
    private final FilterLogicContainer<FilterLogic> filterLogicContainer;

    public RemoteRefillUpgradeContainer(Player player, int containerId, RemoteRefillUpgradeWrapper wrapper,
            UpgradeContainerType<RemoteRefillUpgradeWrapper, RemoteRefillUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        filterLogicContainer = new FilterLogicContainer<>(wrapper::getFilterLogic, this, slots::add);
    }

    public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
        return filterLogicContainer;
    }

    public TargetSlot getTargetSlot(int filterSlot) {
        return upgradeWrapper.getTargetSlot(filterSlot);
    }

    public void setTargetSlot(int filterSlot, TargetSlot targetSlot) {
        if (filterSlot < 0 || filterSlot >= FILTER_COUNT || targetSlot == null) {
            return;
        }
        upgradeWrapper.setTargetSlot(filterSlot, targetSlot);
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putInt(DATA_TARGET_SLOT_INDEX, filterSlot);
            data.putString(DATA_TARGET_SLOT, targetSlot.getSerializedName());
            return data;
        });
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        super.handleSettingsPacket(data);
        filterLogicContainer.handlePacket(data);
        if (data.contains(DATA_TARGET_SLOT_INDEX) && data.contains(DATA_TARGET_SLOT)) {
            int index = data.getInt(DATA_TARGET_SLOT_INDEX);
            if (index >= 0 && index < RemoteRefillUpgradeWrapper.FILTER_SLOT_COUNT) {
                upgradeWrapper.setTargetSlot(index, TargetSlot.fromName(data.getString(DATA_TARGET_SLOT)));
            }
        }
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        // Refill is tick- and block-pick driven; it has no GUI transfer action.
    }
}
