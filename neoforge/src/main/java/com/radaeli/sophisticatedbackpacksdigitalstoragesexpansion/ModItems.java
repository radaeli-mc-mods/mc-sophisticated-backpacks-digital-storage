package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(
            SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID);

    public static final DeferredItem<RemoteDepositUpgradeItem> REMOTE_DEPOSIT_UPGRADE = ITEMS.register(
            "remote_deposit_upgrade", RemoteDepositUpgradeItem::new);
    public static final DeferredItem<RemoteRestockUpgradeItem> REMOTE_RESTOCK_UPGRADE = ITEMS.register(
            "remote_restock_upgrade", RemoteRestockUpgradeItem::new);
    public static final DeferredItem<RemotePickupUpgradeItem> REMOTE_PICKUP_UPGRADE = ITEMS.register(
            "remote_pickup_upgrade", RemotePickupUpgradeItem::new);
    public static final DeferredItem<RemoteMagnetUpgradeItem> REMOTE_MAGNET_UPGRADE = ITEMS.register(
            "remote_magnet_upgrade", () -> new RemoteMagnetUpgradeItem(
                    () -> net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.ADVANCED_MAGNET_UPGRADE.get().getRadius()));
    public static final DeferredItem<RemoteRefillUpgradeItem> REMOTE_REFILL_UPGRADE = ITEMS.register(
            "remote_refill_upgrade", RemoteRefillUpgradeItem::new);
    public static final DeferredItem<RemoteFeedingUpgradeItem> REMOTE_FEEDING_UPGRADE = ITEMS.register(
            "remote_feeding_upgrade", RemoteFeedingUpgradeItem::new);
    public static final DeferredItem<RemoteAlchemyUpgradeItem> REMOTE_ALCHEMY_UPGRADE = ITEMS.register(
            "remote_alchemy_upgrade", RemoteAlchemyUpgradeItem::new);
    public static final DeferredItem<RemotePumpUpgradeItem> REMOTE_PUMP_UPGRADE = ITEMS.register(
            "remote_pump_upgrade", RemotePumpUpgradeItem::new);
    public static final DeferredItem<RemoteCraftingUpgradeItem> REMOTE_CRAFTING_UPGRADE = ITEMS.register(
            "remote_crafting_upgrade", RemoteCraftingUpgradeItem::new);

    private ModItems() {
    }
}
