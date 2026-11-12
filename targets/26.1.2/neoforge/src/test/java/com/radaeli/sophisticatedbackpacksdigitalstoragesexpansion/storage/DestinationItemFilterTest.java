package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.DestinationItemFilter;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRestockUpgradeWrapper;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.deposit.DepositFilterType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DestinationItemFilterTest extends MinecraftStorageTest {
    private ContentsFilterLogic filters() {
        return new ContentsFilterLogic(new ItemStack(Items.STICK), ignored -> {}, 9, () -> null, null,
                ModCoreDataComponents.FILTER_ATTRIBUTES);
    }
    private StorageTarget target(ItemStack... items) {
        return (StorageTarget) Proxy.newProxyInstance(StorageTarget.class.getClassLoader(), new Class<?>[]{StorageTarget.class},
            (proxy, method, args) -> {
                if (!method.getName().equals("hasStoredMatching")) throw new AssertionError(method.getName());
                @SuppressWarnings("unchecked") var matches = (java.util.function.Predicate<ItemStack>) args[1];
                @SuppressWarnings("unchecked") var item = (java.util.Optional<net.minecraft.world.item.Item>) args[2];
                return List.of(items).stream().anyMatch(stack -> item.map(stack::is).orElse(true) && matches.test(stack));
            });
    }
    @Test void remoteStorageUsesNetworkWithoutReadingLocalMemoryOrExtracting() {
        var f = filters(); f.setDepositFilterType(ContentsFilterType.STORAGE);
        assertTrue(DestinationItemFilter.remote(f, target(new ItemStack(Items.STONE)), null, new ItemStack(Items.STONE)));
        assertFalse(DestinationItemFilter.remote(f, target(new ItemStack(Items.DIRT)), null, new ItemStack(Items.STONE)));
        assertFalse(DestinationItemFilter.remote(f, null, null, new ItemStack(Items.STONE)));
    }
    @Test void modMatchingDoesNotRestrictQueryToOfferedItem() {
        var f = filters(); f.setDepositFilterType(ContentsFilterType.STORAGE); f.setPrimaryMatch(PrimaryMatch.MOD);
        assertTrue(DestinationItemFilter.remote(f, target(new ItemStack(Items.DIRT)), null, new ItemStack(Items.STONE)));
    }
    @Test void nativeComponentComparisonIsPreserved() {
        var f = filters(); f.setDepositFilterType(ContentsFilterType.STORAGE); f.setMatchComponents(true);
        ItemStack named = new ItemStack(Items.STONE); named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
        assertFalse(DestinationItemFilter.remote(f, target(named), null, new ItemStack(Items.STONE)));
        assertTrue(DestinationItemFilter.remote(f, target(named), null, named.copy()));
    }
    @Test void allowAndBlockDoNotQueryNetworkContents() {
        var f = filters(); f.getFilterHandler().setStackInSlot(0, new ItemStack(Items.STONE));
        StorageTarget inaccessible = target();
        f.setDepositFilterType(ContentsFilterType.ALLOW);
        assertTrue(DestinationItemFilter.remote(f, inaccessible, null, new ItemStack(Items.STONE)));
        f.setDepositFilterType(ContentsFilterType.BLOCK);
        assertFalse(DestinationItemFilter.remote(f, inaccessible, null, new ItemStack(Items.STONE)));
    }
    @Test void restockReconstructionPreservesEachSavedMode() {
        for (var mode : DepositFilterType.values()) {
            ItemStack upgrade = new ItemStack(ModItems.REMOTE_RESTOCK_UPGRADE.get());
            var first = new RemoteRestockUpgradeWrapper(null, upgrade, ignored -> {});
            assertEquals(DepositFilterType.INVENTORY, first.getFilterLogic().getDepositFilterType());
            first.getFilterLogic().setDepositFilterType(mode);
            var recreated = new RemoteRestockUpgradeWrapper(null, upgrade.copy(), ignored -> {});
            assertEquals(mode, recreated.getFilterLogic().getDepositFilterType());
        }
    }
    @Test void routingOffersOnlyRemainderAndHonorsEachDestinationsDecision() {
        for (var priority : com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority.values()) {
            for (boolean remoteAllows : new boolean[]{false, true}) for (boolean localAllows : new boolean[]{false, true}) {
                var offers = new java.util.ArrayList<String>();
                java.util.function.UnaryOperator<ItemStack> remote = stack -> {
                    offers.add("remote:" + stack.getCount());
                    return remoteAllows ? stack.copyWithCount(Math.max(0, stack.getCount() - 3)) : stack;
                };
                java.util.function.UnaryOperator<ItemStack> local = stack -> {
                    offers.add("local:" + stack.getCount());
                    return localAllows ? stack.copyWithCount(Math.max(0, stack.getCount() - 2)) : stack;
                };
                ItemStack input = new ItemStack(Items.STONE, 8);
                ItemStack remainder = DestinationItemFilter.route(priority, input, remote, local);
                assertEquals(8 - (remoteAllows ? 3 : 0) - (localAllows ? 2 : 0), remainder.getCount());
                boolean networkFirst = priority == com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority.NETWORK_FIRST;
                assertEquals((networkFirst ? "remote:" : "local:") + "8", offers.get(0));
                assertEquals((networkFirst ? "local:" : "remote:") + (8 - (networkFirst ? (remoteAllows ? 3 : 0) : (localAllows ? 2 : 0))), offers.get(1));
                assertEquals(8, input.getCount());
            }
        }
    }
}
