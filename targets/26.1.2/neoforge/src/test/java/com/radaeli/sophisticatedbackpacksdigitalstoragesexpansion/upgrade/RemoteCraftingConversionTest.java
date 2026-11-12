package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import org.junit.jupiter.api.Test;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteCraftingConversionTest {
    @Test
    void packagedRecipePreservesNativeCraftingComponents() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.MinecraftStorageTest.bootstrapMinecraft();
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        String path = "/data/" + SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + "/recipe/remote_crafting_upgrade.json";
        CraftingRecipe recipe;
        try (var input = getClass().getResourceAsStream(path);
                var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            recipe = (CraftingRecipe) Recipe.CODEC
                    .parse(RegistryOps.create(JsonOps.INSTANCE, registries), JsonParser.parseReader(reader)).getOrThrow();
        }
        var original = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_UPGRADE.get());
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Named workbench"));
        var wrapper = new CraftingUpgradeWrapper(null, original, ignored -> {});
        var namedPaper = new ItemStack(Items.PAPER, 3);
        namedPaper.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved ingredient"));
        wrapper.getInventory().set(7, net.neoforged.neoforge.transfer.item.ItemResource.of(namedPaper), namedPaper.getCount());
        wrapper.setShiftClickIntoStorage(false);
        wrapper.setRefillCraftingGridNBT(true);

        var input = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.DIAMOND), new ItemStack(Items.ENDER_PEARL),
                new ItemStack(Items.DIAMOND), original, new ItemStack(Items.DIAMOND),
                new ItemStack(Items.REDSTONE), ItemStack.EMPTY, new ItemStack(Items.REDSTONE)));
        assertTrue(recipe.matches(input, null));
        ItemStack result = recipe.assemble(input);
        assertTrue(result.is(ModItems.REMOTE_CRAFTING_UPGRADE.get()));
        assertEquals(original.getHoverName(), result.getHoverName());
        var converted = new RemoteCraftingUpgradeWrapper(null, result, ignored -> {});
        assertEquals(wrapper.shouldShiftClickIntoStorage(), converted.shouldShiftClickIntoStorage());
        assertEquals(wrapper.shouldRefillCraftingGridNBT(), converted.shouldRefillCraftingGridNBT());
        assertTrue(ItemStack.matches(namedPaper, converted.getInventory().getStackInSlot(7)));
    }
}
