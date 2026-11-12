package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RemoteCraftingRecipes {
    private RemoteCraftingRecipes() {
    }

    public static boolean isSupported(CraftingRecipe recipe) {
        return (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped
                && shaped.getWidth() <= 3 && shaped.getHeight() <= 3)
                || (recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe
                && !ingredients(recipe).isEmpty() && ingredients(recipe).size() <= 9);
    }

    public static java.util.List<net.minecraft.world.item.crafting.Ingredient> ingredients(CraftingRecipe recipe) {
        return recipe.placementInfo().ingredients();
    }
}
