package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RemotePumpUpgradeWrapperTest {
    @Test
    public void everyNativeBackpackTierCanTransferAWholeBucketWithoutATank() {
        for (var item : List.of(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK,
                net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.COPPER_BACKPACK,
                net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.IRON_BACKPACK,
                net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.GOLD_BACKPACK,
                net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.DIAMOND_BACKPACK,
                net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK)) {
            var storage = new BackpackWrapper(new ItemStack(item.get()));
            var wrapper = new RemotePumpUpgradeWrapper(storage, new ItemStack(
                    com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_PUMP_UPGRADE.get()),
                    ignored -> {});
            assertEquals(1_000, wrapper.getMaxInputOutput(), item.getId().toString());
            assertTrue(storage.getFluidHandler().isEmpty());
        }
    }

    @Test
    public void reservationSurvivesItemCopyWithNbtAndBlocksUnlinking() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack upgrade = new ItemStack(
                com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_PUMP_UPGRADE.get());
        FluidStack reserved = new FluidStack(Fluids.WATER, 700);
        CompoundTag custom = new CompoundTag();
        custom.putString("custom_name", "Special water");
        reserved.setTag(custom);
        PumpReservation.reserve(upgrade, reserved, true, registries);
        ItemStack removed = upgrade.copy();
        assertTrue(PumpReservation.hasPending(removed));
        assertTrue(PumpReservation.hasRemoteOrigin(removed));
        assertFalse(((RemotePumpUpgradeItem) removed.getItem()).canChangeBinding(removed));
        FluidStack decoded = PumpReservation.fluid(removed, registries);
        assertEquals(700, decoded.getAmount());
        assertTrue(reserved.isFluidEqual(decoded));
        PumpReservation.clear(removed);
        assertTrue(((RemotePumpUpgradeItem) removed.getItem()).canChangeBinding(removed));
        assertTrue(PumpReservation.hasPending(upgrade));
    }

    @Test
    public void nativePlacedBackpackClassLoadsWithTheFluidCapabilityMixin() throws ClassNotFoundException {
        assertNotNull(Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity"));
    }
}
