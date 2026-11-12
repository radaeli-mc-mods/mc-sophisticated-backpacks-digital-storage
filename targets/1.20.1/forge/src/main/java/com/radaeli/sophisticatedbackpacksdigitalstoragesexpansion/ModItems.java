package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteDepositUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteCraftingUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteAlchemyUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteFeedingUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteMagnetUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePickupUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePumpUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRefillUpgradeItem;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeItem;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS, SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID);

    public static final RegistryObject<RemoteDepositUpgradeItem> REMOTE_DEPOSIT_UPGRADE = ITEMS.register(
            "remote_deposit_upgrade", RemoteDepositUpgradeItem::new);
    public static final RegistryObject<RemoteRestockUpgradeItem> REMOTE_RESTOCK_UPGRADE = ITEMS.register(
            "remote_restock_upgrade", RemoteRestockUpgradeItem::new);
    public static final RegistryObject<RemotePickupUpgradeItem> REMOTE_PICKUP_UPGRADE = ITEMS.register(
            "remote_pickup_upgrade", RemotePickupUpgradeItem::new);
    public static final RegistryObject<RemoteMagnetUpgradeItem> REMOTE_MAGNET_UPGRADE = ITEMS.register(
            "remote_magnet_upgrade", () -> new RemoteMagnetUpgradeItem(
                    () -> net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_MAGNET_UPGRADE.get().getRadius()));
    public static final RegistryObject<RemoteRefillUpgradeItem> REMOTE_REFILL_UPGRADE = ITEMS.register(
            "remote_refill_upgrade", RemoteRefillUpgradeItem::new);
    public static final RegistryObject<RemoteFeedingUpgradeItem> REMOTE_FEEDING_UPGRADE = ITEMS.register(
            "remote_feeding_upgrade", RemoteFeedingUpgradeItem::new);
    public static final RegistryObject<RemoteAlchemyUpgradeItem> REMOTE_ALCHEMY_UPGRADE = ITEMS.register(
            "remote_alchemy_upgrade", RemoteAlchemyUpgradeItem::new);
    public static final RegistryObject<RemotePumpUpgradeItem> REMOTE_PUMP_UPGRADE = ITEMS.register(
            "remote_pump_upgrade", RemotePumpUpgradeItem::new);
    public static final RegistryObject<RemoteCraftingUpgradeItem> REMOTE_CRAFTING_UPGRADE = ITEMS.register(
            "remote_crafting_upgrade", RemoteCraftingUpgradeItem::new);

    private ModItems() {
    }
}
