package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid adapter for the AE2 version pinned by P01. AE2 stores fluids in mB. */
public final class Ae2FluidStorageAdapter implements FluidStorageAdapter {
    public static final String BACKEND_ID = Ae2StorageAdapter.BACKEND_ID;

    @Override
    public String backendId() {
        return BACKEND_ID;
    }

    @Override
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (level.getBlockEntity(endpoint.pos()) instanceof IWirelessAccessPoint accessPoint) {
            return Optional.of(new Target(endpoint, accessPoint));
        }
        return Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, IWirelessAccessPoint accessPoint) implements FluidStorageTarget {
        private Optional<MEStorage> storage() {
            IGrid grid = accessPoint.getGrid();
            return accessPoint.isActive() && grid != null && grid.getEnergyService().isNetworkPowered()
                    ? Optional.of(grid.getStorageService().getInventory())
                    : Optional.empty();
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source, FluidStorageAccess access) {
            if (!source.dimension().equals(endpoint.dimension()) || !accessPoint.isActive()
                    || accessPoint.getActionableNode() == null || !accessPoint.getActionableNode().isActive()) {
                return false;
            }
            IGrid grid = accessPoint.getGrid();
            if (grid == null || !grid.getEnergyService().isNetworkPowered()) {
                return false;
            }
            double range = accessPoint.getRange();
            Vec3 target = Vec3.atCenterOf(accessPoint.getLocation().getPos());
            return source.position().distanceToSqr(target) <= range * range;
        }

        @Override
        public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            List<FluidStorageAmount> result = new ArrayList<>();
            storage().ifPresent(meStorage -> {
                var available = meStorage.getAvailableStacks();
                for (AEKey key : available.keySet()) {
                    long amount = available.get(key);
                    if (key instanceof AEFluidKey fluidKey && amount > 0) {
                        result.add(new FluidStorageAmount(FluidStorageKey.of(fluidKey.toStack(1)), amount));
                    }
                }
            });
            return List.copyOf(result);
        }

        @Override
        public long simulateInsert(FluidStack stack, ServerPlayer player) {
            if (stack.isEmpty()) {
                return 0;
            }
            return storage().map(meStorage -> meStorage.insert(AEFluidKey.of(stack), stack.getAmount(),
                    Actionable.SIMULATE, actionSource(player))).orElse(0L);
        }

        @Override
        public FluidStack insert(FluidStack stack, ServerPlayer player) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            long accepted = storage().map(meStorage -> meStorage.insert(AEFluidKey.of(stack), stack.getAmount(),
                    Actionable.MODULATE, actionSource(player))).orElse(0L);
            return remainder(stack, accepted);
        }

        @Override
        public long simulateExtract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            if (amountMb <= 0) {
                return 0;
            }
            return storage().map(meStorage -> meStorage.extract(keyFor(key), amountMb,
                    Actionable.SIMULATE, actionSource(player))).orElse(0L);
        }

        @Override
        public FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            if (amountMb <= 0) {
                return FluidStack.EMPTY;
            }
            long extracted = storage().map(meStorage -> meStorage.extract(keyFor(key), amountMb,
                    Actionable.MODULATE, actionSource(player))).orElse(0L);
            return key.toStack((int) Math.min(Math.max(0, extracted), Integer.MAX_VALUE));
        }

        private AEFluidKey keyFor(FluidStorageKey key) {
            return AEFluidKey.of(key.toStack(1));
        }

        private IActionSource actionSource(ServerPlayer player) {
            return IActionSource.ofPlayer(player, (IActionHost) accessPoint);
        }

        private static FluidStack remainder(FluidStack offered, long accepted) {
            FluidStack remainder = offered.copy();
            remainder.setAmount(offered.getAmount() - (int) Math.min(offered.getAmount(), Math.max(0, accepted)));
            return remainder.isEmpty() ? FluidStack.EMPTY : remainder;
        }
    }
}
