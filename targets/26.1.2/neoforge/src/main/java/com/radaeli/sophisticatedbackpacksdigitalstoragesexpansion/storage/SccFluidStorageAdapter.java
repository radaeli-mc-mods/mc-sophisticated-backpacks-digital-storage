package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import de.scc.calculator.api.SccNetwork;
import de.scc.calculator.network.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;

/** SCC's public drain API identifies fluids by type only. Modified components are unsupported. */
public final class SccFluidStorageAdapter implements FluidStorageAdapter {
    public String backendId() { return "scc"; }
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) { return resolve(level, endpoint, null); }
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        return level.isLoaded(endpoint.pos()) && level.getBlockEntity(endpoint.pos()) instanceof NetworkAntennaBlockEntity
                ? Optional.of(new Target(endpoint, source)) : Optional.empty();
    }
    private record Target(StorageEndpoint endpoint, SourceLocation source) implements FluidStorageTarget {
        private Optional<NetworkView> view(ServerPlayer player, SccSecurity.Right right) {
            return SccAccessPolicy.resolve(player, endpoint, source, right);
        }
        public boolean isReachable(ServerPlayer player, SourceLocation origin, FluidStorageAccess access) {
            return SccAccessPolicy.resolve(player, endpoint, origin, access == FluidStorageAccess.INSERT
                    ? SccSecurity.Right.PUT : SccSecurity.Right.TAKE).isPresent();
        }
        public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            var resolved = view(player, SccSecurity.Right.TAKE);
            if (resolved.isEmpty()) return List.of();
            NetworkView network = resolved.get();
            var result = new ArrayList<FluidStorageAmount>();
            for (var fluid : network.fluidContents().keySet()) {
                int amount = SccNetwork.drainFluid(network, fluid, 1000, true);
                if (amount > 0) result.add(new FluidStorageAmount(FluidStorageKey.of(new FluidStack(fluid, 1)), amount));
            }
            return List.copyOf(result);
        }
        public long simulateInsert(FluidStack stack, ServerPlayer player) {
            if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) return 0;
            return stack.getAmount() - view(player, SccSecurity.Right.PUT)
                    .map(network -> SccNetwork.insertFluid(network, stack.copy(), true)).orElse(stack).getAmount();
        }
        public FluidStack insert(FluidStack stack, ServerPlayer player) {
            if (!stack.getComponentsPatch().isEmpty()) return stack.copy();
            return view(player, SccSecurity.Right.PUT).map(network -> SccNetwork.insertFluid(network, stack.copy(), false)).orElseGet(stack::copy);
        }
        public long simulateExtract(FluidStorageKey key, long amount, ServerPlayer player) {
            if (!key.components().isEmpty() || amount <= 0) return 0;
            return view(player, SccSecurity.Right.TAKE).map(network -> SccNetwork.drainFluid(network, key.fluid(), (int)Math.min(1000, amount), true)).orElse(0);
        }
        public FluidStack extract(FluidStorageKey key, long amount, ServerPlayer player) {
            if (!key.components().isEmpty() || amount <= 0) return FluidStack.EMPTY;
            return key.toStack(view(player, SccSecurity.Right.TAKE)
                    .map(network -> SccNetwork.drainFluid(network, key.fluid(), (int)Math.min(1000, amount), false)).orElse(0));
        }
    }
}
