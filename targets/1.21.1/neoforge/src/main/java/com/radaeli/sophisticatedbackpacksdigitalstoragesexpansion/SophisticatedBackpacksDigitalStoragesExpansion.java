package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.EndpointLinkHandler;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe.ModRecipes;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.registry.ModCreativeTabs;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteDepositUpgradeRegistration;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeRegistration;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteUpgradeRegistrations;

/** NeoForge entry point for Sophisticated Backpacks digital storage integrations. */
@Mod(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID)
public final class SophisticatedBackpacksDigitalStoragesExpansion {
    public static final String MOD_ID = "sophisticated_backpacks_digital_storages_expansion";

    public SophisticatedBackpacksDigitalStoragesExpansion(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        RemoteDepositUpgradeRegistration.registerContainer();
        RemoteRestockUpgradeRegistration.registerContainer();
        RemoteUpgradeRegistrations.registerContainers();
        NeoForge.EVENT_BUS.addListener(EndpointLinkHandler::onRightClickBlock);
    }
}
