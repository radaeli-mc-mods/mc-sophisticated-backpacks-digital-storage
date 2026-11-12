package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Supplier;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;

import static org.junit.jupiter.api.Assertions.*;

public class PrioritySettingsTest {
    private static boolean loadedTestConfig;
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!Config.SERVER_SPEC.isLoaded()) {
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
            loadedTestConfig = true;
        }
    }

    @AfterAll
    static void unloadTestConfig() {
        if (loadedTestConfig) ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER);
    }

    @Test
    public void priorityClickUpdatesDisplayedStateBeforePacketAndSurvivesReconstruction() {
        ItemStack stack = new ItemStack(ModItems.REMOTE_FEEDING_UPGRADE.get());
        var wrapper = new RemoteFeedingUpgradeWrapper(null, stack, ignored -> {});
        var container = new RecordingContainer(wrapper);
        var serverWrapper = new RemoteFeedingUpgradeWrapper(null, stack.copy(), ignored -> {});
        var serverContainer = new RecordingContainer(serverWrapper);
        assertEquals(SourcePriority.NETWORK_FIRST, container.getSourcePriority());

        container.toggleSourcePriority();
        assertEquals(SourcePriority.BACKPACK_FIRST, container.getSourcePriority());
        assertTrue(container.packet.getBoolean("sourcePriority"));
        serverContainer.handleSettingsPacket(container.packet);
        assertEquals(SourcePriority.BACKPACK_FIRST, serverWrapper.getSourcePriority());
        var recreated = new RemoteFeedingUpgradeWrapper(null, stack.copy(), ignored -> {});
        assertEquals(SourcePriority.BACKPACK_FIRST, recreated.getSourcePriority());

        container.toggleSourcePriority();
        assertEquals(SourcePriority.NETWORK_FIRST, container.getSourcePriority());
        assertFalse(container.packet.getBoolean("sourcePriority"));
        serverContainer.handleSettingsPacket(container.packet);
        assertEquals(SourcePriority.NETWORK_FIRST, serverWrapper.getSourcePriority());
    }

    @Test
    public void feedingFilterUsesCurrentWrapperAfterSynchronizationAndPreservesListMode() {
        ItemStack stack = new ItemStack(ModItems.REMOTE_FEEDING_UPGRADE.get());
        var original = new RemoteFeedingUpgradeWrapper(null, stack, ignored -> {});
        original.getFilterLogic().getFilterHandler().setStackInSlot(0, new ItemStack(Items.APPLE));
        original.getFilterLogic().setAllowList(true);
        var container = new RemoteFeedingUpgradeContainer(null, 0, original, null);
        var synchronizedWrapper = new RemoteFeedingUpgradeWrapper(null, stack.copy(), ignored -> {});
        synchronizedWrapper.getFilterLogic().setAllowList(false);
        container.setUpgradeWrapper(synchronizedWrapper);
        assertFalse(container.getFilterLogicContainer().isAllowList());
        assertFalse(synchronizedWrapper.getFilterLogic().matchesFilter(new ItemStack(Items.APPLE)));
        assertTrue(synchronizedWrapper.getFilterLogic().matchesFilter(new ItemStack(Items.BREAD)));
        assertTrue(original.getFilterLogic().isAllowList());
        var recreated = new RemoteFeedingUpgradeWrapper(null, synchronizedWrapper.getUpgradeStack().copy(), ignored -> {});
        assertFalse(recreated.getFilterLogic().isAllowList());
        assertEquals(Items.APPLE, recreated.getFilterLogic().getFilterHandler().getStackInSlot(0).getItem());
    }

    @Test
    public void pickupAndMagnetBackpackFirstUseNativeInventoryBeforeNetworkInBothPhases() {
        for (boolean magnet : new boolean[]{false, true}) {
            for (ContentsFilterType mode : ContentsFilterType.values()) {
                var storage = BackpackWrapper.fromStack(new ItemStack(
                        net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get()));
                var wrapper = magnet
                        ? new RemoteMagnetUpgradeWrapper(storage, new ItemStack(ModItems.REMOTE_MAGNET_UPGRADE.get()), ignored -> {})
                        : new RemotePickupUpgradeWrapper(storage, new ItemStack(ModItems.REMOTE_PICKUP_UPGRADE.get()), ignored -> {});
                var filters = ((net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade) wrapper).getFilterLogic();
                filters.setDepositFilterType(mode);
                if (mode == ContentsFilterType.ALLOW) filters.getFilterHandler().setStackInSlot(0, new ItemStack(Items.STONE));
                storage.getInventoryHandler().setStackInSlot(0, new ItemStack(Items.STONE, 1));
                wrapper.setSourcePriority(SourcePriority.BACKPACK_FIRST);
                for (boolean simulate : new boolean[]{true, false}) {
                    ItemStack remainder = DestinationItemFilter.route(wrapper.getSourcePriority(), new ItemStack(Items.STONE, 8),
                            offered -> { fail("Network used before available backpack: " + magnet + "/" + mode + "/" + simulate); return offered; },
                            offered -> DestinationItemFilter.local(filters, offered)
                                    ? storage.getInventoryForUpgradeProcessing().insertItem(offered, simulate) : offered);
                    assertTrue(remainder.isEmpty());
                    assertEquals(simulate ? 1 : 9, storage.getInventoryHandler().getStackInSlot(0).getCount());
                }
            }
        }
    }

    @Test
    public void storedContentStillRequiresLocalItemOrMemoryWithBackpackFirst() {
        for (boolean magnet : new boolean[]{false, true}) {
            var storage = BackpackWrapper.fromStack(new ItemStack(
                    net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get()));
            var wrapper = magnet
                    ? new RemoteMagnetUpgradeWrapper(storage, new ItemStack(ModItems.REMOTE_MAGNET_UPGRADE.get()), ignored -> {})
                    : new RemotePickupUpgradeWrapper(storage, new ItemStack(ModItems.REMOTE_PICKUP_UPGRADE.get()), ignored -> {});
            var filters = ((net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade) wrapper).getFilterLogic();
            filters.setDepositFilterType(ContentsFilterType.STORAGE);
            wrapper.setSourcePriority(SourcePriority.BACKPACK_FIRST);
            for (boolean simulate : new boolean[]{true, false}) {
                int[] remoteOffers = {0};
                ItemStack remainder = DestinationItemFilter.route(wrapper.getSourcePriority(), new ItemStack(Items.STONE, 8),
                        offered -> { remoteOffers[0] += offered.getCount(); return ItemStack.EMPTY; },
                        offered -> DestinationItemFilter.local(filters, offered)
                                ? storage.getInventoryForUpgradeProcessing().insertItem(offered, simulate) : offered);
                assertTrue(remainder.isEmpty());
                assertEquals(8, remoteOffers[0]);
                assertTrue(storage.getInventoryHandler().getStackInSlot(0).isEmpty());
            }
        }
    }

    private static final class RecordingContainer extends
            PriorityLinkedStorageUpgradeContainer<RemoteFeedingUpgradeWrapper, RecordingContainer> {
        private CompoundTag packet;

        private RecordingContainer(RemoteFeedingUpgradeWrapper wrapper) {
            super(null, 0, wrapper, null);
        }

        @Override
        public void sendDataToServer(Supplier<CompoundTag> data) {
            packet = data.get();
        }

        @Override
        protected void handleAction(String action, UpgradeOperationContext context) {}
    }
}
