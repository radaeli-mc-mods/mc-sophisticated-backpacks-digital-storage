package com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton.StateData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;

import com.radaeli.sophisticatedbackpacksdigitalstoragesexpansion.SophisticatedBackpacksDigitalStoragesExpansion;

/** Shared small checkmark sized for Sophisticated's 12x12 toggle buttons. */
final class RemoteCheckmark {
    private static final TextureBlitData TEXTURE = new TextureBlitData(
            ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacksDigitalStoragesExpansion.MOD_ID,
                    "textures/gui/minimal_checkmark.png"),
            new Position(2, 2), new Dimension(8, 8), new UV(0, 0), new Dimension(8, 8));

    private RemoteCheckmark() {
    }

    static StateData state(Component tooltip) {
        return new StateData(TEXTURE, tooltip);
    }
}
