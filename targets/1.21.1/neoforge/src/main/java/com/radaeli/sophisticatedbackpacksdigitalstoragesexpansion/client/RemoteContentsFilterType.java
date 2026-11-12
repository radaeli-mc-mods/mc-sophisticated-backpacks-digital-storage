package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.Map;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterType;

final class RemoteContentsFilterType {
    static final ButtonDefinition.Toggle<ContentsFilterType> DEFINITION = ButtonDefinitions.createToggleButtonDefinition(Map.of(
            ContentsFilterType.ALLOW,
            GuiHelper.getButtonStateData(new UV(0, 0), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.filter.allow")),
            ContentsFilterType.BLOCK,
            GuiHelper.getButtonStateData(new UV(16, 0), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.filter.block")),
            ContentsFilterType.STORAGE,
            GuiHelper.getButtonStateData(new UV(64, 16), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.filter.storage"))));

    private RemoteContentsFilterType() {
    }
}
