package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuardedFluidResourceHandlerTest extends MinecraftStorageTest {
    @Test void cachedNativeHandlerChecksGuardOnEveryCallAndRestoresAfterException() {
        var internal = new FluidStacksResourceHandler(1, 2000);
        var resource = FluidResource.of(Fluids.WATER);
        UUID id = UUID.randomUUID();
        var current = new AtomicReference<>(id);
        var cached = new GuardedFluidResourceHandler(internal, current::get);
        try (var tx = Transaction.openRoot()) { assertEquals(1000, cached.insert(resource, 1000, tx)); tx.commit(); }
        assertThrows(IllegalStateException.class, () -> {
            try (var outer = FluidTransferGuard.blockExternalAccess(id);
                 var inner = FluidTransferGuard.blockExternalAccess(id);
                 var tx = Transaction.openRoot()) {
                assertEquals(0, cached.insert(0, resource, 1000, tx));
                assertEquals(0, cached.extract(0, resource, 1000, tx));
                assertEquals(0, cached.getAmountAsLong(0));
                assertEquals(1000, internal.getAmountAsLong(0));
                current.set(UUID.randomUUID());
                assertEquals(1000, cached.getAmountAsLong(0));
                current.set(id);
                throw new IllegalStateException("interrupted");
            }
        });
        assertEquals(1000, cached.getAmountAsLong(0));
        try (var aborted = Transaction.openRoot()) { assertEquals(1000, cached.extract(resource, 1000, aborted)); }
        assertEquals(1000, internal.getAmountAsLong(0));
    }
}
