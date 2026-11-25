package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.core.part.PartTypes;
import org.cyclops.integrateddynamics.core.helper.*;
import org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.EndpointLinkHandler;

/** Native fixtures are loaded only by the Integrated-enabled profile. */
@PrefixGameTestTemplate(false)
public final class IntegratedAccessGameTests {
    static BlockPos network(GameTestHelper h, int x) {
        BlockPos pos = h.absolutePos(new BlockPos(x, 2, 2));
        var level = h.getLevel();
        level.setBlockAndUpdate(pos, RegistryEntries.BLOCK_CABLE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), RegistryEntries.BLOCK_CABLE.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), RegistryEntries.BLOCK_ENERGY_BATTERY_CREATIVE.defaultBlockState());
        var state = new PartTypeConnectorOmniDirectional.State();
        state.generateId();
        state.setGroupId(org.cyclops.integrateddynamics.part.PartTypeConnectorOmniDirectional.generateGroupId());
        h.assertTrue(PartHelpers.addPart(level, pos, Direction.UP, PartTypes.CONNECTOR_OMNI, state), "Omni placement failed");
        var reader = PartTypes.INVENTORY_READER.constructDefaultState();
        reader.generateId();
        h.assertTrue(PartHelpers.addPart(level, pos.east(), Direction.UP, PartTypes.INVENTORY_READER, reader), "Reader placement failed");
        return pos;
    }
    private static BlockPos secondOmni(GameTestHelper h, BlockPos first) {
        BlockPos pos = first.east(2);
        h.getLevel().setBlockAndUpdate(pos, RegistryEntries.BLOCK_CABLE.defaultBlockState());
        var state = new PartTypeConnectorOmniDirectional.State();
        state.generateId();
        state.setGroupId(PartTypeConnectorOmniDirectional.generateGroupId());
        h.assertTrue(PartHelpers.addPart(h.getLevel(), pos, Direction.UP, PartTypes.CONNECTOR_OMNI, state), "Second Omni failed");
        return pos;
    }
    static ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "access-test");
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile) {};
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.REMOTE_DEPOSIT_UPGRADE.get()));
        return player;
    }
    private static void click(ServerPlayer player, BlockPos pos) {
        EndpointLinkHandler.onRightClickBlock(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND,
                pos, new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.UP, pos, false)));
    }
    static void whenReady(GameTestHelper h, BlockPos pos, ServerPlayer player, Runnable action) {
        h.startSequence().thenWaitUntil(() -> h.assertTrue(IntegratedDynamicsAccessPolicy.resolve(player,
                new StorageEndpoint("integrateddynamics", h.getLevel().dimension(), pos), SourceLocation.carriedBy(player)).isPresent(),
                "Native Omni infrastructure is not ready")).thenExecute(action);
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 200)
    public static void topology(GameTestHelper h) {
        var a = network(h, 1);
        var other = secondOmni(h, a);
        var b = network(h, 6);
        var pure = a.south();
        h.getLevel().setBlockAndUpdate(pure, RegistryEntries.BLOCK_CABLE.defaultBlockState());
        h.startSequence().thenWaitUntil(() -> h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), a.east()).isPresent()
                && StorageAdapterRegistry.identify(h.getLevel(), b.east()).isPresent(), "Native topologies are not ready")).thenExecute(() -> {
            h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), pure).isEmpty(), "Pure cable accepted");
            h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), a.east()).orElseThrow().pos().equals(a), "Reader crossed topology A");
            h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), b.east()).orElseThrow().pos().equals(b), "Reader crossed topology B");
            h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), b).orElseThrow().pos().equals(b), "Clicked Omni not preferred");
            h.assertTrue(StorageAdapterRegistry.identify(h.getLevel(), other).orElseThrow().pos().equals(other), "Clicked Omni lost precedence");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 200)
    public static void binding(GameTestHelper h) {
        var pos = network(h, 1);
        var player = player(h);
        whenReady(h, pos, player, () -> {
            click(player, pos.east());
            var stack = player.getMainHandItem();
            var endpoint = EndpointBinding.read(stack).orElseThrow();
            h.assertTrue(endpoint.pos().equals(pos) && endpoint.backendId().equals("integrateddynamics"), "Wrong persisted anchor");
            h.assertTrue(EndpointBinding.read(ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()))).orElseThrow().equals(endpoint), "Binding lost after serialization");
            click(player, pos.offset(0, 0, 3));
            h.assertTrue(EndpointBinding.read(stack).orElseThrow().equals(endpoint), "Invalid click replaced binding");
            h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isPresent(), "No terminal should be required");
            h.assertTrue(StorageAdapterRegistry.connect(player, endpoint, SourceLocation.carriedBy(player)).target().orElseThrow()
                    .simulateInsert(new ItemStack(Items.DIAMOND), player) == 0, "Network without interfaces must not accept resources");
            for (var item : ModItems.ITEMS.getEntries()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item.get()));
                click(player, pos);
                h.assertTrue(EndpointBinding.read(player.getMainHandItem()).orElseThrow().equals(endpoint), "Upgrade failed to bind: " + item.getId());
            }
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 200)
    public static void unavailable(GameTestHelper h) {
        var pos = network(h, 1);
        secondOmni(h, pos);
        var noPower = network(h, 6);
        h.getLevel().destroyBlock(noPower.below(), false);
        var player = player(h);
        whenReady(h, pos, player, () -> {
            click(player, pos);
            var stack = player.getMainHandItem();
            var endpoint = EndpointBinding.read(stack).orElseThrow();
            int multiplier = org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier;
            try {
                org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier = 1;
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isPresent(), "Powered native Omni rejected");
                var offline = new StorageEndpoint("integrateddynamics", h.getLevel().dimension(), noPower);
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, offline, SourceLocation.carriedBy(player)).target().isEmpty(), "Unpowered native Omni accepted");
                click(player, noPower);
                h.assertTrue(EndpointBinding.read(stack).orElseThrow().equals(endpoint), "Unpowered click replaced binding");
            } finally {
                org.cyclops.integrateddynamics.GeneralConfig.energyConsumptionMultiplier = multiplier;
            }
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isEmpty(), "Spectator accepted");
            click(player, pos);
            h.assertTrue(EndpointBinding.read(stack).orElseThrow().equals(endpoint), "Denied access replaced binding");
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            try {
                var state = PartHelpers.getPartContainerChecked(h.getLevel(), pos, null).getPartState(Direction.UP);
                state.setEnabled(false);
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isEmpty(), "Disabled Omni accepted");
                click(player, pos);
                h.assertTrue(EndpointBinding.read(stack).orElseThrow().equals(endpoint), "Disabled Omni replaced binding");
                state.setEnabled(true);
            } catch (org.cyclops.integrateddynamics.api.PartStateException failure) { throw new IllegalStateException(failure); }
            h.getLevel().destroyBlock(pos, false);
            h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isEmpty(), "Removed Omni accepted");
            h.assertTrue(EndpointBinding.read(stack).orElseThrow().equals(endpoint), "Removed Omni erased binding");
            var unloaded = new StorageEndpoint("integrateddynamics", h.getLevel().dimension(), new BlockPos(29000000, 70, 29000000));
            h.assertTrue(StorageAdapterRegistry.connectForBinding(player, unloaded, SourceLocation.carriedBy(player)).status() == StorageConnection.Status.ENDPOINT_UNLOADED, "Unloaded endpoint accepted");
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void omnimembership(GameTestHelper h) {
        var pos = network(h, 1);
        var player = player(h);
        whenReady(h, pos, player, () -> {
            var endpoint = new StorageEndpoint("integrateddynamics", h.getLevel().dimension(), pos);
            var host = PartHelpers.getPartContainerChecked(h.getLevel(), pos, null);
            var saved = host.serializeNBT();
            host.deserializeNBT(saved);
            try {
                var state = (PartTypeConnectorOmniDirectional.State) host.getPartState(Direction.UP);
                h.assertTrue(!state.isAddedToGroup(), "Fixture did not reproduce native transient membership reset");
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isPresent(),
                        "Deserialized loaded Omni was refused because of its transient flag");
                int group = state.getGroupId();
                state.setGroupId(PartTypeConnectorOmniDirectional.generateGroupId());
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isEmpty(),
                        "An Omni outside its exact loaded group was authorized");
                state.setGroupId(group);
                state.setEnabled(false);
                h.assertTrue(StorageAdapterRegistry.connectForBinding(player, endpoint, SourceLocation.carriedBy(player)).target().isEmpty(),
                        "Disabled deserialized Omni was authorized");
                state.setEnabled(true);
            } catch (org.cyclops.integrateddynamics.api.PartStateException failure) { throw new IllegalStateException(failure); }
            h.succeed();
        });
    }
}
