package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.gametest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.IPartState;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.network.PositionedAddonsNetworkIngredientsFilter;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.network.IIngredientChannelInsertPreConsumer;
import org.cyclops.integratedtunnels.part.PartTypes;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.*;

@PrefixGameTestTemplate(false)
public final class IntegratedResourceGameTests {
    private record Fixture(GameTestHelper helper, BlockPos omni, BlockPos itemPos, BlockPos fluidPos,
            BlockPos otherPos, IntegratedResourceFixture.Store items, IntegratedResourceFixture.Store fluids,
            IntegratedResourceFixture.Store other, ServerPlayer player) {
        StorageEndpoint endpoint() { return new StorageEndpoint("integrateddynamics", helper.getLevel().dimension(), omni); }
        StorageTarget target() { return StorageAdapterRegistry.connect(player, endpoint(), SourceLocation.carriedBy(player)).target().orElseThrow(); }
        FluidStorageTarget fluidTarget() { return FluidStorageAdapterRegistry.connect(player, endpoint(), SourceLocation.carriedBy(player), FluidStorageAccess.QUERY).target().orElseThrow(); }
        org.cyclops.integratedtunnels.api.network.IItemNetwork nativeItems() {
            return NetworkHelpers.getNetwork(helper.getLevel(), omni, null).orElseThrow().getCapability(org.cyclops.integratedtunnels.Capabilities.ItemNetwork.NETWORK).orElseThrow();
        }
        org.cyclops.integratedtunnels.api.network.IFluidNetwork nativeFluids() {
            return NetworkHelpers.getNetwork(helper.getLevel(), omni, null).orElseThrow().getCapability(org.cyclops.integratedtunnels.Capabilities.FluidNetwork.NETWORK).orElseThrow();
        }
        void run(Runnable action) { IntegratedAccessGameTests.whenReady(helper, omni, player, action); }
    }
    private static <P extends IPartType<P, S>, S extends IPartState<P>> void part(GameTestHelper h, BlockPos pos, P type, int channel) {
        S state = type.defaultBlockState();
        state.generateId();
        state.setChannel(channel);
        h.assertTrue(PartHelpers.addPart(h.getLevel(), pos, Direction.DOWN, type, state), "Native interface placement failed");
    }
    private static IntegratedResourceFixture.Store store(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos.below(), IntegratedResourceFixture.BLOCK.get().defaultBlockState());
        return (IntegratedResourceFixture.Store) h.getLevel().getBlockEntity(pos.below());
    }
    private static Fixture fixture(GameTestHelper h) {
        return fixture(h, store -> {});
    }
    private static Fixture fixture(GameTestHelper h, java.util.function.Consumer<IntegratedResourceFixture.Store> initialize) {
        BlockPos omni = IntegratedAccessGameTests.network(h, 1);
        BlockPos item = omni.east();
        BlockPos fluid = omni.east(2);
        BlockPos other = omni.south();
        for (BlockPos pos : List.of(fluid, other)) h.getLevel().setBlockAndUpdate(pos, org.cyclops.integrateddynamics.RegistryEntries.BLOCK_CABLE.get().defaultBlockState());
        var a = store(h, item); var b = store(h, fluid); var c = store(h, other);
        initialize.accept(a);
        part(h, item, PartTypes.INTERFACE_ITEM, 0);
        part(h, fluid, PartTypes.INTERFACE_FLUID, 0);
        part(h, other, PartTypes.INTERFACE_ITEM, 7);
        return new Fixture(h, omni, item, fluid, other, a, b, c, IntegratedAccessGameTests.player(h));
    }
    private static long amount(IntegratedResourceFixture.Store store) {
        long result = 0;
        for (int i = 0; i < store.items.getSlots(); i++) result += store.items.getStackInSlot(i).getCount();
        return result;
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void cachedbackpackguard(GameTestHelper h) {
        var bag = net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper.fromStack(
                new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.NETHERITE_BACKPACK.get()));
        bag.onInit(h.getLevel());
        bag.getUpgradeHandler().setStackInSlot(0, new ItemStack(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems.REMOTE_DEPOSIT_UPGRADE.get()));
        bag.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.EMERALD, 32));
        var f = fixture(h, store -> store.externalItems = bag.getInventoryForInputOutput());
        for (int i = 0; i < 4; i++) f.other.items.setStackInSlot(i, new ItemStack(Items.STONE, 64));
        f.run(() -> {
            var t = f.target();
            h.assertTrue(t.hasStoredMatching(f.player, stack -> stack.is(Items.EMERALD), Optional.of(Items.EMERALD)), "Backpack not exposed to its own network");
            try (var input = StorageTransferGuard.blockExternalInsertion(bag); var output = StorageTransferGuard.blockExternalExtraction(bag)) {
                h.assertTrue(t.simulateExtract(new ItemStack(Items.EMERALD), 1, f.player) == 0, "Cached backpack simulation bypassed guard");
                h.assertTrue(t.extract(new ItemStack(Items.EMERALD), 1, f.player).isEmpty(), "Cached backpack extraction bypassed guard");
                h.assertTrue(t.insert(new ItemStack(Items.EMERALD), f.player).getCount() == 1, "Cached backpack insertion bypassed guard");
                h.assertTrue(bag.getInventoryHandler().getStackInSlot(0).getCount() == 32, "Self-transfer changed backpack contents");
            }
            h.assertTrue(t.extract(new ItemStack(Items.EMERALD), 1, f.player).getCount() == 1, "Backpack guard did not release immediately");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void items(GameTestHelper h) {
        var f = fixture(h);
        ItemStack named = new ItemStack(Items.STONE, 120);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Exact components"));
        f.items.items.setStackInSlot(0, named);
        f.run(() -> {
            var t = f.target();
            h.assertTrue(t.getAvailableItemAmounts(f.player).stream().mapToLong(StoredItemAmount::amount).sum() == 120, "Initial native observation lost quantity");
            var snapshot = t.getAvailableItems(f.player);
            h.assertTrue(snapshot.extractItem(0, 1, false).isEmpty(), "Snapshot allowed extraction");
            snapshot.getStackInSlot(0).setCount(0);
            h.assertTrue(amount(f.items) == 120, "Snapshot mutated native contents");
            h.assertTrue(t.hasStoredMatching(f.player, stack -> ItemStack.isSameItemSameComponents(stack, named), Optional.of(Items.STONE)), "Presence lost components");
            h.assertTrue(t.simulateExtract(named, 20, f.player) == 20 && amount(f.items) == 120, "Extraction simulation mutated storage");
            h.assertTrue(t.extract(new ItemStack(Items.STONE), 20, f.player).isEmpty(), "Extraction ignored components");
            ItemStack extracted = t.extract(named, 20, f.player);
            h.assertTrue(extracted.getCount() == 20 && ItemStack.isSameItemSameComponents(extracted, named) && amount(f.items) == 100, "Wrong real extraction");
            h.assertTrue(t.simulateInsert(extracted, f.player) == 20 && amount(f.items) == 100, "Insertion simulation mutated storage");
            h.assertTrue(t.insert(extracted, f.player).isEmpty() && amount(f.items) + amount(f.other) == 120, "Round trip lost items");
            // Occupy every slot after a successful estimate; execute must report the real remainder.
            var offer = new ItemStack(Items.DIAMOND, 10);
            h.assertTrue(t.simulateInsert(offer, f.player) == 10, "Expected simulated capacity");
            for (var store : List.of(f.items, f.other)) for (int i = 0; i < 4; i++) store.items.setStackInSlot(i, new ItemStack(Items.STONE, 64));
            f.items.items.setStackInSlot(3, new ItemStack(Items.DIAMOND, 61));
            var rest = t.insert(offer, f.player);
            h.assertTrue(rest.getCount() == 7 && offer.getCount() == 10 && f.items.items.getStackInSlot(3).getCount() == 64, "Partial insert lost remainder or mutated input");
            h.getLevel().destroyBlock(f.omni, false);
            h.assertTrue(t.insert(new ItemStack(Items.DIAMOND), f.player).getCount() == 1 && t.getAvailableItems(f.player).getSlots() == 0, "Retained target bypassed endpoint removal");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void channelsandlongcounts(GameTestHelper h) {
        var f = fixture(h);
        f.items.items.setStackInSlot(0, new ItemStack(Items.STONE, Integer.MAX_VALUE));
        f.other.items.setStackInSlot(0, new ItemStack(Items.STONE, Integer.MAX_VALUE));
        f.run(() -> {
            var query = f.target().queryItems(f.player, stack -> stack.is(Items.STONE), Optional.of(Items.STONE), 1);
            h.assertTrue(query.size() == 1 && query.getFirst().amount() == 2L * Integer.MAX_VALUE, "Wildcard channels duplicated, truncated or overflowed counts");
            h.assertTrue(f.target().extract(new ItemStack(Items.STONE), Integer.MAX_VALUE, f.player).getCount() == 64, "Extraction exceeded a real stack");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void nativefilters(GameTestHelper h) {
        var f = fixture(h);
        f.items.items.setStackInSlot(0, new ItemStack(Items.STONE, 20));
        f.run(() -> {
            var t = f.target();
            h.assertTrue(t.hasStoredMatching(f.player, stack -> stack.is(Items.STONE), Optional.of(Items.STONE)), "Presence requires an extraction");
            var network = f.nativeItems();
            var pos = PartPos.of(h.getLevel(), f.itemPos.below(), Direction.UP);
            // The same native filter contract installed by filtering interfaces, applied independently.
            network.setPositionedStorageFilter(pos, new PositionedAddonsNetworkIngredientsFilter<>(stack -> false, false, true, true));
            h.assertTrue(t.extract(new ItemStack(Items.STONE), 1, f.player).isEmpty(), "Native extraction filter bypassed");
            h.assertTrue(t.insert(new ItemStack(Items.STONE, 1), f.player).isEmpty(), "Extraction refusal blocked insertion");
            network.setPositionedStorageFilter(pos, new PositionedAddonsNetworkIngredientsFilter<>(stack -> false, true, false, true));
            network.setPositionedStorageFilter(PartPos.of(h.getLevel(), f.otherPos.below(), Direction.UP), new PositionedAddonsNetworkIngredientsFilter<>(stack -> false, true, false, true));
            h.assertTrue(t.insert(new ItemStack(Items.STONE, 1), f.player).getCount() == 1, "Native insertion filter bypassed");
            h.assertTrue(t.extract(new ItemStack(Items.STONE), 1, f.player).getCount() == 1, "Insertion refusal blocked extraction");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void fluidsandcachedguard(GameTestHelper h) {
        var f = fixture(h);
        var water = new FluidStack(Fluids.WATER, 1500);
        water.set(DataComponents.CUSTOM_NAME, Component.literal("Exact fluid"));
        f.fluids.fluids.setFluid(water.copy());
        f.run(() -> {
            var t = f.fluidTarget(); var key = FluidStorageKey.of(water);
            h.assertTrue(t.getAvailableFluids(f.player).getFirst().amountMb() == 1500, "Initial fluid quantity lost");
            h.assertTrue(t.simulateExtract(key, 1000, f.player) == 1000 && f.fluids.fluids.getFluidAmount() == 1500, "Fluid simulation mutated tank");
            h.assertTrue(t.extract(FluidStorageKey.of(new FluidStack(Fluids.WATER, 1)), 1000, f.player).isEmpty(), "Fluid extraction ignored components");
            var bucket = t.extract(key, 1000, f.player);
            h.assertTrue(bucket.getAmount() == 1000 && FluidStorageKey.of(bucket).equals(key) && f.fluids.fluids.getFluidAmount() == 500, "1000 mB extraction incorrect");
            h.assertTrue(t.simulateInsert(bucket, f.player) == 1000 && f.fluids.fluids.getFluidAmount() == 500, "Fluid insert simulation mutated tank");
            h.assertTrue(t.insert(bucket, f.player).isEmpty() && f.fluids.fluids.getFluidAmount() == 1500, "1000 mB round trip incorrect");
            var overflow = key.toStack(1000);
            h.assertTrue(t.insert(overflow, f.player).getAmount() == 500 && overflow.getAmount() == 1000, "Partial fluid capacity lost remainder");
            // Native Tunnels has already cached the external handler before the guard opens.
            try (var ignored = FluidTransferGuard.blockExternalAccess(f.fluids.contentsId)) {
                h.assertTrue(t.extract(key, 1000, f.player).isEmpty(), "Cached fluid capability bypassed extraction guard");
                h.assertTrue(t.insert(key.toStack(1000), f.player).getAmount() == 1000, "Cached fluid capability bypassed insertion guard");
            }
            h.assertTrue(t.extract(key, 1000, f.player).getAmount() == 1000, "Guard did not release immediately");
            var pos = PartPos.of(h.getLevel(), f.fluidPos.below(), Direction.UP);
            f.nativeFluids().setPositionedStorageFilter(pos, new PositionedAddonsNetworkIngredientsFilter<>(stack -> false, true, false, true));
            h.assertTrue(t.insert(key.toStack(1), f.player).getAmount() == 1 && t.extract(key, 1, f.player).getAmount() == 1, "Fluid permissions were coupled");
            h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "digitalstorage_tests", timeoutTicks = 300)
    public static void nativeinterceptors(GameTestHelper h) {
        var f = fixture(h);
        f.run(() -> {
            var committed = new AtomicInteger();
            IIngredientChannelInsertPreConsumer<ItemStack> consumer = new IIngredientChannelInsertPreConsumer<>() {
                public Result<ItemStack> insert(int channel, ItemStack remaining, ItemStack unclaimed, boolean simulate) {
                    int consumed = Math.min(2, remaining.getCount());
                    if (!simulate) committed.addAndGet(consumed);
                    return new Result<>(remaining.copyWithCount(remaining.getCount() - consumed), unclaimed.copyWithCount(unclaimed.getCount() - consumed));
                }
            };
            var network = f.nativeItems(); network.registerInsertPreConsumer(consumer);
            var t = f.target(); var offer = new ItemStack(Items.DIAMOND, 5);
            try {
                h.assertTrue(t.simulateInsert(offer, f.player) == 5 && committed.get() == 0 && amount(f.items) == 0, "Simulation advanced native interceptor");
                h.assertTrue(t.insert(offer, f.player).isEmpty() && committed.get() == 2 && amount(f.items) + amount(f.other) == 3, "Insertion bypassed or double counted native interceptor");
            } finally { network.unregisterInsertPreConsumer(consumer); }
            IIngredientChannelInsertPreConsumer<ItemStack> uncertain = new IIngredientChannelInsertPreConsumer<>() {
                public Result<ItemStack> insert(int channel, ItemStack remaining, ItemStack unclaimed, boolean simulate) {
                    committed.incrementAndGet(); throw new IllegalStateException("Native result unknown");
                }
            };
            network.registerInsertPreConsumer(uncertain);
            try {
                try { t.insert(offer, f.player); throw new AssertionError("Remote failure was swallowed"); }
                catch (IllegalStateException expected) { h.assertTrue(committed.get() == 3, "Uncertain insertion was retried"); }
            } finally { network.unregisterInsertPreConsumer(uncertain); }
            h.succeed();
        });
    }
}
