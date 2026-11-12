package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeGroup;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;


/** Common item behavior for manually linked upgrades. The Sophisticated upgrade tag is still required. */
public abstract class LinkedStorageUpgradeItem<W extends IUpgradeWrapper> extends UpgradeItemBase<W> implements ManuallyLinkedUpgrade {
    protected LinkedStorageUpgradeItem(int maxPerStorage) {
        super(countLimit(maxPerStorage));
    }

    static IUpgradeCountLimitConfig countLimit(int maxPerStorage) {
        return new IUpgradeCountLimitConfig() {
            @Override
            public int getMaxUpgradesPerStorage(String storageType, ResourceLocation upgradeId) {
                return maxPerStorage;
            }

            @Override
            public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup group) {
                return Integer.MAX_VALUE;
            }
        };
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level context, List<Component> tooltip, TooltipFlag flag) {
        // Preserve Sophisticated's translated description lines with the primary tooltip emphasis.
        List<Component> description = new ArrayList<>();
        super.appendHoverText(stack, context, description, flag);
        ManuallyLinkedUpgrade.appendLinkTooltip(stack, description, tooltip, flag, getLinkStatus(stack));
    }

    /** Recovery details belong to the status, before the conditional unlink instruction. */
    protected Component getLinkStatus(ItemStack stack) {
        return ManuallyLinkedUpgrade.linkStatus(stack);
    }
}
