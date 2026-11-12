package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PumpTransferServiceTest extends MinecraftStorageTest {
    @Test
    void realNeoForgeBucketCapabilitiesWorkInBothDirectionsWithNoBackpackTank() {
        var full = FluidUtil.getFluidHandler(new ItemStack(Items.WATER_BUCKET)).orElseThrow();
        FluidTank network = tank(2_000, 0);
        assertTrue(transfer(full, new PumpStorageRouter(network, null, true), new ArrayList<>()));
        assertTrue(full.getContainer().is(Items.BUCKET));
        assertEquals(1_000, network.getFluidAmount());
        ItemStack stackedEmpty = new ItemStack(Items.BUCKET, 16);
        var empty = FluidUtil.getFluidHandler(stackedEmpty.copyWithCount(1)).orElseThrow();
        assertTrue(transfer(new PumpStorageRouter(network, null, true), empty, new ArrayList<>()));
        assertTrue(empty.getContainer().is(Items.WATER_BUCKET));
        assertEquals(16, stackedEmpty.getCount());
        assertEquals(0, network.getFluidAmount());
    }

    @Test
    void splitExtractionPersistsFirstSourceBeforeCallingTheSecond() {
        FluidTank first = tank(1_000, 400);
        AtomicReference<FluidStack> pending = new AtomicReference<>(FluidStack.EMPTY);
        FluidTank second = new FluidTank(1_000) {
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                if (action.execute()) {
                    assertEquals(400, pending.get().getAmount());
                    throw new IllegalStateException("Second source failed");
                }
                return super.drain(resource, action);
            }
        };
        second.fill(water(600), IFluidHandler.FluidAction.EXECUTE);
        var sources = new PumpStorageRouter(first, second, true);
        sources.onExtraction(pending::set);
        assertThrows(IllegalStateException.class, () -> sources.drain(water(1_000), IFluidHandler.FluidAction.EXECUTE));
        assertEquals(400, pending.get().getAmount());
        assertEquals(0, first.getFluidAmount());
        assertEquals(600, second.getFluidAmount());
    }

    @Test
    void inputSplitsOneBucketBetweenNetworkAndBackpackInPriorityOrder() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank network = tank(600, 0);
        FluidTank backpack = tank(2_000, 0);
        var router = new PumpStorageRouter(network, backpack, true);
        List<FluidStack> persisted = new ArrayList<>();
        assertTrue(transfer(source, router, persisted));
        assertEquals(0, source.getFluidAmount());
        assertEquals(600, network.getFluidAmount());
        assertEquals(400, backpack.getFluidAmount());
        assertEquals(1_000, persisted.getFirst().getAmount());
        assertTrue(persisted.getLast().isEmpty());
    }

    @Test
    void outputCombinesMatchingFluidFromBothSourcesWithoutRequiringLocalTank() {
        FluidTank network = tank(1_000, 400);
        FluidTank backpack = tank(1_000, 600);
        FluidTank external = tank(1_000, 0);
        var router = new PumpStorageRouter(network, backpack, true);
        assertTrue(transfer(router, external, new ArrayList<>()));
        assertEquals(1_000, external.getFluidAmount());
        assertEquals(0, network.getFluidAmount());
        assertEquals(0, backpack.getFluidAmount());
        assertTrue(router.usedRemoteExtraction());
        external.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        network.fill(water(1_000), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(transfer(new PumpStorageRouter(network, null, true), external, new ArrayList<>()));
        assertEquals(1_000, external.getFluidAmount());
    }

    @Test
    void backpackFirstAndNetworkUnavailableUseOnlyTheRequiredFallbackAmount() {
        FluidTank backpack = tank(600, 0);
        FluidTank network = tank(2_000, 0);
        assertTrue(transfer(tank(1_000, 1_000), new PumpStorageRouter(network, backpack, false), new ArrayList<>()));
        assertEquals(600, backpack.getFluidAmount());
        assertEquals(400, network.getFluidAmount());
        var localOnly = new PumpStorageRouter(null, tank(1_000, 0), true);
        assertTrue(transfer(tank(1_000, 1_000), localOnly, new ArrayList<>()));
        assertFalse(new PumpStorageRouter(null, null, true).isAvailable());
    }

    @Test
    void simulationsDoNotConsumeSourcesDestinationsOrRemoteOriginState() {
        FluidTank network = tank(2_000, 1_000);
        FluidTank backpack = tank(2_000, 1_000);
        var router = new PumpStorageRouter(network, backpack, true);
        assertEquals(1_000, router.drain(2_000, IFluidHandler.FluidAction.SIMULATE).getAmount());
        assertEquals(1_000, router.fill(water(2_000), IFluidHandler.FluidAction.SIMULATE));
        assertEquals(1_000, network.getFluidAmount());
        assertEquals(1_000, backpack.getFluidAmount());
        assertFalse(router.usedRemoteExtraction());
    }

    @Test
    void filteredOrRejectedFirstFluidDoesNotHideOtherNetworkFluids() {
        FluidTank water = tank(1_000, 1_000);
        FluidTank lava = new FluidTank(1_000);
        lava.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE);
        var sources = new PumpStorageRouter(water, lava, true);
        FluidTank destination = new FluidTank(1_000, stack -> stack.is(Fluids.LAVA));
        assertTrue(transfer(sources, destination, new ArrayList<>()));
        assertTrue(destination.getFluid().is(Fluids.LAVA));
        assertEquals(1_000, water.getFluidAmount());
        destination.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        lava.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(PumpTransferService.transfer(sources, destination, 1_000, 1,
                stack -> stack.is(Fluids.LAVA), ignored -> {}, false));
    }

    @Test
    void wholeBucketSelectionCanCombineLaterFluidEvenWhenEachSourceHasLessThanABucket() {
        FluidTank networkLava = new FluidTank(1_000);
        networkLava.fill(new FluidStack(Fluids.LAVA, 300), IFluidHandler.FluidAction.EXECUTE);
        FluidTank backpackLava = new FluidTank(1_000);
        backpackLava.fill(new FluidStack(Fluids.LAVA, 700), IFluidHandler.FluidAction.EXECUTE);
        var network = new PumpStorageRouter(tank(1_000, 100), networkLava, true);
        var sources = new PumpStorageRouter(network, backpackLava, true);
        var candidates = PumpTransferService.candidates(sources, 1_000, stack -> stack.getAmount() == 1_000);
        assertEquals(1, candidates.size());
        assertTrue(candidates.getFirst().is(Fluids.LAVA));
        assertEquals(1_000, candidates.getFirst().getAmount());
        assertEquals(300, networkLava.getFluidAmount());
        assertEquals(700, backpackLava.getFluidAmount());
    }

    @Test
    void wholeWorldBucketIsNotRemovedWhenCombinedDestinationHasInsufficientSpace() {
        FluidTank source = tank(1_000, 1_000);
        var storage = new PumpStorageRouter(tank(400, 0), tank(500, 0), true);
        assertFalse(PumpTransferService.transfer(source, storage, 1_000, 1_000,
                stack -> true, ignored -> fail("Must not reserve or extract a partial world bucket"), true));
        assertEquals(1_000, source.getFluidAmount());
        assertTrue(PumpTransferService.transfer(source,
                new PumpStorageRouter(tank(400, 0), tank(600, 0), true), 1_000, 1_000,
                stack -> true, ignored -> {}, true));
        assertEquals(0, source.getFluidAmount());
    }

    @Test
    void destinationChangesAfterSimulationAndRemainderReturnsToSource() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank destination = new FluidTank(1_000) {
            @Override public int fill(FluidStack resource, FluidAction action) {
                return super.fill(resource.copyWithAmount(action.simulate() ? resource.getAmount() : 300), action);
            }
        };
        List<FluidStack> persisted = new ArrayList<>();
        assertTrue(transfer(source, destination, persisted));
        assertEquals(300, destination.getFluidAmount());
        assertEquals(700, source.getFluidAmount());
        assertEquals(1_000, persisted.getFirst().getAmount());
        assertEquals(700, persisted.get(1).getAmount());
        assertTrue(persisted.getLast().isEmpty());
    }

    @Test
    void nonReturnableRemainderStaysReservedAndStopsAfterOneInteraction() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank destination = new FluidTank(1_000) {
            @Override public int fill(FluidStack resource, FluidAction action) {
                return action.simulate() ? resource.getAmount() : 0;
            }
        };
        AtomicReference<FluidStack> pending = new AtomicReference<>(FluidStack.EMPTY);
        assertTrue(PumpTransferService.transfer(source, destination, 1_000, 1,
                stack -> true, pending::set, false));
        assertEquals(0, source.getFluidAmount());
        assertEquals(0, destination.getFluidAmount());
        assertEquals(1_000, pending.get().getAmount());
    }

    @Test
    void componentsRemainDistinctAndArePreservedByRouting() {
        FluidStack named = water(600);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special water"));
        FluidTank first = tank(1_000, 400);
        FluidTank second = new FluidTank(1_000);
        second.fill(named, IFluidHandler.FluidAction.EXECUTE);
        var router = new PumpStorageRouter(first, second, true);
        FluidStack plain = router.drain(water(1_000), IFluidHandler.FluidAction.EXECUTE);
        assertEquals(400, plain.getAmount());
        assertEquals(600, second.getFluidAmount());
        FluidStack extracted = router.drain(named, IFluidHandler.FluidAction.EXECUTE);
        assertTrue(FluidStack.isSameFluidSameComponents(named, extracted));
        assertEquals(600, extracted.getAmount());
    }

    @Test
    void drainOnlyCapabilitiesWorkWithoutTankEnumeration() {
        FluidTank actual = tank(1_000, 1_000);
        FluidTank drainOnly = new FluidTank(1_000) {
            @Override public int getTanks() { return 0; }
            @Override public FluidStack drain(int amount, FluidAction action) { return actual.drain(amount, action); }
            @Override public FluidStack drain(FluidStack resource, FluidAction action) {
                return actual.drain(resource, action);
            }
        };
        FluidTank destination = tank(1_000, 0);
        assertTrue(transfer(drainOnly, destination, new ArrayList<>()));
        assertEquals(1_000, destination.getFluidAmount());
    }

    @Test
    void emptyOrRejectedStorageNeverExtractsOrPersists() {
        FluidTank source = tank(1_000, 1_000);
        assertFalse(PumpTransferService.transfer(source, tank(0, 0), 1_000, 1,
                stack -> true, ignored -> fail("No extraction expected"), true));
        assertEquals(1_000, source.getFluidAmount());
    }

    private static boolean transfer(IFluidHandler source, IFluidHandler destination, List<FluidStack> persisted) {
        return PumpTransferService.transfer(source, destination, 1_000, 1,
                stack -> true, stack -> persisted.add(stack.copy()), true);
    }

    private static FluidTank tank(int capacity, int amount) {
        FluidTank tank = new FluidTank(capacity);
        if (amount > 0) tank.fill(water(amount), IFluidHandler.FluidAction.EXECUTE);
        return tank;
    }

    private static FluidStack water(int amount) { return new FluidStack(Fluids.WATER, amount); }
}
