package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

/** Dedicated creative inventory group for this mod's Sophisticated upgrades. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UPGRADES = TABS.register("upgrades", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + ".upgrades"))
                    .icon(() -> new ItemStack(ModItems.REMOTE_DEPOSIT_UPGRADE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.REMOTE_DEPOSIT_UPGRADE.get());
                        output.accept(ModItems.REMOTE_RESTOCK_UPGRADE.get());
                        output.accept(ModItems.REMOTE_PICKUP_UPGRADE.get());
                        output.accept(ModItems.REMOTE_MAGNET_UPGRADE.get());
                        output.accept(ModItems.REMOTE_REFILL_UPGRADE.get());
                        output.accept(ModItems.REMOTE_FEEDING_UPGRADE.get());
                        output.accept(ModItems.REMOTE_ALCHEMY_UPGRADE.get());
                        output.accept(ModItems.REMOTE_PUMP_UPGRADE.get());
                        output.accept(ModItems.REMOTE_CRAFTING_UPGRADE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
