package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GuardedFluidHandlerTest extends MinecraftStorageTest {
    @Test
    void cachedCapabilityIsBlockedDuringTransferAndResumesAfterwards() {
        UUID id = UUID.randomUUID();
        FluidTank internal = new FluidTank(2_000);
        internal.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler cached = new GuardedFluidHandler(internal, () -> id);
        assertEquals(1_000, cached.getFluidInTank(0).getAmount());
        try (var outer = FluidTransferGuard.blockExternalAccess(id)) {
            try (var nested = FluidTransferGuard.blockExternalAccess(id)) {
                assertEquals(0, cached.getTanks());
                assertEquals(0, cached.getTankCapacity(0));
                assertTrue(cached.getFluidInTank(0).isEmpty());
                for (var action : IFluidHandler.FluidAction.values()) {
                    assertEquals(0, cached.fill(new FluidStack(Fluids.WATER, 1_000), action));
                    assertTrue(cached.drain(1_000, action).isEmpty());
                    assertTrue(cached.drain(new FluidStack(Fluids.WATER, 1_000), action).isEmpty());
                }
                // Pump retains authorized internal access to its tank.
                assertEquals(500, internal.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount());
            }
            assertTrue(cached.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty());
        }
        assertEquals(500, cached.drain(500, IFluidHandler.FluidAction.EXECUTE).getAmount());
    }

    @Test
    void cachedViewTracksTheCurrentBackpackContentsAndScopeReleasesOnException() {
        AtomicReference<UUID> id = new AtomicReference<>(UUID.randomUUID());
        FluidTank tank = new FluidTank(1_000);
        IFluidHandler cached = new GuardedFluidHandler(tank, id::get);
        UUID old = id.get();
        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = FluidTransferGuard.blockExternalAccess(old)) {
                assertEquals(0, cached.getTanks());
                id.set(UUID.randomUUID());
                assertEquals(1, cached.getTanks());
                throw new IllegalStateException("Interrupted operation");
            }
        });
        assertFalse(FluidTransferGuard.isBlocked(old));
    }
}
