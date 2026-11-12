package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.*;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;

@Mod("digitalstorage_tests")
public final class UpgradeNetworkGameTests {
    private static final String ID = "digitalstorage_tests";
    private static final List<String> BACKENDS = List.of("ae2", "refinedstorage", "toms_storage", "scc");
    private static final List<String> UPGRADES = List.of("deposit", "restock", "pickup", "magnet", "refill", "feeding", "alchemy", "pump", "crafting", "access_contract");
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, ID);
    private final Map<String, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> tests = new LinkedHashMap<>();

    public UpgradeNetworkGameTests(IEventBus bus) {
        tests.put("profile", FUNCTIONS.register("profile", () -> this::profile));
        if (!System.getProperty("digitalstorage.restartPhase", "").isEmpty()) {
            for (String backend : expectedBackends()) {
                String name = "restart_" + backend;
                tests.put(name, FUNCTIONS.register(name, () -> helper -> RestartNetworkGameTests.run(helper, backend)));
            }
            FUNCTIONS.register(bus);
            bus.addListener(this::registerTests);
            return;
        }
        for (String backend : expectedBackends()) for (String upgrade : UPGRADES) {
            String name = backend + "_" + upgrade;
            tests.put(name, FUNCTIONS.register(name, () -> helper -> run(helper, backend, upgrade, false)));
            if (List.of("deposit", "restock", "crafting").contains(upgrade)) {
                String placed = name + "_placed";
                tests.put(placed, FUNCTIONS.register(placed, () -> helper -> run(helper, backend, upgrade, false, true)));
            }
            if (!List.of("deposit", "restock", "access_contract").contains(upgrade)) {
                String alternate = name + "_backpack_first";
                tests.put(alternate, FUNCTIONS.register(alternate, () -> helper -> run(helper, backend, upgrade, true)));
            }
        }
        FUNCTIONS.register(bus);
        bus.addListener(this::registerTests);
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ID, name); }
    private static List<String> expectedBackends() {
        return switch (System.getProperty("digitalstorage.backendProfile", "none")) {
            case "none" -> List.of(); case "ae2" -> List.of("ae2"); case "rs" -> List.of("refinedstorage");
            case "toms" -> List.of("toms_storage"); case "scc" -> List.of("scc"); case "all" -> BACKENDS;
            default -> throw new IllegalArgumentException("Invalid backend profile");
        };
    }
    private void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("environment"));
        tests.forEach((name, function) -> event.registerTest(id(name), new FunctionGameTestInstance(
                function.getKey(), new TestData<>(environment, id("empty"), 800, 20, true))));
    }
    private void profile(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("sophisticatedcore") && ModList.get().isLoaded("sophisticatedbackpacks"), "Required Sophisticated mods were not loaded");
        helper.assertTrue(ModList.get().isLoaded("guideme") == expectedBackends().contains("ae2"), "Unexpected GuideME profile");
        for (String backend : BACKENDS) helper.assertTrue(ModList.get().isLoaded(backend) == expectedBackends().contains(backend), "Unexpected mod profile: " + backend);
        helper.assertTrue(ModItems.ITEMS.getEntries().size() == 9, "Expected all nine upgrades");
        helper.succeed();
    }
    private void run(GameTestHelper helper, String backend, String upgrade, boolean backpackFirst) {
        run(helper, backend, upgrade, backpackFirst, false);
    }
    private void run(GameTestHelper helper, String backend, String upgrade, boolean backpackFirst, boolean placed) {
        var level = helper.getLevel();
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "storage-test");
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);
        // A GameTest ServerPlayer subclass is explicitly supported by NeoForge's config-sync lifecycle.
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation()) {};
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1.5, 3.5)));
        RealNetworks network = RealNetworks.create(helper, backend, upgrade.equals("access_contract"));
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(network.ready(player), "Network not ready: " + backend + " / " + network.diagnostic(player)))
                .thenIdle(20).thenExecute(() -> exercise(helper, player, network, upgrade, backpackFirst, placed));
    }
    private void exercise(GameTestHelper helper, ServerPlayer player, RealNetworks network, String function, boolean backpackFirst, boolean placed) {
        if (function.equals("access_contract")) {
            checkAccessContract(helper, player, network);
            return;
        }
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(
                "sophisticated_backpacks_digital_storages_expansion", "remote_" + function + "_upgrade"));
        ItemStack bag = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK.get());
        player.getInventory().setItem(0, bag);
        var storage = BackpackWrapper.fromStack(bag);
        storage.onInit(player.level());
        storage.getUpgradeHandler().set(0, ItemResource.of(item), 1);
        BackpackContext context = new BackpackContext.Item("main", 0);
        if (placed) {
            BlockPos position = helper.absolutePos(new BlockPos(1, 1, 4));
            player.level().setBlockAndUpdate(position, net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks.NETHERITE_BACKPACK.get().defaultBlockState());
            var blockEntity = (net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity) player.level().getBlockEntity(position);
            blockEntity.setBackpack(bag);
            player.getInventory().setItem(0, ItemStack.EMPTY);
            context = new BackpackContext.Block(position);
        }
        var menu = new BackpackContainer(1, player, context);
        helper.assertTrue(menu.getBlockPosition().isPresent() == placed, "Menu did not retain the real backpack context");
        player.containerMenu = menu;
        menu.setOpenTabId(0);
        storage = (BackpackWrapper) menu.getStorageWrapper();
        var wrapper = storage.getUpgradeHandler().getSlotWrappers().get(0);
        if (wrapper instanceof LinkedStorageUpgradeWrapper<?, ?> linked) linked.setEndpoint(network.binding());
        else EndpointBinding.write(wrapper.getUpgradeStack(), network.binding());
        if (wrapper instanceof IFilteredUpgrade filtered) filtered.getFilterLogic().setAllowList(false);
        if (backpackFirst && wrapper instanceof PriorityLinkedStorageUpgradeWrapper<?, ?> priority) priority.setSourcePriority(SourcePriority.BACKPACK_FIRST);
        if (backpackFirst && menu.getUpgradeContainers().get(0) instanceof RemoteCraftingUpgradeContainer crafting) crafting.toggleSourcePriority();
        var target = network.target(player);
        var local = LocalItemAccess.of(storage.getInventoryForUpgradeProcessing());
        helper.assertTrue(StorageAdapterRegistry.identify(player.level(), network.endpoint()).map(value -> value.backendId().equals(network.backend())).orElse(false), "Endpoint identified as wrong backend");
        switch (function) {
            case "deposit" -> {
                local.insertItem(new ItemStack(Items.STONE, 7), false);
                CompoundTag packet = new CompoundTag(); packet.putString("remoteStorageAction", "deposit");
                menu.getUpgradeContainers().get(0).handlePacket(packet);
                helper.assertTrue(count(target, player, Items.STONE) == 7 && local.getStackInSlot(0).isEmpty(), "Deposit did not conserve items: " + network.diagnostic(player));
                helper.succeed();
            }
            case "restock" -> {
                helper.assertTrue(target.insert(new ItemStack(Items.STONE, 7), player).isEmpty(), "Seed insertion failed: " + network.diagnostic(player));
                local.insertItem(new ItemStack(Items.STONE), false);
                CompoundTag packet = new CompoundTag(); packet.putString("remoteStorageAction", "restock");
                menu.getUpgradeContainers().get(0).handlePacket(packet);
                helper.assertTrue(count(target, player, Items.STONE) == 0 && local.getStackInSlot(0).getCount() == 8, "Restock did not conserve items");
                helper.succeed();
            }
            case "pickup" -> {
                var drop = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.STONE, 7));
                drop.setNoPickUpDelay(); player.level().addFreshEntity(drop);
                for (boolean nested : List.of(false, true)) {
                    try (var pickupScope = RemotePickupContext.open(player, drop);
                            var root = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        if (nested) {
                            try (var child = net.neoforged.neoforge.transfer.transaction.Transaction.open(root)) {
                                helper.assertTrue(((RemotePickupUpgradeWrapper) wrapper).pickup(player.level(), ItemResource.of(Items.STONE), 7, child) == 7,
                                        "Nested pickup did not stage the offered amount");
                                child.commit();
                            }
                        } else helper.assertTrue(((RemotePickupUpgradeWrapper) wrapper).pickup(player.level(), ItemResource.of(Items.STONE), 7, root) == 7,
                                "Root pickup did not stage the offered amount");
                        helper.assertTrue(count(target, player, Items.STONE) == 0, "Remote write occurred before root commit");
                    }
                    helper.assertTrue(local.getStackInSlot(0).isEmpty() && count(target, player, Items.STONE) == 0
                            && drop.getItem().getCount() == 7, "Root abort did not restore pickup state");
                }
                NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, drop));
                helper.assertTrue(count(target, player, Items.STONE) == (backpackFirst ? 0 : 7) && drop.getItem().isEmpty()
                        && local.getStackInSlot(0).getCount() == (backpackFirst ? 7 : 0), "Native pickup did not deliver the real remainder");
                if (!network.backend().equals("scc")) {
                    target.insert(new ItemStack(Items.STONE, Integer.MAX_VALUE), player);
                    local.insertItem(new ItemStack(Items.STONE, Integer.MAX_VALUE), false);
                    helper.assertTrue(target.simulateInsert(new ItemStack(Items.STONE, 7), player) == 0
                            && local.insertItem(new ItemStack(Items.STONE, 7), true).getCount() == 7, "Fixture did not fill both real destinations");
                    var rejected = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.STONE, 7));
                    rejected.setNoPickUpDelay(); player.level().addFreshEntity(rejected);
                    NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Pre(player, rejected));
                    helper.assertTrue(rejected.getItem().getCount() == 7, "Full destinations consumed or refunded the original entity offer");
                    helper.assertTrue(((RemotePickupUpgradeItem) wrapper.getUpgradeStack().getItem()).canChangeBinding(wrapper.getUpgradeStack()),
                            "A rejected offer created a committed reservation");
                }
                helper.succeed();
            }
            case "magnet" -> {
                var drop = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX() + 1, player.getY(), player.getZ(), new ItemStack(Items.STONE, 7));
                drop.setNoPickUpDelay(); player.level().addFreshEntity(drop);
                var installed = (ITickableUpgrade) wrapper;
                helper.succeedWhen(() -> { installed.tick(player, player.level(), player.blockPosition()); helper.assertTrue(count(target, player, Items.STONE) == (backpackFirst ? 0 : 7) && !drop.isAlive()
                        && local.getStackInSlot(0).getCount() == (backpackFirst ? 7 : 0), "Magnet has not collected the drop"); });
            }
            case "refill" -> {
                local.insertItem(new ItemStack(Items.TORCH, 5), false);
                helper.assertTrue(target.insert(new ItemStack(Items.TORCH, 16), player).isEmpty(), "Seed insertion failed: " + network.diagnostic(player));
                ((RemoteRefillUpgradeWrapper) wrapper).getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.TORCH));
                var installed = (ITickableUpgrade) wrapper;
                helper.succeedWhen(() -> { installed.tick(player, player.level(), player.blockPosition()); helper.assertTrue(count(target, player, Items.TORCH) == 0 && player.getInventory().countItem(Items.TORCH) == 21, "Refill has not filled the carrier"); });
            }
            case "feeding" -> {
                local.insertItem(new ItemStack(Items.CARROT), false);
                target.insert(new ItemStack(Items.CARROT, 2), player); player.getFoodData().setFoodLevel(10);
                var installed = (ITickableUpgrade) wrapper;
                helper.succeedWhen(() -> { installed.tick(player, player.level(), player.blockPosition()); helper.assertTrue(player.getFoodData().getFoodLevel() > 10 && count(target, player, Items.CARROT) == (backpackFirst ? 2 : 1), "Feeding has not consumed remote food"); });
            }
            case "alchemy" -> {
                var potion = new ItemStack(Items.POTION, 2); potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.FIRE_RESISTANCE));
                target.insert(potion, player);
                local.insertItem(potion.copyWithCount(1), false);
                var alchemy = (RemoteAlchemyUpgradeWrapper) wrapper;
                alchemy.getFilterLogic().getFilterHandler().setStackInSlot(0, potion.copyWithCount(1));
                alchemy.setConditionValue(0, net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition.ALWAYS);
                helper.succeedWhen(() -> { alchemy.tick(player, player.level(), player.blockPosition()); helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE) && count(target, player, Items.POTION) == (backpackFirst ? 2 : 1), "Alchemy has not completed exactly one application"); });
            }
            case "pump" -> {
                player.getInventory().setSelectedSlot(1); player.getInventory().setItem(1, new ItemStack(Items.WATER_BUCKET));
                var pump = (RemotePumpUpgradeWrapper) wrapper;
                pump.setIsInput(true); pump.setInteractWithHand(true); pump.setInteractWithWorld(false); pump.setInteractWithFluidHandlers(false);
                if (network.backend().equals("toms_storage")) {
                    for (var access : FluidStorageAccess.values()) helper.assertTrue(FluidStorageAdapterRegistry.connect(player,
                            Optional.of(network.binding()), SourceLocation.carriedBy(player), access).status() == FluidStorageConnection.Status.UNSUPPORTED_FLUIDS,
                            "Tom's did not report its explicit fluid incompatibility");
                    pump.tick(player, player.level(), player.blockPosition());
                    helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "Tom's incompatible Pump changed the bucket");
                    pump.setIsInput(false); player.getInventory().setItem(1, new ItemStack(Items.BUCKET));
                    pump.tick(player, player.level(), player.blockPosition());
                    helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Tom's incompatible Pump created fluid");
                    helper.succeed();
                } else helper.startSequence().thenWaitUntil(() -> {
                    pump.tick(player, player.level(), player.blockPosition());
                    var fluid = FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()), SourceLocation.carriedBy(player), FluidStorageAccess.INSERT).target().orElseThrow();
                    helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && fluid.getAvailableFluids(player).stream().mapToLong(FluidStorageAmount::amountMb).sum() == 1000, "Pump has not conserved one bucket");
                }).thenExecute(() -> pump.setIsInput(false)).thenWaitUntil(() -> {
                    pump.tick(player, player.level(), player.blockPosition());
                    var fluid = FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()), SourceLocation.carriedBy(player), FluidStorageAccess.EXTRACT).target().orElseThrow();
                    helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && fluid.getAvailableFluids(player).stream().mapToLong(FluidStorageAmount::amountMb).sum() == 0, "Pump output lost or duplicated fluid");
                }).thenSucceed();
            }
            case "crafting" -> {
                target.insert(new ItemStack(Items.OAK_PLANKS, 4), player);
                local.insertItem(new ItemStack(Items.OAK_PLANKS, 2), false);
                var container = (RemoteCraftingUpgradeContainer) menu.getUpgradeContainers().get(0);
                CompoundTag packet = new CompoundTag(); packet.putInt("remoteCraftingMenu", menu.containerId); packet.putString("remoteCraftingRecipe", "minecraft:crafting_table");
                container.handlePacket(packet);
                int ingredients = 0; for (int slot = 0; slot < 9; slot++) ingredients += container.getCraftMatrix().getItem(slot).getCount();
                helper.assertTrue(ingredients == 4 && count(target, player, Items.OAK_PLANKS) == (backpackFirst ? 2 : 0), "Crafting did not preserve fetched ingredients");
                container.setRefillCraftingGrid(false);
                takeCraftingResult(helper, player, menu, container, Items.CRAFTING_TABLE);

                // Shapeless selection and a partial fill must not fabricate a result or consume inputs.
                helper.startSequence().thenIdle(1).thenExecute(() -> {
                target.insert(new ItemStack(Items.PAPER, 2), player);
                target.insert(new ItemStack(Items.LEATHER), player);
                packet.putString("remoteCraftingRecipe", "minecraft:book");
                container.handlePacket(packet);
                helper.assertTrue(container.getSlots().stream().filter(slot -> slot instanceof net.minecraft.world.inventory.ResultSlot)
                        .allMatch(slot -> slot.getItem().isEmpty()), "Partial recipe produced an output");
                }).thenIdle(1).thenExecute(() -> {
                target.insert(new ItemStack(Items.PAPER), player);
                container.handlePacket(packet);
                takeCraftingResult(helper, player, menu, container, Items.BOOK);

                // Native result extraction must return the three milk containers exactly once.
                }).thenIdle(1).thenExecute(() -> {
                target.insert(new ItemStack(Items.MILK_BUCKET, 3), player);
                target.insert(new ItemStack(Items.SUGAR, 2), player);
                target.insert(new ItemStack(Items.EGG), player);
                target.insert(new ItemStack(Items.WHEAT, 3), player);
                packet.putString("remoteCraftingRecipe", "minecraft:cake");
                container.handlePacket(packet);
                takeCraftingResult(helper, player, menu, container, Items.CAKE);
                int buckets = player.getInventory().countItem(Items.BUCKET);
                for (int slot = 0; slot < 9; slot++) if (container.getCraftMatrix().getItem(slot).is(Items.BUCKET)) buckets += container.getCraftMatrix().getItem(slot).getCount();
                helper.assertTrue(buckets == 3, "Crafting lost or duplicated milk containers");
                }).thenSucceed();
            }
        }
    }
    private static void takeCraftingResult(GameTestHelper helper, ServerPlayer player, BackpackContainer menu,
            RemoteCraftingUpgradeContainer container, Item expected) {
        var result = container.getSlots().stream().filter(slot -> slot instanceof net.minecraft.world.inventory.ResultSlot).findFirst().orElseThrow();
        helper.assertTrue(result.getItem().is(expected) && result.getItem().getCount() == 1, "Native crafting result unavailable: " + expected);
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(result.index, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(expected) && menu.getCarried().getCount() == 1, "Native result click failed");
        helper.assertTrue(result.getItem().isEmpty(), "Crafting consumed more than once or left a stale output");
    }

    private static void checkAccessContract(GameTestHelper helper, ServerPlayer player, RealNetworks network) {
        var target = network.target(player);
        var unloadedPosition = new BlockPos(29_000_000, 64, 29_000_000);
        helper.assertTrue(!player.level().isLoaded(unloadedPosition), "Unloaded fixture position was already loaded");
        var unloaded = new StorageEndpoint(network.backend(), player.level().dimension(), unloadedPosition);
        helper.assertTrue(StorageAdapterRegistry.connect(player, unloaded, SourceLocation.carriedBy(player), StorageOperation.EXTRACT).status()
                == StorageConnection.Status.ENDPOINT_UNLOADED && !player.level().isLoaded(unloadedPosition), "Endpoint lookup loaded a chunk");
        var missingDimension = net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, id("missing_dimension"));
        helper.assertTrue(StorageAdapterRegistry.connect(player, new StorageEndpoint(network.backend(), missingDimension, network.endpoint()),
                SourceLocation.carriedBy(player), StorageOperation.EXTRACT).status() == StorageConnection.Status.DIMENSION_UNAVAILABLE,
                "Missing dimension was accepted");
        if (!network.backend().equals("toms_storage")) {
            var otherDimension = new SourceLocation(net.minecraft.world.level.Level.NETHER, player.position());
            for (var operation : StorageOperation.values()) helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(), otherDimension, operation).target().isEmpty(),
                    "Cross-dimension item access ignored the native wireless setup");
            for (var access : FluidStorageAccess.values()) helper.assertTrue(FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()), otherDimension, access).target().isEmpty(),
                    "Cross-dimension fluid access ignored the native wireless setup");
        }
        var named = new ItemStack(Items.PAPER, 7);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("native-network-components"));
        helper.assertTrue(target.insert(named, player).isEmpty(), "Component insertion refused");
        helper.assertTrue(target.insert(new ItemStack(Items.PAPER, 3), player).isEmpty(), "Plain insertion refused");
        helper.assertTrue(target.getAvailableItemAmounts(player).stream().anyMatch(value ->
                ItemStack.isSameItemSameComponents(value.exemplar(), named) && value.amount() == 7), "Component identity lost in snapshot");
        var extracted = target.extract(named, 2, player);
        helper.assertTrue(extracted.getCount() == 2 && ItemStack.isSameItemSameComponents(named, extracted), "Exact extraction changed identity");
        helper.assertTrue(count(target, player, Items.PAPER) == 8, "Partial extraction lost or duplicated items");
        if (network.backend().equals("scc")) SccSecurityFixture.checkCompression(helper, network.endpoint(), player, target);
        if (!network.backend().equals("toms_storage")) {
            var fluid = FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()), SourceLocation.carriedBy(player), FluidStorageAccess.INSERT).target().orElseThrow();
            var tagged = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 750);
            tagged.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("native-fluid-components"));
            if (network.backend().equals("scc")) {
                helper.assertTrue(fluid.simulateInsert(tagged, player) == 0 && fluid.insert(tagged, player).getAmount() == 750,
                        "SCC accepted an identity its API cannot preserve");
            } else {
                helper.assertTrue(fluid.insert(tagged, player).isEmpty(), "Component fluid insertion refused");
                var actual = fluid.extract(FluidStorageKey.of(tagged), 300, player);
                helper.assertTrue(actual.getAmount() == 300 && net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(tagged, actual),
                        "Partial fluid extraction lost identity");
                helper.assertTrue(fluid.getAvailableFluids(player).stream().filter(value -> value.key().equals(FluidStorageKey.of(tagged)))
                        .mapToLong(FluidStorageAmount::amountMb).sum() == 450, "Fluid conservation failed");
            }
        }
        checkIndependentPermissions(helper, player, network);
        var original = player.position();
        player.setPos(original.add(2048, 0, 0));
        for (var operation : StorageOperation.values()) helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(),
                SourceLocation.carriedBy(player), operation).target().isEmpty(), "Distant access allowed: " + operation);
        player.setPos(original);
        Runnable removeEndpoint = () -> {
            helper.getLevel().removeBlock(network.endpoint(), false);
            for (var operation : StorageOperation.values()) helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(),
                    SourceLocation.carriedBy(player), operation).status() == StorageConnection.Status.ENDPOINT_MISSING,
                    "Removed endpoint accepted: " + operation);
        };
        if (network.backend().equals("toms_storage")) { removeEndpoint.run(); helper.succeed(); }
        else {
            helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(), SourceLocation.carriedBy(player), StorageOperation.EXTRACT).target().isPresent(),
                    "Power test started without extraction authorization");
            network.disconnectPower();
            helper.startSequence().thenIdle(20).thenWaitUntil(() -> {
                helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(), SourceLocation.carriedBy(player), StorageOperation.EXTRACT).target().isEmpty(),
                        "Unpowered network still accepted item access");
                helper.assertTrue(FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()), SourceLocation.carriedBy(player), FluidStorageAccess.QUERY).target().isEmpty(),
                        "Unpowered network still accepted fluid access");
            }).thenExecute(removeEndpoint).thenSucceed();
        }
    }

    private static void checkIndependentPermissions(GameTestHelper helper, ServerPlayer player, RealNetworks network) {
        SecurityFixture fixture;
        if (network.backend().equals("scc")) fixture = SccSecurityFixture.create(helper, network.endpoint(), player);
        else if (network.backend().equals("refinedstorage")) fixture = RsSecurityFixture.create(helper, network.endpoint());
        else return;
        try { for (boolean insertion : List.of(true, false)) {
            fixture.allowInsertion().accept(insertion);
            for (var operation : StorageOperation.values()) helper.assertTrue(StorageAdapterRegistry.connect(player, network.binding(),
                    SourceLocation.carriedBy(player), operation).target().isPresent() == (insertion == (operation == StorageOperation.INSERT)),
                    "Item permissions coupled: " + operation);
            for (var operation : FluidStorageAccess.values()) helper.assertTrue(FluidStorageAdapterRegistry.connect(player, Optional.of(network.binding()),
                    SourceLocation.carriedBy(player), operation).target().isPresent() == (operation == FluidStorageAccess.QUERY
                            ? !network.backend().equals("scc") || !insertion
                            : insertion == (operation == FluidStorageAccess.INSERT)),
                    "Fluid permissions coupled: " + operation);
        } } finally { fixture.restore().run(); }
    }

    private static long count(StorageTarget target, ServerPlayer player, Item item) {
        return target.getAvailableItemAmounts(player).stream().filter(value -> value.exemplar().is(item)).mapToLong(StoredItemAmount::amount).sum();
    }
}



