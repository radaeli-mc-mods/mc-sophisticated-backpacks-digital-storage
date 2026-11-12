package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Owns committed pickup resources before entering a non-transactional backend. */
public final class TransactionalItemBuffer extends SnapshotJournal<TransactionalItemBuffer.State> {
    private ItemStack pending;
    private Consumer<ItemStack> delivery;
    private final Consumer<ItemStack> persist;

    public TransactionalItemBuffer(ItemStack initial, Consumer<ItemStack> persist) {
        pending = initial.copy();
        this.persist = persist;
    }

    public boolean hasPending() { return !pending.isEmpty(); }
    public ItemStack pending() { return pending.copy(); }

    public int accept(ItemResource resource, int amount, TransactionContext tx, Consumer<ItemStack> deliver) {
        if (amount <= 0 || resource.isEmpty() || !pending.isEmpty()) return 0;
        int accepted = Math.min(amount, resource.toStack(1).getMaxStackSize());
        updateSnapshots(tx);
        pending = resource.toStack(accepted);
        delivery = deliver;
        return accepted;
    }

    /** Called only after the root transaction has closed, or while recovering a saved reservation. */
    public void replace(ItemStack remaining) {
        if (isInTransaction()) throw new IllegalStateException("Cannot settle an open pickup transaction");
        pending = remaining.copy();
        persist.accept(pending.copy());
    }

    @Override protected State createSnapshot() { return new State(pending.copy(), delivery); }
    @Override protected void revertToSnapshot(State snapshot) {
        pending = snapshot.stack().copy();
        delivery = snapshot.delivery();
    }
    @Override protected void onRootCommit(State snapshot) {
        Consumer<ItemStack> committedDelivery = delivery;
        delivery = null;
        persist.accept(pending.copy());
        if (committedDelivery != null && !pending.isEmpty()) committedDelivery.accept(pending.copy());
    }
    public record State(ItemStack stack, Consumer<ItemStack> delivery) {}
}
