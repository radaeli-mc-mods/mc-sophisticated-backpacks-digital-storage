package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fluids.FluidStack;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.cyclopscore.ingredient.collection.IngredientCollectionPrototypeMap;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.core.network.IngredientChannelPositioned;
import org.cyclops.integrateddynamics.core.network.PositionedAddonsNetworkIngredients;

/** P03/P08/P11: native fluid identities and mB, without bucket/item conversion. */
public final class IntegratedDynamicsFluidStorageAdapter implements FluidStorageAdapter {
    @Override
    public String backendId() {
        return "integrateddynamics";
    }

    @Override
    public Optional<FluidStorageTarget> resolve(ServerLevel level, StorageEndpoint endpoint) {
        if (!backendId().equals(endpoint.backendId()) || !level.dimension().equals(endpoint.dimension())
                || !level.isLoaded(endpoint.pos())) {
            return Optional.empty();
        }
        return Optional.of(new Target(endpoint));
    }

    private record Target(StorageEndpoint endpoint) implements FluidStorageTarget {
        private Optional<IPositionedAddonsNetworkIngredients<FluidStack, Integer>> network(ServerPlayer player) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, IngredientComponent.FLUIDSTACK);
        }

        private Optional<INetworkIngredientsChannel<FluidStack, Integer>> channel(ServerPlayer player) {
            return network(player).map(IntegratedDynamicsAccessPolicy::liveChannel);
        }

        @Override
        public boolean isReachable(ServerPlayer player, SourceLocation source, FluidStorageAccess access) {
            return IntegratedDynamicsAccessPolicy.ingredients(player, endpoint, source, IngredientComponent.FLUIDSTACK)
                    .isPresent();
        }

        @Override
        public List<FluidStorageAmount> getAvailableFluids(ServerPlayer player) {
            Optional<IPositionedAddonsNetworkIngredients<FluidStack, Integer>> resolved = network(player);
            if (resolved.isEmpty()) {
                return List.of();
            }
            var amounts = new IngredientCollectionPrototypeMap<>(IngredientComponent.FLUIDSTACK);
            IntegratedDynamicsAccessPolicy.liveChannel(resolved.get()).iterator().forEachRemaining(amounts::add);

            List<FluidStorageAmount> result = new ArrayList<>();
            var prototypes = amounts.prototypeIterator();
            while (prototypes.hasNext()) {
                var entry = prototypes.next();
                FluidStack identity = entry.getKey();
                long amount = entry.getValue();
                if (!identity.isEmpty() && amount > 0) {
                    result.add(new FluidStorageAmount(FluidStorageKey.of(identity), amount));
                }
            }
            return List.copyOf(result);
        }

        @Override
        public long simulateInsert(FluidStack stack, ServerPlayer player) {
            return (long) stack.getAmount() - insert(stack, player, true).getAmount();
        }

        @Override
        public FluidStack insert(FluidStack stack, ServerPlayer player) {
            return insert(stack, player, false);
        }

        private FluidStack insert(FluidStack stack, ServerPlayer player, boolean simulate) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return channel(player).map(value -> value.insert(stack.copy(), simulate)).orElseGet(stack::copy);
        }

        @Override
        public long simulateExtract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            return extract(key, amountMb, player, true).getAmount();
        }

        @Override
        public FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player) {
            return extract(key, amountMb, player, false);
        }

        private FluidStack extract(FluidStorageKey key, long amountMb, ServerPlayer player, boolean simulate) {
            if (amountMb <= 0) {
                return FluidStack.EMPTY;
            }
            FluidStack request = key.toStack((int) Math.min(Integer.MAX_VALUE, amountMb));
            return network(player).map(value -> IntegratedDynamicsAccessPolicy.liveChannel(value).extract(request,
                    IngredientComponent.FLUIDSTACK.getMatcher().getExactMatchNoQuantityCondition(), simulate))
                    .orElse(FluidStack.EMPTY);
        }
    }
}
