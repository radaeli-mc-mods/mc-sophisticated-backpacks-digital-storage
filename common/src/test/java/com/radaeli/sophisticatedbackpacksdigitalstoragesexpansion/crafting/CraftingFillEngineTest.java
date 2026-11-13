package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingFillEngineTest {
    private static final Stack EMPTY = new Stack("", "", 0, 64);
    private static final CraftingFillEngine.StackRules<Stack> STACKS = new CraftingFillEngine.StackRules<>() {
        @Override
        public boolean isEmpty(Stack stack) {
            return stack == null || stack.count() == 0;
        }

        @Override
        public int count(Stack stack) {
            return stack.count();
        }

        @Override
        public int maxStackSize(Stack stack) {
            return stack.maxStackSize();
        }

        @Override
        public boolean sameItemAndComponents(Stack left, Stack right) {
            return left.item().equals(right.item()) && left.components().equals(right.components());
        }

        @Override
        public Stack copyWithCount(Stack stack, int count) {
            return new Stack(stack.item(), stack.components(), count, stack.maxStackSize());
        }
    };

    @Test
    void preservesACompatibleIngredientAtAnOffsetAndMirroredPosition() {
        var recipe = shaped(2, 1, ingredient("a"), ingredient("b"));
        FakeGrid grid = grid(slots -> itemAt(slots, 4, "b") && itemAt(slots, 5, "a"));
        grid.rejectedSlots.add(3);
        grid.setItem(4, stack("b"));

        int missing = fill(recipe, grid, List.of(supply(stack("a"), 1, false)),
                CraftingFillEngine.Priority.NETWORK_FIRST, false, false);

        assertEquals(0, missing);
        assertEquals("b", grid.getItem(4).item());
        assertEquals("a", grid.getItem(5).item());
    }

    @Test
    void assignsOverlappingShapelessAlternativesWithoutSpendingTheRestrictedItemEarly() {
        var flexible = (CraftingFillEngine.Ingredient<Stack>) stack ->
                stack.item().equals("a") || stack.item().equals("b");
        var recipe = shapeless(flexible, ingredient("a"));
        FakeGrid grid = grid(slots -> itemAt(slots, 0, "b") && itemAt(slots, 1, "a"));

        int missing = fill(recipe, grid, List.of(supply(stack("a"), 1, false), supply(stack("b"), 1, false)),
                CraftingFillEngine.Priority.BACKPACK_FIRST, false, false);

        assertEquals(0, missing);
        assertEquals("b", grid.getItem(0).item());
        assertEquals("a", grid.getItem(1).item());
    }

    @Test
    void sourcePrioritySelectsNetworkOrBackpackWhenBothItemsMatch() {
        var recipe = shapeless(ingredient("material"));
        FakeGrid networkFirst = grid(slots -> "network".equals(componentAt(slots, 0)));
        FakeGrid backpackFirst = grid(slots -> "backpack".equals(componentAt(slots, 0)));
        var sources = List.of(supply(stack("material", "network"), 1, true),
                supply(stack("material", "backpack"), 1, false));

        assertEquals(0, fill(recipe, networkFirst, sources, CraftingFillEngine.Priority.NETWORK_FIRST, false, false));
        assertEquals(0, fill(recipe, backpackFirst, sources, CraftingFillEngine.Priority.BACKPACK_FIRST, false, false));
        assertEquals("network", networkFirst.getItem(0).components());
        assertEquals("backpack", backpackFirst.getItem(0).components());
    }

    @Test
    void keepsComponentVariantsAsDistinctStocks() {
        var recipe = shapeless(component("red"), component("blue"));
        FakeGrid grid = grid(slots -> "red".equals(componentAt(slots, 0)) && "blue".equals(componentAt(slots, 1)));

        int missing = fill(recipe, grid,
                List.of(supply(stack("dye", "red"), 1, false), supply(stack("dye", "blue"), 1, false)),
                CraftingFillEngine.Priority.BACKPACK_FIRST, false, false);

        assertEquals(0, missing);
        assertEquals("red", grid.getItem(0).components());
        assertEquals("blue", grid.getItem(1).components());
    }

    @Test
    void refusesAnIncompatibleGridWithoutExtractingAnything() {
        var recipe = shaped(2, 1, ingredient("a"), ingredient("b"));
        FakeGrid grid = grid(ignored -> false);
        grid.setItem(0, stack("unrelated"));
        FakeSupply source = supply(stack("a"), 1, false);

        int missing = fill(recipe, grid, List.of(source), CraftingFillEngine.Priority.NETWORK_FIRST, false, false);

        assertEquals(-1, missing);
        assertEquals(0, source.extractionAttempts);
    }

    @Test
    void recalculatesAfterAnEstimatedSourceRunsShort() {
        var recipe = shapeless(ingredient("a"));
        FakeGrid grid = grid(slots -> itemAt(slots, 0, "a"));
        FakeSupply shortRemote = new FakeSupply(stack("a"), 5, true, requested -> EMPTY);
        FakeSupply local = supply(stack("a"), 1, false);

        int missing = fill(recipe, grid, List.of(shortRemote, local),
                CraftingFillEngine.Priority.NETWORK_FIRST, false, false);

        assertEquals(0, missing);
        assertEquals(1, shortRemote.extractionAttempts);
        assertEquals(1, local.extracted);
        assertEquals("a", grid.getItem(0).item());
    }

    @Test
    void shiftTransferFillsTheLargestBatchWithinGridAndStackLimits() {
        var recipe = shapeless(ingredient("a"));
        FakeGrid grid = grid(slots -> slots.get(0).count() == 16);
        grid.maxStackSize = 16;
        FakeSupply source = supply(stack("a"), 80, true);

        int missing = fill(recipe, grid, List.of(source), CraftingFillEngine.Priority.NETWORK_FIRST, true, true);

        assertEquals(0, missing);
        assertEquals(16, grid.getItem(0).count());
        assertEquals(16, source.extracted);
    }

    @Test
    void shiftTransferFallsBackToOnePartialFillWhenNoWholeCraftIsAvailable() {
        var recipe = shapeless(ingredient("a"), ingredient("b"));
        FakeGrid grid = grid(slots -> false);
        FakeSupply source = supply(stack("a"), 1, true);

        int missing = fill(recipe, grid, List.of(source), CraftingFillEngine.Priority.NETWORK_FIRST, true, true);

        assertEquals(1, missing);
        assertEquals("a", grid.getItem(0).item());
        assertEquals(1, source.extracted);
    }

    @Test
    void recoversAnItemRejectedAfterExtraction() {
        var recipe = shapeless(ingredient("a"));
        FakeGrid grid = grid(ignored -> false);
        FakeSupply source = supply(stack("a"), 1, true, requested -> {
            grid.rejectNewItems = true;
            return stack("a", "", requested);
        });

        int missing = fill(recipe, grid, List.of(source), CraftingFillEngine.Priority.NETWORK_FIRST, false, false);

        assertEquals(1, missing);
        assertTrue(grid.getItem(0).count() == 0);
        assertEquals(1, source.recovered.size());
        assertEquals("a", source.recovered.get(0).item());
    }

    @Test
    void recoversAnUnexpectedStackInsteadOfInsertingIt() {
        var recipe = shapeless(ingredient("a"));
        FakeGrid grid = grid(ignored -> false);
        FakeSupply source = supply(stack("a"), 1, true, requested -> stack("other", "", requested));

        int missing = fill(recipe, grid, List.of(source), CraftingFillEngine.Priority.NETWORK_FIRST, false, false);

        assertEquals(1, missing);
        assertEquals(1, source.recovered.size());
        assertEquals("other", source.recovered.get(0).item());
    }

    @Test
    void includesPlayerInventoryOnlyWhenRequestedByTheViewerAdapter() {
        var recipe = shapeless(ingredient("a"));
        FakeGrid grid = grid(slots -> itemAt(slots, 0, "a"));
        FakeSupply player = supply(stack("a"), 1, false);
        boolean[] included = {false};
        CraftingFillEngine.SourceProvider<Stack> provider = (eligible, includePlayer, maxTransfer) -> {
            included[0] = includePlayer;
            return includePlayer && eligible.test(player.exemplar()) ? List.of(player.asSupply()) : List.of();
        };

        int missing = CraftingFillEngine.fill(recipe, grid, STACKS, provider,
                CraftingFillEngine.Priority.NETWORK_FIRST, true, false);

        assertEquals(0, missing);
        assertTrue(included[0]);
        assertEquals(1, player.extracted);
    }

    private static int fill(CraftingFillEngine.Recipe<Stack> recipe, FakeGrid grid, List<FakeSupply> supplies,
            CraftingFillEngine.Priority priority, boolean includePlayerInventory, boolean maxTransfer) {
        return CraftingFillEngine.fill(recipe, grid, STACKS,
                (eligible, includePlayer, batchTransfer) -> supplies.stream()
                        .filter(supply -> eligible.test(supply.exemplar()))
                        .map(FakeSupply::asSupply)
                        .toList(),
                priority, includePlayerInventory, maxTransfer);
    }

    private static CraftingFillEngine.Recipe<Stack> shaped(int width, int height,
            CraftingFillEngine.Ingredient<Stack>... ingredients) {
        return new CraftingFillEngine.Recipe<>(true, width, height, Arrays.asList(ingredients));
    }

    private static CraftingFillEngine.Recipe<Stack> shapeless(CraftingFillEngine.Ingredient<Stack>... ingredients) {
        return new CraftingFillEngine.Recipe<>(false, 0, 0, List.of(ingredients));
    }

    private static CraftingFillEngine.Ingredient<Stack> ingredient(String item) {
        return stack -> item.equals(stack.item());
    }

    private static CraftingFillEngine.Ingredient<Stack> component(String component) {
        return stack -> "dye".equals(stack.item()) && component.equals(stack.components());
    }

    private static FakeGrid grid(Predicate<List<Stack>> complete) {
        return new FakeGrid(complete);
    }

    private static boolean itemAt(List<Stack> slots, int index, String item) {
        return item.equals(slots.get(index).item());
    }

    private static String componentAt(List<Stack> slots, int index) {
        return slots.get(index).components();
    }

    private static Stack stack(String item) {
        return stack(item, "", 1);
    }

    private static Stack stack(String item, String components) {
        return stack(item, components, 1);
    }

    private static Stack stack(String item, String components, int count) {
        return new Stack(item, components, count, 64);
    }

    private record Stack(String item, String components, int count, int maxStackSize) {
    }

    @FunctionalInterface
    private interface Extractor {
        Stack extract(int requested);
    }

    private static FakeSupply supply(Stack exemplar, int amount, boolean remote) {
        return supply(exemplar, amount, remote, null);
    }

    private static FakeSupply supply(Stack exemplar, int amount, boolean remote, Extractor extractor) {
        return new FakeSupply(exemplar, amount, remote, extractor);
    }

    private static final class FakeSupply {
        private final Stack exemplar;
        private final int amount;
        private final boolean remote;
        private final Extractor override;
        private final List<Stack> recovered = new ArrayList<>();
        private int extractionAttempts;
        private int extracted;

        private FakeSupply(Stack exemplar, int amount, boolean remote, Extractor override) {
            this.exemplar = STACKS.copyWithCount(exemplar, 1);
            this.amount = amount;
            this.remote = remote;
            this.override = override;
        }

        private Stack exemplar() {
            return exemplar;
        }

        private CraftingFillEngine.Supply<Stack> asSupply() {
            return new CraftingFillEngine.Supply<>(exemplar, amount, remote, this::extract, recovered::add);
        }

        private Stack extract(int requested) {
            extractionAttempts++;
            if (override != null) {
                Stack stack = override.extract(requested);
                if (stack != null && stack.count() > 0) {
                    extracted += stack.count();
                }
                return stack;
            }
            extracted += requested;
            return STACKS.copyWithCount(exemplar, requested);
        }
    }

    private static final class FakeGrid implements CraftingFillEngine.Grid<Stack> {
        private final List<Stack> items = new ArrayList<>(java.util.Collections.nCopies(9, EMPTY));
        private final Predicate<List<Stack>> complete;
        private final Set<Integer> rejectedSlots = new HashSet<>();
        private boolean rejectNewItems;
        private int maxStackSize = 64;

        private FakeGrid(Predicate<List<Stack>> complete) {
            this.complete = complete;
        }

        @Override
        public Stack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public int getMaxStackSize() {
            return maxStackSize;
        }

        @Override
        public boolean canPlaceItem(int slot, Stack stack) {
            return !rejectNewItems && !rejectedSlots.contains(slot);
        }

        @Override
        public void setItem(int slot, Stack stack) {
            items.set(slot, stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean matchesRecipe() {
            return complete.test(List.copyOf(items));
        }
    }
}
