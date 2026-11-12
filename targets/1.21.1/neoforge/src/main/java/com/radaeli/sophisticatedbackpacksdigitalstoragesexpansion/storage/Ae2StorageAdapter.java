package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class Ae2StorageAdapter implements StorageAdapter {
    public static final String BACKEND_ID = "ae2";

    public Ae2StorageAdapter() {
    }

    @Override
    public String backendId() {
        return BACKEND_ID;
    }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IWirelessAccessPoint) {
            return Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<StorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (level.getBlockEntity(endpoint.pos()) instanceof IWirelessAccessPoint accessPoint) {
            return Optional.of(new Target(endpoint, accessPoint));
        }
        return Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, IWirelessAccessPoint accessPoint) implements StorageTarget {
        private Optional<MEStorage> storage() {
            IGrid grid = accessPoint.getGrid();
            return accessPoint.isActive() && grid != null && grid.getEnergyService().isNetworkPowered()
                    ? Optional.of(grid.getStorageService().getInventory())
                    : Optional.empty();
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source) {
            if (!source.dimension().equals(endpoint.dimension()) || !accessPoint.isActive()
                    || accessPoint.getActionableNode() == null || !accessPoint.getActionableNode().isActive()) {
                return false;
            }
            IGrid grid = accessPoint.getGrid();
            if (grid == null || !grid.getEnergyService().isNetworkPowered()) return false;
            double range = accessPoint.getRange();
            net.minecraft.world.phys.Vec3 target = net.minecraft.world.phys.Vec3.atCenterOf(accessPoint.getLocation().getPos());
            return source.position().distanceToSqr(target) <= range * range;
        }

        @Override
        public boolean hasStoredMatching(ServerPlayer player, java.util.function.Predicate<ItemStack> matches,
                java.util.Optional<net.minecraft.world.item.Item> item) {
            return storage().map(storage -> {
                var counter = storage.getAvailableStacks();
                for (var key : counter.keySet()) {
                    if (key instanceof AEItemKey itemKey && counter.get(key) > 0) {
                        ItemStack stack = itemKey.toStack(1);
                        if (item.map(stack::is).orElse(true) && matches.test(stack)) return true;
                    }
                }
                return false;
            }).orElse(false);
        }

        @Override
        public IItemHandler getAvailableItems(ServerPlayer player) {
            List<ItemStack> items = new ArrayList<>();
            storage().ifPresent(meStorage -> {
                for (AEKey key : meStorage.getAvailableStacks().keySet()) {
                    if (key instanceof AEItemKey itemKey) items.add(itemKey.toStack(1));
                }
            });
            return new SnapshotItemHandler(items);
        }

        @Override
        public int simulateInsert(ItemStack stack, ServerPlayer player) {
            return storage().map(meStorage -> ItemInsertion.acceptedCount(stack,
                    meStorage.insert(AEItemKey.of(stack), stack.getCount(), Actionable.SIMULATE,
                            IActionSource.ofPlayer(player, (IActionHost) accessPoint)))).orElse(0);
        }

        @Override
        public ItemStack insert(ItemStack stack, ServerPlayer player) {
            long accepted = storage().map(meStorage -> meStorage.insert(AEItemKey.of(stack), stack.getCount(),
                    Actionable.MODULATE, IActionSource.ofPlayer(player, (IActionHost) accessPoint))).orElse(0L);
            return ItemInsertion.remainder(stack, accepted);
        }

        @Override
        public List<StoredItemAmount> getAvailableItemAmounts(ServerPlayer player) {
            List<StoredItemAmount> items = new ArrayList<>();
            storage().ifPresent(meStorage -> {
                KeyCounter counter = meStorage.getAvailableStacks();
                for (AEKey key : counter.keySet()) {
                    if (key instanceof AEItemKey itemKey && counter.get(key) > 0) {
                        items.add(new StoredItemAmount(itemKey.toStack(1), counter.get(key)));
                    }
                }
            });
            return List.copyOf(items);
        }

        @Override
        public int simulateExtract(ItemStack stack, int amount, ServerPlayer player) {
            long extracted = storage().map(meStorage -> meStorage.extract(AEItemKey.of(stack), amount,
                    Actionable.SIMULATE, IActionSource.ofPlayer(player, (IActionHost) accessPoint))).orElse(0L);
            return ItemInsertion.acceptedCount(stack.copyWithCount(amount), extracted);
        }

        @Override
        public ItemStack extract(ItemStack stack, int amount, ServerPlayer player) {
            long extracted = storage().map(meStorage -> meStorage.extract(AEItemKey.of(stack), amount,
                    Actionable.MODULATE, IActionSource.ofPlayer(player, (IActionHost) accessPoint))).orElse(0L);
            int count = ItemInsertion.acceptedCount(stack.copyWithCount(amount), extracted);
            return count == 0 ? ItemStack.EMPTY : stack.copyWithCount(count);
        }
    }
}
