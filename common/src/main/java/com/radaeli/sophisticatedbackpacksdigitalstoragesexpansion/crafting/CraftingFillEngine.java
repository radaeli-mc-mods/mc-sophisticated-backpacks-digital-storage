package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/** Minecraft-independent planning and execution for the remote crafting grid. */
public final class CraftingFillEngine {
    public static final int GRID_SIDE = 3;
    public static final int GRID_SIZE = GRID_SIDE * GRID_SIDE;
    private static final int MAX_CRAFTS = 64;

    private CraftingFillEngine() {
    }

    /** A null ingredient represents an empty cell in a shaped recipe. */
    @FunctionalInterface
    public interface Ingredient<S> {
        boolean test(S stack);
    }

    /** Recipe ingredients are in pattern order for shaped recipes and ingredient order otherwise. */
    public record Recipe<S>(boolean shaped, int width, int height, List<Ingredient<S>> ingredients) {
        public Recipe {
            Objects.requireNonNull(ingredients, "ingredients");
            ingredients = Collections.unmodifiableList(new ArrayList<>(ingredients));
            if (shaped) {
                if (width < 1 || width > GRID_SIDE || height < 1 || height > GRID_SIDE
                        || ingredients.size() != width * height) {
                    throw new IllegalArgumentException("Invalid shaped recipe dimensions or ingredients");
                }
            } else if (ingredients.isEmpty() || ingredients.size() > GRID_SIZE
                    || ingredients.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Invalid shapeless recipe ingredients");
            }
        }

        private List<Ingredient<S>> requiredIngredients() {
            return ingredients.stream().filter(Objects::nonNull).toList();
        }
    }

    public enum Priority {
        NETWORK_FIRST,
        BACKPACK_FIRST
    }

    public interface Grid<S> {
        S getItem(int slot);

        int getMaxStackSize();

        boolean canPlaceItem(int slot, S stack);

        void setItem(int slot, S stack);

        void setChanged();

        boolean matchesRecipe();
    }

    public interface StackRules<S> {
        boolean isEmpty(S stack);

        int count(S stack);

        int maxStackSize(S stack);

        boolean sameItemAndComponents(S left, S right);

        S copyWithCount(S stack, int count);
    }

    /** Source operations are supplied by the target adapter; the engine never sees a storage API. */
    public record Supply<S>(S exemplar, long amount, boolean remote, IntFunction<S> extractor,
            Consumer<S> recoverer) {
        public Supply {
            Objects.requireNonNull(exemplar, "exemplar");
            Objects.requireNonNull(extractor, "extractor");
            Objects.requireNonNull(recoverer, "recoverer");
            if (amount < 0) {
                throw new IllegalArgumentException("amount must not be negative");
            }
        }

        public S extract(int amount) {
            return extractor.apply(amount);
        }

        public void recover(S stack) {
            if (stack != null) {
                recoverer.accept(stack);
            }
        }
    }

    @FunctionalInterface
    public interface SourceProvider<S> {
        List<Supply<S>> find(Predicate<S> eligible, boolean includePlayerInventory, boolean maxTransfer);
    }

    /** Returns missing ingredient slots, or -1 when the current grid cannot fit this recipe. */
    public static <S> int fill(Recipe<S> recipe, Grid<S> grid, StackRules<S> stackRules,
            SourceProvider<S> sourceProvider, Priority priority, boolean includePlayerInventory,
            boolean maxTransfer) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(stackRules, "stackRules");
        Objects.requireNonNull(sourceProvider, "sourceProvider");
        Objects.requireNonNull(priority, "priority");

        List<S> initialGrid = readGrid(grid);
        List<Layout<S>> layouts = layouts(recipe, initialGrid, stackRules);
        List<Ingredient<S>> ingredients = recipe.requiredIngredients();
        Predicate<S> eligible = stack -> !stackRules.isEmpty(stack)
                && ingredients.stream().anyMatch(ingredient -> ingredient.test(stack));
        List<Supply<S>> supplies = new ArrayList<>(sourceProvider.find(eligible, includePlayerInventory, maxTransfer));
        // Stable sorting keeps each target's deterministic order within the preferred source group.
        supplies.sort(Comparator.comparingInt(supply -> supply.remote() == (priority == Priority.NETWORK_FIRST) ? 0 : 1));
        List<AvailableSupply<S>> stock = supplies.stream()
                .filter(supply -> supply.amount() > 0 && eligible.test(supply.exemplar()))
                .map(AvailableSupply::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (maxTransfer && includePlayerInventory) {
            Optional<BulkPlan<S>> bulkPlan = planMaximum(grid, layouts, stock, stackRules);
            if (bulkPlan.isPresent()) {
                return applyMaximum(grid, recipe, layouts, stock, bulkPlan.get(), stackRules, priority);
            }
        }
        return fillSingle(grid, recipe, layouts, stock, stackRules, priority);
    }

