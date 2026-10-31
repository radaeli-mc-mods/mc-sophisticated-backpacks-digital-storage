package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.stream.Stream;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteDepositUpgradeRegistration;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeRegistration;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteUpgradeRegistrations;

@EventBusSubscriber(modid = SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Stream.of(ModItems.REMOTE_DEPOSIT_UPGRADE, ModItems.REMOTE_RESTOCK_UPGRADE, ModItems.REMOTE_PICKUP_UPGRADE,
                    ModItems.REMOTE_MAGNET_UPGRADE, ModItems.REMOTE_REFILL_UPGRADE, ModItems.REMOTE_FEEDING_UPGRADE,
                    ModItems.REMOTE_ALCHEMY_UPGRADE, ModItems.REMOTE_PUMP_UPGRADE)
                    .forEach(item -> LinkedUpgradeAppearance.register(item.get(), item.getId().getPath()));
            NeoForge.EVENT_BUS.addListener(RemoteRefillUpgradeTab::appendTargetSlotTooltip);
            UpgradeGuiManager.registerTab(RemoteDepositUpgradeRegistration.CONTAINER_TYPE, RemoteDepositUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteRestockUpgradeRegistration.CONTAINER_TYPE, RemoteRestockUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.PICKUP, RemotePickupUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.MAGNET, RemoteMagnetUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.REFILL, RemoteRefillUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.FEEDING, RemoteFeedingUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.ALCHEMY, RemoteAlchemyUpgradeTab::new);
            UpgradeGuiManager.registerTab(RemoteUpgradeRegistrations.PUMP, RemotePumpUpgradeTab::new);
        });
    }
}
