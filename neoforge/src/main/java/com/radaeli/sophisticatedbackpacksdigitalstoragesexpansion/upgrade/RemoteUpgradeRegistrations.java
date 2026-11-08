package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;

/** Container registrations for the universal upgrades beyond Deposit and Restock. */
public final class RemoteUpgradeRegistrations {
    public static final UpgradeContainerType<RemotePickupUpgradeWrapper, RemotePickupUpgradeContainer> PICKUP =
            new UpgradeContainerType<>(RemotePickupUpgradeContainer::new);
    public static final UpgradeContainerType<RemoteMagnetUpgradeWrapper, RemoteMagnetUpgradeContainer> MAGNET =
            new UpgradeContainerType<>(RemoteMagnetUpgradeContainer::new);
    public static final UpgradeContainerType<RemoteRefillUpgradeWrapper, RemoteRefillUpgradeContainer> REFILL =
            new UpgradeContainerType<>(RemoteRefillUpgradeContainer::new);
    public static final UpgradeContainerType<RemoteFeedingUpgradeWrapper, RemoteFeedingUpgradeContainer> FEEDING =
            new UpgradeContainerType<>(RemoteFeedingUpgradeContainer::new);
    public static final UpgradeContainerType<RemoteAlchemyUpgradeWrapper, RemoteAlchemyUpgradeContainer> ALCHEMY =
            new UpgradeContainerType<>(RemoteAlchemyUpgradeContainer::new);
    public static final UpgradeContainerType<RemotePumpUpgradeWrapper, RemotePumpUpgradeContainer> PUMP =
            new UpgradeContainerType<>(RemotePumpUpgradeContainer::new);

    private RemoteUpgradeRegistrations() {
    }

    public static void registerContainers() {
        register(ModItems.REMOTE_PICKUP_UPGRADE.getId(), PICKUP);
        register(ModItems.REMOTE_MAGNET_UPGRADE.getId(), MAGNET);
        register(ModItems.REMOTE_REFILL_UPGRADE.getId(), REFILL);
        register(ModItems.REMOTE_FEEDING_UPGRADE.getId(), FEEDING);
        register(ModItems.REMOTE_ALCHEMY_UPGRADE.getId(), ALCHEMY);
        register(ModItems.REMOTE_PUMP_UPGRADE.getId(), PUMP);
        register(ModItems.REMOTE_CRAFTING_UPGRADE.getId(), RemoteCraftingUpgradeContainer.TYPE);
    }

    private static <W extends net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper,
            C extends net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase<W, C>> void register(
                    ResourceLocation id, UpgradeContainerType<W, C> type) {
        UpgradeContainerRegistry.register(id, type);
    }
}
