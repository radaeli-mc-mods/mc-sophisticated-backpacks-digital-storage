package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Map;
import java.util.HashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackLinkedStorageResolver;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageTransferGuard;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageAdapterRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageOperation;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.ItemInsertion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Player and entity context scoped to Sophisticated's native pickup callback.
 * Capacity is queried before the native transaction; the scope supports reentrant pickups.
 */
public final class RemotePickupContext {
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private RemotePickupContext() {
    }

    public static Scope open(Player player, ItemEntity itemEntity) {
        Context context = new Context(player, itemEntity);
        context.prepareCapacity();
        CONTEXTS.get().push(context);
        return new Scope(context);
    }

    public static Optional<Context> current() {
        Deque<Context> contexts = CONTEXTS.get();
        if (contexts.isEmpty()) {
            CONTEXTS.remove();
            return Optional.empty();
        }
        Context context = contexts.peek();
        if (!context.isValid()) {
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public static final class Context {
        private final Player player;
        private final ItemEntity itemEntity;
        private final Map<OfferKey, Integer> capacities = new HashMap<>();
        private final ItemResource resource;

        private Context(Player player, ItemEntity itemEntity) {
            this.player = player;
            this.itemEntity = itemEntity;
            resource = itemEntity == null ? ItemResource.EMPTY
                    : ItemResource.of(itemEntity.getItem());
        }
        public Player player() { return player; }
        public ItemEntity itemEntity() { return itemEntity; }

        private static OfferKey key(RemotePickupUpgradeWrapper wrapper) {
            var storage = wrapper.getStorageWrapper();
            int slot = storage.getUpgradeHandler().getSlotWrappers().entrySet().stream()
                    .filter(entry -> entry.getValue() == wrapper).mapToInt(Map.Entry::getKey).findFirst().orElse(-1);
            return new OfferKey(storage.getContentsUuid().<Object>map(id -> id).orElse(storage), slot);
        }
        public int remoteCapacity(RemotePickupUpgradeWrapper wrapper,
                ItemResource offered) {
            return resource.equals(offered) ? capacities.getOrDefault(key(wrapper), 0) : 0;
        }
        private void prepareCapacity() {
            if (!isValid() || resource.isEmpty() || !(player instanceof ServerPlayer serverPlayer)
                    || !player.isAlive() || player.isSpectator()) return;
            // Use exactly the native backpack provider, worn-backpack setting, and linked-host resolver.
            // This runs before CommonEventHandler opens its root transaction. Backend simulations
            // may open their own roots and must never run inside the pickup transaction.
            PlayerInventoryProvider.get().runOnBackpacks(player,
                    (stack, handler, identifier, slot) -> {
                        var storage = BackpackLinkedStorageResolver
                                .resolveForGlobalUpgradeProcessing(player.level(), stack);
                        for (var upgrade : storage.getUpgradeHandler().getSlotWrappers().values()) {
                            if (!(upgrade instanceof RemotePickupUpgradeWrapper pickup) || !pickup.isEnabled()
                                    || PickupReservation.hasPending(pickup.getUpgradeStack())) continue;
                            try (var guard = StorageTransferGuard.blockExternalInsertion(storage)) {
                                var target = StorageAdapterRegistry.connect(
                                        serverPlayer, pickup.getEndpoint(), SourceLocation.carriedBy(player),
                                        StorageOperation.INSERT).target();
                                int offered = Math.min(itemEntity.getItem().getCount(), resource.toStack().getMaxStackSize());
                                int accepted = target.filter(remote -> DestinationItemFilter.remote(pickup.getFilterLogic(), remote, serverPlayer, resource.toStack(offered)))
                                        .map(remote -> ItemInsertion.acceptedCount(
                                                resource.toStack(offered), remote.simulateInsert(resource.toStack(offered), serverPlayer))).orElse(0);
                                capacities.put(key(pickup), accepted);
                            }
                        }
                        return false;
                    }, Config.SERVER.nerfsConfig.onlyWornBackpackTriggersUpgrades.get());
        }
        public boolean isValid() {
            return player != null && itemEntity != null
                    && !player.isRemoved() && !itemEntity.isRemoved()
                    && !player.level().isClientSide()
                    && player.level() == itemEntity.level();
        }
    }

    private record OfferKey(Object storage, int slot) {}

    public static final class Scope implements AutoCloseable {
        private final Context context;
        private boolean closed;

        private Scope(Context context) {
            this.context = context;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Deque<Context> contexts = CONTEXTS.get();
            if (contexts.peek() != context) {
                contexts.clear();
                CONTEXTS.remove();
                throw new IllegalStateException("Pickup context scopes must close in reverse order");
            }
            contexts.pop();
            if (contexts.isEmpty()) {
                CONTEXTS.remove();
            }
        }
    }
}
