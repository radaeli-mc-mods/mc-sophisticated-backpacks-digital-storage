package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Read-only native view for Sophisticated's destination-content filter. */
public record ResourceSnapshots(IItemHandler contents) implements ResourceHandler<ItemResource> {
    @Override public int size() { return contents.getSlots(); }
    @Override public ItemResource getResource(int index) { return ItemResource.of(contents.getStackInSlot(index)); }
    @Override public long getAmountAsLong(int index) { return contents.getStackInSlot(index).getCount(); }
    @Override public long getCapacityAsLong(int index, ItemResource resource) { return contents.getSlotLimit(index); }
    @Override public boolean isValid(int index, ItemResource resource) { return false; }
    @Override public int insert(int index, ItemResource resource, int amount, TransactionContext tx) { return 0; }
    @Override public int extract(int index, ItemResource resource, int amount, TransactionContext tx) { return 0; }
}
