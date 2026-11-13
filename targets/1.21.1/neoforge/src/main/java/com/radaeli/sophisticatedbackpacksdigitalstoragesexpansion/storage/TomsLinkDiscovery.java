package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.block.IInventoryCable;
import com.tom.storagemod.block.InventoryCableBlock;
import com.tom.storagemod.block.FramedInventoryCableBlock;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** On-click discovery only. Terminal persistence and operation-time access stay unchanged. */
final class TomsLinkDiscovery {
    private TomsLinkDiscovery() {}

    static Optional<StorageEndpoint> identify(Level level, BlockPos clicked) {
        if (!level.isLoaded(clicked)) return Optional.empty();
        if (level.getBlockEntity(clicked) instanceof StorageTerminalBlockEntity) {
            return Optional.of(endpoint(level, clicked));
        }
        var block = level.getBlockState(clicked).getBlock();
        if (!(block instanceof IInventoryCable) || block instanceof InventoryCableBlock
                || block instanceof FramedInventoryCableBlock) return Optional.empty();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        List<BlockPos> terminals = new ArrayList<>();
        pending.add(clicked.immutable());
        // Connectors terminate native forward scans. Follow verified incoming edges too
        // to find the terminals attached to them, rather than guessing by proximity.
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (!visited.add(pos) || !level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal) {
                if (ready(level, pos, terminal)) terminals.add(pos);
            }
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (visited.contains(next) || !level.isLoaded(next)) continue;
                BlockState nextState = level.getBlockState(next);
                if (edge(level, pos, state, next, nextState, side)
                        || edge(level, next, nextState, pos, state, side.getOpposite())) pending.addLast(next);
            }
        }
        return terminals.stream().min(Comparator.<BlockPos>comparingDouble(clicked::distSqr).thenComparing(Comparator.naturalOrder()))
                .map(pos -> endpoint(level, pos));
    }

    private static StorageEndpoint endpoint(Level level, BlockPos pos) {
        return new StorageEndpoint(TomsStorageAdapter.BACKEND_ID, level.dimension(), pos.immutable());
    }

    static Direction back(BlockState state) {
        return switch (state.getValue(AbstractStorageTerminalBlock.TERMINAL_POS)) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            default -> state.getValue(AbstractStorageTerminalBlock.FACING);
        };
    }

    private static boolean edge(Level level, BlockPos from, BlockState state, BlockPos to, BlockState target, Direction side) {
        boolean terminal = state.getBlock() instanceof AbstractStorageTerminalBlock;
        if (!(state.getBlock() instanceof IInventoryCable) && !terminal) return false;
        boolean targetTerminal = target.getBlock() instanceof AbstractStorageTerminalBlock;
        if (!(target.getBlock() instanceof IInventoryCable) && !targetTerminal) return false;
        if (targetTerminal ? back(target) != side.getOpposite()
                : !((IInventoryCable) target.getBlock()).canConnectFrom(target, side.getOpposite())) return false;
        if (terminal) return back(state) == side;
        IInventoryCable cable = (IInventoryCable) state.getBlock();
        return cable.nextScan(level, state, from).stream().anyMatch(face -> face.pos().equals(to)
                && (face.from() == null || face.from() == side.getOpposite()));
    }

    private static boolean ready(Level level, BlockPos pos, StorageTerminalBlockEntity terminal) {
        BlockPos inventoryPos = pos.relative(back(terminal.getBlockState()));
        if (!level.isLoaded(inventoryPos)) return false;
        var inventory = new com.tom.storagemod.inventory.NetworkInventory();
        inventory.onLoad(level, inventoryPos, back(terminal.getBlockState()).getOpposite(), terminal);
        inventory.scanNetwork(level, pos);
        if (!inventory.isValid()) return false;
        var access = inventory.getAccess(level, pos);
        access.tracker().getChangeTracker(level);
        return access.getSlotCount() > 0;
    }
}
