package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.inventory.NetworkInventory;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class TomsStorageAdapter implements StorageAdapter {
    public static final String BACKEND_ID = "toms_storage";

    public TomsStorageAdapter() {
    }

    @Override
    public String backendId() {
        return BACKEND_ID;
    }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity) {
            return Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!level.isLoaded(endpoint.pos())) return Optional.empty();
        if (level.getBlockEntity(endpoint.pos()) instanceof StorageTerminalBlockEntity terminal) {
            return Optional.of(new Target(endpoint, level, terminal));
        }
        return Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ServerLevel terminalLevel, StorageTerminalBlockEntity terminal) implements StorageTarget {
        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source) {
            // Keep Tom's own range, wireless-terminal, dimension, beacon, and endpoint-validity checks authoritative.
            // Its native terminal access is player-based, so `source` is intentionally not used here.
            return terminalLevel.isLoaded(endpoint.pos()) && terminal.canInteractWith(player, false);
        }

        @Override
        public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                java.util.Optional<net.minecraft.world.item.Item> item) {
            Direction direction = switch (terminal.getBlockState().getValue(AbstractStorageTerminalBlock.TERMINAL_POS)) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                default -> terminal.getBlockState().getValue(AbstractStorageTerminalBlock.FACING);
            };
            NetworkInventory inventory = new NetworkInventory();
            inventory.onLoad(terminalLevel, endpoint.pos().relative(direction), direction.getOpposite(), terminal);
            inventory.scanNetwork(terminalLevel, endpoint.pos());
            try (var stacks = inventory.getAccess(terminalLevel, endpoint.pos()).tracker().streamWrappedStacks(false)) {
                return stacks.anyMatch(value -> value.getQuantity() > 0
                        && item.map(value.getStack()::is).orElse(true) && matches.test(value.getStack()));
            }
        }

        @Override
        public IItemHandler getAvailableItems(ServerPlayer player) {
            // getStacks() is a terminal GUI cache populated asynchronously after a read request.
            // Use Tom's native inventory view for a current snapshot on the first deposit too.
            Direction direction = switch (terminal.getBlockState().getValue(AbstractStorageTerminalBlock.TERMINAL_POS)) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                default -> terminal.getBlockState().getValue(AbstractStorageTerminalBlock.FACING);
            };
            NetworkInventory inventory = new NetworkInventory();
            inventory.onLoad(terminalLevel, endpoint.pos().relative(direction), direction.getOpposite(), terminal);
            inventory.scanNetwork(terminalLevel, endpoint.pos());
            List<ItemStack> items = new ArrayList<>();
            try (var stacks = inventory.getAccess(terminalLevel, endpoint.pos()).tracker().streamWrappedStacks(false)) {
                stacks.filter(stack -> stack.getQuantity() > 0)
                        .forEach(stack -> items.add(stack.getStack().copyWithCount(1)));
            }
            return new SnapshotItemHandler(items);
        }

        @Override
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            // Tom's Storage does not expose a simulation operation for terminal insertion.
            // The common transfer flow extracts first and restores every unaccepted item.
            return stack.getCount();
        }

        @Override
        public ItemStack insert(ItemStack stack, ServerPlayer player) {
            return terminal.pushStack(stack.copy());
        }

        @Override
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            List<StoredItemAmount> items = new ArrayList<>();
            Direction direction = terminalDirection();
            NetworkInventory inventory = new NetworkInventory();
            inventory.onLoad(terminalLevel, endpoint.pos().relative(direction), direction.getOpposite(), terminal);
            inventory.scanNetwork(terminalLevel, endpoint.pos());
            try (var stacks = inventory.getAccess(terminalLevel, endpoint.pos()).tracker().streamWrappedStacks(false)) {
                stacks.filter(stack -> stack.getQuantity() > 0)
                        .forEach(stack -> items.add(new StoredItemAmount(stack.getStack().copyWithCount(1), stack.getQuantity())));
            }
            return List.copyOf(items);
        }

        @Override
        public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            // Tom's terminal API has no simulation; its live tracker is only an estimate.
            return amount;
        }

        @Override
        public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            StoredItemStack extracted = terminal.pullStack(new StoredItemStack(stack.copyWithCount(1)), amount);
            if (extracted == null || extracted.getActualStack().isEmpty()) return ItemStack.EMPTY;
            return extracted.getActualStack().copy();
        }

        private Direction terminalDirection() {
            return switch (terminal.getBlockState().getValue(AbstractStorageTerminalBlock.TERMINAL_POS)) {
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
                default -> terminal.getBlockState().getValue(AbstractStorageTerminalBlock.FACING);
            };
        }
    }
}
