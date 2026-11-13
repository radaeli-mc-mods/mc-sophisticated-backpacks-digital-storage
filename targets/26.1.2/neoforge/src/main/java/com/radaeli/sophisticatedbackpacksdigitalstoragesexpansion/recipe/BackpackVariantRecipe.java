package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
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

    public BackpackVariantRecipe(Ingredient marker, BackpackVariant variant) {
        super();
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
    public ItemStack assemble(CraftingInput input) {
        ItemStack source = backpack(input);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copyWithCount(1);
        // Unlike 1.21.1, Sophisticated 26.1.2 stores the colors as given; its defaults and dye results are opaque ARGB.
        BackpackItem.setColors(result, 0xFF000000 | variant.mainColor(), 0xFF000000 | variant.accentColor());
        return result;
    }

    @Override
    public RecipeSerializer<BackpackVariantRecipe> getSerializer() {
        return ModRecipes.BACKPACK_VARIANT.get();
    }

    public static RecipeSerializer<BackpackVariantRecipe> serializer() {
        MapCodec<BackpackVariantRecipe> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(BackpackVariantRecipe::marker),
                BackpackVariant.CODEC.fieldOf("variant").forGetter(BackpackVariantRecipe::variant)
        ).apply(instance, BackpackVariantRecipe::new));
        StreamCodec<RegistryFriendlyByteBuf, BackpackVariantRecipe> streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, BackpackVariantRecipe::marker,
                BackpackVariant.STREAM_CODEC, BackpackVariantRecipe::variant,
                BackpackVariantRecipe::new);
        return new RecipeSerializer<>(codec, streamCodec);
    }
}
