package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;

public final class RemoteRestockUpgradeRegistration {
    public static final UpgradeContainerType<RemoteRestockUpgradeWrapper, RemoteRestockUpgradeContainer> CONTAINER_TYPE =
            new UpgradeContainerType<>(RemoteRestockUpgradeContainer::new);
    private RemoteRestockUpgradeRegistration() {}
    public static void registerContainer() {
        ResourceLocation id = ModItems.REMOTE_RESTOCK_UPGRADE.getId();
        UpgradeContainerRegistry.register(id, CONTAINER_TYPE);
    }
}
