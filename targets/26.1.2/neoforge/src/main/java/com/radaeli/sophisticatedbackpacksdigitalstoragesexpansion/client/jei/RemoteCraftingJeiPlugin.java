package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.jei;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.registration.IRecipeRegistration;


import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.RecipeTransferRequest;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.RecipeViewerAdapter;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.RecipeViewerIntegrationRegistry;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteCraftingUpgradeContainer;

@JeiPlugin
public final class RemoteCraftingJeiPlugin implements IModPlugin {
    private static final String ADAPTER_ID = "jei";
    private static final Identifier UID = Identifier.fromNamespaceAndPath(
            "sophisticated_backpacks_digital_storages_expansion", "remote_crafting_jei");

    private volatile IJeiRuntime runtime;
    private RemoteCraftingRecipeTransferHandler transferHandler;

    private final RecipeViewerAdapter adapter = new RecipeViewerAdapter() {
        @Override
        public String id() {
            return ADAPTER_ID;
        }

        @Override
        public boolean isReadyFor(Object menu) {
            IJeiRuntime activeRuntime = runtime;
            RemoteCraftingRecipeTransferHandler activeHandler = transferHandler;
            if (activeRuntime == null || activeHandler == null || !(menu instanceof BackpackContainer backpack)) {
                return false;
            }
            var category = activeRuntime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
            return category != null && activeRuntime.getRecipeTransferManager()
                    .getRecipeTransferHandler(backpack, category)
                    .filter(selected -> selected == activeHandler).isPresent();
        }
    };

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Sophisticated's own dye recipes are shown by its plugin; ours are custom recipes JEI cannot discover.
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        for (var variant : com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.variant.BackpackVariant.values()) {
            if (!variant.isAvailable()) continue;
            var markerId = Identifier.parse(variant.markerItemId());
            var markerItem = BuiltInRegistries.ITEM.getOptional(markerId);
            if (markerItem.isEmpty()) continue;
            var base = net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.BACKPACK.get();
            ItemStack result = variant.stack(base);
            var backpack = Ingredient.of(base);
            var marker = Ingredient.of(markerItem.get());
            var ingredients = List.of(backpack, marker);
            var id = Identifier.fromNamespaceAndPath(com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID, "jei/backpack_variant_" + variant.getSerializedName());
            recipes.add(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, id), new ShapelessRecipe(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, ""), net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result), ingredients)));
        }
        if (!recipes.isEmpty()) registration.addRecipes(RecipeTypes.CRAFTING, recipes);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper transferHelper = registration.getTransferHelper();
        IStackHelper stackHelper = registration.getJeiHelpers().getStackHelper();
        transferHandler = new RemoteCraftingRecipeTransferHandler(transferHelper, stackHelper);
        registration.addRecipeTransferHandler(transferHandler, RecipeTypes.CRAFTING);
        RecipeViewerIntegrationRegistry.register(adapter);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private static final class RemoteCraftingRecipeTransferHandler extends
            JeiCraftingContainerRecipeTransferHandlerBase<BackpackContainer, RecipeHolder<CraftingRecipe>> {
        private RemoteCraftingRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
            super(transferHelper, stackHelper);
        }

        @Override
        public Class<BackpackContainer> getContainerClass() {
            return BackpackContainer.class;
        }

        @Override
        public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public IRecipeTransferError transferRecipe(BackpackContainer menu, RecipeHolder<CraftingRecipe> recipe,
                IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            Optional<RemoteCraftingUpgradeContainer> remote = menu
                    .getOpenOrFirstCraftingContainer(recipe.value().getType())
                    .filter(RemoteCraftingUpgradeContainer.class::isInstance)
                    .map(RemoteCraftingUpgradeContainer.class::cast);

            if (remote.isEmpty() || remote.get().getEndpoint().isEmpty()) {
                return super.transferRecipe(menu, recipe, recipeSlots, player, maxTransfer, doTransfer);
            }

            if (maxTransfer) {
                IRecipeTransferError validation = super.transferRecipe(menu, recipe, recipeSlots, player, true, false);
                if (validation != null && validation.getType() != IRecipeTransferError.Type.USER_FACING) {
                    return validation;
                }
                if (doTransfer) {
                    remote.get().requestRecipeTransfer(new RecipeTransferRequest(recipe.id().identifier().toString(),
                            RecipeTransferRequest.Mode.MAX));
                }
                return null;
            }

            IRecipeTransferError nativeError = super.transferRecipe(menu, recipe, recipeSlots, player,
                    false, doTransfer);
            if (nativeError == null || nativeError.getType() != IRecipeTransferError.Type.USER_FACING) {
                return nativeError;
            }
            if (doTransfer) {
                remote.get().requestRecipeTransfer(new RecipeTransferRequest(recipe.id().identifier().toString(),
                        RecipeTransferRequest.Mode.SINGLE));
            }
            return null;
        }
    }
}
