package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.upgrade.SourcePriority;

final class RemoteSourcePriorityButton {
    private static final ButtonDefinition.Toggle<SourcePriority> DEFINITION = ButtonDefinitions.createToggleButtonDefinition(Map.of(
            SourcePriority.NETWORK_FIRST,
            GuiHelper.getButtonStateData(new UV(128, 48), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.network_first")),
            SourcePriority.BACKPACK_FIRST,
            GuiHelper.getButtonStateData(new UV(144, 48), Dimension.SQUARE_16, new Position(1, 1),
                    Component.translatable("gui.sophisticated_backpacks_digital_storages_expansion.backpack_first"))));

    private RemoteSourcePriorityButton() {
    }

    static ToggleButton<SourcePriority> create(Position position, Supplier<SourcePriority> getPriority,
            Runnable togglePriority) {
        return new ToggleButton<>(position, DEFINITION, ignored -> togglePriority.run(), getPriority);
    }
}
