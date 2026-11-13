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

/** Native Tom's devices, directed cable connections and all nine binding events. */
final class TomsLinkingFixture {
    static void prepare(RealNetworks network) {
        var connector = network.endpoint().north();
        place(network, connector.east(), "toms_storage:inventory_cable");
        place(network, connector.east(2), "toms_storage:inventory_cable_connector");
        var level = network.helper().getLevel();
        level.setBlockAndUpdate(connector.east(2), level.getBlockState(connector.east(2))
                .setValue(com.tom.storagemod.block.InventoryCableConnectorBlock.FACING, Direction.SOUTH));
        place(network, connector.east(2).south(), "minecraft:chest");
        place(network, connector.above(), "toms_storage:inventory_cable_framed");
        place(network, network.endpoint().above(3), "toms_storage:inventory_connector");
        // This terminal is adjacent, but its inventory face points north, not at the connector.
        place(network, connector.west(), "toms_storage:storage_terminal");
    }

    static void check(RealNetworks network, ServerPlayer player) {
        var helper = network.helper();
        var level = helper.getLevel();
        var connector = network.endpoint().north();
        var source = SourceLocation.carriedBy(player);
        var expected = java.util.Optional.of(network.binding());
        helper.assertTrue(StorageAdapterRegistry.identify(level, connector, hit(connector), source).equals(expected), "Tom's connector did not find its terminal");
        var device = connector.east(2);
        helper.assertTrue(StorageAdapterRegistry.identify(level, device, hit(device), source).equals(expected), "Tom's cable connector did not find its terminal");
        for (var cable : java.util.List.of(connector.east(), connector.above())) {
            helper.assertTrue(StorageAdapterRegistry.identify(level, cable, hit(cable), source).isEmpty(), "Tom's cable accepted as a binding target");
        }
        var offline = network.endpoint().above(3);
        helper.assertTrue(StorageAdapterRegistry.identify(level, offline, hit(offline), source).isEmpty(), "Disconnected Tom's connector accepted");
        player.setShiftKeyDown(true);
        for (String function : java.util.List.of("deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump", "crafting")) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sophisticated_backpacks_digital_storages_expansion", "remote_" + function + "_upgrade"));
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, device, hit(device)));
            helper.assertTrue(EndpointBinding.read(stack).equals(expected), "Tom's device event failed: " + function);
            for (var rejected : java.util.List.of(connector.east(), connector.above(), offline)) {
                EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, rejected, hit(rejected)));
                helper.assertTrue(EndpointBinding.read(stack).equals(expected), "Invalid click changed Tom's endpoint: " + function);
            }
        }
        player.setShiftKeyDown(false);
        var original = player.position();
        player.setPos(original.add(2048, 0, 0));
        helper.assertTrue(StorageAdapterRegistry.connectForBinding(player, network.binding(), SourceLocation.carriedBy(player)).target().isEmpty(),
                "Tom's discovery bypassed native access/range rules");
        player.setPos(original);
    }

    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    private static void place(RealNetworks network, BlockPos pos, String id) {
        var key = Identifier.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalStateException("Missing Tom's linking fixture: " + id);
        network.helper().getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(key).defaultBlockState());
    }
}
