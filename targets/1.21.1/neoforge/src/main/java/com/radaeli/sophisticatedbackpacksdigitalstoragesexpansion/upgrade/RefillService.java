package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper.TargetSlot;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

/** Runtime behavior for remote Refill; all counts come from the actual selected destination and extraction. */
final class RefillService {
    private static final int COOLDOWN_TICKS = 5;

    private RefillService() {
    }

    static void tick(RemoteRefillUpgradeWrapper wrapper, ServerPlayer player) {
        Level level = player.level();
        if (!isInstalled(wrapper) || wrapper.isInCooldown(level)) {
            return;
        }
        if (!wrapper.isEnabled()) {
            wrapper.setServiceCooldown(level, COOLDOWN_TICKS);
            return;
        }

        FilterLogic filters = wrapper.getFilterLogic();
        for (int filterSlot = 0; filterSlot < RemoteRefillUpgradeWrapper.FILTER_SLOT_COUNT; filterSlot++) {
            ItemStack filter = filters.getFilterHandler().getStackInSlot(filterSlot);
            if (filter.isEmpty()) {
                continue;
            }
            TargetSlot targetSlot = wrapper.getTargetSlot(filterSlot);
            Destination destination = findDestination(player, targetSlot, filter);
            if (destination == null || destination.missing() <= 0) {
                continue;
            }
            List<UpgradeItemSources.SourceItem> sources = UpgradeItemSources.find(wrapper, player,
                    wrapper.getSourcePriority(), candidate -> filters.stackMatchesFilter(candidate, filter), java.util.Optional.empty(), destination.missing());
            for (UpgradeItemSources.SourceItem source : sources) {
                int requested = (int) Math.min(destination.missing(), Math.min(source.amount(), Integer.MAX_VALUE));
                if (requested <= 0) {
                    continue;
                }
                ItemStack extracted = UpgradeItemSources.extract(wrapper, player, source, requested);
                if (extracted.isEmpty()) {
                    continue;
                }
                ItemStack remainder = insertIntoDestination(player, destination, filter, extracted);
                if (!remainder.isEmpty()) {
                    ItemStack returned = UpgradeItemSources.returnToSource(wrapper, player, source, remainder);
                    if (!returned.isEmpty()) {
                        UpgradeItemSources.recover(wrapper, player, returned);
                    }
                }
                if (extracted.getCount() > 0) {
                    wrapper.setServiceCooldown(level, COOLDOWN_TICKS);
                    return;
                }
            }
        }
        wrapper.setServiceCooldown(level, COOLDOWN_TICKS);
    }

    static boolean pickBlock(RemoteRefillUpgradeWrapper wrapper, Player player, ItemStack picked) {
        if (!(player instanceof ServerPlayer serverPlayer) || picked.isEmpty() || !wrapper.isEnabled()
                || !isInstalled(wrapper)) {
            return false;
        }
        FilterLogic filters = wrapper.getFilterLogic();
        boolean configured = false;
        for (int slot = 0; slot < RemoteRefillUpgradeWrapper.FILTER_SLOT_COUNT; slot++) {
            ItemStack filter = filters.getFilterHandler().getStackInSlot(slot);
            if (!filter.isEmpty() && filters.stackMatchesFilter(picked, filter)) {
                configured = true;
                break;
            }
        }
        if (!configured) {
            return false;
        }

        int hotbarSlot = player.getInventory().getSuitableHotbarSlot();
        Inventory inventory = player.getInventory();
        ItemStack previous = inventory.getItem(hotbarSlot);
        int missing = Math.max(0, picked.getMaxStackSize() - (ItemStack.isSameItemSameComponents(previous, picked)
                ? previous.getCount() : 0));
        if (missing == 0) {
            return true;
        }
        if (!previous.isEmpty() && !ItemStack.isSameItemSameComponents(previous, picked)) {
            ItemStack remainder = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing().insertItem(previous.copy(), true);
            if (!remainder.isEmpty()) {
                return false;
            }
        }

        int requested = missing;
        List<UpgradeItemSources.SourceItem> sources = UpgradeItemSources.find(wrapper, serverPlayer,
                wrapper.getSourcePriority(), candidate -> ItemStack.isSameItemSameComponents(candidate, picked), java.util.Optional.of(picked.getItem()), missing);
        for (UpgradeItemSources.SourceItem source : sources) {
            ItemStack extracted = UpgradeItemSources.extract(wrapper, serverPlayer, source, requested);
            if (extracted.isEmpty()) {
                continue;
            }
            if (!previous.isEmpty() && !ItemStack.isSameItemSameComponents(previous, picked)) {
                ItemStack backpackRemainder = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing()
                        .insertItem(previous.copy(), false);
                if (!backpackRemainder.isEmpty()) {
                    inventory.placeItemBackInInventory(backpackRemainder);
                }
            }
            inventory.selected = hotbarSlot;
            if (ItemStack.isSameItemSameComponents(previous, extracted)) {
                extracted.grow(previous.getCount());
            }
            inventory.setItem(hotbarSlot, extracted);
            return true;
        }
        return false;
    }

