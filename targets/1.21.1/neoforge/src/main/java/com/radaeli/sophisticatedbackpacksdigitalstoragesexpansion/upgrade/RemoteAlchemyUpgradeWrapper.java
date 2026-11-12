package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterAttributes;
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
        normalizeFilterAttributes();
        filterLogic = new FilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT, this::isDefinedAlchemyItem,
                ModCoreDataComponents.FILTER_ATTRIBUTES);
        var filterHandler = filterLogic.getFilterHandler();
        for (int slot = 0; slot < FILTER_SLOT_COUNT; slot++) {
            previousFilters[slot] = filterHandler.getStackInSlot(slot).copy();
        }
        filterLogic.getFilterHandler().setOnSlotChange(this::syncFilterAttributes);
        normalizeAlchemyAttributes();
    }

    @Override
    public FilterLogic getFilterLogic() {
        return filterLogic;
    }

    public List<AlchemyFilterAttribute> getAlchemyAttributes() {
        List<AlchemyFilterAttribute> stored = upgrade.getOrDefault(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, List.of());
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
        value = condition == AlchemyCondition.HURT ? Math.clamp(value, 0.0F, 1.0F) : condition.defaultValue();
        List<AlchemyFilterAttribute> attributes = getAlchemyAttributes();
        attributes.set(slot, attributes.get(slot).setConditionAndValue(condition, value));
        upgrade.set(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, List.copyOf(attributes));
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
        setConditionValue(slot, AlchemyCondition.HURT, Math.clamp(value, 0.0F, 1.0F));
    }

    public boolean shouldMatchAllEffects() {
        return upgrade.getOrDefault(ModCoreDataComponents.MATCH_ALL_EFFECTS, true);
    }

    public void setMatchAllEffects(boolean value) {
        upgrade.set(ModCoreDataComponents.MATCH_ALL_EFFECTS, value);
        save();
    }

    public boolean shouldMatchEffectDuration() {
        return upgrade.getOrDefault(ModCoreDataComponents.MATCH_EFFECT_DURATION, true);
    }

    public void setMatchEffectDuration(boolean value) {
        upgrade.set(ModCoreDataComponents.MATCH_EFFECT_DURATION, value);
        save();
    }

    public boolean shouldMatchEffectAmplifier() {
        return upgrade.getOrDefault(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER, true);
    }

    public void setMatchEffectAmplifier(boolean value) {
        upgrade.set(ModCoreDataComponents.MATCH_EFFECT_AMPLIFIER, value);
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
        upgrade.set(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, List.copyOf(attributes));
        save();
    }

    private void normalizeAlchemyAttributes() {
        List<AlchemyFilterAttribute> stored = upgrade.getOrDefault(
                ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES, List.of());
        if (stored.size() > FILTER_SLOT_COUNT) {
            upgrade.set(ModCoreDataComponents.ALCHEMY_FILTER_ATTRIBUTES,
                    List.copyOf(stored.subList(0, FILTER_SLOT_COUNT)));
            save();
        }
    }

    private void normalizeFilterAttributes() {
        FilterAttributes stored = upgrade.get(ModCoreDataComponents.FILTER_ATTRIBUTES);
        if (stored == null || stored.filterItems().getSlots() <= FILTER_SLOT_COUNT) {
            return;
        }
        net.minecraft.core.NonNullList<ItemStack> storedFilters = net.minecraft.core.NonNullList.withSize(
                stored.filterItems().getSlots(), ItemStack.EMPTY);
        stored.filterItems().copyInto(storedFilters);
        List<ItemStack> filters = List.copyOf(storedFilters.subList(0, FILTER_SLOT_COUNT));
        FilterAttributes normalized = new FilterAttributes(stored.tagKeys(), stored.isAllowList(),
                stored.matchDurability(), stored.matchComponents(), stored.primaryMatch(), stored.matchAnyTag(),
                net.minecraft.world.item.component.ItemContainerContents.fromItems(filters),
                stored.filterByStorage(), stored.filterByInventory());
        upgrade.set(ModCoreDataComponents.FILTER_ATTRIBUTES, normalized);
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
