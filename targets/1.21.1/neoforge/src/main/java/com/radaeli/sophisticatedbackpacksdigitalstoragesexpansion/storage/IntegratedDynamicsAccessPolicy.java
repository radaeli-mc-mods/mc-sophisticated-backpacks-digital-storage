package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetworkElement;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetwork;
import org.cyclops.integrateddynamics.api.network.IPositionedAddonsNetworkIngredients;
import org.cyclops.integrateddynamics.api.network.INetworkIngredientsChannel;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integrateddynamics.core.network.IngredientChannelPositioned;
import org.cyclops.integrateddynamics.core.network.PositionedAddonsNetworkIngredients;

/** P03/P04: revalidate the persisted Omni, without failover or a terminal item requirement. */
public final class IntegratedDynamicsAccessPolicy {
    private IntegratedDynamicsAccessPolicy() {}

    /** No native network or channel survives an operation. Native channels enforce identity-specific filters. */
    static <T, M> Optional<IPositionedAddonsNetworkIngredients<T, M>> ingredients(ServerPlayer player,
            StorageEndpoint endpoint, SourceLocation source, IngredientComponent<T, M> component) {
        return NetworkHelpers.getIngredientNetwork(resolve(player, endpoint, source), component);
    }

    static <T, M> INetworkIngredientsChannel<T, M> liveChannel(IPositionedAddonsNetworkIngredients<T, M> network) {
        // The indexed channel can omit newly available identities (including a just-released guard).
        // The pinned native positioned channel performs directed storage lookups and keeps native
        // view/extraction filters, priority, rate limits and reentrancy protection. No physical IO here.
        return new IngredientChannelPositioned<>((PositionedAddonsNetworkIngredients<T, M>) network,
                IPositionedAddonsNetwork.WILDCARD_CHANNEL);
    }

    public static Optional<INetwork> resolve(ServerPlayer player, StorageEndpoint endpoint, SourceLocation source) {
        if (player == null || source == null || !player.isAlive() || player.isSpectator() || player instanceof FakePlayer
                || !"integrateddynamics".equals(endpoint.backendId())) return Optional.empty();
        var level = player.server.getLevel(endpoint.dimension());
        if (level == null || !level.isLoaded(endpoint.pos()) || !level.mayInteract(player, endpoint.pos())) return Optional.empty();
        return NetworkHelpers.getNetwork(level, endpoint.pos(), null)
                .filter(IntegratedDynamicsLinkDiscovery::online)
                .filter(network -> network.getElements().stream().anyMatch(element ->
                        element instanceof IPartNetworkElement<?, ?> part
                        && part.getPosition().getLevelKey().equals(endpoint.dimension())
                        && part.getPosition().getBlockPos().equals(endpoint.pos())
                        && IntegratedDynamicsLinkDiscovery.eligible(level, network, part)));
    }
}
