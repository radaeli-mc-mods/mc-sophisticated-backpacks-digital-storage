package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageText;

public interface ManuallyLinkedUpgrade {
    /** Upgrades with pending reservations can postpone a change of endpoint. */
    default boolean canChangeBinding(ItemStack stack) {
        return true;
    }

    static Component linkStatus(ItemStack stack) {
        return EndpointBinding.read(stack).map(StorageText::tooltipSummary)
                .orElseGet(() -> Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.not_linked")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    static void appendLinkTooltip(ItemStack stack, List<Component> description, List<Component> tooltip,
            TooltipFlag flag, Component status) {
        description.forEach(line -> tooltip.add(line.copy().withStyle(ChatFormatting.GRAY)));
        var endpoint = EndpointBinding.read(stack);
        tooltip.add(status);
        if (endpoint.isPresent()) {
            tooltip.add(Component.translatable("item.sophisticated_backpacks_digital_storages_expansion.linked_upgrade.unlink_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (flag.isAdvanced()) {
            endpoint.ifPresent(value -> tooltip.add(StorageText.endpoint("gui.sophisticated_backpacks_digital_storages_expansion.endpoint", value)
                    .copy().withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
