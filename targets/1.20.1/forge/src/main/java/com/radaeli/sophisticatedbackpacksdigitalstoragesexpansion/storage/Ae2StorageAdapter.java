package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.IPartHost;
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
import net.minecraftforge.items.IItemHandler;

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
        return identify(level, pos, null, null);
    }

    @Override
    public Optional<StorageEndpoint> identify(net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.phys.BlockHitResult hit, SourceLocation source) {
        var blockEntity = level.getBlockEntity(pos);
        // Preserve direct access-point linking and all existing persisted endpoints.
        if (blockEntity instanceof IWirelessAccessPoint) {
            return Optional.of(new StorageEndpoint(BACKEND_ID, level.dimension(), pos.immutable()));
        }
        IGridNode node;
        if (blockEntity instanceof IPartHost host) {
            if (hit == null) return Optional.empty();
            var selected = host.selectPartWorld(hit.getLocation());
            // The center part is the cable. Facades and empty hits are not network devices.
            if (selected.part == null || selected.side == null) return Optional.empty();
            node = selected.part.getGridNode();
        } else if (blockEntity instanceof IActionHost host) {
            node = host.getActionableNode();
        } else if (blockEntity instanceof IInWorldGridNodeHost host) {
            node = host.getGridNode(hit == null ? null : hit.getDirection());
        } else {
            return Optional.empty();
        }
        if (node == null || !node.isActive()) return Optional.empty();
        IGrid grid = node.getGrid();
        if (!grid.getEnergyService().isNetworkPowered()) return Optional.empty();

        // Only discovery changes (P02/P03/P11): save one existing access point, never
        // the clicked device, and leave operation-time range/access and no-failover intact.
        StorageEndpoint nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (IGridNode candidate : grid.getNodes()) {
            if (!(candidate.getOwner() instanceof IWirelessAccessPoint accessPoint)
                    || !candidate.isActive() || !accessPoint.isActive() || accessPoint.getGrid() != grid) continue;
            var location = accessPoint.getLocation();
            if (location.getLevel() != level || !level.isLoaded(location.getPos())) continue;
            var center = net.minecraft.world.phys.Vec3.atCenterOf(location.getPos());
            double distance = source == null ? pos.distSqr(location.getPos()) : source.position().distanceToSqr(center);
            double range = accessPoint.getRange();
            if (source != null && (!source.dimension().equals(level.dimension()) || distance > range * range)) continue;
            if (distance < nearestDistance || (distance == nearestDistance && nearest != null
                    && location.getPos().compareTo(nearest.pos()) < 0)) {
                nearest = new StorageEndpoint(BACKEND_ID, level.dimension(), location.getPos().immutable());
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
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
