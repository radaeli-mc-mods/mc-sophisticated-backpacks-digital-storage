package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Loaded only by the SCC profile; isolates optional SCC types from RS-only verification. */
final class SccLinkingPartsFixture {
    static void prepare(RealNetworks network) {
        var pos = network.endpoint().west();
        var level = network.helper().getLevel();
        var state = BuiltInRegistries.BLOCK.getValue(Identifier.parse("scc:cable_bus")).defaultBlockState()
                .setValue(de.scc.calculator.network.CableBusBlock.CABLE, true);
        level.setBlockAndUpdate(pos, state);
        var bus = (de.scc.calculator.network.CableBusBlockEntity) level.getBlockEntity(pos);
        var terminal = BuiltInRegistries.BLOCK.getValue(Identifier.parse("scc:terminal")).defaultBlockState();
        bus.addPart(Direction.UP, terminal, new de.scc.calculator.network.TerminalBlockEntity(pos, terminal));
    }
    static void check(RealNetworks network, ServerPlayer player) {
        var helper = network.helper();
        var level = helper.getLevel();
        var origin = SourceLocation.carriedBy(player);
        var bus = network.endpoint().west();
        var partHit = new BlockHitResult(Vec3.atLowerCornerOf(bus).add(0.5, 0.99, 0.5), Direction.UP, bus, false);
        helper.assertTrue(StorageAdapterRegistry.identify(level, bus, partHit, origin).equals(java.util.Optional.of(network.binding())), "SCC multipart terminal rejected");
        var cableHit = new BlockHitResult(Vec3.atCenterOf(bus), Direction.SOUTH, bus, false);
        helper.assertTrue(StorageAdapterRegistry.identify(level, bus, cableHit, origin).isEmpty(), "SCC multipart cable accepted");
        var distant = new SourceLocation(origin.dimension(), origin.position().add(2048, 0, 0));
        var device = network.endpoint().north(2);
        var deviceHit = new BlockHitResult(Vec3.atCenterOf(device), Direction.SOUTH, device, false);
        helper.assertTrue(StorageAdapterRegistry.identify(level, device, deviceHit, distant).isEmpty(), "SCC discovery bypassed antenna coverage");
    }
}
