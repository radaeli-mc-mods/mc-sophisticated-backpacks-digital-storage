package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;

public final class RemoteAlchemyUpgradeContainer
        extends PriorityLinkedStorageUpgradeContainer<RemoteAlchemyUpgradeWrapper, RemoteAlchemyUpgradeContainer> {
    private static final String DATA_SLOT = "alchemySlot";
    private static final String DATA_CONDITION = "alchemyCondition";
    private static final String DATA_VALUE = "alchemyValue";
    private static final String DATA_MATCH_ALL = "alchemyMatchAll";
    private static final String DATA_MATCH_DURATION = "alchemyMatchDuration";
    private static final String DATA_MATCH_AMPLIFIER = "alchemyMatchAmplifier";
    public RemoteAlchemyUpgradeContainer(Player player, int containerId, RemoteAlchemyUpgradeWrapper wrapper,
            UpgradeContainerType<RemoteAlchemyUpgradeWrapper, RemoteAlchemyUpgradeContainer> type) {
        super(player, containerId, wrapper, type);
        for (int slot = 0; slot < RemoteAlchemyUpgradeWrapper.FILTER_SLOT_COUNT; slot++) {
            FilterSlotItemHandler filterSlot = new FilterSlotItemHandler(
                    () -> wrapper.getFilterLogic().getFilterHandler(), slot, 0, 0);
            filterSlot.setBackground(InventoryMenu.BLOCK_ATLAS,
                    net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeContainer.EMPTY_POTION_SLOT_BACKGROUND);
            slots.add(filterSlot);
        }
    }

    public AlchemyCondition getCondition(int slot) {
        return upgradeWrapper.getCondition(slot);
    }

    public float getValue(int slot) {
        return upgradeWrapper.getValue(slot);
    }

    public boolean hasNoFilter(int slot) {
        return slot < 0 || slot >= RemoteAlchemyUpgradeWrapper.FILTER_SLOT_COUNT
                || upgradeWrapper.getFilterLogic().getFilterHandler().getStackInSlot(slot).isEmpty();
    }

    public boolean shouldMatchAllEffects() {
        return upgradeWrapper.shouldMatchAllEffects();
    }

    public boolean shouldMatchEffectDuration() {
        return upgradeWrapper.shouldMatchEffectDuration();
    }

    public boolean shouldMatchEffectAmplifier() {
        return upgradeWrapper.shouldMatchEffectAmplifier();
    }

    public void toggleCondition(int slot) {
        if (hasNoFilter(slot)) return;
        AlchemyCondition condition = getCondition(slot).next();
        setConditionValue(slot, condition, condition.defaultValue());
    }

    public void setValue(int slot, float value) {
        if (slot < 0 || slot >= RemoteAlchemyUpgradeWrapper.FILTER_SLOT_COUNT || hasNoFilter(slot)
                || getCondition(slot) != AlchemyCondition.HURT || !Float.isFinite(value)) return;
        setConditionValue(slot, AlchemyCondition.HURT, Math.clamp(value, 0.0F, 1.0F));
    }

    public void setConditionValue(int slot, AlchemyCondition condition, float value) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putInt(DATA_SLOT, slot);
            data.putString(DATA_CONDITION, condition.getSerializedName());
            data.putFloat(DATA_VALUE, value);
            return data;
        });
    }

    public void setMatchAllEffects(boolean value) {
        sendBooleanSetting(DATA_MATCH_ALL, value);
    }

    public void setMatchEffectDuration(boolean value) {
        sendBooleanSetting(DATA_MATCH_DURATION, value);
    }

    public void setMatchEffectAmplifier(boolean value) {
        sendBooleanSetting(DATA_MATCH_AMPLIFIER, value);
    }

    private void sendBooleanSetting(String key, boolean value) {
        sendDataToServer(() -> {
            CompoundTag data = new CompoundTag();
            data.putBoolean(key, value);
            return data;
        });
    }

    @Override
    protected void handleSettingsPacket(CompoundTag data) {
        super.handleSettingsPacket(data);
        if (data.contains(DATA_SLOT) && data.contains(DATA_CONDITION) && data.contains(DATA_VALUE)) {
            int slot = data.getInt(DATA_SLOT);
            AlchemyCondition condition = AlchemyCondition.fromName(data.getString(DATA_CONDITION));
            float value = data.getFloat(DATA_VALUE);
            if (!hasNoFilter(slot) && condition != null && Float.isFinite(value)) {
                upgradeWrapper.setConditionValue(slot, condition, value);
            }
        }
        if (data.contains(DATA_MATCH_ALL)) {
            upgradeWrapper.setMatchAllEffects(data.getBoolean(DATA_MATCH_ALL));
        }
        if (data.contains(DATA_MATCH_DURATION)) {
            upgradeWrapper.setMatchEffectDuration(data.getBoolean(DATA_MATCH_DURATION));
        }
        if (data.contains(DATA_MATCH_AMPLIFIER)) {
            upgradeWrapper.setMatchEffectAmplifier(data.getBoolean(DATA_MATCH_AMPLIFIER));
        }
    }

    @Override
    protected void handleAction(String action, UpgradeOperationContext context) {
        // Alchemy is automatic and has no manual GUI action.
    }
}
