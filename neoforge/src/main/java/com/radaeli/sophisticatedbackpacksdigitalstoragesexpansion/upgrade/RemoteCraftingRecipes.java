package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RemoteCraftingRecipes {
    private RemoteCraftingRecipes() {
    }

    public static boolean isSupported(CraftingRecipe recipe) {
        RecipeSerializer<?> serializer = recipe.getSerializer();
        return (serializer == RecipeSerializer.SHAPED_RECIPE || serializer == RecipeSerializer.SHAPELESS_RECIPE)
                && recipe.canCraftInDimensions(3, 3) && !recipe.getIngredients().isEmpty();
    }
}
