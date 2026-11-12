package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SBPButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeTab;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteCraftingRecipes;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteCraftingUpgradeContainer;

final class RemoteCraftingUpgradeTab extends CraftingUpgradeTab {
    static final String TEXT = "gui.sophisticated_backpacks_digital_storages_expansion.crafting.";
    private final RemoteCraftingUpgradeContainer remote;

    RemoteCraftingUpgradeTab(CraftingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, SBPButtonDefinitions.SHIFT_CLICK_TARGET, SBPButtonDefinitions.REFILL_CRAFTING_GRID);
        remote = (RemoteCraftingUpgradeContainer) container;
        addHideableChild(RemoteSourcePriorityButton.create(new Position(x + 4, y + 139),
                remote::getSourcePriority, remote::toggleSourcePriority));
        addHideableChild(LinkedStorageUpgradeTab.connectionLabel(new Position(x + 25, y + 144), remote::getEndpoint));
        addHideableChild(new Action(new Position(x + 4, y + 158), this::selectionLabel, () -> true, this::openSelector));
        addHideableChild(new Action(new Position(x + 4, y + 176), () -> Component.translatable(TEXT + "fetch"),
                () -> remote.getSelectedRecipe() != null, remote::requestIngredients));
    }

    private Component selectionLabel() {
        var id = remote.getSelectedRecipe();
        if (id == null) {
            return Component.translatable(TEXT + "choose");
        }
        var level = remote.getPlayer().level();
        return level.getRecipeManager().byKey(id)
                .map(recipe -> recipe.getResultItem(level.registryAccess()).getHoverName())
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private void openSelector() {
        var level = remote.getPlayer().level();
        List<CraftingRecipeSelector.Entry> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)
                .stream().filter(recipe -> RemoteCraftingRecipes.isSupported(recipe))
                .map(recipe -> new CraftingRecipeSelector.Entry(recipe.getId(),
                        recipe.getResultItem(level.registryAccess())))
                .filter(entry -> !entry.result().isEmpty() && entry.result().isItemEnabled(level.enabledFeatures()))
                .sorted(Comparator.comparing((CraftingRecipeSelector.Entry entry) -> entry.result().getHoverName().getString())
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
        screen.setModalOverlay(new CraftingRecipeSelector(recipes, entry -> {
            remote.selectRemoteRecipe(entry.id());
            screen.setModalOverlay(null);
        }));
    }

    private static final class Action extends ButtonBase {
        private final Supplier<Component> label;
        private final BooleanSupplier enabled;

        private Action(Position position, Supplier<Component> label, BooleanSupplier enabled, Runnable action) {
            super(position, new Dimension(112, 16), button -> {
                if (button == 0 && enabled.getAsBoolean()) {
                    action.run();
                }
            });
            this.label = label;
            this.enabled = enabled;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            graphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF373737);
            int color = enabled.getAsBoolean() && isMouseOver(mouseX, mouseY) ? 0xFF696969 : 0xFF505050;
            graphics.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, color);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            String text = font.plainSubstrByWidth(label.get().getString(), getWidth() - 8);
            graphics.drawCenteredString(font, text, x + getWidth() / 2, y + 4,
                    enabled.getAsBoolean() ? 0xFFFFFF : 0xAAAAAA);
        }

        @Override
        public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, label.get(), mouseX, mouseY);
            }
        }
    }
}
