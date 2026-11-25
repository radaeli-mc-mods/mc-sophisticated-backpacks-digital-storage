package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;

/** P03/P08/P11: native fluid identities and mB, without bucket/item conversion. */
public final class IntegratedDynamicsFluidStorageAdapter implements FluidStorageAdapter {
    public String backendId() { return "integrateddynamics"; }
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) { return resolve(level, endpoint, null); }
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint, SourceLocation source) {
        if (!backendId().equals(endpoint.backendId()) || !level.dimension().equals(endpoint.dimension()) || !level.isLoaded(endpoint.pos())) return Optional.empty();
        return Optional.of(new Target(endpoint, source));
    }
    private record Target(StorageEndpoint endpoint, SourceLocation source) implements FluidStorageTarget {
        private Optional<IPositionedAddonsNetworkIngredients<FluidStack, Integer>> network(ServerPlayer player, SourceLocation origin) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, origin, IngredientComponent.FLUIDSTACK);
        }
        private Optional<INetworkIngredientsChannel<FluidStack, Integer>> channel(ServerPlayer player) {
            return network(player, source).map(network -> network.getChannel(IPositionedAddonsNetwork.WILDCARD_CHANNEL));
        }
        public boolean isReachable(ServerPlayer player, SourceLocation origin, FluidStorageAccess access) { return network(player, origin).isPresent(); }
        public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            try (var positionScope = IntegratedPositionScope.open()) {
                var resolved = network(player, source);
                if (resolved.isEmpty()) return List.of();
                var amounts = new IngredientCollectionPrototypeMap<>(IngredientComponent.FLUIDSTACK);
                IntegratedDynamicsAccessPolicy.liveChannel(resolved.get()).iterator().forEachRemaining(amounts::add);
                var result = new ArrayList<FluidStorageAmount>();
                var prototypes = amounts.prototypeIterator();
                while (prototypes.hasNext()) {
                    var entry = prototypes.next();
                    FluidStack identity = entry.getKey();
                    long amount = entry.getValue();
                    if (!identity.isEmpty() && amount > 0) result.add(new FluidStorageAmount(FluidStorageKey.of(identity), amount));
                }
                return List.copyOf(result);
            }
        }
        private FluidStack insert(FluidStack stack, ServerPlayer player, boolean simulate) {
            try (var positionScope = IntegratedPositionScope.open()) {
                if (stack.isEmpty()) return FluidStack.EMPTY;
                return channel(player).map(channel -> channel.insert(stack.copy(), simulate)).orElseGet(stack::copy);
            }
        }
        public long simulateInsert(FluidStack stack, ServerPlayer player) { return (long) stack.getAmount() - insert(stack, player, true).getAmount(); }
        public FluidStack insert(FluidStack stack, ServerPlayer player) { return insert(stack, player, false); }
        private FluidStack extract(FluidStorageKey key, long amount, ServerPlayer player, boolean simulate) {
            try (var positionScope = IntegratedPositionScope.open()) {
                if (amount <= 0) return FluidStack.EMPTY;
                var request = key.toStack((int) Math.min(Integer.MAX_VALUE, amount));
                return network(player, source).map(network -> {
                    return IntegratedDynamicsAccessPolicy.liveChannel(network).extract(request,
                            IngredientComponent.FLUIDSTACK.getMatcher().getExactMatchNoQuantityCondition(), simulate);
                }).orElse(FluidStack.EMPTY);
            }
        }
        public long simulateExtract(FluidStorageKey key, long amount, ServerPlayer player) { return extract(key, amount, player, true).getAmount(); }
        public FluidStack extract(FluidStorageKey key, long amount, ServerPlayer player) { return extract(key, amount, player, false); }
    }
}
