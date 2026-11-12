package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

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

    public static int pickupTransactional(RemotePickupUpgradeWrapper wrapper, ServerPlayer player,
            net.neoforged.neoforge.transfer.item.ItemResource resource, int amount,
            net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        if (!player.isAlive() || player.isSpectator() || !wrapper.isEnabled()
                || !wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper)
                || wrapper.buffer(player).hasPending()) return 0;
        var local = wrapper.getStorageWrapper().getInventoryForUpgradeProcessing();
        int localAccepted = 0;
        int remoteAccepted = 0;
        if (wrapper.getSourcePriority() == SourcePriority.BACKPACK_FIRST
                && DestinationItemFilter.local(wrapper.getFilterLogic(), resource.toStack(amount))) {
            localAccepted = local.insert(resource, amount, transaction);
        }
        int remaining = amount - localAccepted;
        if (remaining > 0) {
            try (var guard = StorageTransferGuard.blockExternalInsertion(wrapper.getStorageWrapper())) {
                var remote = StorageAdapterRegistry.connect(player, wrapper.getEndpoint(), SourceLocation.carriedBy(player),
                        StorageOperation.INSERT).target().orElse(null);
                if (remote != null && DestinationItemFilter.remote(wrapper.getFilterLogic(), remote, player, resource.toStack(remaining))) {
                    int capacity = RemotePickupContext.current().map(context -> context.remoteCapacity(wrapper, resource)).orElse(0);
                    // The preflight is only an estimate. Revalidate access/filter here and persist the
                    // real remainder after commit if another destination or network state changed.
                    remoteAccepted = wrapper.buffer(player).accept(resource, Math.min(remaining, capacity), transaction,
                            committed -> recoverCommitted(wrapper, player));
                }
            }
        }
        remaining -= remoteAccepted;
        if (remaining > 0 && wrapper.getSourcePriority() == SourcePriority.NETWORK_FIRST
                && DestinationItemFilter.local(wrapper.getFilterLogic(), resource.toStack(remaining))) {
            localAccepted += local.insert(resource, remaining, transaction);
        }
        return localAccepted + remoteAccepted;
    }

    static void recoverCommitted(RemotePickupUpgradeWrapper wrapper, ServerPlayer player) {
        if (PickupReservation.uncertain(wrapper.getUpgradeStack())) return;
        var buffer = wrapper.buffer(player);
        if (!buffer.hasPending() || !player.isAlive() || player.isSpectator()
                || !wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper)) return;
        ItemStack pending = buffer.pending();
        try {
            var remote = StorageAdapterRegistry.connect(player, wrapper.getEndpoint(), SourceLocation.carriedBy(player),
                    StorageOperation.INSERT).target().orElse(null);
            // Persist the in-flight phase before entering an immediate destination. A restored
            // upgrade must not retry an operation whose outcome was never recorded.
            wrapper.markUncertainPickup();
            ItemStack remaining = insertRemote(wrapper, player, remote, pending, false);
            buffer.replace(remaining);
            if (!remaining.isEmpty() && DestinationItemFilter.local(wrapper.getFilterLogic(), remaining)) {
                wrapper.markUncertainPickup();
                remaining = com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.LocalItemAccess
                        .of(wrapper.getStorageWrapper().getInventoryForUpgradeProcessing()).insertItem(remaining, false);
                buffer.replace(remaining);
            }
            if (!remaining.isEmpty()) {
                wrapper.markUncertainPickup();
                player.getInventory().placeItemBackInInventory(remaining.copy());
                buffer.replace(ItemStack.EMPTY);
            }
        } catch (RuntimeException exception) {
            // Do not throw out of a root-commit callback: the native caller still needs to update
            // the source entity. Preserve the last checkpoint and quarantine an unknown outcome.
            wrapper.markUncertainPickup();
            com.mojang.logging.LogUtils.getLogger().error("Pickup delivery returned an uncertain result; reservation retained without retry", exception);
        }
    }

    public static ItemStack pickup(RemotePickupUpgradeWrapper wrapper, Player player, ItemStack offered, boolean simulate) {
        if (!(player instanceof ServerPlayer serverPlayer) || offered.isEmpty() || !wrapper.isEnabled()
                || !wrapper.getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(wrapper)) {
            return offered;
        }
        StorageConnection connection = StorageAdapterRegistry.connect(serverPlayer, wrapper.getEndpoint(),
                SourceLocation.carriedBy(serverPlayer), StorageOperation.INSERT);
        StorageTarget remote = connection.target().orElse(null);
        com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.LocalItemAccess local =
                com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.LocalItemAccess.of(wrapper.getStorageWrapper().getInventoryForUpgradeProcessing());
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
