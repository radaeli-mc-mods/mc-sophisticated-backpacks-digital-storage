package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import org.jetbrains.annotations.Nullable;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant;

/**
 * Recolors a Sophisticated backpack to a network {@link BackpackVariant} palette by crafting it with one marker item.
 *
 * <p>Follows the same flow as Sophisticated's own {@code BackpackDyeRecipe}: the result is a one-count copy of the
 * ingredient backpack (so contents UUID, upgrades, name and every other tag survive untouched) and only the two color
 * tags are rewritten through {@link BackpackItem#setColors}, which is what the wrapper's own {@code setColors} calls.</p>
 */
public final class BackpackVariantRecipe extends CustomRecipe {
    private final Ingredient marker;
    private final BackpackVariant variant;

    public BackpackVariantRecipe(ResourceLocation id, CraftingBookCategory category, Ingredient marker, BackpackVariant variant) {
        super(id, category);
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
    private ItemStack backpack(CraftingContainer input) {
        ItemStack backpack = ItemStack.EMPTY;
        boolean hasMarker = false;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
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
    public boolean matches(CraftingContainer input, Level level) {
        return !backpack(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BACKPACK_VARIANT.get();
    }

    public static final class Serializer implements RecipeSerializer<BackpackVariantRecipe> {
        @Override
        public BackpackVariantRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient marker = Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient"));
            String variantId = GsonHelper.getAsString(json, "variant");
            BackpackVariant variant = BackpackVariant.byId(variantId)
                    .orElseThrow(() -> new JsonSyntaxException("Unknown backpack variant " + variantId));
            return new BackpackVariantRecipe(id, CraftingBookCategory.MISC, marker, variant);
        }

        @Nullable
        @Override
        public BackpackVariantRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient marker = Ingredient.fromNetwork(buffer);
            BackpackVariant variant = BackpackVariant.byId(buffer.readUtf(32))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown backpack variant in recipe " + id));
            return new BackpackVariantRecipe(id, CraftingBookCategory.MISC, marker, variant);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BackpackVariantRecipe recipe) {
            recipe.marker().toNetwork(buffer);
            buffer.writeUtf(recipe.variant().id(), 32);
        }
    }
}
