package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameRules;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;

public final class RemoteCraftingUpgradeContainer extends CraftingUpgradeContainer {
    public static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> TYPE =
            new UpgradeContainerType<>(RemoteCraftingUpgradeContainer::new);
    private static final String RECIPE = "remoteCraftingRecipe";
    private static final String MENU = "remoteCraftingMenu";
    private static final String PRIORITY = "remoteCraftingPriority";
    private static final String MESSAGE = "message.sophisticated_backpacks_digital_storages_expansion.crafting.";

    private ResourceLocation selectedRecipe;

    private RemoteCraftingUpgradeContainer(Player player, int id, CraftingUpgradeWrapper wrapper,
            UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
        super(player, id, wrapper, type);
    }

    private RemoteCraftingUpgradeWrapper remoteWrapper() {
        return (RemoteCraftingUpgradeWrapper) getUpgradeWrapper();
    }

    public Optional<StorageEndpoint> getEndpoint() {
        return remoteWrapper().getEndpoint();
    }

    public SourcePriority getSourcePriority() {
        return remoteWrapper().getSourcePriority();
    }

    public void toggleSourcePriority() {
        SourcePriority priority = getSourcePriority().next();
        remoteWrapper().setSourcePriority(priority);
        CompoundTag data = request();
        data.putBoolean(PRIORITY, priority == SourcePriority.BACKPACK_FIRST);
        sendDataToServer(() -> data);
    }

    public ResourceLocation getSelectedRecipe() {
        return selectedRecipe;
    }

    public void selectRemoteRecipe(ResourceLocation recipeId) {
        selectedRecipe = recipeId;
    }

    public void requestIngredients() {
        if (selectedRecipe != null) {
            CompoundTag data = request();
            data.putString(RECIPE, selectedRecipe.toString());
            sendDataToServer(() -> data);
        }
    }

    private CompoundTag request() {
        CompoundTag data = new CompoundTag();
        data.putInt(MENU, player.containerMenu.containerId);
        return data;
    }

    @Override
    public void handlePacket(CompoundTag data) {
        if (!data.contains(RECIPE) && !data.contains(PRIORITY)) {
            super.handlePacket(data);
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.isAlive() || serverPlayer.isSpectator()
                || !data.contains(MENU, Tag.TAG_INT)
                || data.getInt(MENU) != player.containerMenu.containerId || !remoteWrapper().isEnabled()) {
            return;
        }
        UpgradeOperationContext.fromCurrentMenu(serverPlayer, this).ifPresent(context -> {
            if (data.contains(PRIORITY, Tag.TAG_BYTE)) {
                remoteWrapper().setSourcePriority(data.getBoolean(PRIORITY)
                        ? SourcePriority.BACKPACK_FIRST : SourcePriority.NETWORK_FIRST);
            } else if (data.contains(RECIPE, Tag.TAG_STRING)
                    && remoteWrapper().beginFill(serverPlayer.serverLevel().getGameTime())) {
                fill(context, data.getString(RECIPE));
            }
        });
    }

    private void fill(UpgradeOperationContext context, String recipeId) {
        ResourceLocation id = recipeId.length() <= 512 ? ResourceLocation.tryParse(recipeId) : null;
        ServerPlayer serverPlayer = context.player();
        if (id == null) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        var level = serverPlayer.serverLevel();
        var entry = level.getRecipeManager().byKey(id).orElse(null);
        if (!(entry != null && entry.value() instanceof CraftingRecipe recipe) || !RemoteCraftingRecipes.isSupported(recipe)
                || (level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING) && !serverPlayer.getRecipeBook().contains(entry))) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        var output = recipe.getResultItem(level.registryAccess());
        if (output.isEmpty() || !output.isItemEnabled(level.enabledFeatures())) {
            feedback(serverPlayer, "unavailable");
            return;
        }
        int missing = RemoteCraftingFill.fill(context, remoteWrapper(), getCraftMatrix(), recipe);
        if (missing < 0) {
            feedback(serverPlayer, "incompatible");
        } else if (missing == 0) {
            setRecipeUsed(id);
            feedback(serverPlayer, "ready");
        } else {
            feedback(serverPlayer, "missing", missing);
        }
    }

    private static void feedback(ServerPlayer player, String message, Object... arguments) {
        player.displayClientMessage(Component.translatable(MESSAGE + message, arguments), true);
    }
}
