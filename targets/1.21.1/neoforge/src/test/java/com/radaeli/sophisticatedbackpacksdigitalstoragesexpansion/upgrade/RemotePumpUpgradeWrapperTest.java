package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemotePumpUpgradeWrapperTest {
    private static boolean loadedTestConfig;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!Config.SERVER_SPEC.isLoaded()) {
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
            loadedTestConfig = true;
        }
    }

    @AfterAll
    static void unloadTestConfig() {
        if (loadedTestConfig) ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER);
    }

    @Test
    void everyNativeBackpackTierCanTransferAWholeBucketWithoutATank() {
        for (var item : List.of(ModItems.BACKPACK, ModItems.COPPER_BACKPACK, ModItems.IRON_BACKPACK,
                ModItems.GOLD_BACKPACK, ModItems.DIAMOND_BACKPACK, ModItems.NETHERITE_BACKPACK)) {
            var storage = BackpackWrapper.fromStack(new ItemStack(item.get()));
            var wrapper = new RemotePumpUpgradeWrapper(storage, new ItemStack(
                    com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_PUMP_UPGRADE.get()),
                    ignored -> {});
            assertEquals(1_000, wrapper.getMaxInputOutput(), item.getId().toString());
            assertTrue(storage.getFluidHandler().isEmpty());
        }
    }

    @Test
    void reservationSurvivesItemCopyWithComponentsAndBlocksUnlinking() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        ItemStack upgrade = new ItemStack(
                com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_PUMP_UPGRADE.get());
        FluidStack reserved = new FluidStack(Fluids.WATER, 700);
        reserved.set(DataComponents.CUSTOM_NAME, Component.literal("Special water"));
        PumpReservation.reserve(upgrade, reserved, true, registries);
        ItemStack removed = upgrade.copy();
        assertTrue(PumpReservation.hasPending(removed));
        assertTrue(PumpReservation.hasRemoteOrigin(removed));
        assertFalse(((RemotePumpUpgradeItem) removed.getItem()).canChangeBinding(removed));
        FluidStack decoded = PumpReservation.fluid(removed, registries);
        assertEquals(700, decoded.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(reserved, decoded));
        PumpReservation.clear(removed);
        assertTrue(((RemotePumpUpgradeItem) removed.getItem()).canChangeBinding(removed));
        assertTrue(PumpReservation.hasPending(upgrade));
    }

    @Test
    void nativePlacedBackpackClassLoadsWithTheFluidCapabilityMixin() throws ClassNotFoundException {
        assertNotNull(Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity"));
    }
}