    private static <S> int fillSingle(Grid<S> grid, Recipe<S> recipe, List<Layout<S>> layouts,
            List<AvailableSupply<S>> stock, StackRules<S> stackRules, Priority priority) {
        if (grid.matchesRecipe()) {
            return 0;
        }
        if (plan(layouts, readGrid(grid), List.of(), priority, grid, stackRules).isEmpty()) {
            return -1;
        }
        while (true) {
            Optional<Plan<S>> proposed = plan(layouts, readGrid(grid), stock, priority, grid, stackRules);
            if (proposed.isEmpty()) {
                return -1;
            }
            Plan<S> next = proposed.get();
            if (next.placements().isEmpty()) {
                return grid.matchesRecipe() ? 0 : Math.max(1, next.missing());
            }

            Placement placement = next.placements().get(0);
            AvailableSupply<S> available = stock.get(placement.source());
            Supply<S> source = available.supply;
            S extracted = source.extract(1);
            boolean accepted = false;
            S remainder = null;
            if (extracted != null && !stackRules.isEmpty(extracted)) {
                accepted = stackRules.sameItemAndComponents(source.exemplar(), extracted)
                        && stackRules.count(extracted) >= 1
                        && stackRules.isEmpty(grid.getItem(placement.slot()))
                        && grid.canPlaceItem(placement.slot(), extracted);
                if (accepted) {
                    grid.setItem(placement.slot(), stackRules.copyWithCount(extracted, 1));
                    if (stackRules.count(extracted) > 1) {
                        remainder = stackRules.copyWithCount(extracted, stackRules.count(extracted) - 1);
                    }
                    available.remaining = Math.max(0, available.remaining - 1);
                } else {
                    remainder = extracted;
                }
            }
            if (!accepted) {
                available.remaining = 0;
                stock.remove(available);
            }
            if (remainder != null && !stackRules.isEmpty(remainder)) {
                source.recover(remainder);
            }
        }
    }

    private static <S> Optional<BulkPlan<S>> planMaximum(Grid<S> grid, List<Layout<S>> layouts,
            List<AvailableSupply<S>> stock, StackRules<S> stackRules) {
        List<BulkItem<S>> items = groupStock(stock, grid, stackRules);
        for (int crafts = Math.min(MAX_CRAFTS, grid.getMaxStackSize()); crafts >= 1; crafts--) {
            for (Layout<S> layout : layouts) {
                Optional<BulkPlan<S>> plan = planMaximum(grid, layout, items, crafts, stackRules);
                if (plan.isPresent()) {
                    return plan;
                }
            }
        }
        return Optional.empty();
    }

    private static <S> Optional<BulkPlan<S>> planMaximum(Grid<S> grid, Layout<S> layout,
            List<BulkItem<S>> items, int crafts, StackRules<S> stackRules) {
        List<GridSource> gridSources = new ArrayList<>();
        List<IngredientAssignment.Candidate> candidates = new ArrayList<>();
        long[] gridDeficits = new long[items.size()];
        List<Integer> freeSlots = new ArrayList<>();

        for (int slot = 0; slot < GRID_SIZE; slot++) {
            S stack = grid.getItem(slot);
            if (stackRules.isEmpty(stack)) {
                freeSlots.add(slot);
                continue;
            }
            if (crafts > stackRules.maxStackSize(stack)) {
                return Optional.empty();
            }
            int item = findBulkItem(items, stack, stackRules);
            int compatible = layout.shaped()
                    ? slot < layout.ingredients().size() && matches(layout.ingredients().get(slot), stack)
                            ? 1 << slot : 0
                    : compatibility(layout.ingredients(), stack);
            if (compatible == 0) {
                return Optional.empty();
            }
            gridSources.add(new GridSource(slot, item));
            candidates.add(new IngredientAssignment.Candidate(compatible, 1));
            gridDeficits[item] += Math.max(0, crafts - stackRules.count(stack));
        }

        int requiredGridSources = gridSources.size();
        List<BulkItem<S>> storageCandidates = new ArrayList<>();
        int recipeSlots = layout.ingredients().size();
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            BulkItem<S> item = items.get(itemIndex);
            long available = item.available - gridDeficits[itemIndex];
            if (available < crafts || crafts > stackRules.maxStackSize(item.stack)) {
                continue;
            }
            int compatible = bulkCompatibility(layout, grid, item.stack, freeSlots, stackRules);
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
            if (layout.ingredients().get(ingredient) != null && owners[ingredient] < 0) {
                return Optional.empty();
            }
        }

