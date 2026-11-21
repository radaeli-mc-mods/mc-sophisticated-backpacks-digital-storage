package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.cyclops.integrateddynamics.api.network.*;
import org.cyclops.integrateddynamics.api.PartStateException;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional;

/** P02/P03/P11: topology-only discovery; never resolves a world by loading it. */
public final class IntegratedDynamicsLinkDiscovery {
    private IntegratedDynamicsLinkDiscovery() {}

    public static Optional<StorageEndpoint> identify(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !server.isLoaded(pos)) return Optional.empty();
        var host = PartHelpers.getPartContainer(level, pos, null);
        if (host.isEmpty() || !host.get().hasParts()) return Optional.empty();
        var network = NetworkHelpers.getNetwork(level, pos, null);
        if (network.isEmpty() || !online(network.get())) return Optional.empty();
        var candidates = network.get().getElements().stream()
                .filter(element -> element instanceof IPartNetworkElement<?, ?>)
                .<IPartNetworkElement<?, ?>>map(element -> (IPartNetworkElement<?, ?>) element)
                .filter(element -> eligible(server, network.get(), element))
                .sorted(Comparator.comparing((IPartNetworkElement<?, ?> e) -> e.getPosition().getLevelKey().identifier().toString())
                        .thenComparingInt(e -> e.getPosition().getBlockPos().getX())
                        .thenComparingInt(e -> e.getPosition().getBlockPos().getY())
                        .thenComparingInt(e -> e.getPosition().getBlockPos().getZ())
                        .thenComparingInt(e -> e.getSide().ordinal())).toList();
        // The clicked host wins even when another Omni sorts first.
        var clicked = candidates.stream().filter(e -> e.getPosition().getLevelKey().equals(level.dimension())
                && e.getPosition().getBlockPos().equals(pos)).findFirst();
        // An ineligible Omni is a failed direct link, not an invitation to bind another block.
        boolean omniHost = host.get().getParts().values().stream().anyMatch(PartTypeConnectorOmniDirectional.class::isInstance);
        return (omniHost ? clicked : candidates.stream().findFirst()).map(e -> new StorageEndpoint(
                        "integrateddynamics", e.getPosition().getLevelKey(), e.getPosition().getBlockPos()));
    }

    static boolean online(INetwork network) {
        return network.isInitialized() && !network.isKilled() && !network.isCrashed();
    }

    static boolean eligible(ServerLevel context, INetwork network, IPartNetworkElement<?, ?> element) {
        var position = element.getPosition();
        var world = context.getServer().getLevel(position.getLevelKey());
        if (world == null || !world.isLoaded(position.getBlockPos())
                || !(element.getPart() instanceof PartTypeConnectorOmniDirectional) || !element.isLoaded()) return false;
        // Re-read the actual host and network; stale topology entries are not authority.
        var host = PartHelpers.getPartContainer(world, position.getBlockPos(), null);
        if (host.isEmpty() || host.get().getPart(element.getSide()) != element.getPart()
                || NetworkHelpers.getNetwork(world, position.getBlockPos(), null).orElse(null) != network) return false;
        try {
            if (!(host.get().getPartState(element.getSide()) instanceof PartTypeConnectorOmniDirectional.State state)
                    || !state.isEnabled() || !state.hasConnectorId()) return false;
            // Portable terminals resolve the native loaded-group positions. The transient
            // addedToGroup flag can reset when the part state is deserialized after topology init.
            // Require membership of this exact loaded Omni, never another member of the group.
            if (!PartTypeConnectorOmniDirectional.LOADED_GROUPS.getPositions(state.getGroupId()).contains(
                    org.cyclops.integrateddynamics.api.part.PartPos.of(world, position.getBlockPos(), element.getSide()))) return false;
            // Native listeners include the energy policy. canUpdate only simulates energy extraction.
            for (var listener : network.getFullNetworkListeners()) {
                // Native canUpdate uses the legacy boolean helper, which opens a root transaction.
                // Pickup revalidates while its caller's journal is open: use the same native energy
                // channel in an aborting child transaction instead of opening a second root.
                var current = net.neoforged.neoforge.transfer.transaction.Transaction.getCurrentOpenedTransaction();
                if (current != null && listener instanceof org.cyclops.integrateddynamics.core.network.EnergyNetwork energy
                        && element instanceof IEnergyConsumingNetworkElement consuming) {
                    int rate = consuming.getConsumptionRate() * org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier;
                    if (rate != 0) {
                        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(current)) {
                            if (energy.getChannel(element.getChannel()).extract((long) rate, tx) != rate) return false;
                        }
                    }
                } else if (!listener.canUpdate(element)) return false;
            }
            return network.isValid(element);
        } catch (PartStateException unavailable) {
            return false;
        }
    }
}
