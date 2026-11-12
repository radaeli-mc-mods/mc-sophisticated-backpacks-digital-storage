package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IIOFilterUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
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

    @Override
    public boolean extractFromStorageOrPlayer(Player player, ItemStack stack) {
        if (super.extractFromStorageOrPlayer(player, stack)) {
            return true;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()
                || !(serverPlayer.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
            return false;
        }

        Optional<RemoteCraftingUpgradeContainer> container = menu.getOpenContainer()
                .filter(RemoteCraftingUpgradeContainer.class::isInstance)
                .map(RemoteCraftingUpgradeContainer.class::cast)
                .filter(openContainer -> openContainer.getUpgradeWrapper() == this);
        if (container.isEmpty()) {
            return false;
        }

        return UpgradeOperationContext.fromCurrentMenu(serverPlayer, container.get()).map(context -> {
            Predicate<ItemStack> exactStack = candidate -> ItemStack.isSameItemSameComponents(stack, candidate);
            return UpgradeItemSources.find(this, serverPlayer, context.source(), getSourcePriority(), exactStack,
                            Optional.of(stack.getItem()), 1)
                    .stream()
                    .filter(UpgradeItemSources.SourceItem::remote)
                    .findFirst()
                    .map(source -> {
                        ItemStack extracted = UpgradeItemSources.extract(this, serverPlayer, context.source(), source, 1);
                        return !extracted.isEmpty() && ItemStack.isSameItemSameComponents(stack, extracted);
                    })
                    .orElse(false);
        }).orElse(false);
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
