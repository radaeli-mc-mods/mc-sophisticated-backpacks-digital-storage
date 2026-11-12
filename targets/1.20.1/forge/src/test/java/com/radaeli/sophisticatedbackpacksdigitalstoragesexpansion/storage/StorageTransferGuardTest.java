package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler;
import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.junit.jupiter.api.Test;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteDepositUpgradeWrapper;

import static org.junit.jupiter.api.Assertions.*;

class StorageTransferGuardTest extends MinecraftStorageTest {
    @Test
    void nativeIoBlocksSelfInsertionDuringDepositAndRecoversAfterwards() {
        Fixture fixture = new Fixture(UUID.randomUUID());
        ItemStackHandler source = new ItemStackHandler(1);
        source.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        FilteredItemHandler<ItemStackHandler> externalIo = fixture.io(source, List.of());
        ItemInsertion sameNetwork = new ItemInsertion() {
            public int simulateInsert(ItemStack stack) {
                return stack.getCount() - externalIo.insertItem(0, stack, true).getCount();
            }
            public ItemStack insert(ItemStack stack) {
                return externalIo.insertItem(0, stack, false);
            }
        };
        try (var ignored = StorageTransferGuard.blockExternalInsertion(fixture.storage)) {
            var result = ItemTransferService.deposit(source, sameNetwork, stack -> true, stack -> fail("Unexpected remainder"));
            assertFalse(result.movedAnything());
            assertEquals(8, source.getStackInSlot(0).getCount());
        }
        assertTrue(externalIo.insertItem(0, new ItemStack(Items.STONE), false).isEmpty());
        assertEquals(9, source.getStackInSlot(0).getCount());
    }

    @Test
    void guardPreservesNativeAllowAndBlockCompositionOutsideTransfers() {
        List<List<FilterLogic>> combinations = List.of(
                List.of(),
                List.of(filter(true, Items.STONE)),
                List.of(filter(true, Items.STONE), filter(true, Items.DIRT)),
                List.of(filter(false, Items.STONE)),
                List.of(filter(false, Items.STONE), filter(true, Items.DIRT)));
        for (List<FilterLogic> filters : combinations) {
            Fixture fixture = new Fixture(UUID.randomUUID());
            FilteredItemHandler<ItemStackHandler> guarded = fixture.io(new ItemStackHandler(1), filters);
            FilteredItemHandler<ItemStackHandler> baseline = new FilteredItemHandler<>(new ItemStackHandler(1), filters, List.of());
            for (Item item : List.of(Items.STONE, Items.DIRT, Items.DIAMOND)) {
                ItemStack stack = new ItemStack(item, 8);
                assertEquals(baseline.insertItem(0, stack, true).getCount(), guarded.insertItem(0, stack, true).getCount());
                try (var ignored = StorageTransferGuard.blockExternalInsertion(fixture.storage)) {
                    assertEquals(8, guarded.insertItem(0, stack, true).getCount());
                    assertEquals(8, guarded.insertItem(0, stack, false).getCount());
                }
                assertEquals(baseline.insertItem(0, stack, true).getCount(), guarded.insertItem(0, stack, true).getCount());
            }
        }
    }

    @Test
    void scopesAreNestedAndSharedAcrossWrappersWithTheSameContentsId() {
        UUID id = UUID.randomUUID();
        Fixture first = new Fixture(id);
        Fixture linked = new Fixture(id);
        Fixture unrelated = new Fixture(UUID.randomUUID());
        try (var outer = StorageTransferGuard.blockExternalInsertion(first.storage)) {
            assertTrue(StorageTransferGuard.isExternalInsertionBlocked(linked.storage));
            assertFalse(StorageTransferGuard.isExternalInsertionBlocked(unrelated.storage));
            try (var inner = StorageTransferGuard.blockExternalInsertion(linked.storage)) {
                assertTrue(StorageTransferGuard.isExternalInsertionBlocked(first.storage));
            }
            assertTrue(StorageTransferGuard.isExternalInsertionBlocked(first.storage));
        }
        assertFalse(StorageTransferGuard.isExternalInsertionBlocked(linked.storage));
    }

    @Test
    void anExceptionDoesNotLeaveExternalInsertionBlocked() {
        Fixture fixture = new Fixture(UUID.randomUUID());
        assertThrows(IllegalStateException.class, () -> {
            try (var ignored = StorageTransferGuard.blockExternalInsertion(fixture.storage)) {
                throw new IllegalStateException("Aborted operation");
            }
        });
        assertFalse(StorageTransferGuard.isExternalInsertionBlocked(fixture.storage));
    }

    @Test
    void multipleLinkedUpgradesDoNotBypassAnExistingAllowList() {
        Fixture fixture = new Fixture(UUID.randomUUID());
        fixture.providers.add(new RemoteDepositUpgradeWrapper(fixture.storage,
                new ItemStack(ModItems.REMOTE_DEPOSIT_UPGRADE.get()), ignored -> {}));
        FilteredItemHandler<ItemStackHandler> handler = fixture.io(new ItemStackHandler(1), List.of(filter(true, Items.STONE)));
        assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), true).isEmpty());
        assertEquals(1, handler.insertItem(0, new ItemStack(Items.DIRT), true).getCount());
    }

    private static FilterLogic filter(boolean allowList, Item item) {
        return new FilterLogic(new ItemStack(Items.STICK), ignored -> {}, 0) {
            public boolean isAllowList() { return allowList; }
            public boolean matchesFilter(ItemStack stack) { return stack.is(item) == allowList; }
        };
    }

    private static final class Fixture {
        private final List<IIOFilterUpgrade> providers = new ArrayList<>();
        private final IStorageWrapper storage;

        private Fixture(UUID id) {
            AtomicReference<UpgradeHandler> handler = new AtomicReference<>();
            storage = (IStorageWrapper) Proxy.newProxyInstance(IStorageWrapper.class.getClassLoader(),
                    new Class<?>[]{IStorageWrapper.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getContentsUuid" -> Optional.of(id);
                        case "getUpgradeHandler" -> handler.get();
                        case "getRenderInfo" -> renderInfo();
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "Test storage " + id;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            handler.set(new UpgradeHandler(0, storage, new CompoundTag(), () -> {}, () -> {}) {
                @Override
                public <T> List<T> getWrappersThatImplement(Class<T> type) {
                    return providers.stream().filter(type::isInstance).map(type::cast).toList();
                }
            });
            providers.add(new RemoteDepositUpgradeWrapper(storage,
                    new ItemStack(ModItems.REMOTE_DEPOSIT_UPGRADE.get()), ignored -> {}));
        }

        private static RenderInfo renderInfo() {
            return new RenderInfo(() -> () -> {}) {
                @Override protected void serializeRenderInfo(CompoundTag tag) {}
                @Override protected Optional<CompoundTag> getRenderInfoTag() { return Optional.empty(); }
            };
        }

        private FilteredItemHandler<ItemStackHandler> io(ItemStackHandler source, List<FilterLogic> nativeFilters) {
            nativeFilters.forEach(filter -> providers.add(new IIOFilterUpgrade() {
                public Optional<FilterLogic> getInputFilter() { return Optional.of(filter); }
                public Optional<FilterLogic> getOutputFilter() { return Optional.empty(); }
            }));
            List<FilterLogic> allFilters = providers.stream().map(IIOFilterUpgrade::getInputFilter)
                    .flatMap(Optional::stream).toList();
            return new FilteredItemHandler<>(source, allFilters, List.of());
        }
    }
}
