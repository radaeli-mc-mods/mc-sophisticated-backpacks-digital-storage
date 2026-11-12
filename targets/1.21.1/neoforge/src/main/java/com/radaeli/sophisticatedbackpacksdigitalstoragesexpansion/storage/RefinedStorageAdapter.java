package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class RefinedStorageAdapter implements StorageAdapter {
    public static final String BACKEND_ID = "refinedstorage";

    public RefinedStorageAdapter() {
    }

    @Override
    public String backendId() {
        return BACKEND_ID;
    }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ControllerBlockEntity) {
            return Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (level.getBlockEntity(endpoint.pos()) instanceof ControllerBlockEntity controller) {
            return Optional.of(new Target(endpoint, controller));
        }
        return Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ControllerBlockEntity controller) implements StorageTarget {
        private Optional<StorageNetworkComponent> storage() {
            return Optional.ofNullable(controller.getNetworkForItem())
                    .map(network -> network.getComponent(StorageNetworkComponent.class));
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source) {
            return canAccess(player, source, StorageOperation.INSERT);
        }

        @Override
        public boolean canAccess(ServerPlayer player, SourceLocation source, StorageOperation operation) {
            Network network = controller.getNetworkForItem();
            if (network == null || network.getComponent(StorageNetworkComponent.class) == null) return false;
            PlatformSecurityNetworkComponent security = network.getComponent(PlatformSecurityNetworkComponent.class);
            BuiltinPermission permission = operation == StorageOperation.INSERT ? BuiltinPermission.INSERT : BuiltinPermission.EXTRACT;
            if (security != null && !security.isAllowed(permission, player)) return false;
            GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
            if (graph == null) return false;

            NetworkItemPlayerValidator.PlayerCoordinates coordinates =
                    new NetworkItemPlayerValidator.PlayerCoordinates(source.dimension(), source.position());
            return graph.getContainers(NetworkItemPlayerValidator.class).stream().anyMatch(validator -> validator.isValid(coordinates));
        }

        @Override
        public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                java.util.Optional<net.minecraft.world.item.Item> item) {
            return storage().map(storage -> storage.getAll().stream().anyMatch(value ->
                    value.amount() > 0 && value.resource() instanceof ItemResource resource
                    && item.map(resource.toItemStack(1)::is).orElse(true)
                    && matches.test(resource.toItemStack(1)))).orElse(false);
        }

        @Override
        public IItemHandler getAvailableItems(ServerPlayer player) {
            List<ItemStack> items = new ArrayList<>();
            storage().ifPresent(storage -> storage.getAll().forEach(resourceAmount -> {
                ResourceKey resource = resourceAmount.resource();
                if (resource instanceof ItemResource itemResource) items.add(itemResource.toItemStack(1));
            }));
            return new SnapshotItemHandler(items);
        }

        @Override
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            long accepted = storage().map(storage -> storage.insert(ItemResource.ofItemStack(stack), stack.getCount(),
                    Action.SIMULATE, actor(player))).orElse(0L);
            return ItemInsertion.acceptedCount(stack, accepted);
        }

        @Override
        public ItemStack insert(ItemStack stack, ServerPlayer player) {
            long accepted = storage().map(storage -> storage.insert(ItemResource.ofItemStack(stack), stack.getCount(),
                    Action.EXECUTE, actor(player))).orElse(0L);
            return ItemInsertion.remainder(stack, accepted);
        }

        @Override
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            List<StoredItemAmount> items = new ArrayList<>();
            storage().ifPresent(storage -> {
                IItemHandler identities = getAvailableItems(player);
                for (int slot = 0; slot < identities.getSlots(); slot++) {
                    ItemStack stack = identities.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        ItemResource resource = ItemResource.ofItemStack(stack);
                        long amount = storage.get(resource);
                        if (amount > 0) items.add(new StoredItemAmount(resource.toItemStack(1), amount));
                    }
                }
            });
            return List.copyOf(items);
        }

        @Override
        public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            long extracted = storage().map(storage -> storage.extract(ItemResource.ofItemStack(stack), amount,
                    Action.SIMULATE, actor(player))).orElse(0L);
            return ItemInsertion.acceptedCount(stack.copyWithCount(amount), extracted);
        }

        @Override
        public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            long extracted = storage().map(storage -> storage.extract(ItemResource.ofItemStack(stack), amount,
                    Action.EXECUTE, actor(player))).orElse(0L);
            int count = ItemInsertion.acceptedCount(stack.copyWithCount(amount), extracted);
            return count == 0 ? ItemStack.EMPTY : stack.copyWithCount(count);
        }

        private static Actor actor(ServerPlayer player) {
            return () -> player.getGameProfile().getName();
        }
    }
}