        List<BulkDestination> destinations = new ArrayList<>();
        long[] requiredItems = new long[items.size()];
        List<Integer> assignedFreeSlots = new ArrayList<>();
        for (int ingredient = 0; ingredient < recipeSlots; ingredient++) {
            if (layout.ingredients().get(ingredient) == null) {
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
                BulkItem<S> item = storageCandidates.get(owner - requiredGridSources);
                itemIndex = findBulkItem(items, item.stack, stackRules);
                if (layout.shaped()) {
                    slot = ingredient;
                } else {
                    slot = freeSlots.stream()
                            .filter(candidate -> !assignedFreeSlots.contains(candidate))
                            .filter(candidate -> grid.canPlaceItem(candidate, item.stack))
                            .findFirst().orElse(-1);
                    if (slot < 0) {
                        return Optional.empty();
                    }
                    assignedFreeSlots.add(slot);
                }
            }
            S current = grid.getItem(slot);
            int currentCount = stackRules.isEmpty(current) ? 0 : stackRules.count(current);
            int targetCount = Math.max(currentCount, crafts);
            if (targetCount > grid.getMaxStackSize()
                    || targetCount > stackRules.maxStackSize(items.get(itemIndex).stack)) {
                return Optional.empty();
            }
            if (!stackRules.isEmpty(current)
                    && !stackRules.sameItemAndComponents(current, items.get(itemIndex).stack)) {
                return Optional.empty();
            }
            if (stackRules.isEmpty(current) && !grid.canPlaceItem(slot, items.get(itemIndex).stack)) {
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
        return Optional.of(new BulkPlan<>(items, destinations, requiredItems));
    }

    private static <S> int bulkCompatibility(Layout<S> layout, Grid<S> grid, S stack,
            List<Integer> freeSlots, StackRules<S> stackRules) {
        int mask = 0;
        for (int ingredient = 0; ingredient < layout.ingredients().size(); ingredient++) {
            if (!matches(layout.ingredients().get(ingredient), stack)) {
                continue;
            }
            if (layout.shaped()) {
                if (stackRules.isEmpty(grid.getItem(ingredient)) && grid.canPlaceItem(ingredient, stack)) {
                    mask |= 1 << ingredient;
                }
            } else if (freeSlots.stream().anyMatch(slot -> grid.canPlaceItem(slot, stack))) {
                mask |= 1 << ingredient;
            }
        }
        return mask;
    }

    private static <S> List<BulkItem<S>> groupStock(List<AvailableSupply<S>> stock, Grid<S> grid,
            StackRules<S> stackRules) {
        List<BulkItem<S>> items = new ArrayList<>();
        for (AvailableSupply<S> source : stock) {
            getOrCreateBulkItem(items, source.supply.exemplar(), stackRules).add(source);
        }
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            S stack = grid.getItem(slot);
            if (!stackRules.isEmpty(stack)) {
                getOrCreateBulkItem(items, stack, stackRules);
            }
        }
        return items;
    }

    private static <S> BulkItem<S> getOrCreateBulkItem(List<BulkItem<S>> items, S stack,
            StackRules<S> stackRules) {
        for (BulkItem<S> item : items) {
            if (stackRules.sameItemAndComponents(item.stack, stack)) {
                return item;
            }
        }
        BulkItem<S> item = new BulkItem<>(stack);
        items.add(item);
        return item;
    }

    private static <S> int findBulkItem(List<BulkItem<S>> items, S stack, StackRules<S> stackRules) {
        for (int i = 0; i < items.size(); i++) {
            if (stackRules.sameItemAndComponents(items.get(i).stack, stack)) {
                return i;
            }
        }
        throw new IllegalStateException("Crafting item was not grouped");
    }

