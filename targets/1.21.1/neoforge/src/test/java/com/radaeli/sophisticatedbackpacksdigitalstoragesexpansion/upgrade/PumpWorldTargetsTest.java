package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.HashSet;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PumpWorldTargetsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void worldPickupUsesNativeBoundedBreadthFirstOrder() {
        BlockPos origin = new BlockPos(25, 70, -30);
        var targets = PumpWorldTargets.pickup(origin);
        assertEquals(origin, targets.getFirst());
        assertEquals(targets.size(), new HashSet<>(targets).size());
        assertTrue(targets.stream().allMatch(pos -> origin.distSqr(pos) < 16));
        assertFalse(targets.contains(origin.offset(4, 0, 0)));
        assertTrue(targets.contains(origin.offset(3, 2, 1)));
        assertEquals(7, targets.subList(0, 7).stream().filter(pos -> origin.distSqr(pos) <= 1).count());
        assertEquals(20, PumpWorldTargets.pickupCooldown(origin, origin));
        assertEquals(60, PumpWorldTargets.pickupCooldown(origin, origin.offset(3, 0, 0)));
    }

    @Test
    void worldOutputAcceptsAirAndFlowingFluidButDoesNotReplaceSourcesOrSolidBlocks() {
        assertTrue(PumpWorldTargets.canPlace(Blocks.AIR.defaultBlockState()));
        assertTrue(PumpWorldTargets.canPlace(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1)));
        assertFalse(PumpWorldTargets.canPlace(Blocks.WATER.defaultBlockState()));
        assertFalse(PumpWorldTargets.canPlace(Blocks.LAVA.defaultBlockState()));
        assertFalse(PumpWorldTargets.canPlace(Blocks.STONE.defaultBlockState()));
    }
}
