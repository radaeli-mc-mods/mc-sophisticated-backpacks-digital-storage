package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;

public final class RemoteDepositUpgradeRegistration {
    public static final UpgradeContainerType<RemoteDepositUpgradeWrapper, RemoteDepositUpgradeContainer> CONTAINER_TYPE =
            new UpgradeContainerType<>(RemoteDepositUpgradeContainer::new);

    private RemoteDepositUpgradeRegistration() {
    }

    public static void registerContainer() {
        ResourceLocation itemId = ModItems.REMOTE_DEPOSIT_UPGRADE.getId();
        UpgradeContainerRegistry.register(itemId, CONTAINER_TYPE);
    }
}
