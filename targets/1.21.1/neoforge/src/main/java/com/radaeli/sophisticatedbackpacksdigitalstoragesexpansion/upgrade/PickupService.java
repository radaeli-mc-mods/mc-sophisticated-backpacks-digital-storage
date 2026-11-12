package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.ItemInsertion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageConnection;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageOperation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTarget;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;

/** Routes Sophisticated's offered pickup, preserving the actual remainder for its native event handler. */
public final class PickupService {
    private PickupService() {}

    public static ItemStack pickup(RemotePickupUpgradeWrapper wrapper, Player player, ItemStack offered, boolean simulate) {
        if (!(player instanceof ServerPlayer serverPlayer) || offered.isEmpty() || !wrapper.isEnabled()
                || !wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper)) {
            return offered;
        }
        StorageConnection connection = StorageAdapterRegistry.connect(serverPlayer, wrapper.getEndpoint(),
                SourceLocation.carriedBy(serverPlayer), StorageOperation.INSERT);
        StorageTarget remote = connection.target().orElse(null);
        net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler local =
                wrapper.getStorageWrapper().getInventoryForUpgradeProcessing();
        return DestinationItemFilter.route(wrapper.getSourcePriority(), offered,
                stack -> insertRemote(wrapper, serverPlayer, remote, stack, simulate),
                stack -> DestinationItemFilter.local(wrapper.getFilterLogic(), stack)
                        ? local.insertItem(stack, simulate) : stack);
    }

    private static ItemStack insertRemote(RemotePickupUpgradeWrapper wrapper, ServerPlayer player,
            StorageTarget remote, ItemStack offered, boolean simulate) {
        if (offered.isEmpty() || remote == null) return offered;
        try (StorageTransferGuard ignored = StorageTransferGuard.blockExternalInsertion(wrapper.getStorageWrapper())) {
            if (!DestinationItemFilter.remote(wrapper.getFilterLogic(), remote, player, offered)) return offered;
            if (simulate) {
                int accepted = ItemInsertion.acceptedCount(offered, remote.simulateInsert(offered.copy(), player));
                return ItemInsertion.remainder(offered, accepted);
            }
            return remote.insert(offered.copy(), player);
        }
    }
}
