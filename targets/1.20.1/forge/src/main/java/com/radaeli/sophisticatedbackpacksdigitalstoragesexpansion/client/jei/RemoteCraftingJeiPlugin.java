package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client.jei;

import java.util.Optional;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteCraftingUpgradeContainer;

@JeiPlugin
@OnlyIn(Dist.CLIENT)
public final class RemoteCraftingJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            "sophisticated_backpacks_digital_storages_expansion", "remote_crafting_jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper transferHelper = registration.getTransferHelper();
        IStackHelper stackHelper = registration.getJeiHelpers().getStackHelper();
        registration.addRecipeTransferHandler(
                new RemoteCraftingRecipeTransferHandler(transferHelper, stackHelper), RecipeTypes.CRAFTING);
    }

    private static final class RemoteCraftingRecipeTransferHandler extends
            JeiCraftingContainerRecipeTransferHandlerBase<BackpackContainer, CraftingRecipe> {
        private RemoteCraftingRecipeTransferHandler(IRecipeTransferHandlerHelper transferHelper, IStackHelper stackHelper) {
            super(transferHelper, stackHelper);
        }

        @Override
        public Class<BackpackContainer> getContainerClass() {
            return BackpackContainer.class;
        }

        @Override
        public RecipeType<CraftingRecipe> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public IRecipeTransferError transferRecipe(BackpackContainer menu, CraftingRecipe recipe,
                IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
            Optional<RemoteCraftingUpgradeContainer> remote = menu
                    .getOpenOrFirstCraftingContainer(recipe.getType())
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
                    remote.get().requestJeiIngredients(recipe.getId(), true);
                }
                return null;
            }

            IRecipeTransferError nativeError = super.transferRecipe(menu, recipe, recipeSlots, player,
                    false, doTransfer);
            if (nativeError == null || nativeError.getType() != IRecipeTransferError.Type.USER_FACING) {
                return nativeError;
            }
            if (doTransfer) {
                remote.get().requestJeiIngredients(recipe.getId(), false);
            }
            return null;
        }
    }
}
