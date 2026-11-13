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
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.config.CraftingClientConfig;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.ManualControlsPreference;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.crafting.RecipeViewerIntegrationRegistry;

final class RemoteCraftingUpgradeTab extends CraftingUpgradeTab {
    static final String TEXT = "gui.sophisticated_backpacks_digital_storages_expansion.crafting.";
    private final RemoteCraftingUpgradeContainer remote;
    private final Action manualControlsButton;
    private final Action chooseRecipeButton;
    private final Action fetchIngredientsButton;
    private final int compactHeight;
    private final int expandedHeight;
    private boolean manualControlsVisible;

    RemoteCraftingUpgradeTab(CraftingUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen, SBPButtonDefinitions.SHIFT_CLICK_TARGET, SBPButtonDefinitions.REFILL_CRAFTING_GRID);
        remote = (RemoteCraftingUpgradeContainer) container;
        addHideableChild(RemoteSourcePriorityButton.create(new Position(x + 4, y + 139),
                remote::getSourcePriority, remote::toggleSourcePriority));
        addHideableChild(LinkedStorageUpgradeTab.connectionLabel(new Position(x + 25, y + 144), remote::getEndpoint));
        manualControlsButton = addHideableChild(new Action(new Position(x + 4, y + 158),
                this::manualControlsLabel, () -> true, this::toggleManualControls));
        chooseRecipeButton = addHideableChildWithoutSizing(new Action(new Position(x + 4, y + 178),
                this::selectionLabel, () -> manualControlsVisible, () -> manualControlsVisible, this::openSelector));
        fetchIngredientsButton = addHideableChildWithoutSizing(new Action(new Position(x + 4, y + 196),
                () -> Component.translatable(TEXT + "fetch"),
                () -> manualControlsVisible && remote.getSelectedRecipe() != null,
                () -> manualControlsVisible, remote::requestIngredients));
        compactHeight = openTabDimension.height();
        expandedHeight = Math.max(compactHeight, fetchIngredientsButton.getY() + fetchIngredientsButton.getHeight() + 6 - y);
        refreshManualControls();
    }

    @Override
    public void tick() {
        refreshManualControls();
        super.tick();
    }

    private Component manualControlsLabel() {
        return Component.translatable(TEXT + (manualControlsVisible ? "hide_manual_controls" : "show_manual_controls"));
    }

    private void toggleManualControls() {
        ManualControlsPreference current = CraftingClientConfig.manualControls();
        CraftingClientConfig.setManualControls(current.toggledFrom(manualControlsVisible));
        refreshManualControls();
    }

    private void refreshManualControls() {
        boolean viewerReady = RecipeViewerIntegrationRegistry.isReadyFor(remote.getPlayer().containerMenu);
        boolean visible = CraftingClientConfig.manualControls().isVisible(viewerReady);
        chooseRecipeButton.setVisible(visible);
        fetchIngredientsButton.setVisible(visible);
        if (manualControlsVisible != visible) {
            manualControlsVisible = visible;
            openTabDimension = new Dimension(openTabDimension.width(), visible ? expandedHeight : compactHeight);
            if (isOpen) {
                setHeight(openTabDimension.height());
            }
        }
    }

    private Component selectionLabel() {
        var id = remote.getSelectedRecipe();
        if (id == null) {
            return Component.translatable(TEXT + "choose");
        }
        var level = remote.getPlayer().level();
        return level.getRecipeManager().byKey(id)
                .map(recipe -> recipe.value().getResultItem(level.registryAccess()).getHoverName())
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private void openSelector() {
        var level = remote.getPlayer().level();
        List<CraftingRecipeSelector.Entry> recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)
                .stream().filter(recipe -> RemoteCraftingRecipes.isSupported(recipe.value()))
                .map(recipe -> new CraftingRecipeSelector.Entry(recipe.id(),
                        recipe.value().getResultItem(level.registryAccess())))
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
        private final BooleanSupplier visible;

        private Action(Position position, Supplier<Component> label, BooleanSupplier enabled, Runnable action) {
            this(position, label, enabled, () -> true, action);
        }

        private Action(Position position, Supplier<Component> label, BooleanSupplier enabled,
                BooleanSupplier visible, Runnable action) {
            super(position, new Dimension(112, 16), button -> {
                if (button == 0 && enabled.getAsBoolean() && visible.getAsBoolean()) {
                    action.run();
                }
            });
            this.label = label;
            this.enabled = enabled;
            this.visible = visible;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
            if (!visible.getAsBoolean()) {
                return;
            }
            graphics.fill(x, y, x + getWidth(), y + getHeight(), 0xFF373737);
            int color = enabled.getAsBoolean() && isMouseOver(mouseX, mouseY) ? 0xFF696969 : 0xFF505050;
            graphics.fill(x + 1, y + 1, x + getWidth() - 1, y + getHeight() - 1, color);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible.getAsBoolean()) {
                return;
            }
            String text = font.plainSubstrByWidth(label.get().getString(), getWidth() - 8);
            graphics.drawCenteredString(font, text, x + getWidth() / 2, y + 4,
                    enabled.getAsBoolean() ? 0xFFFFFF : 0xAAAAAA);
        }

        @Override
        public void renderTooltip(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
            if (visible.getAsBoolean() && isMouseOver(mouseX, mouseY)) {
                graphics.renderTooltip(font, label.get(), mouseX, mouseY);
            }
        }
    }
}
