package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.refinedmods.refinedstorage.api.network.IWirelessTransmitter;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.blockentity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/** Refined Storage 1.x adapter. It deliberately uses the 1.x INetwork API. */
public final class RefinedStorageAdapter implements StorageAdapter {
    public static final String BACKEND_ID = "refinedstorage";

    @Override public String backendId() { return BACKEND_ID; }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ControllerBlockEntity
                ? Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable())) : Optional.empty();
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!level.isLoaded(endpoint.pos())) return Optional.empty();
        return level.getBlockEntity(endpoint.pos()) instanceof ControllerBlockEntity controller
                ? Optional.of(new Target(endpoint, controller)) : Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ControllerBlockEntity controller) implements StorageTarget {
        private INetwork network() {
            INetwork network = controller.getNetwork();
            return network != null && network.canRun() ? network : null;
        }

        @Override public boolean isReachable(ServerPlayer player, SourceLocation source) {
            return canAccess(player, source, StorageOperation.INSERT);
        }

        @Override
        public boolean canAccess(ServerPlayer player, SourceLocation source, StorageOperation operation) {
            INetwork network = network();
            if (network == null) return false;
            Permission permission = operation == StorageOperation.INSERT ? Permission.INSERT : Permission.EXTRACT;
            if (!network.getSecurityManager().hasPermission(permission, player)) return false;
            long rangeSquared;
            for (var entry : network.getNodeGraph().all()) {
                INetworkNode node = entry.getNode();
                if (!(node instanceof IWirelessTransmitter transmitter) || !node.isActive()
                        || !transmitter.getDimension().equals(source.dimension())) continue;
                rangeSquared = (long) transmitter.getRange() * transmitter.getRange();
                if (transmitter.getOrigin().distToCenterSqr(source.position().x(), source.position().y(), source.position().z())
                        < rangeSquared) return true;
            }
            return false;
        }

        @Override
        public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                java.util.Optional<net.minecraft.world.item.Item> item) {
            INetwork network = network();
            if (network == null) return false;
            for (var entry : network.getItemStorageCache().getList().getStacks()) {
                ItemStack stack = entry.getStack();
                if (!stack.isEmpty() && item.map(stack::is).orElse(true) && matches.test(stack)) return true;
            }
            return false;
        }

        @Override
        public IItemHandler getAvailableItems(ServerPlayer player) {
            List<ItemStack> items = new ArrayList<>();
            INetwork network = network();
            if (network != null) network.getItemStorageCache().getList().getStacks().forEach(entry -> {
                ItemStack stack = entry.getStack();
                if (!stack.isEmpty()) items.add(stack.copyWithCount(1));
            });
            return new SnapshotItemHandler(items);
        }

        @Override public int simulateInsert(ItemStack stack, ServerPlayer player) {
            INetwork network = network();
            if (network == null) return 0;
            ItemStack remainder = network.insertItem(stack.copy(), stack.getCount(), Action.SIMULATE);
            return ItemInsertion.acceptedCount(stack, stack.getCount() - remainder.getCount());
        }

        @Override public ItemStack insert(ItemStack stack, ServerPlayer player) {
            INetwork network = network();
            return network == null ? stack.copy() : network.insertItem(stack.copy(), stack.getCount(), Action.PERFORM);
        }

        @Override
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            List<StoredItemAmount> items = new ArrayList<>();
            INetwork network = network();
            if (network == null) return List.of();
            var list = network.getItemStorageCache().getList();
            list.getStacks().forEach(entry -> {
                ItemStack stack = entry.getStack();
                long amount = list.getCount(stack);
                if (!stack.isEmpty() && amount > 0) items.add(new StoredItemAmount(stack.copyWithCount(1), amount));
            });
            return List.copyOf(items);
        }

        @Override public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            INetwork network = network();
            return network == null ? 0 : network.extractItem(stack.copyWithCount(1), amount, Action.SIMULATE).getCount();
        }

        @Override public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            INetwork network = network();
            return network == null ? ItemStack.EMPTY : network.extractItem(stack.copyWithCount(1), amount, Action.PERFORM);
        }
    }
}
