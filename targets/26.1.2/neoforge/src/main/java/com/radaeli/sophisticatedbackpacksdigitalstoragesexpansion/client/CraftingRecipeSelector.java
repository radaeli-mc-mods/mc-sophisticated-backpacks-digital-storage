package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.TextBox;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import org.lwjgl.glfw.GLFW;

/** Searchable recipe list hosted by the Sophisticated screen's modal layer. */
final class CraftingRecipeSelector extends WidgetBase {
    private static final int WIDTH = 216;
    private static final int ROWS = 6;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 38;
    private static final int HEIGHT = LIST_TOP + ROWS * ROW_HEIGHT + 18;
    private static final String TEXT = RemoteCraftingUpgradeTab.TEXT;

    private List<Entry> entries;
    private final Consumer<Entry> select;
    private final TextBox search;
    private List<Entry> visibleEntries;
    private int offset;

    CraftingRecipeSelector(List<Entry> entries, Consumer<Entry> select) {
        super(new Position(0, 0), new Dimension(WIDTH, HEIGHT));
        this.entries = entries;
        this.select = select;
        visibleEntries = entries;
        search = new TextBox(new Position(0, 0), new Dimension(WIDTH - 16, 14));
        search.setBordered(true);
        search.setMaxLength(160);
        search.setResponder(this::filter);
        search.setFocused(true);
    }

    void updateEntries(List<Entry> updated) {
        entries = List.copyOf(updated);
        filter(search.getValue());
    }

    private void filter(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        visibleEntries = entries.stream().filter(entry -> entry.id().toString().contains(needle)
                || entry.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)).toList();
        offset = 0;
    }

    @Override
    public void setPosition(Position position) {
        super.setPosition(position);
        search.setPosition(new Position(x + 8, y + 18));
    }

    @Override
    protected void extractBg(GuiGraphicsExtractor graphics, Minecraft minecraft, int mouseX, int mouseY) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF171717);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFFC6C6C6);
    }

    @Override
    protected void extractWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, Component.translatable(TEXT + "search"), x + 8, y + 6, 0xFF404040, false);
        search.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (visibleEntries.isEmpty()) {
            graphics.text(font, Component.translatable(TEXT + "empty"), x + 8, y + LIST_TOP, 0xFF404040, false);
            return;
        }
        for (int row = 0; row < ROWS && offset + row < visibleEntries.size(); row++) {
            Entry entry = visibleEntries.get(offset + row);
            int top = y + LIST_TOP + row * ROW_HEIGHT;
            graphics.fill(x + 8, top, x + WIDTH - 8, top + 17,
                    rowAt(mouseX, mouseY) == row ? 0xFFEFEFEF : 0xFFA8A8A8);
            graphics.item(entry.result(), x + 9, top);
            String name = font.plainSubstrByWidth(entry.result().getHoverName().getString(), WIDTH - 41);
            graphics.text(font, name, x + 30, top + 5, 0xFF202020, false);
        }
        String range = (offset + 1) + "–" + Math.min(offset + ROWS, visibleEntries.size()) + " / " + visibleEntries.size();
        graphics.text(font, range, x + 8, y + HEIGHT - 12, 0xFF404040, false);
    }

    private int rowAt(double mouseX, double mouseY) {
        double top = y + LIST_TOP;
        if (mouseX < x + 8 || mouseX >= x + WIDTH - 8 || mouseY < top || mouseY >= top + ROWS * ROW_HEIGHT) {
            return -1;
        }
        return (int) ((mouseY - top) / ROW_HEIGHT);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        int row = rowAt(mouseX, mouseY);
        if (button == 0 && row >= 0 && offset + row < visibleEntries.size()) {
            select.accept(visibleEntries.get(offset + row));
        } else {
            search.mouseClicked(event, doubleClick);
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
            scroll(key == GLFW.GLFW_KEY_PAGE_UP ? -ROWS : ROWS);
            return true;
        }
        return search.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return search.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll(vertical > 0 ? -1 : vertical < 0 ? 1 : 0);
        return true;
    }

    private void scroll(int rows) {
        offset = Math.max(0, Math.min(offset + rows, visibleEntries.size() - ROWS));
    }

    @Override
    public void extractTooltip(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int row = rowAt(mouseX, mouseY);
        if (row >= 0 && offset + row < visibleEntries.size()) {
            Entry entry = visibleEntries.get(offset + row);
            graphics.setComponentTooltipForNextFrame(font, List.of(entry.result().getHoverName(),
                    Component.literal(entry.id().toString()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    record Entry(Identifier id, ItemStack result) {
    }
}
