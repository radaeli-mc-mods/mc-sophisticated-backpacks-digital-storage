package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransactionalItemBufferTest extends MinecraftStorageTest {
    @Test void rootAbortHasNoExternalSideEffects() {
        List<ItemStack> persisted = new ArrayList<>();
        AtomicInteger deliveries = new AtomicInteger();
        var buffer = new TransactionalItemBuffer(ItemStack.EMPTY, persisted::add);
        try (var tx = Transaction.openRoot()) {
            assertEquals(8, buffer.accept(ItemResource.of(Items.DIAMOND), 8, tx, stack -> deliveries.incrementAndGet()));
            assertEquals(0, deliveries.get());
            assertTrue(persisted.isEmpty());
        }
        assertFalse(buffer.hasPending());
        assertEquals(0, deliveries.get());
        assertTrue(persisted.isEmpty());
    }
    @Test void committedChildIsStillRevertedByRootAbort() {
        AtomicInteger deliveries = new AtomicInteger();
        var buffer = new TransactionalItemBuffer(ItemStack.EMPTY, ignored -> fail("Aborted state was persisted"));
        try (var root = Transaction.openRoot()) {
            try (var child = Transaction.open(root)) {
                buffer.accept(ItemResource.of(Items.DIAMOND), 3, child, stack -> deliveries.incrementAndGet());
                child.commit();
            }
            assertEquals(0, deliveries.get());
        }
        assertEquals(0, deliveries.get());
        assertFalse(buffer.hasPending());
    }
    @Test void abortedChildCannotDeliverWhenParentCommits() {
        AtomicInteger deliveries = new AtomicInteger();
        var buffer = new TransactionalItemBuffer(ItemStack.EMPTY, ignored -> {});
        try (var root = Transaction.openRoot()) {
            try (var child = Transaction.open(root)) {
                buffer.accept(ItemResource.of(Items.DIAMOND), 3, child, stack -> deliveries.incrementAndGet());
            }
            root.commit();
        }
        assertEquals(0, deliveries.get());
        assertFalse(buffer.hasPending());
    }
    @Test void persistencePrecedesDeliveryAndCallbackCanOpenANewTransaction() {
        List<String> events = new ArrayList<>();
        var buffer = new TransactionalItemBuffer(ItemStack.EMPTY, stack -> events.add("persist:" + stack.getCount()));
        try (var root = Transaction.openRoot()) {
            buffer.accept(ItemResource.of(Items.DIAMOND), 8, root, stack -> {
                events.add("deliver:" + stack.getCount());
                try (var recovery = Transaction.openRoot()) { recovery.commit(); }
                buffer.replace(stack.copyWithCount(2));
            });
            assertTrue(events.isEmpty());
            root.commit();
        }
        assertEquals(List.of("persist:8", "deliver:8", "persist:2"), events);
        assertEquals(2, buffer.pending().getCount());
    }
    @Test void savedRemainderKeepsComponentsAndBlocksAnotherOffer() {
        ItemStack named = new ItemStack(Items.PAPER, 2);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("reserved"));
        var buffer = new TransactionalItemBuffer(named, ignored -> {});
        try (var tx = Transaction.openRoot()) {
            assertEquals(0, buffer.accept(ItemResource.of(Items.DIAMOND), 64, tx, ignored -> fail()));
            tx.commit();
        }
        assertTrue(ItemStack.matches(named, buffer.pending()));
        ItemStack copy = buffer.pending(); copy.setCount(64);
        assertEquals(2, buffer.pending().getCount());
    }
    @Test void reservationIsBoundedByTheResourceStackLimit() {
        var buffer = new TransactionalItemBuffer(ItemStack.EMPTY, ignored -> {});
        try (var tx = Transaction.openRoot()) {
            assertEquals(1, buffer.accept(ItemResource.of(Items.DIAMOND_SWORD), Integer.MAX_VALUE, tx, ignored -> {}));
        }
    }
}