    private static Destination findDestination(Player player, TargetSlot targetSlot, ItemStack filter) {
        Inventory inventory = player.getInventory();
        if (targetSlot == TargetSlot.MAIN_HAND) {
            return handDestination(player, InteractionHand.MAIN_HAND, filter);
        }
        if (targetSlot == TargetSlot.OFF_HAND) {
            return handDestination(player, InteractionHand.OFF_HAND, filter);
        }
        if (targetSlot.name().startsWith("TOOLBAR_")) {
            int slot = Integer.parseInt(targetSlot.name().substring("TOOLBAR_".length())) - 1;
            return inventoryDestination(inventory, slot, filter);
        }
        // ANY means refill an existing stack first, then use an empty slot. This
        // avoids fragmenting the selected item across inventory slots.
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                continue;
            }
            Destination destination = inventoryDestination(inventory, slot, filter);
            if (destination != null && destination.missing() > 0) {
                return destination;
            }
        }
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                continue;
            }
            Destination destination = inventoryDestination(inventory, slot, filter);
            if (destination != null && destination.missing() > 0) {
                return destination;
            }
        }
        return null;
    }

    private static Destination handDestination(Player player, InteractionHand hand, ItemStack filter) {
        ItemStack existing = player.getItemInHand(hand);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, filter)) {
            return null;
        }
        int count = existing.isEmpty() ? 0 : existing.getCount();
        return new Destination(-1, hand, existing.copy(), Math.max(0, filter.getMaxStackSize() - count), filter.copy());
    }

    private static Destination inventoryDestination(Inventory inventory, int slot, ItemStack filter) {
        if (slot < 0 || slot >= inventory.items.size()) {
            return null;
        }
        ItemStack existing = inventory.getItem(slot);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, filter)) {
            return null;
        }
        int count = existing.isEmpty() ? 0 : existing.getCount();
        int capacity = Math.min(filter.getMaxStackSize(), inventory.getMaxStackSize()) - count;
        return new Destination(slot, null, existing.copy(), Math.max(0, capacity), filter.copy());
    }

    private static ItemStack insertIntoDestination(Player player, Destination destination, ItemStack filter, ItemStack offered) {
        ItemStack current = destination.hand() == null
                ? player.getInventory().getItem(destination.slot())
                : player.getItemInHand(destination.hand());
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, filter)) {
            return offered;
        }
        int currentCount = current.isEmpty() ? 0 : current.getCount();
        int currentCapacity = destination.hand() == null
                ? Math.min(filter.getMaxStackSize(), player.getInventory().getMaxStackSize())
                : filter.getMaxStackSize();
        int accepted = Math.min(Math.max(0, currentCapacity - currentCount), offered.getCount());
        if (accepted <= 0) {
            return offered;
        }
        ItemStack updated = current.isEmpty() ? offered.copyWithCount(accepted) : current.copy();
        if (!current.isEmpty()) {
            updated.grow(accepted);
        }
        if (destination.hand() == null) {
            player.getInventory().setItem(destination.slot(), updated);
        } else {
            player.setItemInHand(destination.hand(), updated);
        }
        ItemStack remainder = offered.copy();
        remainder.shrink(accepted);
        return remainder;
    }

    private static boolean isInstalled(RemoteRefillUpgradeWrapper wrapper) {
        return wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper);
    }

    private record Destination(int slot, InteractionHand hand, ItemStack original, int missing, ItemStack desired) {
    }
}
