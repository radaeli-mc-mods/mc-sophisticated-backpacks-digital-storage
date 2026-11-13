package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.CraftingFillEngine;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.UpgradeItemSources.SourceItem;

/** Minecraft 26.1.2 bridge to the shared, Minecraft-independent crafting flow. */
final class RemoteCraftingFill {
    private static final int PLAYER_MAIN_INVENTORY_SIZE = 36;
    private static final int MAX_TRANSFER_ITEMS = CraftingFillEngine.GRID_SIZE * 64;

    private RemoteCraftingFill() {
    }

    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe) {
        return fill(context, wrapper, grid, recipe, false, false);
    }

    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe, boolean includePlayerInventory, boolean maxTransfer) {
        CraftingFillEngine.Recipe<ItemStack> model = model(recipe);
        List<CraftingFillEngine.Ingredient<ItemStack>> ingredients = model.ingredients().stream()
                .filter(java.util.Objects::nonNull).toList();
        CraftingFillEngine.Grid<ItemStack> gridAccess = new CraftingFillEngine.Grid<>() {
            @Override
            public ItemStack getItem(int slot) {
                return grid.getItem(slot);
            }

            @Override
            public int getMaxStackSize() {
                return grid.getMaxStackSize();
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return grid.canPlaceItem(slot, stack);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                grid.setItem(slot, stack);
            }

            @Override
            public void setChanged() {
                grid.setChanged();
            }

            @Override
            public boolean matchesRecipe() {
                return recipe.matches(CraftingInput.ofPositioned(3, 3, readGrid(grid)).input(), context.player().level());
            }
        };

        CraftingFillEngine.StackRules<ItemStack> stackRules = new CraftingFillEngine.StackRules<>() {
            @Override
            public boolean isEmpty(ItemStack stack) {
                return stack.isEmpty();
            }

            @Override
            public int count(ItemStack stack) {
                return stack.getCount();
            }

            @Override
            public int maxStackSize(ItemStack stack) {
                return stack.getMaxStackSize();
            }

            @Override
            public boolean sameItemAndComponents(ItemStack left, ItemStack right) {
                return ItemStack.isSameItemSameComponents(left, right);
            }

            @Override
            public ItemStack copyWithCount(ItemStack stack, int count) {
                return stack.copyWithCount(count);
            }
        };

        CraftingFillEngine.SourceProvider<ItemStack> sources = (eligible, includePlayer, batchTransfer) -> {
            List<CraftingFillEngine.Supply<ItemStack>> found = new ArrayList<>();
            for (SourceItem source : UpgradeItemSources.find(wrapper, context.player(), context.source(),
                    SourcePriority.NETWORK_FIRST, stack -> matchesAny(ingredients, stack), Optional.empty(),
                    batchTransfer ? MAX_TRANSFER_ITEMS : CraftingFillEngine.GRID_SIZE)) {
                found.add(new CraftingFillEngine.Supply<>(source.stack().copyWithCount(1), source.amount(),
                        source.remote(), amount -> UpgradeItemSources.extract(wrapper, context.player(),
                                context.source(), source, amount),
                        stack -> recover(context, wrapper, source, stack)));
            }
            if (includePlayer) {
                for (int slot = 0; slot < PLAYER_MAIN_INVENTORY_SIZE; slot++) {
                    ItemStack stack = context.player().getInventory().getItem(slot);
                    if (!stack.isEmpty() && eligible.test(stack)) {
                        int playerSlot = slot;
                        found.add(new CraftingFillEngine.Supply<>(stack.copyWithCount(1), stack.getCount(), false,
                                amount -> context.player().getInventory().removeItem(playerSlot, amount),
                                context.player().getInventory()::placeItemBackInInventory));
                    }
                }
            }
            return found;
        };

        return CraftingFillEngine.fill(model, gridAccess, stackRules, sources,
                wrapper.getSourcePriority() == SourcePriority.NETWORK_FIRST
                        ? CraftingFillEngine.Priority.NETWORK_FIRST : CraftingFillEngine.Priority.BACKPACK_FIRST,
                includePlayerInventory, maxTransfer);
    }

    private static CraftingFillEngine.Recipe<ItemStack> model(CraftingRecipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            List<CraftingFillEngine.Ingredient<ItemStack>> ingredients = new ArrayList<>();
            for (Optional<Ingredient> ingredient : shaped.pattern.ingredients()) {
                ingredients.add(ingredient.map(value -> (CraftingFillEngine.Ingredient<ItemStack>) value::test)
                        .orElse(null));
            }
            return new CraftingFillEngine.Recipe<>(true, shaped.getWidth(), shaped.getHeight(), ingredients);
        }
        List<CraftingFillEngine.Ingredient<ItemStack>> ingredients = RemoteCraftingRecipes.ingredients(recipe).stream()
                .map(ingredient -> (CraftingFillEngine.Ingredient<ItemStack>) ingredient::test)
                .toList();
        return new CraftingFillEngine.Recipe<>(false, 0, 0, ingredients);
    }

    private static boolean matchesAny(List<CraftingFillEngine.Ingredient<ItemStack>> ingredients, ItemStack stack) {
        return ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
    }

    private static List<ItemStack> readGrid(Container grid) {
        List<ItemStack> contents = new ArrayList<>(CraftingFillEngine.GRID_SIZE);
        for (int slot = 0; slot < CraftingFillEngine.GRID_SIZE; slot++) {
            contents.add(grid.getItem(slot));
        }
        return contents;
    }

    private static void recover(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            SourceItem source, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, context.player(), context.source(), source, stack);
        if (!remainder.isEmpty()) {
            if (source.remote()) {
                UpgradeItemSources.recover(wrapper, context.player(), remainder);
            } else {
                context.player().getInventory().placeItemBackInInventory(remainder);
            }
        }
    }
}
