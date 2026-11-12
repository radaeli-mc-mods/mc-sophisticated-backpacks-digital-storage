package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER,
                    SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<UnlinkUpgradeRecipe>> UNLINK_UPGRADE =
            SERIALIZERS.register("unlink_upgrade", () -> new SimpleCraftingRecipeSerializer<>(UnlinkUpgradeRecipe::new));

    private ModRecipes() {
    }
}
