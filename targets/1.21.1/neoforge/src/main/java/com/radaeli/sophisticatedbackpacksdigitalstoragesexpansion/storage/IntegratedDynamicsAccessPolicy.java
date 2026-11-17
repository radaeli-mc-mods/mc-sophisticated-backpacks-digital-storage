package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.cyclops.integrateddynamics.api.network.*;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

/** P03/P04: revalidate the persisted Omni, without failover or a terminal item requirement. */
public final class IntegratedDynamicsAccessPolicy {
    private IntegratedDynamicsAccessPolicy() {}

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
