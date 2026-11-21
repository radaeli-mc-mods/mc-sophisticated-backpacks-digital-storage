package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional;

/** Native Omni, creative battery and Tunnels interfaces; resources persist across JVMs. */
final class IntegratedNetworkFixture {
    static String diagnostic(GameTestHelper helper, BlockPos origin, String status) {
        var level = helper.getLevel();
        var network = org.cyclops.integrateddynamics.core.helper.NetworkHelpers.getNetwork(level, origin, null);
        var host = PartHelpers.getPartContainer(level, origin, null);
        String state = host.map(h -> {
            try {
                var part = (PartTypeConnectorOmniDirectional.State) h.getPartState(Direction.UP);
                return "enabled="+part.isEnabled()+",group="+part.getGroupId()+",added="+part.isAddedToGroup()+",members="+
                        PartTypeConnectorOmniDirectional.LOADED_GROUPS.getPositions(part.getGroupId());
            } catch (Exception failure) { return failure.toString(); }
        }).orElse("missing host");
        var battery = level.getBlockEntity(origin.below());
        return status+"; block="+level.getBlockState(origin)+"; "+state+"; energy="+
                (battery instanceof org.cyclops.integrateddynamics.blockentity.BlockEntityEnergyBattery b ? b.getEnergyStored() : "no battery")+
                "; network="+network.map(n -> "elements="+n.getElements()+",initialized="+n.isInitialized()+",crashed="+n.isCrashed()).orElse("missing");
    }
    static void create(GameTestHelper helper, BlockPos origin) {
        var level = helper.getLevel();
        for (var pos : java.util.List.of(origin, origin.east(), origin.east(2))) {
            level.setBlockAndUpdate(pos, RegistryEntries.BLOCK_CABLE.get().defaultBlockState());
        }
        level.setBlockAndUpdate(origin.below(), ((net.minecraft.world.level.block.Block)
                RegistryEntries.BLOCK_ENERGY_BATTERY_CREATIVE.get()).defaultBlockState());
        ((org.cyclops.integrateddynamics.blockentity.BlockEntityEnergyBattery) level.getBlockEntity(origin.below())).setEnergyStored(1_000_000);
        var omni = new PartTypeConnectorOmniDirectional.State();
        omni.generateId();
        omni.setGroupId(PartTypeConnectorOmniDirectional.generateGroupId());
        helper.assertTrue(PartHelpers.addPart(level, origin, Direction.UP,
                org.cyclops.integrateddynamics.core.part.PartTypes.CONNECTOR_OMNI, omni), "Omni placement failed");
        level.setBlockAndUpdate(origin.east().below(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(origin.east(2).below(), IntegratedResourceFixture.BLOCK.get().defaultBlockState());
        var item = org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM.defaultBlockState();
        item.generateId();
        helper.assertTrue(PartHelpers.addPart(level, origin.east(), Direction.DOWN,
                org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_ITEM, item), "Item interface placement failed");
        var fluid = org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID.defaultBlockState();
        fluid.generateId();
        helper.assertTrue(PartHelpers.addPart(level, origin.east(2), Direction.DOWN,
                org.cyclops.integratedtunnels.part.PartTypes.INTERFACE_FLUID, fluid), "Fluid interface placement failed");
    }
}
