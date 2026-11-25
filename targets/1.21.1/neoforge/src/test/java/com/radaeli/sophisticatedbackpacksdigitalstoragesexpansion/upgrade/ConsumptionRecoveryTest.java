package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.ItemDelivery;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.transfer.DeliveryJournal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConsumptionRecoveryTest {
    @BeforeAll static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    @Test public void eachConsumptionReservationCheckpointsPartialReturnsAndSurvivesReloadWithoutRetry() {
        for (boolean alchemy : new boolean[]{false, true}) {
            var upgrade = new ItemStack(Items.PAPER);
            var reservation = reservation(upgrade, alchemy, true);
            AtomicInteger local = new AtomicInteger(), remote = new AtomicInteger(), calls = new AtomicInteger();
            var steps = List.of(ItemDelivery.step("remote", stack -> { remote.addAndGet(1); return stack.copyWithCount(2); }),
                    ItemDelivery.step("local", stack -> { local.addAndGet(1); return stack.copyWithCount(1); }),
                    ItemDelivery.step("player", stack -> {
                        calls.incrementAndGet();
                        assertEquals(1, reservation.pending().get().getCount());
                        throw new IllegalStateException("injected container return failure");
                    }));
            assertFalse(ConsumptionRecovery.deliver(new ItemStack(Items.GLASS_BOTTLE, 3), steps,
                    reservation.journal(), reservation.uncertain(), reservation.clear()));
            assertEquals(3, local.get() + remote.get() + reservation.pending().get().getCount());
            var reloaded = reservation(upgrade.copy(), alchemy, false);
            assertTrue(reloaded.uncertain().getAsBoolean());
            assertFalse(ConsumptionRecovery.deliver(reloaded.pending().get(), steps, reloaded.journal(),
                    reloaded.uncertain(), reloaded.clear()));
            assertEquals(1, calls.get());
            assertEquals(1, reloaded.pending().get().getCount());
        }
    }
    @Test public void largeReturnedCountsAreIndependentOfNativeStackCountEncoding() {
        for (boolean alchemy : new boolean[]{false, true}) {
            var upgrade = new ItemStack(Items.PAPER);
            var reservation = reservation(upgrade, alchemy, true);
            reservation.journal().checkpoint(new ItemStack(Items.GLASS_BOTTLE, 16_384));
            reservation.journal().beginDelivery("large_container_return");
            var restored = reservation(upgrade.copy(), alchemy, false);
            assertEquals(16_384, restored.pending().get().getCount());
            assertTrue(restored.uncertain().getAsBoolean());
        }
    }
    @Test public void knownCompleteReturnClearsOnlyTheCorrespondingReservation() {
        for (boolean alchemy : new boolean[]{false, true}) {
            var upgrade = new ItemStack(Items.PAPER);
            var reservation = reservation(upgrade, alchemy, true);
            assertTrue(ConsumptionRecovery.deliver(new ItemStack(Items.GLASS_BOTTLE, 3),
                    List.of(ItemDelivery.step("local", stack -> ItemStack.EMPTY)),
                    reservation.journal(), reservation.uncertain(), reservation.clear()));
            assertFalse(alchemy ? AlchemyReservation.hasPending(upgrade) : FeedingReservation.hasPending(upgrade));
        }
    }
    private record Reservation(DeliveryJournal<ItemStack> journal, Supplier<ItemStack> pending, BooleanSupplier uncertain, Runnable clear) {}
    private static Reservation reservation(ItemStack upgrade, boolean alchemy, boolean initialize) {
        var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        if (initialize) assertTrue(alchemy ? AlchemyReservation.reserve(upgrade, new ItemStack(Items.GLASS_BOTTLE, 3), true, registries)
                : FeedingReservation.reserve(upgrade, new ItemStack(Items.GLASS_BOTTLE, 3), true, registries));
        return alchemy ? new Reservation(AlchemyReservation.journal(upgrade, registries, () -> {}), () -> AlchemyReservation.getStack(upgrade, registries),
                () -> AlchemyReservation.state(upgrade).orElse(null) == AlchemyReservation.State.IN_FLIGHT, () -> AlchemyReservation.clear(upgrade))
                : new Reservation(FeedingReservation.journal(upgrade, registries, () -> {}), () -> FeedingReservation.stack(upgrade, registries),
                () -> FeedingReservation.state(upgrade) == FeedingReservation.State.IN_FLIGHT, () -> FeedingReservation.clear(upgrade));
    }
}
