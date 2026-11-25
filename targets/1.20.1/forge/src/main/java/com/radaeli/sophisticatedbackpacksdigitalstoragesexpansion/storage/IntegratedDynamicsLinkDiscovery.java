package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Comparator;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.cyclops.integrateddynamics.api.PartStateException;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.IPartNetworkElement;
import org.cyclops.integrateddynamics.api.part.IPartContainer;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional;

/** P02/P03/P11: discover a linked Omni without loading a dimension or chunk. */
public final class IntegratedDynamicsLinkDiscovery {
    private IntegratedDynamicsLinkDiscovery() {
    }

    public static Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !server.isLoaded(pos)) {
            return Optional.empty();
        }
        Optional<IPartContainer> host = PartHelpers.getPartContainer(level, pos, null).resolve();
        if (host.isEmpty() || !host.get().hasParts()) {
            return Optional.empty();
        }
        Optional<INetwork> network = NetworkHelpers.getNetwork(level, pos, null).resolve();
        if (network.isEmpty() || !online(network.get())) {
            return Optional.empty();
        }

        var candidates = network.get().getElements().stream()
                .filter(element -> element instanceof IPartNetworkElement<?, ?>)
                .<IPartNetworkElement<?, ?>>map(element -> (IPartNetworkElement<?, ?>) element)
                .filter(element -> eligible(server, network.get(), element))
                .sorted(Comparator.comparing((IPartNetworkElement<?, ?> element) ->
                                element.getPosition().getLevelKey().location().toString())
                        .thenComparingInt(element -> element.getPosition().getBlockPos().getX())
                        .thenComparingInt(element -> element.getPosition().getBlockPos().getY())
                        .thenComparingInt(element -> element.getPosition().getBlockPos().getZ())
                        .thenComparingInt(element -> element.getSide().ordinal()))
                .toList();

        // The clicked host wins even when another Omni sorts first. An ineligible Omni host fails
        // directly rather than binding an unrelated Omni elsewhere in the network.
        var clicked = candidates.stream().filter(element ->
                element.getPosition().getLevelKey().equals(level.dimension())
                        && element.getPosition().getBlockPos().equals(pos)).findFirst();
        boolean omniHost = host.get().getParts().values().stream()
                .anyMatch(PartTypeConnectorOmniDirectional.class::isInstance);
        return (omniHost ? clicked : candidates.stream().findFirst()).map(element ->
                new StorageEndpoint("integrateddynamics", element.getPosition().getLevelKey(),
                        element.getPosition().getBlockPos()));
    }

    static boolean online(INetwork network) {
        return network.isInitialized() && !network.isKilled() && !network.isCrashed();
    }

    static boolean eligible(ServerLevel context, INetwork network, IPartNetworkElement<?, ?> element) {
        var position = element.getPosition();
        ServerLevel world = context.getServer().getLevel(position.getLevelKey());
        if (world == null || !world.isLoaded(position.getBlockPos())
                || !(element.getPart() instanceof PartTypeConnectorOmniDirectional) || !element.isLoaded()) {
            return false;
        }

        // Re-read the actual host and network; stale topology entries are never authority.
        Optional<IPartContainer> host = PartHelpers.getPartContainer(world, position.getBlockPos(), null).resolve();
        if (host.isEmpty() || host.get().getPart(element.getSide()) != element.getPart()
                || NetworkHelpers.getNetwork(world, position.getBlockPos(), null).resolve().orElse(null) != network) {
            return false;
        }
        try {
            if (!(host.get().getPartState(element.getSide()) instanceof PartTypeConnectorOmniDirectional.State state)
                    || !state.isEnabled() || !state.hasConnectorId()) {
                return false;
            }
            // The native loaded group is authority; deserialization resets addedToGroup.
            if (!PartTypeConnectorOmniDirectional.LOADED_GROUPS.getPositions(state.getGroupId()).contains(
                    org.cyclops.integrateddynamics.api.part.PartPos.of(world, position.getBlockPos(), element.getSide()))) return false;
            // Integrated Dynamics listeners include the energy policy; canUpdate simulates its energy check.
            for (var listener : network.getFullNetworkListeners()) {
                if (!listener.canUpdate(element)) {
                    return false;
                }
            }
            // Forge 1.20.1 does not expose the newer INetwork#isValid hook; confirm the element
            // remains in the live network after the host, part state and native listeners pass.
            return network.getElements().contains(element);
        } catch (PartStateException unavailable) {
            return false;
        }
    }
}
