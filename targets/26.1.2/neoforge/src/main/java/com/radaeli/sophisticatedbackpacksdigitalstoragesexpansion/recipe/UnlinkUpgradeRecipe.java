package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.EndpointBinding;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.ManuallyLinkedUpgrade;

/** Removes only the endpoint; filter settings and all other components survive crafting. */
public final class UnlinkUpgradeRecipe extends CustomRecipe {
    public UnlinkUpgradeRecipe() {
        super();
    }

    private ItemStack ingredient(CraftingInput input) {
        ItemStack found = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
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
    public boolean matches(CraftingInput input, Level level) {
        return !ingredient(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack source = ingredient(input);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copyWithCount(1);
        EndpointBinding.clear(result);
        return result;
    }

    @Override
    public RecipeSerializer<UnlinkUpgradeRecipe> getSerializer() {
        return ModRecipes.UNLINK_UPGRADE.get();
    }
}
