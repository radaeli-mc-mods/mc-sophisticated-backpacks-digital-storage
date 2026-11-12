package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.IWirelessTransmitter;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.blockentity.ControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidStack;

/** Fluid half of the Refined Storage 1.x backend. */
public final class RefinedStorageFluidStorageAdapter implements FluidStorageAdapter {
    @Override public String backendId() { return RefinedStorageAdapter.BACKEND_ID; }

    @Override
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!level.isLoaded(endpoint.pos())) return Optional.empty();
        return level.getBlockEntity(endpoint.pos()) instanceof ControllerBlockEntity controller
                ? Optional.of(new Target(endpoint, controller)) : Optional.empty();
    }

    private record Target(StorageEndpoint endpoint, ControllerBlockEntity controller) implements FluidStorageTarget {
        private INetwork network() {
            INetwork network = controller.getNetwork();
            return network != null && network.canRun() ? network : null;
        }

        @Override public boolean isReachable(ServerPlayer player, SourceLocation source, FluidStorageAccess access) {
            INetwork network = network();
            if (network == null || !source.dimension().equals(endpoint.dimension())) return false;
            if (access == FluidStorageAccess.INSERT
                    && !network.getSecurityManager().hasPermission(Permission.INSERT, player)) return false;
            if (access == FluidStorageAccess.EXTRACT
                    && !network.getSecurityManager().hasPermission(Permission.EXTRACT, player)) return false;
            for (var entry : network.getNodeGraph().all()) {
                INetworkNode node = entry.getNode();
                if (node instanceof IWirelessTransmitter transmitter && node.isActive()
                        && transmitter.getDimension().equals(source.dimension())) {
                    long range = transmitter.getRange();
                    if (transmitter.getOrigin().distToCenterSqr(source.position().x(), source.position().y(), source.position().z())
                            < range * range) return true;
                }
            }
            return false;
        }

        @Override public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            INetwork network = network();
            if (network == null) return List.of();
            var list = network.getFluidStorageCache().getList();
            List<FluidStorageAmount> result = new ArrayList<>();
            list.getStacks().forEach(entry -> {
                FluidStack fluid = entry.getStack();
                int amount = list.getCount(fluid);
                if (!fluid.isEmpty() && amount > 0) result.add(new FluidStorageAmount(
                        FluidStorageKey.of(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.FluidStacks.copyWithAmount(fluid, 1)), amount));
            });
            return List.copyOf(result);
        }

        @Override public long simulateInsert(FluidStack stack, ServerPlayer player) {
            INetwork network = network();
            if (network == null || stack.isEmpty()) return 0;
            FluidStack remainder = network.insertFluid(stack.copy(), stack.getAmount(), Action.SIMULATE);
            return Math.max(0, stack.getAmount() - remainder.getAmount());
        }

        @Override public FluidStack insert(FluidStack stack, ServerPlayer player) {
            INetwork network = network();
            return network == null ? stack.copy() : network.insertFluid(stack.copy(), stack.getAmount(), Action.PERFORM);
        }

        @Override public long simulateExtract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            INetwork network = network();
            if (network == null) return 0;
            FluidStack extracted = network.extractFluid(key.toStack(1), (int) Math.min(Integer.MAX_VALUE, amountMb), Action.SIMULATE);
            return extracted.getAmount();
        }

        @Override public FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            INetwork network = network();
            return network == null ? FluidStack.EMPTY
                    : network.extractFluid(key.toStack(1), (int) Math.min(Integer.MAX_VALUE, amountMb), Action.PERFORM);
        }
    }
}
