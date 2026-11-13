package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import java.util.UUID;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingItemHandler;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

@GameTestHolder(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BackpackVariantGameTests {
    private BackpackVariantGameTests() {
    }

    private static ItemStack contentfulBackpack() {
        ItemStack backpack = new ItemStack(net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.IRON_BACKPACK.get());
        backpack.setHoverName(Component.literal("Expedition pack"));
        CompoundTag tag = backpack.getOrCreateTag();
        tag.putUUID("contentsUuid", UUID.fromString("11111111-2222-3333-4444-555555555555"));
        tag.putString("other_addon", "keep");
        BackpackItem.setColors(backpack, 0x123456, 0x654321);
        return backpack;
    }

    private static CraftingItemHandler grid(ItemStack... stacks) {
        var inventory = new ItemStackHandler(9);
        var grid = new CraftingItemHandler(() -> inventory, ignored -> {});
        for (int slot = 0; slot < stacks.length; slot++) grid.setItem(slot, stacks[slot]);
        return grid;
    }

    @GameTest(template = "empty")
    public static void recolorKeepsEveryTagExceptTheTwoColors(GameTestHelper helper) {
        var recipe = new BackpackVariantRecipe(new ResourceLocation(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, "test"),
                CraftingBookCategory.MISC, Ingredient.of(Items.AMETHYST_SHARD), BackpackVariant.AE2);
        ItemStack source = contentfulBackpack();
        var crafting = grid(source, new ItemStack(Items.AMETHYST_SHARD));
        helper.assertTrue(recipe.matches(crafting, helper.getLevel()), "Backpack + marker must match");
        ItemStack result = recipe.assemble(crafting, helper.getLevel().registryAccess());

        helper.assertTrue(result.getCount() == 1 && result.getItem() == source.getItem(), "Result is one backpack of the same tier");
        CompoundTag expected = source.getTag().copy();
        expected.putInt("clothColor", BackpackVariant.AE2.mainColor());
        expected.putInt("borderColor", BackpackVariant.AE2.accentColor());
        helper.assertTrue(expected.equals(result.getTag()), "Contents UUID, name and foreign tags must survive; only colors change");
        helper.assertTrue(BackpackVariant.fromStack(result).orElse(null) == BackpackVariant.AE2, "Result must be recognized as the AE2 variant");
        helper.assertTrue(source.getTag().getInt("clothColor") == 0x123456, "The ingredient stack must not be mutated");

        BackpackItem.setColors(result, 0x123456, BackpackVariant.AE2.accentColor());
        helper.assertTrue(BackpackVariant.fromStack(result).isEmpty(), "Dyeing afterwards must drop the skin");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matchesOnlyOneBackpackPlusOneMarker(GameTestHelper helper) {
        var recipe = new BackpackVariantRecipe(new ResourceLocation(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, "test"),
                CraftingBookCategory.MISC, Ingredient.of(Items.AMETHYST_SHARD), BackpackVariant.AE2);
        var level = helper.getLevel();
        ItemStack backpack = contentfulBackpack();
        ItemStack marker = new ItemStack(Items.AMETHYST_SHARD);
        helper.assertTrue(!recipe.matches(grid(backpack), level), "Backpack alone must not match");
        helper.assertTrue(!recipe.matches(grid(marker), level), "Marker alone must not match");
        helper.assertTrue(!recipe.matches(grid(backpack, marker, marker), level), "Two markers must not match");
        helper.assertTrue(!recipe.matches(grid(backpack, backpack.copy(), marker), level), "Two backpacks must not match");
        helper.assertTrue(!recipe.matches(grid(backpack, marker, new ItemStack(Items.RED_DYE)), level), "Extra items must not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void variantRecipesLoadOnlyWithTheirMods(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (BackpackVariant variant : BackpackVariant.values()) {
            var id = new ResourceLocation(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, "backpack_variant_" + variant.id());
            boolean loaded = ModList.get().isLoaded(variant.modId());
            helper.assertTrue(manager.byKey(id).isPresent() == loaded,
                    "Recipe " + id + " must load exactly when " + variant.modId() + " is present");
            helper.assertTrue(variant.isAvailable() == loaded, "isAvailable must follow the mod list");
        }
        helper.succeed();
    }
}
