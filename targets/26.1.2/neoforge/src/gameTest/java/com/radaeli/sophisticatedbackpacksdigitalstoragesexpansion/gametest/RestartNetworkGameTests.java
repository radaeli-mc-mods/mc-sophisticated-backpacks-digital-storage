package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

/** Uses vanilla chunk data, Sophisticated SavedData, and native backend persistence across JVMs. */
final class RestartNetworkGameTests {
    private static final List<String> BACKENDS = List.of("ae2", "refinedstorage", "toms_storage", "scc");
    static void run(GameTestHelper helper, String backend) {
        var level = helper.getLevel();
        BlockPos origin = new BlockPos(10_000 + BACKENDS.indexOf(backend) * 128, 80, 0);
        for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 1; x++) {
            for (int z = -1; z <= 1; z++) { level.setChunkForced(x, z, true); level.getChunk(x, z); }
        }
        boolean prepare = "prepare".equals(System.getProperty("digitalstorage.restartPhase"));
        RealNetworks network = prepare ? RealNetworks.createAt(helper, backend, origin, false) : new RealNetworks(helper, backend, origin);
        ServerPlayer player = player(helper, backend, origin);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(network.ready(player), "Restart network unavailable: " + backend))
                .thenIdle(20).thenExecute(() -> {
                    BlockPos chestPosition = origin.south(3);
                    var target = network.target(player);
                    if (prepare) {
                        helper.assertTrue(target.insert(reserved("uncertain", 5), player).isEmpty(), "Could not seed the delivered resource");
                        level.setBlockAndUpdate(chestPosition, Blocks.CHEST.defaultBlockState());
                        var chest = (ChestBlockEntity) level.getBlockEntity(chestPosition);
                        chest.setItem(0, backpack(helper, network.binding(), false));
                        chest.setItem(1, backpack(helper, network.binding(), true));
                        chest.setChanged();
                        helper.succeed();
                    } else {
                        helper.assertTrue(level.getBlockEntity(chestPosition) instanceof ChestBlockEntity, "The saved vanilla chest did not survive restart");
                        var chest = (ChestBlockEntity) level.getBlockEntity(chestPosition);
                        ItemStack knownBag = chest.removeItemNoUpdate(0), uncertainBag = chest.removeItemNoUpdate(1);
                        helper.assertTrue(!knownBag.isEmpty() && !uncertainBag.isEmpty(), "Backpack items did not survive restart");
                        player.getInventory().setItem(0, knownBag); player.getInventory().setItem(1, uncertainBag);
                        var known = pickup(knownBag, player); var uncertain = pickup(uncertainBag, player);
                        helper.assertTrue(known.getEndpoint().orElseThrow().equals(network.binding()) && uncertain.getEndpoint().orElseThrow().equals(network.binding()), "Saved endpoint changed on restart");
                        checkSavedReservation(helper, known, player, false); checkSavedReservation(helper, uncertain, player, true);
                        helper.assertTrue(count(target, player, reserved("known", 1)) == 0 && count(target, player, reserved("uncertain", 1)) == 5,
                                "Native backend contents did not survive the first server process");
                        helper.startSequence().thenWaitUntil(() -> {
                            known.tick(player, level, player.blockPosition()); uncertain.tick(player, level, player.blockPosition());
                            helper.assertTrue(count(target, player, reserved("known", 1)) == 7 && count(target, player, reserved("uncertain", 1)) == 5,
                                    "Restart recovery lost or duplicated native resources");
                        }).thenIdle(10).thenExecute(() -> {
                            known.tick(player, level, player.blockPosition()); uncertain.tick(player, level, player.blockPosition());
                            helper.assertTrue(((RemotePickupUpgradeItem) known.getUpgradeStack().getItem()).canChangeBinding(known.getUpgradeStack()), "Known recovery did not settle");
                            helper.assertTrue(!((RemotePickupUpgradeItem) uncertain.getUpgradeStack().getItem()).canChangeBinding(uncertain.getUpgradeStack()), "Unknown delivery lost its quarantine");
                            helper.assertTrue(count(target, player, reserved("known", 1)) == 7 && count(target, player, reserved("uncertain", 1)) == 5,
                                    "Recovery repeated after the first successful tick");
                        }).thenSucceed();
                    }
                });
    }
    private static ItemStack reserved(String state, int amount) {
        var stack = new ItemStack(Items.DIAMOND, amount);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("restart-" + state));
        return stack;
    }
    private static ItemStack backpack(GameTestHelper helper, StorageEndpoint endpoint, boolean uncertain) {
        var bag = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK.get());
        var storage = BackpackWrapper.fromStack(bag); storage.onInit(helper.getLevel());
        var upgrade = new ItemStack(ModItems.REMOTE_PICKUP_UPGRADE.get());
        EndpointBinding.write(upgrade, endpoint);
        var pending = reserved(uncertain ? "uncertain" : "known", uncertain ? 5 : 7);
        var encoded = ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess()), pending).getOrThrow();
        CustomData.update(DataComponents.CUSTOM_DATA, upgrade, tag -> {
            tag.put("remote_pickup_reservation", encoded);
            if (uncertain) tag.putBoolean("remote_pickup_uncertain", true);
        });
        storage.getUpgradeHandler().set(0, ItemResource.of(upgrade), 1);
        return bag;
    }
    private static RemotePickupUpgradeWrapper pickup(ItemStack bag, ServerPlayer player) {
        var storage = BackpackWrapper.fromStack(bag); storage.onInit(player.level());
        return (RemotePickupUpgradeWrapper) storage.getUpgradeHandler().getSlotWrappers().get(0);
    }
    private static void checkSavedReservation(GameTestHelper helper, RemotePickupUpgradeWrapper wrapper, ServerPlayer player, boolean uncertain) {
        var data = wrapper.getUpgradeStack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        var stack = ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, player.registryAccess()), data.get("remote_pickup_reservation")).getOrThrow();
        helper.assertTrue(ItemStack.matches(reserved(uncertain ? "uncertain" : "known", uncertain ? 5 : 7), stack), "Saved count or components changed on restart");
        helper.assertTrue(data.getBooleanOr("remote_pickup_uncertain", false) == uncertain, "In-flight phase did not survive restart");
    }
    private static long count(StorageTarget target, ServerPlayer player, ItemStack exemplar) {
        return target.getAvailableItemAmounts(player).stream().filter(value -> ItemStack.isSameItemSameComponents(value.exemplar(), exemplar)).mapToLong(StoredItemAmount::amount).sum();
    }
    private static ServerPlayer player(GameTestHelper helper, String backend, BlockPos origin) {
        var profile = new GameProfile(UUID.nameUUIDFromBytes(("restart-" + backend).getBytes(StandardCharsets.UTF_8)), "storage-restart");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, cookie.clientInformation()) {};
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setNoGravity(true);
        player.setPos(Vec3.atCenterOf(origin.south(4)));
        return player;
    }
}
