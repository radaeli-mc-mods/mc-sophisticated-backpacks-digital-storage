package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Native Pump geometry: breadth-first pickup within distance squared < 16, output except up. */
final class PumpWorldTargets {
    private static final List<BlockPos> PICKUP_OFFSETS = pickupOffsets();

    private PumpWorldTargets() {}

    static List<BlockPos> pickup(BlockPos origin) {
        return PICKUP_OFFSETS.stream().map(origin::offset).toList();
    }

    static boolean canPlace(BlockState state) {
        return state.isAir() || (!state.getFluidState().isEmpty() && !state.getFluidState().isSource());
    }

    static int pickupCooldown(BlockPos origin, BlockPos target) {
        return (int) (Math.max(1, Math.sqrt(origin.distSqr(target))) * 20);
    }

    private static List<BlockPos> pickupOffsets() {
        List<BlockPos> result = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(BlockPos.ZERO);
        visited.add(BlockPos.ZERO);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            result.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.distSqr(BlockPos.ZERO) < 16 && visited.add(next)) queue.addLast(next);
            }
        }
        return List.copyOf(result);
    }
}
