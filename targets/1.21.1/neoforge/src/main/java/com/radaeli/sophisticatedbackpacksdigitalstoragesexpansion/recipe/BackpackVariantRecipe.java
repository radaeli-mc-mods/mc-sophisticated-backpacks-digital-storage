package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/**
 * Recolors a Sophisticated backpack to a network {@link BackpackVariant} palette by crafting it with one marker item.
 *
 * <p>Follows the same flow as Sophisticated's own {@code BackpackDyeRecipe}: the result is a one-count copy of the
 * ingredient backpack (so contents UUID, upgrades, name and every other data component survive untouched) and only
 * the two color components are rewritten through {@link BackpackItem#setColors}.</p>
 */
public final class BackpackVariantRecipe extends CustomRecipe {
    private final Ingredient marker;
    private final BackpackVariant variant;

    public BackpackVariantRecipe(CraftingBookCategory category, Ingredient marker, BackpackVariant variant) {
        super(category);
        this.marker = marker;
        this.variant = variant;
    }

    public Ingredient marker() {
        return marker;
    }

    public BackpackVariant variant() {
        return variant;
    }

    /** Returns the single backpack in the grid, or empty when the grid is not exactly one backpack + one marker. */
    private ItemStack backpack(CraftingInput input) {
        ItemStack backpack = ItemStack.EMPTY;
        boolean hasMarker = false;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BackpackItem) {
                if (!backpack.isEmpty()) return ItemStack.EMPTY;
                backpack = stack;
            } else if (marker.test(stack)) {
                if (hasMarker) return ItemStack.EMPTY;
                hasMarker = true;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return hasMarker ? backpack : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !backpack(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack source = backpack(input);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copyWithCount(1);
        BackpackItem.setColors(result, variant.mainColor(), variant.accentColor());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return ModRecipes.BACKPACK_VARIANT.get();
    }

    public static final class Serializer implements RecipeSerializer<BackpackVariantRecipe> {
        private static final MapCodec<BackpackVariantRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BackpackVariantRecipe::marker),
                BackpackVariant.CODEC.fieldOf("variant").forGetter(BackpackVariantRecipe::variant)
        ).apply(instance, (marker, variant) -> new BackpackVariantRecipe(CraftingBookCategory.MISC, marker, variant)));

        private static final StreamCodec<RegistryFriendlyByteBuf, BackpackVariantRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, BackpackVariantRecipe::marker,
                BackpackVariant.STREAM_CODEC, BackpackVariantRecipe::variant,
                (marker, variant) -> new BackpackVariantRecipe(CraftingBookCategory.MISC, marker, variant));

        @Override
        public MapCodec<BackpackVariantRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackVariantRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
