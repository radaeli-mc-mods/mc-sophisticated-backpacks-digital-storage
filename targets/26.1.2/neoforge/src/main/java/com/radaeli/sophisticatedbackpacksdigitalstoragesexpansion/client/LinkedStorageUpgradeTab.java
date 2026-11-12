package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Label;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageText;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.storage.StorageEndpoint;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.LinkedStorageUpgradeContainer;

/** Shared connection controls; each upgrade chooses its own filter and action layout. */
public abstract class LinkedStorageUpgradeTab<C extends LinkedStorageUpgradeContainer<?, ?>> extends UpgradeSettingsTab<C> {
    protected LinkedStorageUpgradeTab(C container, Position position, StorageScreenBase<?> screen,
            Component title, Component tooltip) {
        super(container, position, screen, title, tooltip);
    }

    protected final void addConnectionStatus(int offsetX, int offsetY) {
        addHideableChild(connectionLabel(new Position(x + offsetX, y + offsetY), getContainer()::getEndpoint));
    }

    static WidgetBase connectionLabel(Position position, Supplier<Optional<StorageEndpoint>> endpoint) {
        return new ConnectionLabel(position, () -> endpoint.get()
                .map(StorageText::summary)
                .orElseGet(() -> Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.not_linked")),
                () -> endpoint.get()
                        .map(value -> StorageText.endpoint("gui.sophisticated_backpacks_digital_storages_expansion.endpoint", value))
                        .orElseGet(() -> Component.translatable("item.sophisticated_backpacks_digital_storages_expansion.linked_upgrade.hint")));
    }

    private static final class ConnectionLabel extends WidgetBase {
        private final Supplier<Component> textSupplier;
        private final Supplier<Component> tooltipSupplier;
        private Label label;
        private Component previous;

        private ConnectionLabel(Position position, Supplier<Component> textSupplier, Supplier<Component> tooltipSupplier) {
            super(position, new Dimension(86, 10));
            this.textSupplier = textSupplier;
            this.tooltipSupplier = tooltipSupplier;
        }

        @Override
        protected void extractBg(GuiGraphicsExtractor graphics, Minecraft minecraft, int mouseX, int mouseY) {
        }

        @Override
        protected void extractWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Component current = textSupplier.get();
            if (!current.equals(previous)) {
                // Replace the native label to avoid MutableComponent's cached visual-order text.
                label = new Label(new Position(x, y), current, () -> 86);
                previous = current;
            }
            label.setPosition(new Position(x, y));
            label.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void extractTooltip(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
            if (isMouseOver(mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, List.of(tooltipSupplier.get()), mouseX, mouseY);
            }
        }
    }
}
