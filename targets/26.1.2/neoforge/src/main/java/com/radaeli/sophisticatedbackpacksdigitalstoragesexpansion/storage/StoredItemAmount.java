package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.minecraft.world.item.ItemStack;

/** One item identity and a long-sized available quantity from a network snapshot. */
public record StoredItemAmount(ItemStack exemplar, long amount) {
    public StoredItemAmount {
        exemplar = exemplar.copyWithCount(1);
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
    }
}
