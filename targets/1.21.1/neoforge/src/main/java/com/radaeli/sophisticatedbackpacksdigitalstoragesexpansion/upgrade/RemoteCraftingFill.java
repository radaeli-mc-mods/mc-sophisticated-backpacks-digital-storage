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
    private static final int PLAYER_MAIN_INVENTORY_SIZE = 36;
    private static final int MAX_GRID_STACK_SIZE = 64;
    private static final int MAX_TRANSFER_ITEMS = GRID_SIZE * MAX_GRID_STACK_SIZE;

    private RemoteCraftingFill() {
    }

    /** Returns missing ingredient slots, or -1 when the existing grid is incompatible. */
    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe) {
        return fill(context, wrapper, grid, recipe, false, false);
    }

    /** JEI can supply items from the player's inventory as well as the backpack and linked network. */
    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe, boolean includePlayerInventory) {
        return fill(context, wrapper, grid, recipe, includePlayerInventory, false);
    }

    /** JEI's max-transfer request fills each recipe slot as far as the available sources and slot limits allow. */
    static int fill(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe, boolean includePlayerInventory, boolean maxTransfer) {
        List<ItemStack> contents = readGrid(grid);
        List<Layout> layouts = layouts(recipe, contents);
        List<Ingredient> ingredients = recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).toList();
        List<CraftingSource> stock = UpgradeItemSources.find(wrapper, context.player(), context.source(),
                wrapper.getSourcePriority(), stack -> ingredients.stream().anyMatch(ingredient -> ingredient.test(stack)),
                Optional.empty(), maxTransfer ? MAX_TRANSFER_ITEMS : GRID_SIZE).stream().map(CraftingSource::storage).collect(ArrayList::new,
                        ArrayList::add, ArrayList::addAll);
        if (includePlayerInventory) {
            for (int slot = 0; slot < PLAYER_MAIN_INVENTORY_SIZE; slot++) {
                ItemStack stack = context.player().getInventory().getItem(slot);
                if (!stack.isEmpty() && ingredients.stream().anyMatch(ingredient -> ingredient.test(stack))) {
                    stock.add(CraftingSource.player(stack, slot));
                }
            }
        }

        if (maxTransfer && includePlayerInventory) {
            Optional<BulkPlan> bulkPlan = planMaximum(grid, layouts, stock);
            if (bulkPlan.isPresent()) {
                return applyMaximum(context, wrapper, grid, recipe, layouts, stock, bulkPlan.get());
            }
        }

        return fillSingle(context, wrapper, grid, recipe, layouts, stock);
    }

    private static int fillSingle(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe, List<Layout> layouts, List<CraftingSource> stock) {
        List<ItemStack> contents = readGrid(grid);
        if (matches(context, grid, recipe)) {
            return 0;
        }
        if (plan(layouts, contents, List.of(), wrapper.getSourcePriority()).isEmpty()) {
            return -1;
        }
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
            CraftingSource source = stock.get(index);
            ItemStack extracted = source.playerSlot() >= 0
                    ? context.player().getInventory().removeItem(source.playerSlot(), 1)
                    : UpgradeItemSources.extract(wrapper, context.player(), context.source(), source.storageSource(), 1);
            boolean accepted = !extracted.isEmpty() && ItemStack.isSameItemSameComponents(source.stack(), extracted)
                    && grid.getItem(placement.slot()).isEmpty() && grid.canPlaceItem(placement.slot(), extracted);
            if (accepted) {
                grid.setItem(placement.slot(), extracted.copyWithCount(1));
                extracted = extracted.copyWithCount(extracted.getCount() - 1);
                long remaining = source.amount() - 1;
                if (remaining > 0) {
                    stock.set(index, source.withAmount(remaining));
                } else {
                    stock.remove(index);
                }
            } else {
                stock.remove(index);
            }
            if (!extracted.isEmpty()) {
                ItemStack remainder;
                if (source.playerSlot() >= 0) {
                    context.player().getInventory().placeItemBackInInventory(extracted);
                    remainder = ItemStack.EMPTY;
                } else {
                    remainder = UpgradeItemSources.returnToSource(wrapper, context.player(), context.source(),
                            source.storageSource(), extracted);
                }
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

    private static Optional<BulkPlan> planMaximum(Container grid, List<Layout> layouts, List<CraftingSource> stock) {
        List<BulkItem> items = groupStock(stock, grid);
        for (int crafts = MAX_GRID_STACK_SIZE; crafts >= 1; crafts--) {
            for (Layout layout : layouts) {
                Optional<BulkPlan> plan = planMaximum(grid, layout, items, crafts);
                if (plan.isPresent()) {
                    return plan;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BulkPlan> planMaximum(Container grid, Layout layout, List<BulkItem> items, int crafts) {
        List<GridSource> gridSources = new ArrayList<>();
        List<IngredientAssignment.Candidate> candidates = new ArrayList<>();
        long[] gridDeficits = new long[items.size()];
        List<Integer> freeSlots = new ArrayList<>();

        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = grid.getItem(slot);
            if (stack.isEmpty()) {
                freeSlots.add(slot);
                continue;
            }
            if (crafts > grid.getMaxStackSize(stack) || crafts > stack.getMaxStackSize()) {
                return Optional.empty();
            }
            int item = findBulkItem(items, stack);
            int compatible = layout.shaped()
                    ? slot < layout.ingredients().size() && layout.ingredients().get(slot).test(stack) ? 1 << slot : 0
                    : compatibility(layout.ingredients(), stack);
            if (compatible == 0) {
                return Optional.empty();
            }
            gridSources.add(new GridSource(slot, item));
            candidates.add(new IngredientAssignment.Candidate(compatible, 1));
            gridDeficits[item] += Math.max(0, crafts - stack.getCount());
        }

        int requiredGridSources = gridSources.size();
        List<BulkItem> storageCandidates = new ArrayList<>();
        int recipeSlots = layout.ingredients().size();
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            BulkItem item = items.get(itemIndex);
            long available = item.available - gridDeficits[itemIndex];
            if (available < crafts || crafts > item.stack.getMaxStackSize()) {
                continue;
            }
            int compatible = bulkCompatibility(layout, grid, item.stack, freeSlots);
            int capacity = (int) Math.min(recipeSlots, available / crafts);
            if (compatible != 0 && capacity > 0) {
                candidates.add(new IngredientAssignment.Candidate(compatible, capacity));
                storageCandidates.add(item);
            }
        }

        int[] owners = IngredientAssignment.match(recipeSlots, candidates, requiredGridSources).orElse(null);
        if (owners == null) {
            return Optional.empty();
        }
        for (int ingredient = 0; ingredient < recipeSlots; ingredient++) {
            if (!layout.ingredients().get(ingredient).isEmpty() && owners[ingredient] < 0) {
                return Optional.empty();
            }
        }

        List<BulkDestination> destinations = new ArrayList<>();
        long[] requiredItems = new long[items.size()];
        int nextFreeSlot = 0;
        for (int ingredient = 0; ingredient < recipeSlots; ingredient++) {
            if (layout.ingredients().get(ingredient).isEmpty()) {
                continue;
            }
            int owner = owners[ingredient];
            int slot;
            int itemIndex;
            if (owner < requiredGridSources) {
                GridSource source = gridSources.get(owner);
                slot = source.slot();
                itemIndex = source.item();
            } else {
                BulkItem item = storageCandidates.get(owner - requiredGridSources);
                itemIndex = findBulkItem(items, item.stack);
                if (layout.shaped()) {
                    slot = ingredient;
                } else {
                    while (nextFreeSlot < freeSlots.size()
                            && !grid.canPlaceItem(freeSlots.get(nextFreeSlot), item.stack)) {
                        nextFreeSlot++;
                    }
                    if (nextFreeSlot >= freeSlots.size()) {
                        return Optional.empty();
                    }
                    slot = freeSlots.get(nextFreeSlot++);
                }
            }
            ItemStack current = grid.getItem(slot);
            int currentCount = current.isEmpty() ? 0 : current.getCount();
            int targetCount = Math.max(currentCount, crafts);
            if (targetCount > grid.getMaxStackSize(items.get(itemIndex).stack)
                    || targetCount > items.get(itemIndex).stack.getMaxStackSize()) {
                return Optional.empty();
            }
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, items.get(itemIndex).stack)) {
                return Optional.empty();
            }
            if (current.isEmpty() && !grid.canPlaceItem(slot, items.get(itemIndex).stack)) {
                return Optional.empty();
            }
            destinations.add(new BulkDestination(slot, itemIndex, targetCount, currentCount));
            requiredItems[itemIndex] += targetCount - currentCount;
        }

        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            if (requiredItems[itemIndex] > items.get(itemIndex).available) {
                return Optional.empty();
            }
        }
        return Optional.of(new BulkPlan(items, destinations, requiredItems));
    }

    private static int bulkCompatibility(Layout layout, Container grid, ItemStack stack, List<Integer> freeSlots) {
        int mask = 0;
        for (int ingredient = 0; ingredient < layout.ingredients().size(); ingredient++) {
            if (layout.ingredients().get(ingredient).isEmpty() || !layout.ingredients().get(ingredient).test(stack)) {
                continue;
            }
            if (layout.shaped()) {
                if (grid.getItem(ingredient).isEmpty() && grid.canPlaceItem(ingredient, stack)) {
                    mask |= 1 << ingredient;
                }
            } else if (freeSlots.stream().anyMatch(slot -> grid.canPlaceItem(slot, stack))) {
                mask |= 1 << ingredient;
            }
        }
        return mask;
    }

    private static List<BulkItem> groupStock(List<CraftingSource> stock, Container grid) {
        List<BulkItem> items = new ArrayList<>();
        for (CraftingSource source : stock) {
            BulkItem item = getOrCreateBulkItem(items, source.stack());
            item.add(source);
        }
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = grid.getItem(slot);
            if (!stack.isEmpty()) {
                getOrCreateBulkItem(items, stack);
            }
        }
        return items;
    }

    private static BulkItem getOrCreateBulkItem(List<BulkItem> items, ItemStack stack) {
        for (BulkItem item : items) {
            if (ItemStack.isSameItemSameComponents(item.stack, stack)) {
                return item;
            }
        }
        BulkItem item = new BulkItem(stack.copyWithCount(1));
        items.add(item);
        return item;
    }

    private static int findBulkItem(List<BulkItem> items, ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (ItemStack.isSameItemSameComponents(items.get(i).stack, stack)) {
                return i;
            }
        }
        throw new IllegalStateException("Bulk crafting item was not grouped");
    }

    private static int applyMaximum(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            Container grid, CraftingRecipe recipe, List<Layout> layouts, List<CraftingSource> stock, BulkPlan plan) {
        int[] extracted = new int[plan.items().size()];
        for (int item = 0; item < plan.items().size(); item++) {
            long needed = plan.requiredItems()[item];
            if (needed > 0) {
                extracted[item] = extractBulk(context, wrapper, plan.items().get(item), (int) needed);
            }
        }
        for (BulkDestination destination : plan.destinations()) {
            int available = extracted[destination.item()];
            int needed = destination.targetCount() - destination.currentCount();
            int moved = Math.min(available, needed);
            if (moved <= 0) {
                continue;
            }
            ItemStack current = grid.getItem(destination.slot());
            ItemStack updated = current.isEmpty()
                    ? plan.items().get(destination.item()).stack.copyWithCount(moved)
                    : current.copyWithCount(current.getCount() + moved);
            grid.setItem(destination.slot(), updated);
            extracted[destination.item()] -= moved;
        }
        grid.setChanged();
        return matches(context, grid, recipe) ? 0 : fillSingle(context, wrapper, grid, recipe, layouts, stock);
    }

    private static int extractBulk(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            BulkItem item, int amount) {
        int extracted = 0;
        for (BulkSupply supply : item.supplies) {
            if (extracted >= amount || supply.remaining <= 0) {
                continue;
            }
            while (extracted < amount && supply.remaining > 0) {
                int requested = (int) Math.min(amount - extracted, Math.min(supply.remaining, Integer.MAX_VALUE));
                CraftingSource source = supply.source;
                ItemStack pulled = source.playerSlot() >= 0
                        ? context.player().getInventory().removeItem(source.playerSlot(), requested)
                        : UpgradeItemSources.extract(wrapper, context.player(), context.source(), source.storageSource(), requested);
                if (pulled.isEmpty()) {
                    supply.remaining = 0;
                    break;
                }
                if (!ItemStack.isSameItemSameComponents(item.stack, pulled)) {
                    recoverBulk(context, wrapper, source, pulled);
                    supply.remaining = 0;
                    break;
                }
                int accepted = Math.min(requested, pulled.getCount());
                supply.remaining = Math.max(0, supply.remaining - pulled.getCount());
                extracted += accepted;
                if (pulled.getCount() > accepted) {
                    recoverBulk(context, wrapper, source, pulled.copyWithCount(pulled.getCount() - accepted));
                }
            }
        }
        return extracted;
    }

    private static void recoverBulk(UpgradeOperationContext context, RemoteCraftingUpgradeWrapper wrapper,
            CraftingSource source, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (source.playerSlot() >= 0) {
            context.player().getInventory().placeItemBackInInventory(stack);
            return;
        }
        ItemStack remainder = UpgradeItemSources.returnToSource(wrapper, context.player(), context.source(),
                source.storageSource(), stack);
        if (!remainder.isEmpty()) {
            UpgradeItemSources.recover(wrapper, context.player(), remainder);
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
            return List.of(new Layout(recipe.getIngredients(), false));
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
        List<Ingredient> slots = new ArrayList<>(java.util.Collections.nCopies(GRID_SIZE, Ingredient.EMPTY));
        for (int row = 0; row < recipe.getHeight(); row++) {
            for (int column = 0; column < width; column++) {
                int original = row * width + (mirrored ? width - 1 - column : column);
                slots.set((y + row) * SIDE + x + column, recipe.getIngredients().get(original));
            }
        }
        return new Layout(slots, true);
    }

    private static Optional<Plan> plan(List<Layout> layouts, List<ItemStack> grid, List<CraftingSource> stock,
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

    private static Plan plan(Layout layout, List<ItemStack> grid, List<CraftingSource> stock, SourcePriority priority) {
        List<Candidate> candidates = new ArrayList<>();
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            ItemStack stack = grid.get(slot);
            if (stack.isEmpty()) {
                free.add(slot);
                continue;
            }
            int mask = layout.shaped()
                    ? (layout.ingredients().get(slot).test(stack) ? 1 << slot : 0)
                    : compatibility(layout.ingredients(), stack);
            candidates.add(new Candidate(mask, 1));
        }
        int existing = candidates.size();
        for (CraftingSource item : stock) {
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
            if (layout.ingredients().get(ingredient).isEmpty()) {
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
            if (ingredients.get(index).test(stack)) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private record CraftingSource(ItemStack stack, long amount, SourceItem storageSource, int playerSlot) {
        private static CraftingSource storage(SourceItem source) {
            return new CraftingSource(source.stack(), source.amount(), source, -1);
        }

        private static CraftingSource player(ItemStack stack, int slot) {
            return new CraftingSource(stack.copyWithCount(1), stack.getCount(), null, slot);
        }

        private boolean remote() {
            return storageSource != null && storageSource.remote();
        }

        private CraftingSource withAmount(long remaining) {
            return storageSource == null
                    ? new CraftingSource(stack, remaining, null, playerSlot)
                    : new CraftingSource(stack, remaining,
                            new SourceItem(storageSource.stack(), remaining, storageSource.target()), -1);
        }
    }

    private record Layout(List<Ingredient> ingredients, boolean shaped) {
    }

    private record Plan(List<Placement> placements, int missing, int preferred) {
    }

    private record Placement(int slot, int source) {
    }

    private static final class BulkItem {
        private final ItemStack stack;
        private final List<BulkSupply> supplies = new ArrayList<>();
        private long available;

        private BulkItem(ItemStack stack) {
            this.stack = stack;
        }

        private void add(CraftingSource source) {
            supplies.add(new BulkSupply(source, source.amount()));
            available = Math.min(MAX_TRANSFER_ITEMS, available + Math.min(source.amount(), MAX_TRANSFER_ITEMS));
        }
    }

    private static final class BulkSupply {
        private final CraftingSource source;
        private long remaining;

        private BulkSupply(CraftingSource source, long remaining) {
            this.source = source;
            this.remaining = remaining;
        }
    }

    private record GridSource(int slot, int item) {
    }

    private record BulkDestination(int slot, int item, int targetCount, int currentCount) {
    }

    private record BulkPlan(List<BulkItem> items, List<BulkDestination> destinations, long[] requiredItems) {
    }
}
