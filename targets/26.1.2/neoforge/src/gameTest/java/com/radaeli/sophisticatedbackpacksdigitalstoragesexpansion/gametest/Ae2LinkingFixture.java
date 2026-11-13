package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import appeng.api.parts.PartHelper;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
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

/** Exercises native multipart selection and the real linking event for all nine items. */
final class Ae2LinkingFixture {
    private static BlockPos powerPos(RealNetworks network) {
        var point = (WirelessAccessPointBlockEntity) network.helper().getLevel().getBlockEntity(network.endpoint());
        return network.endpoint().relative(point.getGridConnectableSides(point.getOrientation()).iterator().next());
    }

    static void prepare(RealNetworks network, ServerPlayer player) {
        var level = network.helper().getLevel();
        var power = powerPos(network);
        level.setBlockAndUpdate(power.west(), AEBlocks.CONTROLLER.block().defaultBlockState());
        PartHelper.setPart(level, power.west(2), null, player, AEParts.GLASS_CABLE.item(AEColor.TRANSPARENT));
        PartHelper.setPart(level, power.west(2), Direction.UP, player, AEParts.TERMINAL.get());
        level.setBlockAndUpdate(network.endpoint().above(3), AEBlocks.DRIVE.block().defaultBlockState());
    }

    static void check(RealNetworks network, ServerPlayer player) {
        var helper = network.helper();
        var level = helper.getLevel();
        var power = powerPos(network);
        var controller = power.west();
        var cable = power.west(2);
        var terminalHit = new BlockHitResult(Vec3.atLowerCornerOf(cable).add(0.5, 0.99, 0.5), Direction.UP, cable, false);
        var cableHit = new BlockHitResult(Vec3.atCenterOf(cable), Direction.SOUTH, cable, false);
        var source = SourceLocation.carriedBy(player);
        helper.assertTrue(StorageAdapterRegistry.identify(level, controller, hit(controller), source).equals(java.util.Optional.of(network.binding())),
                "Online controller did not discover its network's access point");
        helper.assertTrue(StorageAdapterRegistry.identify(level, power.east(), hit(power.east()), source).equals(java.util.Optional.of(network.binding())),
                "Online drive did not discover its network's access point");
        helper.assertTrue(StorageAdapterRegistry.identify(level, cable, terminalHit, source).equals(java.util.Optional.of(network.binding())),
                "Online multipart terminal did not discover its network's access point");
        helper.assertTrue(StorageAdapterRegistry.identify(level, cable, cableHit, source).isEmpty(), "Cable was accepted beside a terminal");
        helper.assertTrue(StorageAdapterRegistry.identify(level, network.endpoint().above(3), hit(network.endpoint().above(3)), source).isEmpty(),
                "Offline drive accepted a link");
        var distant = new SourceLocation(source.dimension(), source.position().add(2048, 0, 0));
        helper.assertTrue(StorageAdapterRegistry.identify(level, controller, hit(controller), distant).isEmpty(), "Discovery bypassed wireless coverage");

        player.setShiftKeyDown(true);
        for (String function : java.util.List.of("deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump", "crafting")) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("sophisticated_backpacks_digital_storages_expansion", "remote_" + function + "_upgrade"));
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, cable, terminalHit));
            helper.assertTrue(EndpointBinding.read(stack).equals(java.util.Optional.of(network.binding())), "Terminal event failed to bind " + function);
            EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, cable, cableHit));
            helper.assertTrue(EndpointBinding.read(stack).equals(java.util.Optional.of(network.binding())), "Cable click changed an existing link: " + function);
        }
        player.setShiftKeyDown(false);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
