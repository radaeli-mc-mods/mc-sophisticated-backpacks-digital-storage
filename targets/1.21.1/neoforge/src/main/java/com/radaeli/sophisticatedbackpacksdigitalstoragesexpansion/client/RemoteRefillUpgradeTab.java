package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.lang.ref.WeakReference;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.refill.RefillUpgradeWrapper.TargetSlot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.common.FilterSlotGrid;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.RemoteRefillUpgradeContainer;
import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

public final class RemoteRefillUpgradeTab extends LinkedStorageUpgradeTab<RemoteRefillUpgradeContainer> {
    private static final Component SCROLL_TOOLTIP = Component.translatable(
            "gui.sophisticatedbackpacks.upgrades.refill.scroll.tooltip")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
    private static WeakReference<RemoteRefillUpgradeTab> tooltipTab = new WeakReference<>(null);

    private final RefillFilterControl filterControl;

    public RemoteRefillUpgradeTab(RemoteRefillUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
        super(container, position, screen,
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_refill_upgrade"),
                Component.translatable("upgrade.sophisticated_backpacks_digital_storages_expansion.remote_refill_upgrade.tooltip"));
        filterControl = addHideableChild(new RefillFilterControl(screen, new Position(x + 3, y + 24), container));

        int footerY = filterControl.getY() + filterControl.getHeight() + 6;
        addHideableChild(SourcePriorityControl.create(new Position(x + 5, footerY),
                () -> container.getSourcePriority() == SourcePriority.NETWORK_FIRST, container::toggleSourcePriority));
        addConnectionStatus(25, footerY - y + 5);
    }

    @Override
    protected void moveSlotsToTab() {
        filterControl.moveSlotsToView();
    }

    @Override
    protected void onTabOpen() {
        super.onTabOpen();
        tooltipTab = new WeakReference<>(this);
    }

    @Override
    protected void onTabClose() {
        if (tooltipTab.get() == this) {
            tooltipTab.clear();
        }
        super.onTabClose();
    }

    // Append through the same event as native Refill, so the normal item tooltip
    // retains all of its lines and cannot paint over a second custom tooltip.
    static void appendTargetSlotTooltip(ItemTooltipEvent event) {
        RemoteRefillUpgradeTab tab = tooltipTab.get();
        Minecraft minecraft = Minecraft.getInstance();
        if (tab == null || minecraft.screen != tab.screen || !tab.isOpen
                || !tab.shouldRender.getAsBoolean()) {
            return;
        }
        var window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        Slot slot = tab.filterControl.filterAt(mouseX, mouseY);
        if (slot == null || !ItemStack.isSameItemSameComponents(slot.getItem(), event.getItemStack())) {
            return;
        }
        TargetSlot target = tab.getContainer().getTargetSlot(slot.getSlotIndex());
        event.getToolTip().add(Component.translatable(
                "gui.sophisticatedbackpacks.upgrades.refill.target_slot.tooltip",
                target.getDescription()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(SCROLL_TOOLTIP);
    }

    private static final class RefillFilterControl extends FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> {
        private static final int SLOTS_IN_ROW = 3;
        private final RemoteRefillUpgradeContainer refillContainer;

        private RefillFilterControl(StorageScreenBase<?> screen, Position position, RemoteRefillUpgradeContainer container) {
            super(screen, position, container.getFilterLogicContainer(), SLOTS_IN_ROW);
            refillContainer = container;
        }

        private FilterSlotGrid grid() {
            // Slot.x/y belong to the menu. Widgets and mouse events use screen coordinates.
            return new FilterSlotGrid(getX(), getY() + slotsTopYOffset, SLOTS_IN_ROW,
                    container.getFilterSlots().size());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);

            FilterSlotGrid grid = grid();
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(0, 0, 300);
            for (Slot slot : container.getFilterSlots()) {
                if (slot.hasItem()) {
                    int index = slot.getSlotIndex();
                    graphics.drawString(font, refillContainer.getTargetSlot(index).getAcronym(),
                            grid.labelX(index), grid.labelY(index), DyeColor.GREEN.getTextColor());
                }
            }
            pose.popPose();
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
            Slot slot = filterAt(mouseX, mouseY);
            if (slot == null || scrollDelta == 0) {
                return false;
            }
            int index = slot.getSlotIndex();
            TargetSlot target = refillContainer.getTargetSlot(index);
            refillContainer.setTargetSlot(index, scrollDelta > 0 ? target.next() : target.previous());
            return true;
        }

        private Slot filterAt(double mouseX, double mouseY) {
            int index = grid().slotAt(mouseX, mouseY);
            if (index < 0) {
                return null;
            }
            Slot slot = container.getFilterSlots().get(index);
            return slot.hasItem() ? slot : null;
        }
    }
}
