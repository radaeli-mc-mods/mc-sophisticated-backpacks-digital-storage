package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton.StateData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.FluidFilterControl;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemotePumpUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

/** Pump controls built around Sophisticated's native fluid filter widget. */
public final class RemotePumpUpgradeTab extends LinkedStorageUpgradeTab<RemotePumpUpgradeContainer> {
    // These sprites and state meanings mirror the controls in SophisticatedCore's PumpUpgradeTab.
    private static final ButtonDefinition.Toggle<Boolean> INPUT_BUTTON = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(state(144, 0, "gui.sophisticatedcore.upgrades.buttons.pump_input"),
                    state(160, 0, "gui.sophisticatedcore.upgrades.buttons.pump_output")));
    private static final ButtonDefinition.Toggle<Boolean> HAND_BUTTON = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    state(208, 0, "gui.sophisticatedcore.upgrades.buttons.interact_with_hand"),
                    state(224, 0, "gui.sophisticatedcore.upgrades.buttons.do_not_interact_with_hand")));
    private static final ButtonDefinition.Toggle<Boolean> WORLD_BUTTON = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    state(176, 0, "gui.sophisticatedcore.upgrades.buttons.interact_with_world"),
                    state(192, 0, "gui.sophisticatedcore.upgrades.buttons.do_not_interact_with_world")));
    private static final ButtonDefinition.Toggle<Boolean> HANDLER_BUTTON = ButtonDefinitions.createToggleButtonDefinition(
            ButtonDefinitions.getBooleanStateData(
                    state(0, 112, "gui.sophisticatedcore.upgrades.buttons.interact_with_tanks_and_pipes"),
                    state(16, 112, "gui.sophisticatedcore.upgrades.buttons.do_not_interact_with_tanks_and_pipes")));

    public RemotePumpUpgradeTab(RemotePumpUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_pump_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_pump_upgrade.tooltip"));

        var fluidFilter = addHideableChild(new FluidFilterControlBridge(new Position(x + 3, y + 24), container));
        int controlsY = fluidFilter.getY() + fluidFilter.getHeight() + 6;
        addHideableChild(new ToggleButton<>(new Position(x + 5, controlsY), INPUT_BUTTON,
                ignored -> container.toggleInput(), container::isInput));
        addHideableChild(new ToggleButton<>(new Position(x + 23, controlsY), HAND_BUTTON,
                ignored -> container.toggleHand(), container::shouldInteractWithHand));
        addHideableChild(new ToggleButton<>(new Position(x + 41, controlsY), WORLD_BUTTON,
                ignored -> container.toggleWorld(), container::shouldInteractWithWorld));
        addHideableChild(new ToggleButton<>(new Position(x + 59, controlsY), HANDLER_BUTTON,
                ignored -> container.toggleHandlers(), container::shouldInteractWithFluidHandlers));
        var priorityControl = addHideableChild(SourcePriorityControl.create(new Position(x + 5, controlsY + 21),
                () -> container.getSourcePriority() == SourcePriority.NETWORK_FIRST, container::toggleSourcePriority));
        int connectionY = priorityControl.getY() + priorityControl.getHeight() + 8;
        addConnectionStatus(5, connectionY - y);
        addHideableChild(new PumpStatusLabel(new Position(x + 5, connectionY + 14), container));
    }

    private static StateData state(int u, int v, String tooltipKey) {
        return GuiHelper.getButtonStateData(new UV(u, v), Dimension.SQUARE_16, new Position(1, 1),
                Component.translatable(tooltipKey));
    }

    @Override
    protected void moveSlotsToTab() {
        // Native FluidFilterControl renders and handles its own fluid-filter slots.
    }

    private static final class FluidFilterControlBridge extends FluidFilterControl {
        private FluidFilterControlBridge(Position position, RemotePumpUpgradeContainer container) {
            super(position, container.getFluidFilterContainer());
        }
    }

    private static final class PumpStatusLabel extends WidgetBase {
        private final RemotePumpUpgradeContainer container;

        private PumpStatusLabel(Position position, RemotePumpUpgradeContainer container) {
            super(position, new Dimension(94, 10));
            this.container = container;
        }

        @Override
        protected void renderBg(GuiGraphics graphics, Minecraft minecraft, int mouseX, int mouseY) {
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Component status = container.getPumpStatus();
            if (!status.getString().isEmpty()) {
                graphics.drawString(minecraft.font, status, x, y, 0xFFFFFFFF, false);
            }
        }
    }
}