    private static <S> int applyMaximum(Grid<S> grid, Recipe<S> recipe, List<Layout<S>> layouts,
            List<AvailableSupply<S>> stock, BulkPlan<S> plan, StackRules<S> stackRules, Priority priority) {
        int[] extracted = new int[plan.items().size()];
        for (int item = 0; item < plan.items().size(); item++) {
            long needed = plan.requiredItems()[item];
            if (needed > 0) {
                extracted[item] = extractBulk(plan.items().get(item), (int) Math.min(needed, Integer.MAX_VALUE),
                        stackRules);
            }
        }
        for (BulkDestination destination : plan.destinations()) {
            int available = extracted[destination.item()];
            int needed = destination.targetCount() - destination.currentCount();
            int moved = Math.min(available, needed);
            if (moved <= 0) {
                continue;
            }
            S current = grid.getItem(destination.slot());
            S updated = stackRules.isEmpty(current)
                    ? stackRules.copyWithCount(plan.items().get(destination.item()).stack, moved)
                    : stackRules.copyWithCount(current, stackRules.count(current) + moved);
            grid.setItem(destination.slot(), updated);
            extracted[destination.item()] -= moved;
        }
        grid.setChanged();
        return grid.matchesRecipe() ? 0 : fillSingle(grid, recipe, layouts, stock, stackRules, priority);
    }

    private static <S> int extractBulk(BulkItem<S> item, int amount, StackRules<S> stackRules) {
        int extracted = 0;
        for (AvailableSupply<S> supply : item.supplies) {
            if (extracted >= amount || supply.remaining <= 0) {
                continue;
            }
            while (extracted < amount && supply.remaining > 0) {
                int requested = (int) Math.min(amount - extracted,
                        Math.min(supply.remaining, Integer.MAX_VALUE));
                S pulled = supply.supply.extract(requested);
                if (pulled == null || stackRules.isEmpty(pulled)) {
                    supply.remaining = 0;
                    break;
                }
                if (!stackRules.sameItemAndComponents(item.stack, pulled)) {
                    supply.supply.recover(pulled);
                    supply.remaining = 0;
                    break;
                }
                int pulledCount = stackRules.count(pulled);
                int accepted = Math.min(requested, pulledCount);
                supply.remaining = Math.max(0, supply.remaining - pulledCount);
                extracted += accepted;
                if (pulledCount > accepted) {
                    supply.supply.recover(stackRules.copyWithCount(pulled, pulledCount - accepted));
                }
            }
        }
        return extracted;
    }

