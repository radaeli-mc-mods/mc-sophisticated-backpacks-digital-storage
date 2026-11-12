package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import de.scc.calculator.network.SccFluidStore;
import de.scc.calculator.network.NetworkAntennaBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Run explicitly with -PsccTests; this does not substitute for a server/world matrix. */
class SccNativeContractTest extends MinecraftStorageTest {
    @Test void auditedAntennaConstantsRemainPinned() {
        assertTrue(net.neoforged.fml.ModList.get().isLoaded("scc"));
        assertEquals(32, NetworkAntennaBlockEntity.BASE_RANGE);
        assertEquals(32, NetworkAntennaBlockEntity.RANGE_PER_UPGRADE);
        assertEquals(8, NetworkAntennaBlockEntity.MAX_RANGE_UPGRADES);
    }

    @Test void nativePlainDrainDoesNotClaimComponentFluid() {
        int[] changes = {0};
        SccFluidStore store = new SccFluidStore(2, 4000, () -> changes[0]++);
        FluidStack modified = new FluidStack(Fluids.WATER, 1000);
        modified.set(DataComponents.CUSTOM_NAME, Component.literal("variant"));
        assertEquals(1000, store.fill(modified, FluidAction.EXECUTE));
        assertTrue(store.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE).isEmpty());
        assertEquals(1000, store.getFluidInTank(0).getAmount());
        assertEquals(1, changes[0]);
        assertEquals(500, store.fill(new FluidStack(Fluids.WATER, 500), FluidAction.SIMULATE));
        assertEquals(1, changes[0]);
        assertEquals(500, store.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE));
        assertEquals(500, store.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(modified, store.getFluidInTank(0)));
    }
}
