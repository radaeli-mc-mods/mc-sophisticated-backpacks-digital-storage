package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;


import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ItemTransferServiceTest extends MinecraftStorageTest {
    @Test
    void movesOnlyMatchingStacksAndReportsActualCounts() {
        ItemStackHandler source = new ItemStackHandler(3);
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        source.setStackInSlot(1, new ItemStack(Items.DIRT, 7));
        var result = ItemTransferService.deposit(source, destination(64, 64), stack -> stack.is(Items.STONE), this::unexpectedRecovery);
        assertEquals(1, result.movedStacks());
        assertEquals(8, result.movedItems());
        assertTrue(source.getStackInSlot(0).isEmpty());
        assertEquals(7, source.getStackInSlot(1).getCount());
    }

    @Test
    void restoresDifferenceBetweenSimulatedAndActualAcceptance() {
        ItemStackHandler source = source(12);
        var result = ItemTransferService.deposit(source, destination(12, 3), stack -> true, this::unexpectedRecovery);
        assertEquals(3, result.movedItems());
        assertEquals(1, result.movedStacks());
        assertEquals(9, source.getStackInSlot(0).getCount());
    }

    @Test
    void zeroActualAcceptanceRestoresEverythingAndReportsNoMovement() {
        ItemStackHandler source = source(12);
        var result = ItemTransferService.deposit(source, destination(12, 0), stack -> true, this::unexpectedRecovery);
        assertEquals(12, source.getStackInSlot(0).getCount());
        assertFalse(result.movedAnything());
        assertEquals(0, result.movedStacks());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void nonPositiveSimulationNeverExtracts(int simulated) {
        ItemStackHandler source = source(8);
        ItemInsertion target = new ItemInsertion() {
            public int simulateInsert(ItemStack stack) { return simulated; }
            public ItemStack insert(ItemStack stack) { throw new AssertionError("Insertion must not run"); }
        };
        assertFalse(ItemTransferService.deposit(source, target, stack -> true, this::unexpectedRecovery).movedAnything());
        assertEquals(8, source.getStackInSlot(0).getCount());
    }

    @Test
    void simulationCannotOfferMoreThanTheSourceContains() {
        ItemStackHandler source = source(8);
        var result = ItemTransferService.deposit(source, destination(Integer.MAX_VALUE, Integer.MAX_VALUE),
                stack -> true, this::unexpectedRecovery);
        assertEquals(8, result.movedItems());
    }

    @Test
    void extractionCanReturnLessThanRequested() {
        ItemStackHandler source = new ItemStackHandler(1) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return super.extractItem(slot, Math.min(3, amount), simulate);
            }
        };
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        var result = ItemTransferService.deposit(source, destination(64, 64), stack -> true, this::unexpectedRecovery);
        assertEquals(3, result.movedItems());
        assertEquals(5, source.getStackInSlot(0).getCount());
    }

    @Test
    void aSourceThatRefusesExtractionDoesNotInsert() {
        ItemStackHandler source = new ItemStackHandler(1) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        };
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        ItemInsertion target = new ItemInsertion() {
            public int simulateInsert(ItemStack stack) { return 8; }
            public ItemStack insert(ItemStack stack) { throw new AssertionError("Nothing was extracted"); }
        };
        assertFalse(ItemTransferService.deposit(source, target, stack -> true, this::unexpectedRecovery).movedAnything());
        assertEquals(8, source.getStackInSlot(0).getCount());
    }

    @Test
    void restoresIntoAnotherSourceSlotBeforeUsingRecovery() {
        ItemStackHandler source = changedSource(2);
        ItemTransferService.deposit(source, destination(64, 0), stack -> stack.is(Items.STONE), this::unexpectedRecovery);
        assertTrue(source.getStackInSlot(0).is(Items.DIRT));
        assertTrue(source.getStackInSlot(1).is(Items.STONE));
        assertEquals(8, source.getStackInSlot(1).getCount());
    }

    @Test
    void recoveryReceivesEveryItemThatNoLongerFitsInSource() {
        ItemStackHandler source = changedSource(1);
        List<ItemStack> recovered = new ArrayList<>();
        var result = ItemTransferService.deposit(source, destination(64, 0), stack -> true, recovered::add);
        assertFalse(result.movedAnything());
        assertEquals(1, recovered.size());
        assertTrue(recovered.get(0).is(Items.STONE));
        assertEquals(8, recovered.get(0).getCount());
    }

    @Test
    void componentsSurvivePartialInsertionAndRecovery() {
        ItemStackHandler source = source(8);
        Component name = Component.literal("Named stone");
        source.getStackInSlot(0).setHoverName(name);
        ItemTransferService.deposit(source, destination(8, 3), stack -> true, this::unexpectedRecovery);
        assertEquals(name, source.getStackInSlot(0).getHoverName());
        assertEquals(5, source.getStackInSlot(0).getCount());
    }

    @Test
    void backendInputMutationDoesNotMutateTheSourceOrAccounting() {
        ItemStackHandler source = source(8);
        ItemInsertion target = new ItemInsertion() {
            public int simulateInsert(ItemStack stack) { stack.setCount(1); return 4; }
            public ItemStack insert(ItemStack stack) { stack.setCount(2); return stack; }
        };
        var result = ItemTransferService.deposit(source, target, stack -> true, this::unexpectedRecovery);
        assertEquals(2, result.movedItems());
        assertEquals(6, source.getStackInSlot(0).getCount());
    }

    @Test
    void nativeLongAcceptedCountsAreClampedBeforeConversionToInt() {
        ItemStack stack = new ItemStack(Items.STONE, 8);
        assertEquals(8, ItemInsertion.acceptedCount(stack, Long.MAX_VALUE));
        assertEquals(0, ItemInsertion.acceptedCount(stack, Long.MIN_VALUE));
        assertTrue(ItemInsertion.remainder(stack, Long.MAX_VALUE).isEmpty());
        assertEquals(8, ItemInsertion.remainder(stack, Long.MIN_VALUE).getCount());
    }

    private static ItemStackHandler source(int count) {
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.STONE, count));
        return source;
    }

    private static ItemStackHandler changedSource(int slots) {
        ItemStackHandler source = new ItemStackHandler(slots) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack extracted = super.extractItem(slot, amount, simulate);
                if (!simulate && !extracted.isEmpty()) {
                    setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
                }
                return extracted;
            }
        };
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        return source;
    }

    private static ItemInsertion destination(int simulated, int actual) {
        return new ItemInsertion() {
            public int simulateInsert(ItemStack stack) { return simulated; }
            public ItemStack insert(ItemStack stack) { return ItemInsertion.remainder(stack, actual); }
        };
    }

    private void unexpectedRecovery(ItemStack stack) {
        fail("Remainder unexpectedly left the source: " + stack);
    }
}
