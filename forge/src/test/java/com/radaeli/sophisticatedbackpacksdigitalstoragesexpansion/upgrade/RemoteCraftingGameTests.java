package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.ModItems;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.SourceLocation;

@GameTestHolder(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RemoteCraftingGameTests {
    private RemoteCraftingGameTests() {
    }

    @GameTest(template = "empty")
    public static void remoteCraftingConversion(GameTestHelper helper) {
        var level = helper.getLevel();
        var id = ResourceLocation.tryParse(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + ":remote_crafting_upgrade");
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(id).orElseThrow();
        var inventory = new ItemStackHandler(9);
        var grid = new CraftingItemHandler(() -> inventory, ignored -> {});
        ItemStack upgrade = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CRAFTING_UPGRADE.get());
        upgrade.setHoverName(Component.literal("Named workbench"));
        ItemStack ingredient = new ItemStack(Items.PAPER, 3);
        ingredient.setHoverName(Component.literal("Preserved ingredient"));
        var nativeGrid = new ItemStackHandler(9);
        nativeGrid.setStackInSlot(7, ingredient);
        upgrade.getOrCreateTag().put("craftingInventory", nativeGrid.serializeNBT());
        upgrade.getOrCreateTag().putBoolean("shiftClickIntoStorage", false);
        grid.setItem(0, new ItemStack(Items.ENDER_PEARL));
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        grid.setItem(2, new ItemStack(Items.ENDER_PEARL));
        grid.setItem(3, new ItemStack(Items.DIAMOND));
        grid.setItem(4, upgrade);
        grid.setItem(5, new ItemStack(Items.DIAMOND));
        grid.setItem(6, new ItemStack(Items.REDSTONE));
        grid.setItem(8, new ItemStack(Items.REDSTONE));
        helper.assertTrue(recipe.matches(grid, level), "Crafting conversion must match the P07 recipe");
        ItemStack result = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(result.is(ModItems.REMOTE_CRAFTING_UPGRADE.get()), "Conversion must yield the remote upgrade");
        helper.assertTrue(upgrade.getTag().equals(result.getTag()), "Conversion must preserve the name, grid and controls");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void partialShapedFillUsesOnlyTheDeficit(GameTestHelper helper) {
        var fixture = fixture(helper);
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager()
                .byKey(ResourceLocation.tryParse("minecraft:wooden_pickaxe")).orElseThrow();
        var inventory = fixture.storage().getInventoryHandler();
        fixture.grid().setItem(4, new ItemStack(Items.STICK, 4));
        inventory.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));

        helper.assertTrue(fixture.fill(recipe) == 2, "Two slots must still need ingredients");
        helper.assertTrue(fixture.grid().getItem(4).getCount() == 4, "The existing stack must survive");
        helper.assertTrue(inventory.getStackInSlot(0).isEmpty(), "Both available planks must move to the grid");

        inventory.setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 8));
        inventory.setStackInSlot(1, new ItemStack(Items.STICK, 8));
        helper.assertTrue(fixture.fill(recipe) == 0, "The second request must complete the recipe");
        helper.assertTrue(recipe.matches(fixture.grid(), helper.getLevel()), "The native grid must match the recipe");
        helper.assertTrue(inventory.getStackInSlot(0).getCount() == 7, "Only one extra plank is needed");
        helper.assertTrue(inventory.getStackInSlot(1).getCount() == 7, "Only one extra stick is needed");
        helper.assertTrue(fixture.fill(recipe) == 0 && inventory.getStackInSlot(0).getCount() == 7,
                "A complete grid must stay as it is");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shapelessFillPreservesExistingIngredientAndRejectsIncompatibleGrid(GameTestHelper helper) {
        var fixture = fixture(helper);
        var recipe = new ShapelessRecipe(ResourceLocation.tryParse("test:overlapping_ingredients"), "",
                CraftingBookCategory.MISC, new ItemStack(Items.CHEST), NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(Items.OAK_PLANKS, Items.BIRCH_PLANKS), Ingredient.of(Items.OAK_PLANKS)));
        var inventory = fixture.storage().getInventoryHandler();
        inventory.setStackInSlot(0, new ItemStack(Items.BIRCH_PLANKS, 2));
        fixture.grid().setItem(8, new ItemStack(Items.STONE));
        helper.assertTrue(fixture.fill(recipe) == -1 && inventory.getStackInSlot(0).getCount() == 2,
                "An incompatible grid must not extract anything");

        fixture.grid().setItem(8, new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(fixture.fill(recipe) == 0, "The existing oak plank must fill the restricted ingredient");
        helper.assertTrue(fixture.grid().getItem(0).is(Items.BIRCH_PLANKS)
                && fixture.grid().getItem(8).is(Items.OAK_PLANKS), "The birch plank must go into the first free slot");
        helper.assertTrue(recipe.matches(fixture.grid(), helper.getLevel()), "The native grid must match the recipe");
        helper.succeed();
    }

    private static Fixture fixture(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var storage = new BackpackWrapper(new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get()));
        var wrapper = new RemoteCraftingUpgradeWrapper(storage, new ItemStack(ModItems.REMOTE_CRAFTING_UPGRADE.get()), ignored -> {});
        return new Fixture(storage, wrapper, new CraftingItemHandler(wrapper::getInventory, ignored -> {}),
                new UpgradeOperationContext(player, storage, SourceLocation.carriedBy(player)));
    }

    private record Fixture(BackpackWrapper storage, RemoteCraftingUpgradeWrapper wrapper, CraftingItemHandler grid,
            UpgradeOperationContext context) {
        int fill(CraftingRecipe recipe) {
            return RemoteCraftingFill.fill(context, wrapper, grid, recipe);
        }
    }
}
