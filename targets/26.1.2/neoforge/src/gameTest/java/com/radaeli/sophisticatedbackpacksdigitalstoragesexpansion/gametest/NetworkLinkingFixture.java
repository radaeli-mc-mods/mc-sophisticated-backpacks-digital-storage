package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.EndpointLinkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Native RS/SCC discovery, cable rejection and the nine upgrade binding events. */
final class NetworkLinkingFixture {
    static boolean supports(String backend) { return backend.equals("refinedstorage") || backend.equals("scc"); }

    static void prepare(RealNetworks network) {
        String prefix = network.backend();
        place(network, network.endpoint().north(2), prefix + (prefix.equals("scc") ? ":terminal" : ":grid"));
        place(network, network.endpoint().north(), prefix + ":cable");
        place(network, network.endpoint().above(3), prefix + (prefix.equals("scc") ? ":terminal" : ":grid"));
        if (prefix.equals("scc")) SccLinkingPartsFixture.prepare(network);
    }

    static void check(RealNetworks network, ServerPlayer player) {
        var helper = network.helper();
        var level = helper.getLevel();
        var origin = SourceLocation.carriedBy(player);
        var device = network.endpoint().north(2);
        var cable = network.endpoint().north();
        helper.assertTrue(StorageAdapterRegistry.identify(level, device, hit(device), origin).equals(java.util.Optional.of(network.binding())),
                "Online terminal/grid did not discover the original endpoint: " + network.backend());
        helper.assertTrue(StorageAdapterRegistry.identify(level, network.endpoint().east(), hit(network.endpoint().east()), origin).equals(java.util.Optional.of(network.binding())),
                "Online controller/storage did not discover the original endpoint: " + network.backend());
        helper.assertTrue(StorageAdapterRegistry.identify(level, cable, hit(cable), origin).isEmpty(), "Cable accepted: " + network.backend());
        var offline = network.endpoint().above(3);
        helper.assertTrue(StorageAdapterRegistry.identify(level, offline, hit(offline), origin).isEmpty(), "Disconnected device accepted: " + network.backend());
        if (network.backend().equals("scc")) SccLinkingPartsFixture.check(network, player);
        player.setShiftKeyDown(true);
        for (String function : java.util.List.of("deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump", "crafting")) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sophisticated_backpacks_digital_storages_expansion", "remote_" + function + "_upgrade"));
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, device, hit(device)));
            helper.assertTrue(EndpointBinding.read(stack).equals(java.util.Optional.of(network.binding())), "Device event failed: " + network.backend() + "/" + function);
            EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, cable, hit(cable)));
            helper.assertTrue(EndpointBinding.read(stack).equals(java.util.Optional.of(network.binding())), "Cable changed saved endpoint: " + function);
        }
        player.setShiftKeyDown(false);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false);
    }
    private static void place(RealNetworks network, BlockPos pos, String id) {
        var key = Identifier.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalStateException("Missing linking fixture block: " + id);
        network.helper().getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(key).defaultBlockState());
    }
}
