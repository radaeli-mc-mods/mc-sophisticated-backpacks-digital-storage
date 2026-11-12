package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

final class RemoteCraftingUpgradeWrapper extends CraftingUpgradeWrapper implements IIOFilterUpgrade, LinkedUpgradeInventory {
    private final FilterLogic externalInputFilter;
    private final FilterLogic externalOutputFilter;
    private long lastFillTick = Long.MIN_VALUE;

    RemoteCraftingUpgradeWrapper(IStorageWrapper storage, ItemStack stack, Consumer<ItemStack> saveHandler) {
        super(storage, stack, saveHandler);
        externalInputFilter = new TransferInputFilter(storage, stack, saveHandler);
        externalOutputFilter = new TransferOutputFilter(storage, stack, saveHandler);
    }

    @Override
    public IStorageWrapper getStorageWrapper() {
        return storageWrapper;
    }

    @Override
    public Optional<StorageEndpoint> getEndpoint() {
        return EndpointBinding.read(upgrade);
    }

    @Override
    public Optional<FilterLogic> getInputFilter() {
        return Optional.of(externalInputFilter);
    }

    @Override
    public Optional<FilterLogic> getOutputFilter() {
        return Optional.of(externalOutputFilter);
    }

    SourcePriority getSourcePriority() {
        return PriorityLinkedStorageUpgradeWrapper.readSourcePriority(upgrade);
    }

    void setSourcePriority(SourcePriority priority) {
        PriorityLinkedStorageUpgradeWrapper.writeSourcePriority(upgrade, priority);
        save();
    }

    boolean beginFill(long tick) {
        if (lastFillTick == tick) {
            return false;
        }
        lastFillTick = tick;
        return true;
    }
}
