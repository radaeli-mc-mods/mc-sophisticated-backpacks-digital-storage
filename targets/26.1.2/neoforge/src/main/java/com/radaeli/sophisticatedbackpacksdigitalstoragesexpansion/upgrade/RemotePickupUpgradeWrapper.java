package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;

/** Universal pickup filter and routing settings; Sophisticated retains control of the pickup event. */
public final class RemotePickupUpgradeWrapper extends PriorityLinkedStorageUpgradeWrapper<RemotePickupUpgradeWrapper,
        RemotePickupUpgradeItem> implements IContentsFilteredUpgrade, IPickupResponseUpgrade,
        net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade {
    public static final int FILTER_SLOT_COUNT = 9;

    private final ContentsFilterLogic filterLogic;
    private com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.TransactionalItemBuffer buffer;

    void markUncertainPickup() { PickupReservation.markUncertain(upgrade); save(); }

    com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.TransactionalItemBuffer buffer(net.minecraft.server.level.ServerPlayer player) {
        var registries = player.registryAccess();
        if (buffer == null) buffer = new com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.TransactionalItemBuffer(
                PickupReservation.read(upgrade, registries), pending -> {
                    PickupReservation.write(upgrade, pending, registries);
                    save();
                });
        return buffer;
    }

    public RemotePickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade,
            Consumer<ItemStack> saveHandler) {
        super(storageWrapper, upgrade, saveHandler);
        filterLogic = new ContentsFilterLogic(upgrade, saveHandler, FILTER_SLOT_COUNT,
                storageWrapper::getInventoryHandler,
                (MemorySettingsCategory) storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class),
                ModCoreDataComponents.FILTER_ATTRIBUTES);
    }

    @Override
    public ContentsFilterLogic getFilterLogic() {
        return filterLogic;
    }

    @Override
    public int pickup(Level level, net.neoforged.neoforge.transfer.item.ItemResource resource, int amount,
            net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
        if (level.isClientSide() || resource.isEmpty() || amount <= 0 || !isEnabled()) {
            return 0;
        }
        return RemotePickupContext.current()
                .filter(RemotePickupContext.Context::isValid)
                .filter(context -> context.player() instanceof net.minecraft.server.level.ServerPlayer)
                .map(context -> PickupService.pickupTransactional(this, (net.minecraft.server.level.ServerPlayer) context.player(), resource, amount, transaction))
                .orElse(0);
    }

    @Override public void tick(net.minecraft.world.entity.Entity entity, Level level, net.minecraft.core.BlockPos pos) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer player && !level.isClientSide()
                && player.isAlive() && !player.isSpectator()
                && getStorageWrapper().getUpgradeHandler().getSlotWrappers().containsValue(this)
                && PickupReservation.hasPending(upgrade) && !PickupReservation.uncertain(upgrade)) PickupService.recoverCommitted(this, player);
    }
}
