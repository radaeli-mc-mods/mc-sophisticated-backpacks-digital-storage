package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterLogic;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepositFilterTest extends MinecraftStorageTest {
    @Test
    void allowAndBlockModesUseNativeSophisticatedFilters() {
        DepositFilterLogic filters = filters();
        filters.getFilterHandler().setStackInSlot(0, new ItemStack(Items.STONE));
        filters.setDepositFilterType(DepositFilterType.ALLOW);
        assertTrue(filters.matchesFilter(new ItemStack(Items.STONE)));
        assertFalse(filters.matchesFilter(new ItemStack(Items.DIRT)));
        filters.setDepositFilterType(DepositFilterType.BLOCK);
        assertFalse(filters.matchesFilter(new ItemStack(Items.STONE)));
        assertTrue(filters.matchesFilter(new ItemStack(Items.DIRT)));
    }

    @Test
    void inventoryModeMatchesDestinationContentsInsteadOfTheManualList() {
        DepositFilterLogic filters = filters();
        filters.getFilterHandler().setStackInSlot(0, new ItemStack(Items.STONE));
        filters.setDepositFilterType(DepositFilterType.INVENTORY);
        filters.setInventory(new SnapshotItemHandler(List.of(new ItemStack(Items.DIAMOND, 32))));
        assertTrue(filters.matchesFilter(new ItemStack(Items.DIAMOND)));
        assertFalse(filters.matchesFilter(new ItemStack(Items.STONE)));
    }

    @Test
    void nativeNbtMatchingDistinguishesNamedItems() {
        DepositFilterLogic filters = filters();
        ItemStack named = new ItemStack(Items.STONE);
        named.setHoverName(Component.literal("Named stone"));
        filters.getFilterHandler().setStackInSlot(0, named);
        filters.setDepositFilterType(DepositFilterType.ALLOW);
        filters.setMatchNbt(true);
        assertTrue(filters.matchesFilter(named.copy()));
        assertFalse(filters.matchesFilter(new ItemStack(Items.STONE)));
    }

    @Test
    void destinationSnapshotsAreDefensiveAndReadOnly() {
        ItemStack original = new ItemStack(Items.STONE, 8);
        SnapshotItemHandler snapshot = new SnapshotItemHandler(List.of(original, ItemStack.EMPTY));
        original.setCount(0);
        assertEquals(1, snapshot.getSlots());
        assertEquals(1, snapshot.getStackInSlot(0).getCount());
        snapshot.getStackInSlot(0).setCount(0);
        assertEquals(1, snapshot.getStackInSlot(0).getCount());
        assertTrue(snapshot.extractItem(0, 1, false).isEmpty());
        assertEquals(3, snapshot.insertItem(0, new ItemStack(Items.DIRT, 3), false).getCount());
    }

    private static DepositFilterLogic filters() {
        return new DepositFilterLogic(new ItemStack(Items.STICK), ignored -> {}, 9);
    }
}
