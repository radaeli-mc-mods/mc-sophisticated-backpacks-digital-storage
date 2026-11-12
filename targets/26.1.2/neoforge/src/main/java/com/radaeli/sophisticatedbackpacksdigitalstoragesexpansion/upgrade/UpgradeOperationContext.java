package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;

/** Server-only context built from the current live menu, never from packet-supplied positions or inventories. */
public record UpgradeOperationContext(ServerPlayer player, IStorageWrapper storage, SourceLocation source) {
    public static Optional<UpgradeOperationContext> fromCurrentMenu(ServerPlayer player, UpgradeContainerBase<?, ?> container) {
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu) || !menu.stillValid(player)
                || menu.getUpgradeContainers().get(container.getUpgradeContainerId()) != container
                || menu.getOpenContainer().orElse(null) != container
                || menu.getStorageWrapper().getUpgradeHandler().getSlotWrappers().get(container.getUpgradeContainerId())
                        != container.getUpgradeWrapper()) {
            return Optional.empty();
        }
        // getOpenContainer resolves the saved open-tab id; container.isOpen() can be stale just after installation.
        return Optional.of(new UpgradeOperationContext(player, menu.getStorageWrapper(), SourceLocation.fromMenu(menu, player)));
    }
}
