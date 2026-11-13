package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

import static org.junit.jupiter.api.Assertions.*;

class BackpackVariantRecipeTest {
    private static final int MAIN = BackpackVariant.AE2.mainColor();
    private static final int ACCENT = BackpackVariant.AE2.accentColor();

    private final BackpackVariantRecipe recipe = new BackpackVariantRecipe(
            CraftingBookCategory.MISC, Ingredient.of(Items.AMETHYST_SHARD), BackpackVariant.AE2);

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static ItemStack contentfulBackpack() {
        ItemStack backpack = new ItemStack(ModItems.IRON_BACKPACK.get());
        backpack.set(ModCoreDataComponents.STORAGE_UUID.get(), UUID.fromString("11111111-2222-3333-4444-555555555555"));
        backpack.set(DataComponents.CUSTOM_NAME, Component.literal("Expedition pack"));
        CompoundTag tag = new CompoundTag();
        tag.putString("other_addon", "keep");
        backpack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BackpackItem.setColors(backpack, 0x123456, 0x654321);
        return backpack;
    }

    private static CraftingInput grid(ItemStack... stacks) {
        ItemStack[] slots = new ItemStack[9];
        java.util.Arrays.fill(slots, ItemStack.EMPTY);
        System.arraycopy(stacks, 0, slots, 0, stacks.length);
        return CraftingInput.of(3, 3, List.of(slots));
    }

    @Test
    void recolorKeepsEveryComponentExceptTheTwoColors() {
        ItemStack source = contentfulBackpack();
        ItemStack result = recipe.assemble(grid(source, new ItemStack(Items.AMETHYST_SHARD)), null);

        assertEquals(1, result.getCount());
        assertSame(source.getItem(), result.getItem());
        assertEquals(source.get(ModCoreDataComponents.STORAGE_UUID.get()), result.get(ModCoreDataComponents.STORAGE_UUID.get()));
        assertEquals(source.get(DataComponents.CUSTOM_NAME), result.get(DataComponents.CUSTOM_NAME));
        assertEquals(source.get(DataComponents.CUSTOM_DATA), result.get(DataComponents.CUSTOM_DATA));
        assertEquals(MAIN | 0xFF000000, BackpackItem.getMainColor(result));
        assertEquals(ACCENT | 0xFF000000, BackpackItem.getAccentColor(result));

        // Every component other than the two colors must be identical to the source stack.
        ItemStack expected = source.copy();
        BackpackItem.setColors(expected, MAIN, ACCENT);
        assertTrue(ItemStack.isSameItemSameComponents(expected, result));
        // The ingredient stack itself is never mutated by assemble.
        assertEquals(0x123456 | 0xFF000000, BackpackItem.getMainColor(source));
    }

    @Test
    void matchesOnlyOneBackpackPlusOneMarker() {
        ItemStack backpack = contentfulBackpack();
        ItemStack marker = new ItemStack(Items.AMETHYST_SHARD);
        assertTrue(recipe.matches(grid(backpack, marker), null));
        assertTrue(recipe.matches(CraftingInput.of(2, 2, List.of(marker, ItemStack.EMPTY, ItemStack.EMPTY, backpack)), null));

        assertFalse(recipe.matches(grid(backpack), null));
        assertFalse(recipe.matches(grid(marker), null));
        assertFalse(recipe.matches(grid(backpack, marker, marker), null));
        assertFalse(recipe.matches(grid(backpack, backpack.copy(), marker), null));
        assertFalse(recipe.matches(grid(backpack, marker, new ItemStack(Items.RED_DYE)), null));
        assertFalse(recipe.matches(grid(backpack, new ItemStack(Items.RED_DYE)), null));
    }

    @Test
    void recipeJsonRoundTripsThroughItsSerializerCodec() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        JsonObject json = JsonParser.parseString("""
                {"ingredient": {"item": "minecraft:amethyst_shard"}, "variant": "ae2"}
                """).getAsJsonObject();
        BackpackVariantRecipe parsed = new BackpackVariantRecipe.Serializer().codec().codec().parse(ops, json).getOrThrow();
        assertEquals(BackpackVariant.AE2, parsed.variant());
        assertTrue(parsed.marker().test(new ItemStack(Items.AMETHYST_SHARD)));
    }

    @Test
    void packagedAe2RecipeIsConditionalAndUsesFluixCrystal() throws Exception {
        String path = "/data/" + SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + "/recipe/backpack_variant_ae2.json";
        try (var input = getClass().getResourceAsStream(path);
                var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + ":backpack_variant", json.get("type").getAsString());
            assertEquals("ae2", json.getAsJsonArray("neoforge:conditions").get(0).getAsJsonObject().get("modid").getAsString());
            assertEquals("ae2:fluix_crystal", json.getAsJsonObject("ingredient").get("item").getAsString());
            assertEquals("ae2", json.get("variant").getAsString());
        }
    }

    @Test
    void everyVariantHasUniqueColorsTexturesAndAConditionalRecipe() throws Exception {
        var seen = new java.util.HashSet<Integer>();
        for (BackpackVariant variant : BackpackVariant.values()) {
            assertTrue(seen.add((variant.mainColor() << 8) ^ variant.accentColor()), variant + " reuses a color pair");
            assertEquals(variant, BackpackVariant.fromColors(variant.mainColor(), variant.accentColor()).orElseThrow());
            for (String layer : new String[] {"cloth", "border", "clips"}) {
                String texture = "/assets/" + SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID + "/textures/"
                        + variant.texture(layer).getPath() + ".png";
                assertNotNull(getClass().getResource(texture), "missing " + texture);
            }
            String path = "/data/" + SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID
                    + "/recipe/backpack_variant_" + variant.getSerializedName() + ".json";
            try (var input = getClass().getResourceAsStream(path)) {
                assertNotNull(input, "missing " + path);
                JsonObject json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals(variant.getSerializedName(), json.get("variant").getAsString());
                var conditions = json.getAsJsonArray("neoforge:conditions");
                assertEquals(1, conditions.size());
                assertEquals("neoforge:mod_loaded", conditions.get(0).getAsJsonObject().get("type").getAsString());
                assertEquals(variant.modId(), conditions.get(0).getAsJsonObject().get("modid").getAsString());
                assertTrue(json.getAsJsonObject("ingredient").has("item"));
            }
        }
    }

    @Test
    void variantIsRecognizedFromItsReservedColorsOnly() {
        ItemStack result = recipe.assemble(grid(contentfulBackpack(), new ItemStack(Items.AMETHYST_SHARD)), null);
        assertEquals(BackpackVariant.AE2, BackpackVariant.fromStack(result).orElseThrow());
        // Dyeing afterwards changes the colors, which drops the skin.
        BackpackItem.setColors(result, 0x123456, ACCENT);
        assertTrue(BackpackVariant.fromStack(result).isEmpty());
        assertTrue(BackpackVariant.fromStack(contentfulBackpack()).isEmpty());
        assertTrue(BackpackVariant.fromStack(new ItemStack(Items.STICK)).isEmpty());
    }
}
