package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.ItemStack;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.CheckpointedDelivery;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal;

/** Validates destination receipts before settling persisted custody. */
public final class ItemDelivery {
    private ItemDelivery() {}
    public static CheckpointedDelivery.Step<ItemStack> step(String stage, UnaryOperator<ItemStack> destination) {
        return new CheckpointedDelivery.Step<>(stage, offer -> {
            ItemStack remainder = destination.apply(offer.copy());
            if (remainder == null || (!remainder.isEmpty() && (!ItemStack.isSameItemSameComponents(offer, remainder)
                    || remainder.getCount() > offer.getCount()))) {
                throw new IllegalStateException("Invalid item delivery receipt at " + stage);
            }
            return remainder;
        });
    }
    public static CheckpointedDelivery.Result<ItemStack> deliver(ItemStack offered,
            List<CheckpointedDelivery.Step<ItemStack>> steps, DeliveryJournal<ItemStack> journal) {
        var result = CheckpointedDelivery.deliver(offered, steps, ItemStack::isEmpty, ItemStack::copy, journal);
        if (result.failure() != null) com.mojang.logging.LogUtils.getLogger().error(
                "Item delivery failed; uncertain={}, last checkpoint retained by upgrade", result.uncertain(), result.failure());
        return result;
    }
}
