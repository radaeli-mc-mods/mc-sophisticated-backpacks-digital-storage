package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;

/** Native blocks, native topology and native storage; no substituted StorageTarget. */
record RealNetworks(GameTestHelper helper, String backend, BlockPos endpoint) {
    static RealNetworks create(GameTestHelper helper, String backend, boolean security) {
        return createAt(helper, backend, helper.absolutePos(new BlockPos(2, 1, 2)), security);
    }
    static RealNetworks createAt(GameTestHelper helper, String backend, BlockPos origin, boolean security) {
        var network = new RealNetworks(helper, backend, origin);
        switch (backend) {
            case "ae2" -> {
                network.place(origin, "ae2:wireless_access_point");
                var point = (appeng.blockentity.networking.WirelessAccessPointBlockEntity) helper.getLevel().getBlockEntity(origin);
                var back = point.getGridConnectableSides(point.getOrientation()).iterator().next();
                network.place(origin.relative(back), "ae2:creative_energy_cell");
                // The drive cannot connect through its front face.
                BlockPos drivePos = origin.relative(back).east();
                network.place(drivePos, "ae2:drive");
                var drive = (appeng.blockentity.storage.DriveBlockEntity) helper.getLevel().getBlockEntity(drivePos);
                drive.getInternalInventory().setItemDirect(0, item("ae2:item_storage_cell_1k"));
                drive.getInternalInventory().setItemDirect(1, item("ae2:fluid_storage_cell_1k"));
            }
            case "refinedstorage" -> {
                network.place(origin, "refinedstorage:creative_controller");
                network.place(origin.east(), "refinedstorage:1k_storage_block");
                network.place(origin.south(), "refinedstorage:64b_fluid_storage_block");
                if (security) network.place(origin.above(), "refinedstorage:security_manager");
                network.place(origin.west(), "refinedstorage:wireless_transmitter");
                var transmitterPos = origin.west();
                helper.getLevel().setBlockAndUpdate(transmitterPos, ((com.refinedmods.refinedstorage.common.networking.WirelessTransmitterBlock) helper.getLevel().getBlockState(transmitterPos).getBlock())
                        .rotated(net.minecraft.core.Direction.EAST));
            }
            case "toms_storage" -> {
                network.place(origin.north(), "toms_storage:inventory_connector");
                network.place(origin.north(2), "minecraft:chest");
                network.place(origin, "toms_storage:storage_terminal");
            }
            case "scc" -> {
                network.place(origin, "scc:network_antenna");
                network.place(origin.east(), "scc:calculator_controller");
                network.place(origin.east(2), "scc:item_storage");
                network.place(origin.east().south(), "scc:fluid_storage");
                if (security) {
                    network.place(origin.east().north(), "scc:security_terminal");
                    ((de.scc.calculator.network.CalculatorControllerBlockEntity) helper.getLevel().getBlockEntity(origin.east()))
                            .applyCompress(Integer.toString(de.scc.calculator.network.AutoCompress.MAX));
                }
                network.power();
            }
            default -> throw new IllegalArgumentException(backend);
        }
        return network;
    }

    private void place(BlockPos pos, String id) {
        var key = Identifier.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalStateException("Missing fixture block " + id);
        helper.getLevel().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(key).defaultBlockState());
    }
    private static ItemStack item(String id) {
        var key = Identifier.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Missing fixture item " + id);
        return new ItemStack(BuiltInRegistries.ITEM.getValue(key));
    }
    void power() {
        if (backend.equals("scc")) {
            var controller = (de.scc.calculator.network.CalculatorControllerBlockEntity) helper.getLevel().getBlockEntity(endpoint.east());
            controller.acceptFromNetwork(Integer.MAX_VALUE, false);
        }
    }
    void disconnectPower() {
        switch (backend) {
            case "ae2" -> {
                var point = (appeng.blockentity.networking.WirelessAccessPointBlockEntity) helper.getLevel().getBlockEntity(endpoint);
                var back = point.getGridConnectableSides(point.getOrientation()).iterator().next();
                helper.getLevel().removeBlock(endpoint.relative(back), false);
            }
            case "refinedstorage" -> place(endpoint, "refinedstorage:controller");
            case "scc" -> helper.getLevel().removeBlock(endpoint.east(), false);
            default -> throw new IllegalStateException("This backend does not use network power");
        }
    }
    StorageEndpoint binding() { return new StorageEndpoint(backend, helper.getLevel().dimension(), endpoint); }
    StorageTarget target(ServerPlayer player) {
        power();
        return StorageAdapterRegistry.connect(player, binding(), SourceLocation.carriedBy(player),
                StorageOperation.INSERT).target().orElseThrow(() -> new IllegalStateException("Network unavailable: " + backend));
    }
    String diagnostic(ServerPlayer player) {
        var connection = StorageAdapterRegistry.connect(player, binding(), SourceLocation.carriedBy(player), StorageOperation.INSERT);
        if (backend.equals("toms_storage")) return connection.status() + "; connector=" + ((com.tom.storagemod.block.entity.InventoryConnectorBlockEntity) helper.getLevel().getBlockEntity(endpoint.north())).getUsage() + "; nodes=" + com.tom.storagemod.inventory.InventoryCableNetwork.getNetwork(helper.getLevel()).getNetworkNodes(endpoint);
        if (!backend.equals("refinedstorage")) return connection.status().toString();
        var controller = (com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity) helper.getLevel().getBlockEntity(endpoint);
        var network = controller.getNetworkForItem();
        return connection.status() + "; network=" + network + (network == null ? "" : "; validators=" + network.getComponent(com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent.class).getContainers(com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator.class).size() + "; simulated=" + connection.target().map(target -> target.simulateInsert(new ItemStack(net.minecraft.world.item.Items.STONE), player)).orElse(-1)) + "; transmitter=" + helper.getLevel().getBlockState(endpoint.west());
    }
    boolean ready(ServerPlayer player) {
        if (backend.equals("refinedstorage")) RsSecurityFixture.allowAll(helper, endpoint);
        if (backend.equals("toms_storage") && ((com.tom.storagemod.block.entity.InventoryConnectorBlockEntity) helper.getLevel().getBlockEntity(endpoint.north())).getUsage().all() == 0) return false;
        power();
        return StorageAdapterRegistry.connect(player, binding(), SourceLocation.carriedBy(player),
                StorageOperation.INSERT).target().filter(target -> target.simulateInsert(new ItemStack(net.minecraft.world.item.Items.STONE), player) == 1).isPresent();
    }
}



