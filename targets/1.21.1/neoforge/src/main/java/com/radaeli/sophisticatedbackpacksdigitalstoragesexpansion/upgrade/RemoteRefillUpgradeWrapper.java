package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBlockPickResponseUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper.TargetSlot;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

/** Universal refill upgrade configuration; operational behavior is delegated to RefillService. */
public final class RemoteRefillUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemoteRefillUpgradeWrapper,
        RemoteRefillUpgradeItem> implements IFilteredUpgrade, ITickableUpgrade, IBlockPickResponseUpgrade {
    public static final int FILTER_SLOT_COUNT = 9;
    private static final String TARGET_SLOT_PREFIX = "refill_target_";

    private final FilterLogic filterLogic;

    public RemoteRefillUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new FilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT, ModCoreDataComponents.FILTER_ATTRIBUTES);
        filterLogic.getFilterHandler().setOnSlotChange(slot -> {
            if (filterLogic.getFilterHandler().getStackInSlot(slot).isEmpty()
                    || !getTargetSlots().containsKey(slot)) {
                setTargetSlot(slot, TargetSlot.ANY);
            }
        });
    }

    @Override
    public FilterLogic getFilterLogic() {
        return filterLogic;
    }

    public Map<Integer, TargetSlot> getTargetSlots() {
        CustomData data = upgrade.get(DataComponents.CUSTOM_DATA);
        Map<Integer, TargetSlot> slots = new HashMap<>();
        if (data == null) {
            return slots;
        }
        var tag = data.copyTag();
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            String key = TARGET_SLOT_PREFIX + i;
            if (tag.contains(key)) {
                slots.put(i, TargetSlot.fromName(tag.getString(key)));
            }
        }
        return slots;
    }

    public TargetSlot getTargetSlot(int filterSlot) {
        return getTargetSlots().getOrDefault(filterSlot, TargetSlot.ANY);
    }

    public void setTargetSlot(int filterSlot, TargetSlot targetSlot) {
        if (filterSlot < 0 || filterSlot >= FILTER_SLOT_COUNT) {
            return;
        }
        String key = TARGET_SLOT_PREFIX + filterSlot;
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
            if (targetSlot == TargetSlot.ANY) {
                tag.remove(key);
            } else {
                tag.putString(key, targetSlot.getSerializedName());
            }
        });
        save();
    }

    @Override
    public void tick(Entity entity, Level level, net.minecraft.core.BlockPos pos) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            RefillService.tick(this, player);
        }
    }

    public void setServiceCooldown(Level level, int ticks) {
        setCooldown(level, ticks);
    }

    public boolean allowsTargetSlotSelection() {
        return true;
    }

    @Override
    public boolean pickBlock(Player player, ItemStack picked) {
        return RefillService.pickBlock(this, player, picked);
    }
}
