package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RestockTransferServiceTest extends MinecraftStorageTest {
    @Test public void aSingleRequestFillsFourStacksAndRespectsPartialStockAndUnstackables() {
        for (var item : List.of(Items.STONE, Items.DIAMOND_SWORD)) {
            for (int stock : new int[]{3, new ItemStack(item).getMaxStackSize() * 4}) {
                var target = new LimitedTarget(new ItemStack(item), stock);
                var backpack = new ItemStackHandler(4);
                var result = run(backpack, target);
                assertEquals(stock, result.moved());
                assertEquals(0, target.stock);
                assertFalse(result.limited());
                assertEquals(stock, java.util.stream.IntStream.range(0, 4).map(slot -> backpack.getStackInSlot(slot).getCount()).sum());
            }
        }
    }
    @Test public void upgradedSlotsAndNativePartialExtractionContinueWithBoundedWork() {
        var backpack = new ItemStackHandler(1) {
            @Override protected int getStackLimit(int slot, ItemStack stack) { return 200_000; }
            @Override public int getSlotLimit(int slot) { return 200_000; }
        };
        var target = new LimitedTarget(new ItemStack(Items.STONE), 256);
        target.limit = 17;
        assertEquals(256, run(backpack, target).moved());
        assertEquals(256, backpack.getStackInSlot(0).getCount());
        var huge = new LimitedTarget(new ItemStack(Items.STONE), Integer.MAX_VALUE);
        var result = run(backpack, huge);
        assertTrue(result.limited());
        assertEquals(RestockTransferService.MAX_BATCHES, huge.calls);
    }
    private static RestockTransferService.Result run(IItemHandler backpack, LimitedTarget target) {
        return RestockTransferService.transfer(backpack, target, null,
                List.of(new StoredItemAmount(target.identity, target.stock)), stack -> true, () -> true,
                stack -> fail("Unexpected recovery"), new ItemDeliveryJournal(new ItemStack(Items.PAPER),
                        null, () -> {}));
    }
    private static class LimitedTarget implements StorageTarget {
        final ItemStack identity;
        int stock, calls, limit = 64;
        LimitedTarget(ItemStack identity, int stock) { this.identity = identity; this.stock = stock; }
        public StorageEndpoint endpoint() { return null; }
        public boolean isReachable(ServerPlayer player, SourceLocation source) { return true; }
        public IItemHandler getAvailableItems(ServerPlayer player) { return new SnapshotItemHandler(List.of(identity)); }
        public int simulateInsert(ItemStack stack, ServerPlayer player) { return 0; }
        public ItemStack insert(ItemStack stack, ServerPlayer player) { return stack; }
        public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) { return Math.min(stock, Math.min(limit, amount)); }
        public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            assertTrue(amount <= identity.getMaxStackSize());
            calls++;
            int taken = simulateExtract(stack, amount, player); stock -= taken;
            return identity.copyWithCount(taken);
        }
    }
}
