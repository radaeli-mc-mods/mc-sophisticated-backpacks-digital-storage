package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton.StateData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyCondition;
import net.p3pp3rf1y.sophisticatedcore.upgrades.alchemy.AlchemyUpgradeTab;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteAlchemyUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteAlchemyUpgradeWrapper;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

public final class RemoteAlchemyUpgradeTab extends LinkedStorageUpgradeTab<RemoteAlchemyUpgradeContainer> {
    private static final TextureBlitData DISABLED_CONDITION = new TextureBlitData(GuiHelper.ICONS,
            new Position(1, 1), Dimension.SQUARE_256, new UV(240, 64), Dimension.SQUARE_16);

    public RemoteAlchemyUpgradeTab(RemoteAlchemyUpgradeContainer container, Position position,
            StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_alchemy_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_alchemy_upgrade.tooltip"));

        addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), AlchemyUpgradeTab.ANY_EFFECT_MISSING,
                ignored -> container.setMatchAllEffects(!container.shouldMatchAllEffects()),
                container::shouldMatchAllEffects));
        addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), AlchemyUpgradeTab.MATCH_DURATION,
                ignored -> container.setMatchEffectDuration(!container.shouldMatchEffectDuration()),
                container::shouldMatchEffectDuration));
        addHideableChild(new ToggleButton<>(new Position(x + 39, y + 24), AlchemyUpgradeTab.MATCH_AMPLIFIER,
                ignored -> container.setMatchEffectAmplifier(!container.shouldMatchEffectAmplifier()),
                container::shouldMatchEffectAmplifier));

        for (int slot = 0; slot < RemoteAlchemyUpgradeWrapper.FILTER_SLOT_COUNT; slot++) {
            int index = slot;
            int column = slot % 2;
            int row = slot / 2;
            addHideableChild(new ConditionButton(new Position(x + 21 + column * 36, y + 43 + row * 20),
                    container, index));
        }

        int footerY = y + 126;
        addHideableChild(SourcePriorityControl.create(new Position(x + 3, footerY),
                () -> container.getSourcePriority() == SourcePriority.NETWORK_FIRST, container::toggleSourcePriority));
        addConnectionStatus(3, footerY - y + 21);
    }

    @Override
    protected void moveSlotsToTab() {
        int index = 0;
        for (Slot slot : getContainer().getSlots()) {
            slot.x = x - screen.getGuiLeft() + 4 + index % 2 * 36;
            slot.y = y - screen.getGuiTop() + 44 + index / 2 * 20;
            index++;
        }
    }

    private static final class ConditionButton extends ToggleButton<AlchemyCondition> {
        private final RemoteAlchemyUpgradeContainer container;
        private final int slot;

        private ConditionButton(Position position, RemoteAlchemyUpgradeContainer container, int slot) {
            super(position, AlchemyUpgradeTab.ALCHEMY_CONDITION,
                    ignored -> container.toggleCondition(slot), () -> container.getCondition(slot));
            this.container = container;
            this.slot = slot;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (container.hasNoFilter(slot)) {
                GuiHelper.blit(graphics, x, y, DISABLED_CONDITION);
            } else {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        protected List<Component> getTooltip(StateData state) {
            if (container.hasNoFilter(slot)) return Collections.emptyList();
            List<Component> tooltip = new ArrayList<>(super.getTooltip(state));
            if (container.getCondition(slot) == AlchemyCondition.HURT && !tooltip.isEmpty()) {
                int percent = Math.round(container.getValue(slot) * 100.0F);
                tooltip.set(0, Component.translatable("gui.sophisticatedcore.upgrades.buttons.alchemy_condition_hurt",
                        Component.literal(percent + "%").withStyle(ChatFormatting.DARK_GREEN)));
                tooltip.add(Component.translatable(
                        "gui.sophisticatedcore.upgrades.buttons.alchemy_condition.value_controls")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return tooltip;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (isMouseOver(mouseX, mouseY) && !container.hasNoFilter(slot)
                    && container.getCondition(slot) == AlchemyCondition.HURT && scrollY != 0) {
                container.setValue(slot, container.getValue(slot) + Math.copySign(0.05F, (float) scrollY));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }
}
