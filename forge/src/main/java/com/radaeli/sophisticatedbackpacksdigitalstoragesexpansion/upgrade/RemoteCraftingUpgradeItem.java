package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

public final class RemoteCraftingUpgradeItem extends CraftingUpgradeItem implements ManuallyLinkedUpgrade {
    private static final UpgradeType<CraftingUpgradeWrapper> TYPE = new UpgradeType<>(RemoteCraftingUpgradeWrapper::new);

    public RemoteCraftingUpgradeItem() {
        super(LinkedStorageUpgradeItem.countLimit(1));
    }

    @Override
    public UpgradeType<CraftingUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        Component error = Component.translatable("message.sophisticated_backpacks_digital_storages_expansion.crafting_conflict");
        return List.of(new UpgradeConflictDefinition(CraftingUpgradeItem.class::isInstance, 0, error, error));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        List<Component> description = new ArrayList<>();
        super.appendHoverText(stack, level, description, flag);
        ManuallyLinkedUpgrade.appendLinkTooltip(stack, description, tooltip, flag, ManuallyLinkedUpgrade.linkStatus(stack));
    }
}