    private static <S> Optional<Plan<S>> plan(List<Layout<S>> layouts, List<S> grid,
            List<AvailableSupply<S>> stock, Priority priority, Grid<S> gridAccess, StackRules<S> stackRules) {
        Plan<S> best = null;
        for (Layout<S> layout : layouts) {
            Plan<S> candidate = plan(layout, grid, stock, priority, gridAccess, stackRules);
            if (candidate != null && (best == null || candidate.missing() < best.missing()
                    || (candidate.missing() == best.missing() && candidate.preferred() > best.preferred()))) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private static <S> Plan<S> plan(Layout<S> layout, List<S> grid, List<AvailableSupply<S>> stock,
            Priority priority, Grid<S> gridAccess, StackRules<S> stackRules) {
        List<IngredientAssignment.Candidate> candidates = new ArrayList<>();
        List<Integer> freeSlots = new ArrayList<>();
        List<GridSource> existing = new ArrayList<>();
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            S stack = grid.get(slot);
            if (stackRules.isEmpty(stack)) {
                freeSlots.add(slot);
                continue;
            }
            int mask = layout.shaped()
                    ? matches(layout.ingredients().get(slot), stack) ? 1 << slot : 0
                    : compatibility(layout.ingredients(), stack);
            existing.add(new GridSource(slot, -1));
            candidates.add(new IngredientAssignment.Candidate(mask, 1));
        }
        int existingCount = existing.size();
        for (AvailableSupply<S> source : stock) {
            int compatible = compatibility(layout, source.supply.exemplar(), freeSlots, gridAccess, stackRules);
            candidates.add(new IngredientAssignment.Candidate(compatible,
                    (int) Math.min(GRID_SIZE, source.remaining)));
        }

        int[] owners = IngredientAssignment.match(layout.ingredients().size(), candidates, existingCount).orElse(null);
        if (owners == null) {
            return null;
        }
        int missing = 0;
        int preferred = 0;
        List<Placement> placements = new ArrayList<>();
        List<Integer> usedFreeSlots = new ArrayList<>();
        for (int ingredient = 0; ingredient < owners.length; ingredient++) {
            if (layout.ingredients().get(ingredient) == null) {
                continue;
            }
            int owner = owners[ingredient];
            if (owner < 0) {
                missing++;
            } else if (owner >= existingCount) {
                int sourceIndex = owner - existingCount;
                AvailableSupply<S> source = stock.get(sourceIndex);
                int slot;
                if (layout.shaped()) {
                    slot = ingredient;
                } else {
                    slot = freeSlots.stream()
                            .filter(candidate -> !usedFreeSlots.contains(candidate))
                            .filter(candidate -> gridAccess.canPlaceItem(candidate, source.supply.exemplar()))
                            .findFirst().orElse(-1);
                    if (slot < 0) {
                        return null;
                    }
                    usedFreeSlots.add(slot);
                }
                placements.add(new Placement(slot, sourceIndex));
                if (source.supply.remote() == (priority == Priority.NETWORK_FIRST)) {
                    preferred++;
                }
            }
        }
        return new Plan<>(placements, missing, preferred);
    }

    private static <S> int compatibility(Layout<S> layout, S stack, List<Integer> freeSlots,
            Grid<S> grid, StackRules<S> stackRules) {
        int mask = 0;
        for (int index = 0; index < layout.ingredients().size(); index++) {
            if (matches(layout.ingredients().get(index), stack)
                    && (layout.shaped()
                    ? stackRules.isEmpty(grid.getItem(index)) && grid.canPlaceItem(index, stack)
                    : freeSlots.stream().anyMatch(slot -> grid.canPlaceItem(slot, stack)))) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private static <S> int compatibility(List<Ingredient<S>> ingredients, S stack) {
        int mask = 0;
        for (int index = 0; index < ingredients.size(); index++) {
            if (matches(ingredients.get(index), stack)) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private static <S> boolean matches(Ingredient<S> ingredient, S stack) {
        return ingredient != null && ingredient.test(stack);
    }

    private static <S> List<S> readGrid(Grid<S> grid) {
        List<S> contents = new ArrayList<>(GRID_SIZE);
        for (int slot = 0; slot < GRID_SIZE; slot++) {
            contents.add(grid.getItem(slot));
        }
        return contents;
    }

    private static <S> List<Layout<S>> layouts(Recipe<S> recipe, List<S> contents, StackRules<S> stackRules) {
        if (!recipe.shaped()) {
            return List.of(new Layout<>(recipe.ingredients(), false));
        }
        List<Layout<S>> layouts = new ArrayList<>();
        boolean empty = contents.stream().allMatch(stackRules::isEmpty);
        for (int y = 0; y <= GRID_SIDE - recipe.height(); y++) {
            for (int x = 0; x <= GRID_SIDE - recipe.width(); x++) {
                layouts.add(shapedLayout(recipe, x, y, false));
                if (empty) {
                    return layouts;
                }
                if (recipe.width() > 1) {
                    layouts.add(shapedLayout(recipe, x, y, true));
                }
            }
        }
        return layouts;
    }

    private static <S> Layout<S> shapedLayout(Recipe<S> recipe, int x, int y, boolean mirrored) {
        List<Ingredient<S>> slots = new ArrayList<>(Collections.nCopies(GRID_SIZE, null));
        for (int row = 0; row < recipe.height(); row++) {
            for (int column = 0; column < recipe.width(); column++) {
                int original = row * recipe.width() + (mirrored ? recipe.width() - 1 - column : column);
                slots.set((y + row) * GRID_SIDE + x + column, recipe.ingredients().get(original));
            }
        }
        return new Layout<>(slots, true);
    }

    private record Layout<S>(List<Ingredient<S>> ingredients, boolean shaped) {
    }

    private record Plan<S>(List<Placement> placements, int missing, int preferred) {
    }

    private record Placement(int slot, int source) {
    }

    private static final class AvailableSupply<S> {
        private final Supply<S> supply;
        private long remaining;

        private AvailableSupply(Supply<S> supply) {
            this.supply = supply;
            remaining = supply.amount();
        }
    }

    private static final class BulkItem<S> {
        private final S stack;
        private final List<AvailableSupply<S>> supplies = new ArrayList<>();
        private long available;

        private BulkItem(S stack) {
            this.stack = stack;
        }

        private void add(AvailableSupply<S> supply) {
            supplies.add(supply);
            available += supply.remaining;
        }
    }

    private record GridSource(int slot, int item) {
    }

    private record BulkDestination(int slot, int item, int targetCount, int currentCount) {
    }

    private record BulkPlan<S>(List<BulkItem<S>> items, List<BulkDestination> destinations, long[] requiredItems) {
    }
}
