package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;

import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.network.IngredientChannelPositioned;
import org.cyclops.integrateddynamics.core.network.PositionedAddonsNetworkIngredients;

/** P03/P04: revalidate the linked Omni and source when a Forge connection is opened. */
public final class IntegratedDynamicsAccessPolicy {
    private IntegratedDynamicsAccessPolicy() {
    }

    /** Native networks and channels are transient; each operation resolves the live Forge network again. */
    static <T, M> Optional<IPositionedAddonsNetworkIngredients<T, M>> ingredients(ServerPlayer player,
            StorageEndpoint endpoint, IngredientComponent<T, M> component) {
        Optional<INetwork> network = resolve(player, endpoint);
        if (network.isEmpty()) {
            return Optional.empty();
        }
        LazyOptional<INetwork> live = LazyOptional.of(network::get);
        return NetworkHelpers.getIngredientNetwork(live, component).resolve();
    }

    static <T, M> Optional<IPositionedAddonsNetworkIngredients<T, M>> ingredients(ServerPlayer player,
            StorageEndpoint endpoint, SourceLocation source, IngredientComponent<T, M> component) {
        Optional<INetwork> network = resolve(player, endpoint, source);
        if (network.isEmpty()) {
            return Optional.empty();
        }
        LazyOptional<INetwork> live = LazyOptional.of(network::get);
        return NetworkHelpers.getIngredientNetwork(live, component).resolve();
    }

    static <T, M> INetworkIngredientsChannel<T, M> liveChannel(
            IPositionedAddonsNetworkIngredients<T, M> network) {
        // The indexed channel can omit identities created or restored after its last observation.
        // The positioned native channel keeps Integrated Dynamics storage filters, priority, rate limits,
        // and reentrancy protection while resolving actual connected storage on demand.
        return new IngredientChannelPositioned<>((PositionedAddonsNetworkIngredients<T, M>) network,
                IPositionedAddonsNetwork.WILDCARD_CHANNEL);
    }

    public static Optional<INetwork> resolve(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source) {
        if (player == null || source == null || !player.isAlive() || player.isSpectator() || player instanceof FakePlayer
                || !"integrateddynamics".equals(endpoint.backendId())) {
            return Optional.empty();
        }
        return resolveLiveNetwork(player, endpoint);
    }

    private static Optional<INetwork> resolve(ServerPlayer player, StorageEndpoint endpoint) {
        if (player == null || !player.isAlive() || player.isSpectator() || player instanceof FakePlayer
                || !"integrateddynamics".equals(endpoint.backendId())) {
            return Optional.empty();
        }
        return resolveLiveNetwork(player, endpoint);
    }

    private static Optional<INetwork> resolveLiveNetwork(ServerPlayer player, StorageEndpoint endpoint) {
        ServerLevel level = player.server.getLevel(endpoint.dimension());
        if (level == null || !level.isLoaded(endpoint.pos()) || !level.mayInteract(player, endpoint.pos())) {
            return Optional.empty();
        }
        return NetworkHelpers.getNetwork(level, endpoint.pos(), null).resolve()
                .filter(IntegratedDynamicsLinkDiscovery::online)
                .filter(network -> network.getElements().stream().anyMatch(element ->
                        element instanceof org.cyclops.integrateddynamics.api.network.IPartNetworkElement<?, ?> part
                                && part.getPosition().getLevelKey().equals(endpoint.dimension())
                                && part.getPosition().getBlockPos().equals(endpoint.pos())
                                && IntegratedDynamicsLinkDiscovery.eligible(level, network, part)));
    }
}
