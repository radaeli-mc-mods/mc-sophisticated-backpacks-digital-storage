package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.IngredientAssignment;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.IngredientAssignment.Candidate;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.UpgradeItemSources.SourceItem;

final class RemoteCraftingFill {
    private static final int SIDE = 3;
    private static final int GRID_SIZE = SIDE * SIDE;

    private RemoteCraftingFill() {
    }

    /** Returns missing ingredient slots, or -1 when the existing grid is incompatible. */
    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe) {
        List<ItemStack> contents = readGrid(grid);
        if (matches(context, grid, recipe)) {
            return 0;
        }
        List<Layout> layouts = layouts(recipe, contents);
        if (plan(layouts, contents, List.of(), wrapper.getSourcePriority()).isEmpty()) {
            return -1;
        }

        List<Ingredient> ingredients = RemoteCraftingRecipes.ingredients(recipe).stream().filter(ingredient -> !ingredient.isEmpty()).toList();
        List<SourceItem> stock = new ArrayList<>(UpgradeItemSources.find(wrapper, context.player(), context.source(),
                wrapper.getSourcePriority(), stack -> ingredients.stream().anyMatch(ingredient -> ingredient.test(stack)), Optional.empty(), GRID_SIZE));
        while (true) {
            contents = readGrid(grid);
            var proposed = plan(layouts, contents, stock, wrapper.getSourcePriority());
            if (proposed.isEmpty()) {
                return -1;
            }
            Plan next = proposed.get();
            if (next.placements().isEmpty()) {
                return matches(context, grid, recipe) ? 0 : Math.max(1, next.missing());
            }
            // Commit one unit and replan from the actual grid and remaining stock.
            Placement placement = next.placements().get(0);
            int index = placement.source();
            SourceItem source = stock.get(index);
            ItemStack extracted = UpgradeItemSources.extract(wrapper, context.player(), context.source(), source, 1);
            boolean accepted = !extracted.isEmpty() && ItemStack.isSameItemSameComponents(source.stack(), extracted)
                    && grid.getItem(placement.slot()).isEmpty() && grid.canPlaceItem(placement.slot(), extracted);
            if (accepted) {
                grid.setItem(placement.slot(), extracted.copyWithCount(1));
                extracted = extracted.copyWithCount(extracted.getCount() - 1);
                long remaining = source.amount() - 1;
                if (remaining > 0) {
                    stock.set(index, new SourceItem(source.stack(), remaining, source.target()));
                } else {
                    stock.remove(index);
                }
            } else {
                stock.remove(index);
            }
            if (!extracted.isEmpty()) {
                ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, context.player(), context.source(), source, extracted);
                if (!remainder.isEmpty()) {
                    if (source.remote()) {
                        UpgradeItemSources.recover(wrapper, context.player(), remainder);
                    } else {
                        context.player().getInventory().placeItemBackInInventory(remainder);
                    }
                }
            }
        }
    }

    private static boolean matches(UpgradeOperationContext context, Container grid, CraftingRecipe recipe) {
        return recipe.matches(CraftingInput.ofPositioned(SIDE, SIDE, readGrid(grid)).input(), context.player().level());
    }

    private static List<ItemStack> readGrid(Container grid) {
        List<ItemStack> contents = new ArrayList<>(GRID_SIZE);
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            contents.add(grid.getItem(slot));
        }
        return contents;
    }

    private static List<Layout> layouts(CraftingRecipe recipe, List<ItemStack> contents) {
        if (!(recipe instanceof ShapedRecipe shaped)) {
            return List.of(new Layout(RemoteCraftingRecipes.ingredients(recipe), false));
        }
        List<Layout> layouts = new ArrayList<>();
        int width = shaped.getWidth();
        int height = shaped.getHeight();
        boolean empty = contents.stream().allMatch(ItemStack::isEmpty);
        for (int y = 0; y <= SIDE - height; y++) {
            for (int x = 0; x <= SIDE - width; x++) {
                layouts.add(shapedLayout(shaped, x, y, false));
                if (empty) {
                    return layouts;
                }
                if (width > 1) {
                    layouts.add(shapedLayout(shaped, x, y, true));
                }
            }
        }
        return layouts;
    }

    private static Layout shapedLayout(ShapedRecipe recipe, int x, int y, boolean mirrored) {
        int width = recipe.getWidth();
        List<Ingredient> slots = new ArrayList<>(java.util.Collections.nCopies(GRID_SIZE, null));
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int column = 0; column < width; column++) {
                int original = row * width + (mirrored ? width - 1 - column : column);
                slots.set((y + row) * SIDE + x + column, recipe.getIngredients().get(original).orElse(null));
            }
        }
        return new Layout(slots, true);
    }

    private static Optional<Plan> plan(List<Layout> layouts, List<ItemStack> grid, List<SourceItem> stock,
            SourcePriority priority) {
        Plan best = null;
        for (Layout layout : layouts) {
            Plan candidate = plan(layout, grid, stock, priority);
            if (candidate != null && (best == null || candidate.missing() < best.missing()
                    || (candidate.missing() == best.missing() && candidate.preferred() > best.preferred()))) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private static Plan plan(Layout layout, List<ItemStack> grid, List<SourceItem> stock, SourcePriority priority) {
        List<Candidate> candidates = new ArrayList<>();
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = grid.get(slot);
            if (stack.isEmpty()) {
                free.add(slot);
                continue;
            }
            int mask = layout.shaped()
                    ? (layout.ingredients().get(slot) != null && layout.ingredients().get(slot).test(stack) ? 1 << slot : 0)
                    : compatibility(layout.ingredients(), stack);
            candidates.add(new Candidate(mask, 1));
        }
        int existing = candidates.size();
        for (SourceItem item : stock) {
            candidates.add(new Candidate(compatibility(layout.ingredients(), item.stack()), (int) Math.min(GRID_SIZE, item.amount())));
        }
        int[] owners = IngredientAssignment.match(layout.ingredients().size(), candidates, existing).orElse(null);
        if (owners == null) {
            return null;
        }
        int missing = 0;
        int preferred = 0;
        List<Placement> placements = new ArrayList<>();
        for (int ingredient = 0; ingredient < owners.length; ingredient++) {
            if (layout.ingredients().get(ingredient) == null) {
                continue;
            }
            int owner = owners[ingredient];
            if (owner < 0) {
                missing++;
            } else if (owner >= existing) {
                int source = owner - existing;
                int slot = layout.shaped() ? ingredient : free.get(placements.size());
                placements.add(new Placement(slot, source));
                if (stock.get(source).remote() == (priority == SourcePriority.NETWORK_FIRST)) {
                    preferred++;
                }
            }
        }
        return new Plan(placements, missing, preferred);
    }

    private static int compatibility(List<Ingredient> ingredients, ItemStack stack) {
        int mask = 0;
        for (int index = 0; index < ingredients.size(); index++) {
            if (ingredients.get(index) != null && ingredients.get(index).test(stack)) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private record Layout(List<Ingredient> ingredients, boolean shaped) {
    }

    private record Plan(List<Placement> placements, int missing, int preferred) {
    }

    private record Placement(int slot, int source) {
    }
}
