package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.util.Optional;
import de.scc.calculator.api.SccNetwork;
import de.scc.calculator.network.NetworkAntennaBlockEntity;
import de.scc.calculator.network.NetworkView;
import de.scc.calculator.network.SccSecurity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** SCC 1.5.3 access policy. Never uses WirelessAccess.antenna, which can load chunks. */
final class SccAccessPolicy {
    private SccAccessPolicy() {}


    static Optional<NetworkView> resolve(ServerPlayer player, StorageEndpoint endpoint,
            SourceLocation source, SccSecurity.Right right) {
        if (source == null || player == null || !player.isAlive() || player.isSpectator()
                || player instanceof net.neoforged.neoforge.common.util.FakePlayer) return Optional.empty();
        var level = player.server.getLevel(endpoint.dimension());
        if (level == null || !level.isLoaded(endpoint.pos()) || !level.mayInteract(player, endpoint.pos())
                || !(level.getBlockEntity(endpoint.pos()) instanceof NetworkAntennaBlockEntity antenna)
                || antenna.isRemoved()) return Optional.empty();
        if (!SccCoverage.inRange(antenna.dimensional(), source.dimension().equals(endpoint.dimension()), antenna.infinite(),
                source.position().distanceToSqr(Vec3.atCenterOf(endpoint.pos())), antenna.range())) return Optional.empty();
        NetworkView view = SccNetwork.at(level, endpoint.pos());
        if (!view.online() || !SccNetwork.connected(view)) return Optional.empty();
        boolean allowed = right == null
                ? SccSecurity.may(player, view, SccSecurity.Right.PUT) || SccSecurity.may(player, view, SccSecurity.Right.TAKE)
                : SccSecurity.may(player, view, right);
        return allowed ? Optional.of(view) : Optional.empty();
    }
}
