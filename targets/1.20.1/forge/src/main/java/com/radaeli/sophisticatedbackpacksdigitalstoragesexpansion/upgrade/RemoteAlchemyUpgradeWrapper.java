package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyFilterAttribute;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeWrapper.AlchemyItemDefinition;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.mixin.AlchemyDefinitionsAccessor;

/** Universal Alchemy settings. Runtime application and its persisted reservation are in AlchemyService. */
public final class RemoteAlchemyUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemoteAlchemyUpgradeWrapper,
        RemoteAlchemyUpgradeItem> implements IFilteredUpgrade, ITickableUpgrade {
    public static final int FILTER_SLOT_COUNT = 8;
    private final FilterLogic filterLogic;
    private final ItemStack[] previousFilters = new ItemStack[FILTER_SLOT_COUNT];

    public RemoteAlchemyUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        normalizePersistedSettings();
        filterLogic = new FilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT, this::isDefinedAlchemyItem,
                "alchemy");
        for (int slot = 0; slot < FILTER_SLOT_COUNT; slot++) {
            previousFilters[slot] = filterLogic.getFilterHandler().getStackInSlot(slot).copy();
        }
        filterLogic.getFilterHandler().setOnSlotChange(this::syncFilterAttributes);
    }

    @Override
    public FilterLogic getFilterLogic() {
        return filterLogic;
    }

    public List<AlchemyFilterAttribute> getAlchemyAttributes() {
        List<AlchemyFilterAttribute> stored = readAlchemyAttributes();
        List<AlchemyFilterAttribute> result = new ArrayList<>(FILTER_SLOT_COUNT);
        for (int i = 0; i < FILTER_SLOT_COUNT; i++) {
            if (i < stored.size()) {
                result.add(stored.get(i));
            } else {
                result.add(new AlchemyFilterAttribute(ItemStack.EMPTY, AlchemyCondition.NEVER,
                        AlchemyCondition.NEVER.defaultValue()));
            }
        }
        return result;
    }

    public AlchemyCondition getCondition(int slot) {
        return getAlchemyAttributes().get(slot).condition();
    }

    public float getValue(int slot) {
        return getAlchemyAttributes().get(slot).value();
    }

    public void setConditionValue(int slot, AlchemyCondition condition, float value) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT || condition == null || !Float.isFinite(value)
                || filterLogic.getFilterHandler().getStackInSlot(slot).isEmpty()) {
            return;
        }
        value = condition == AlchemyCondition.HURT ? Math.max(0.0F, Math.min(1.0F, value)) : condition.defaultValue();
        List<AlchemyFilterAttribute> attributes = getAlchemyAttributes();
        attributes.set(slot, attributes.get(slot).setConditionAndValue(condition, value));
        writeAlchemyAttributes(attributes);
        save();
    }

    public void setConditionValue(int slot, AlchemyCondition condition) {
        setConditionValue(slot, condition, condition.defaultValue());
    }

    public void setValue(int slot, float value) {
        if (slot < 0 || slot >= FILTER_SLOT_COUNT || getCondition(slot) != AlchemyCondition.HURT
                || !Float.isFinite(value)) {
            return;
        }
        setConditionValue(slot, AlchemyCondition.HURT, Math.max(0.0F, Math.min(1.0F, value)));
    }

    public boolean shouldMatchAllEffects() {
        return getBoolean("matchAllEffects", true);
    }

    public void setMatchAllEffects(boolean value) {
        setBoolean("matchAllEffects", value);
        save();
    }

    public boolean shouldMatchEffectDuration() {
        return getBoolean("matchEffectDuration", true);
    }

    public void setMatchEffectDuration(boolean value) {
        setBoolean("matchEffectDuration", value);
        save();
    }

    public boolean shouldMatchEffectAmplifier() {
        return getBoolean("matchEffectAmplifier", true);
    }

    public void setMatchEffectAmplifier(boolean value) {
        setBoolean("matchEffectAmplifier", value);
        save();
    }

    public boolean isValidAlchemyItem(ItemStack stack) {
        return isDefinedAlchemyItem(stack);
    }

    public List<AlchemyItemDefinition> getItemDefinitions() {
        return AlchemyDefinitionsAccessor.sophisticatedBackpackIsAddon$getItemDefinitions();
    }

    private boolean isDefinedAlchemyItem(ItemStack stack) {
        return !stack.isEmpty() && getItemDefinitions().stream().anyMatch(definition -> definition.filter().test(stack));
    }

    private void syncFilterAttributes(int changedSlot) {
        List<AlchemyFilterAttribute> attributes = getAlchemyAttributes();
        ItemStack filter = filterLogic.getFilterHandler().getStackInSlot(changedSlot).copy();
        AlchemyCondition condition = attributes.get(changedSlot).condition();
        float value = attributes.get(changedSlot).value();
        ItemStack previous = previousFilters[changedSlot];
        if (previous.isEmpty() && !filter.isEmpty()) {
            condition = getItemDefinitions().stream().filter(definition -> definition.filter().test(filter))
                    .map(definition -> definition.getDefaultCondition().apply(filter)).findFirst()
                    .orElse(AlchemyCondition.NEVER);
            value = condition.defaultValue();
        } else if (!previous.isEmpty() && filter.isEmpty()) {
            condition = AlchemyCondition.NEVER;
            value = condition.defaultValue();
        }
        previousFilters[changedSlot] = filter.copy();
        attributes.set(changedSlot, attributes.get(changedSlot).setFilter(filter)
                .setConditionAndValue(condition, value));
        writeAlchemyAttributes(attributes);
        save();
    }

    private void normalizePersistedSettings() {
        boolean changed = false;
        var alchemy = NBTHelper.getCompound(upgrade, "alchemy").orElse(null);
        if (alchemy != null && alchemy.contains("filters", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag filters = alchemy.getCompound("filters");
            if (filters.contains("Items", net.minecraft.nbt.Tag.TAG_LIST)) {
                net.minecraft.nbt.ListTag items = filters.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = items.size() - 1; i >= 0; i--) {
                    if (items.getCompound(i).getByte("Slot") >= FILTER_SLOT_COUNT) {
                        items.remove(i);
                        changed = true;
                    }
                }
            }
        }
        List<AlchemyFilterAttribute> attributes = readAlchemyAttributes();
        if (attributes.size() > FILTER_SLOT_COUNT) {
            writeAlchemyAttributes(List.copyOf(attributes.subList(0, FILTER_SLOT_COUNT)));
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    private List<AlchemyFilterAttribute> readAlchemyAttributes() {
        if (!upgrade.hasTag() || !upgrade.getTag().contains("alchemy_filter_attributes", net.minecraft.nbt.Tag.TAG_LIST)) {
            return List.of();
        }
        net.minecraft.nbt.ListTag list = upgrade.getTag().getList("alchemy_filter_attributes", net.minecraft.nbt.Tag.TAG_COMPOUND);
        List<AlchemyFilterAttribute> attributes = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            attributes.add(AlchemyFilterAttribute.deserializeNBT(list.getCompound(i)));
        }
        return attributes;
    }

    private void writeAlchemyAttributes(List<AlchemyFilterAttribute> attributes) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        attributes.forEach(attribute -> list.add(attribute.serializeNBT()));
        upgrade.getOrCreateTag().put("alchemy_filter_attributes", list);
    }

    private boolean getBoolean(String key, boolean fallback) {
        return !upgrade.hasTag() || !upgrade.getTag().contains(key) ? fallback : upgrade.getTag().getBoolean(key);
    }

    private void setBoolean(String key, boolean value) {
        upgrade.getOrCreateTag().putBoolean(key, value);
        save();
    }

    @Override
    public void tick(Entity entity, Level level, net.minecraft.core.BlockPos pos) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player) {
            AlchemyService.tick(this, player);
        }
    }

    public void setServiceCooldown(Level level, int ticks) {
        setCooldown(level, ticks);
    }

    public boolean hasPendingReservation() {
        return AlchemyReservation.hasPending(upgrade);
    }

    public void saveReservationChange() {
        save();
    }
}
