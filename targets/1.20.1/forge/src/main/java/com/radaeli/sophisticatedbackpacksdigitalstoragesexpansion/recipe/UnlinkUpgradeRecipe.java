package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.ManuallyLinkedUpgrade;

/** Removes only the endpoint; filter settings and all other components survive crafting. */
public final class UnlinkUpgradeRecipe extends CustomRecipe {
    public UnlinkUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private ItemStack ingredient(CraftingContainer input) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!found.isEmpty() || !(stack.getItem() instanceof ManuallyLinkedUpgrade item)
                    || !EndpointBinding.hasBinding(stack) || !item.canChangeBinding(stack)) {
                return ItemStack.EMPTY;
            }
            found = stack;
        }
        return found;
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return !ingredient(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        ItemStack source = ingredient(input);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copyWithCount(1);
        EndpointBinding.clear(result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width > 0 && height > 0;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.UNLINK_UPGRADE.get();
    }
}
