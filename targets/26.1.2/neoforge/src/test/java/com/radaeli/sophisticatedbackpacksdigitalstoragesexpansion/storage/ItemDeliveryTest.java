package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.DestinationItemFilter;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.ManuallyLinkedUpgrade;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemDeliveryTest extends MinecraftStorageTest {
    @Test public void knownPartialDeliveryIsCheckpointedBeforeEitherFailingDestination() {
        for (SourcePriority priority : SourcePriority.values()) {
            var upgrade = new ItemStack(Items.PAPER);
            var journal = journal(upgrade);
            AtomicInteger accepted = new AtomicInteger();
            java.util.function.UnaryOperator<ItemStack> first = offer -> {
                accepted.addAndGet(16);
                return offer.copyWithCount(offer.getCount() - 16);
            };
            java.util.function.UnaryOperator<ItemStack> second = offer -> {
                assertEquals(48, journal.pending().getCount());
                assertTrue(journal.uncertain());
                throw new IllegalStateException("injected failure before mutation");
            };
            ItemStack remainder = DestinationItemFilter.routeDelivery(priority, new ItemStack(Items.STONE, 64),
                    priority == SourcePriority.NETWORK_FIRST ? first : second,
                    priority == SourcePriority.NETWORK_FIRST ? second : first, journal);
            assertTrue(remainder.isEmpty());
            assertEquals(64, accepted.get() + journal.pending().getCount());
            var restored = journal(upgrade.copy());
            assertTrue(restored.uncertain());
            assertEquals(48, restored.pending().getCount());
            assertEquals(5, DestinationItemFilter.routeDelivery(priority, new ItemStack(Items.STONE, 5),
                    offer -> { fail("must not resend on next tick"); return offer; },
                    offer -> { fail("must not resend on next tick"); return offer; }, restored).getCount());
            assertFalse(new ManuallyLinkedUpgrade() {}.canChangeBinding(upgrade));
        }
    }
    @Test public void mutationThenExceptionNeverRefundsOrInvokesTheFallback() {
        var upgrade = new ItemStack(Items.PAPER);
        AtomicInteger accepted = new AtomicInteger();
        ItemStack remainder = DestinationItemFilter.routeDelivery(SourcePriority.NETWORK_FIRST,
                new ItemStack(Items.STONE, 64), offer -> {
                    accepted.set(20);
                    throw new IllegalStateException("injected failure after partial commit");
                }, offer -> { fail("uncertain delivery must stop fallback"); return offer; }, journal(upgrade));
        assertEquals(20, accepted.get());
        assertTrue(remainder.isEmpty());
        assertTrue(journal(upgrade.copy()).uncertain());
        // This is unresolved custody, not 64 spendable items or an automatic refund.
        assertEquals(64, journal(upgrade).pending().getCount());
    }
    @Test public void knownRemainderReturnsToTheDropAndSettlesCustody() {
        var upgrade = new ItemStack(Items.PAPER);
        var remainder = DestinationItemFilter.routeDelivery(SourcePriority.BACKPACK_FIRST,
                new ItemStack(Items.STONE, 64), offer -> offer.copyWithCount(offer.getCount() - 8),
                offer -> offer.copyWithCount(offer.getCount() - 16), journal(upgrade));
        assertEquals(40, remainder.getCount());
        assertFalse(ItemDeliveryJournal.hasPending(upgrade));
    }
    private static ItemDeliveryJournal journal(ItemStack stack) {
        return new ItemDeliveryJournal(stack, net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY), () -> {});
    }
}
