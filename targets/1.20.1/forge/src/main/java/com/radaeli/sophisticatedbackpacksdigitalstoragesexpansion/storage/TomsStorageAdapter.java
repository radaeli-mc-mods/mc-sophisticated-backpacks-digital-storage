package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

/** Tom's Storage 1.7 adapter, using its terminal access check and live adjacent capability. */
public final class TomsStorageAdapter implements StorageAdapter {
    public static final String BACKEND_ID = "toms_storage";
    @Override public String backendId() { return BACKEND_ID; }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity
                ? Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable())) : Optional.empty();
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!level.isLoaded(endpoint.pos())) return Optional.empty();
        return level.getBlockEntity(endpoint.pos()) instanceof StorageTerminalBlockEntity terminal
                ? Optional.of(new Target(endpoint, level, terminal)) : Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ServerLevel level, StorageTerminalBlockEntity terminal)
            implements StorageTarget {
        private boolean allowed(ServerPlayer player) {
            return level.isLoaded(endpoint.pos()) && terminal.canInteractWith(player);
        }

        private Optional<IItemHandler> handler(ServerPlayer player) {
            if (!allowed(player)) return Optional.empty();
            Direction direction = switch (terminal.getBlockState().getValue(AbstractStorageTerminalBlock.TERMINAL_POS)) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                default -> terminal.getBlockState().getValue(AbstractStorageTerminalBlock.FACING);
            };
            BlockPos inventoryPos = endpoint.pos().relative(direction);
            if (!level.isLoaded(inventoryPos)) return Optional.empty();
            return level.getBlockEntity(inventoryPos) == null ? Optional.empty()
                    : level.getBlockEntity(inventoryPos).getCapability(ForgeCapabilities.ITEM_HANDLER,
                            direction.getOpposite()).resolve();
        }

        @Override public boolean isReachable(ServerPlayer player, SourceLocation source) { return allowed(player); }

        @Override public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                java.util.Optional<net.minecraft.world.item.Item> item) {
            return handler(player).map(inventory -> {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty() && item.map(stack::is).orElse(true) && matches.test(stack)) return true;
                }
                return false;
            }).orElse(false);
        }

        @Override public IItemHandler getAvailableItems(ServerPlayer player) {
            List<ItemStack> items = new ArrayList<>();
            handler(player).ifPresent(inventory -> {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) items.add(stack.copyWithCount(1));
                }
            });
            return new SnapshotItemHandler(items);
        }

        @Override public int simulateInsert(ItemStack stack, ServerPlayer player) {
            return handler(player).map(inventory -> insert(inventory, stack.copy(), true).getCount()).map(
                    remainder -> stack.getCount() - remainder).orElse(0);
        }

        @Override public ItemStack insert(ItemStack stack, ServerPlayer player) {
            return handler(player).map(inventory -> insert(inventory, stack.copy(), false)).orElse(stack.copy());
        }

        private ItemStack insert(IItemHandler inventory, ItemStack offered, boolean simulate) {
            ItemStack remainder = offered;
            for (int slot = 0; slot < inventory.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = inventory.insertItem(slot, remainder, simulate);
            }
            return remainder;
        }

        @Override public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            List<StoredItemAmount> result = new ArrayList<>();
            handler(player).ifPresent(inventory -> {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) result.add(new StoredItemAmount(stack.copyWithCount(1), stack.getCount()));
                }
            });
            return List.copyOf(result);
        }

        @Override public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            return extract(stack, amount, player, true).getCount();
        }

        @Override public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            return extract(stack, amount, player, false);
        }

        private ItemStack extract(ItemStack key, int amount, ServerPlayer player, boolean simulate) {
            if (key.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack result = key.copyWithCount(0);
            int remaining = amount;
            Optional<IItemHandler> resolved = handler(player);
            if (resolved.isEmpty()) return ItemStack.EMPTY;
            IItemHandler inventory = resolved.get();
            for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
                ItemStack available = inventory.getStackInSlot(slot);
                if (!ItemStack.isSameItemSameTags(available, key)) continue;
                ItemStack extracted = inventory.extractItem(slot, remaining, simulate);
                if (!extracted.isEmpty()) {
                    result.grow(extracted.getCount());
                    remaining -= extracted.getCount();
                }
            }
            return result;
        }
    }
}
