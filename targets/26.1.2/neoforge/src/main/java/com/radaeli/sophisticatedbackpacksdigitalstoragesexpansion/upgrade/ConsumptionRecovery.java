package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.ItemDelivery;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal;

/** Shared Feeding/Alchemy recovery: each returned container has exactly one recorded custodian. */
final class ConsumptionRecovery {
    private ConsumptionRecovery() {}
    static boolean returnPending(PriorityLinkedStorageUpgradeWrapper<?, ?> wrapper, ServerPlayer player,
            ItemStack pending, boolean networkFirst, DeliveryJournal<ItemStack> journal,
            BooleanSupplier uncertain, Runnable clear) {
        if (uncertain.getAsBoolean()) return false;
        var steps = new ArrayList<CheckpointedDelivery.Step<ItemStack>>();
        if (networkFirst) steps.add(ItemDelivery.step("consumption.remote", stack -> UpgradeItemSources.returnToLinkedNetwork(wrapper, player, stack)));
        var local = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.LocalItemAccess.of(wrapper.getStorageWrapper().getInventoryForUpgradeProcessing());
        for (int slot = 0; slot < local.getSlots(); slot++) {
            int destinationSlot = slot;
            steps.add(ItemDelivery.step("consumption.slot_" + slot, stack -> local.insertItem(destinationSlot, stack, false)));
        }
        steps.add(ItemDelivery.step("consumption.player", stack -> { player.getInventory().placeItemBackInInventory(stack); return ItemStack.EMPTY; }));
        return deliver(pending, steps, journal, uncertain, clear);
    }
    static boolean deliver(ItemStack pending, List<CheckpointedDelivery.Step<ItemStack>> steps,
            DeliveryJournal<ItemStack> journal, BooleanSupplier uncertain, Runnable clear) {
        if (uncertain.getAsBoolean()) return false;
        var result = ItemDelivery.deliver(pending, steps, journal);
        if (!result.uncertain() && result.remainder().isEmpty()) { clear.run(); return true; }
        return false;
    }
}
